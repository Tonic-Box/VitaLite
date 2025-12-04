package com.tonic.services.profiler.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tonic.services.profiler.MetricSnapshot;
import com.tonic.services.profiler.ResourceMetricsCollector;
import com.tonic.services.profiler.recording.JFRMethodRecorder;
import com.tonic.services.profiler.recording.MethodProfiler;
import com.tonic.services.profiler.recording.MethodStats;
import com.tonic.services.profiler.recording.MethodTimingResults;
import com.tonic.services.profiler.recording.CallTreeNode;
import com.tonic.services.profiler.sampling.CPUSampler;
import com.tonic.services.profiler.sampling.MemorySampler;
import com.tonic.services.profiler.sampling.StackSample;
import com.tonic.services.profiler.sampling.HeapHistogramSample;
import com.tonic.services.profiler.sampling.SampleAnalyzer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * HTTP REST API server for profiler data access.
 * Enables LLMs and external tools to query profiler metrics.
 *
 * Endpoints:
 *   GET  /profiler/status              - Server and profiler status
 *   GET  /profiler/exact               - MethodProfiler exact timing data
 *   GET  /profiler/exact/csv           - MethodProfiler data as CSV
 *   GET  /profiler/jfr                 - Last JFR recording results
 *   GET  /profiler/hotspots?top=N      - Top N methods by self time
 *   GET  /profiler/method?name=X       - Method details with callers/callees
 *   POST /profiler/exact/enable        - Enable MethodProfiler
 *   POST /profiler/exact/disable       - Disable MethodProfiler
 *   POST /profiler/exact/clear         - Clear MethodProfiler data
 *   POST /profiler/jfr/start?period=N  - Start JFR recording
 *   POST /profiler/jfr/stop            - Stop JFR recording
 */
public class ProfilerServer {
    private static ProfilerServer instance;

    private HttpServer server;
    private int port = 8787;
    private volatile boolean running = false;

    // JFR state
    private JFRMethodRecorder jfrRecorder;
    private MethodTimingResults lastJFRResults;

    // Sampling state
    private CPUSampler cpuSampler;
    private MemorySampler memorySampler;
    private final SampleAnalyzer sampleAnalyzer = new SampleAnalyzer();

    // Resource metrics
    private ResourceMetricsCollector metricsCollector;

    private ProfilerServer() {
        jfrRecorder = new JFRMethodRecorder();
        cpuSampler = new CPUSampler(10000);
        memorySampler = new MemorySampler(1000);
        metricsCollector = new ResourceMetricsCollector();
    }

    public static ProfilerServer getInstance() {
        if (instance == null) {
            instance = new ProfilerServer();
        }
        return instance;
    }

    public void start() throws IOException {
        start(port);
    }

    public void start(int port) throws IOException {
        if (running) {
            return;
        }
        this.port = port;

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.setExecutor(Executors.newFixedThreadPool(2));

        // Root/help endpoint
        server.createContext("/profiler", this::handleRoot);
        server.createContext("/", this::handleRoot);

        // Status endpoint
        server.createContext("/profiler/status", this::handleStatus);

        // Exact timing endpoints
        server.createContext("/profiler/exact/enable", this::handleExactEnable);
        server.createContext("/profiler/exact/disable", this::handleExactDisable);
        server.createContext("/profiler/exact/clear", this::handleExactClear);
        server.createContext("/profiler/exact/csv", this::handleExactCSV);
        server.createContext("/profiler/exact", this::handleExact);

        // JFR endpoints
        server.createContext("/profiler/jfr/start", this::handleJFRStart);
        server.createContext("/profiler/jfr/stop", this::handleJFRStop);
        server.createContext("/profiler/jfr", this::handleJFR);

        // Analysis endpoints
        server.createContext("/profiler/hotspots", this::handleHotspots);
        server.createContext("/profiler/method", this::handleMethod);

        // CPU sampling endpoints
        server.createContext("/profiler/cpu/start", this::handleCPUStart);
        server.createContext("/profiler/cpu/stop", this::handleCPUStop);
        server.createContext("/profiler/cpu/clear", this::handleCPUClear);
        server.createContext("/profiler/cpu", this::handleCPU);

        // Memory sampling endpoints
        server.createContext("/profiler/memory/start", this::handleMemoryStart);
        server.createContext("/profiler/memory/stop", this::handleMemoryStop);
        server.createContext("/profiler/memory/histogram", this::handleHeapHistogram);
        server.createContext("/profiler/memory", this::handleMemory);

        // Resource monitor endpoint
        server.createContext("/profiler/resources", this::handleResources);

        // Thread endpoints
        server.createContext("/profiler/threads", this::handleThreads);

        // GC control
        server.createContext("/profiler/gc", this::handleGC);

        server.start();
        running = true;
        System.out.println("[ProfilerServer] Started on http://127.0.0.1:" + port);
    }

    public void stop() {
        if (!running || server == null) {
            return;
        }
        server.stop(0);
        running = false;
        System.out.println("[ProfilerServer] Stopped");
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    // ==================== HANDLERS ====================

    private void handleRoot(HttpExchange exchange) throws IOException {
        // Serve help/documentation for any request to root or /profiler without sub-path
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.equals("/profiler") || path.equals("/profiler/")) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"name\": \"VitaLite Profiler API\",\n");
            json.append("  \"version\": \"1.0\",\n");
            json.append("  \"description\": \"REST API for accessing JVM profiler data. Designed for LLM integration.\",\n");
            json.append("  \"endpoints\": {\n");

            // Status
            json.append("    \"status\": {\n");
            json.append("      \"GET /profiler/status\": \"Server and profiler status overview\"\n");
            json.append("    },\n");

            // Exact timing
            json.append("    \"exact_timing\": {\n");
            json.append("      \"description\": \"Manual method timing via MethodProfiler.begin()/end() instrumentation\",\n");
            json.append("      \"GET /profiler/exact\": \"All exact timing data as JSON\",\n");
            json.append("      \"GET /profiler/exact/csv\": \"All exact timing data as CSV\",\n");
            json.append("      \"POST /profiler/exact/enable\": \"Enable MethodProfiler (starts collecting data)\",\n");
            json.append("      \"POST /profiler/exact/disable\": \"Disable MethodProfiler (zero overhead when disabled)\",\n");
            json.append("      \"POST /profiler/exact/clear\": \"Clear all collected timing data\"\n");
            json.append("    },\n");

            // JFR
            json.append("    \"jfr_sampling\": {\n");
            json.append("      \"description\": \"JVM Flight Recorder sampling profiler for statistical CPU profiling\",\n");
            json.append("      \"GET /profiler/jfr\": \"Results from last JFR recording session\",\n");
            json.append("      \"POST /profiler/jfr/start\": \"Start JFR recording. Optional: ?period=N (sample period in ms, default 10)\",\n");
            json.append("      \"POST /profiler/jfr/stop\": \"Stop JFR recording and analyze results\"\n");
            json.append("    },\n");

            // Analysis
            json.append("    \"analysis\": {\n");
            json.append("      \"description\": \"Higher-level analysis endpoints for JFR data\",\n");
            json.append("      \"GET /profiler/hotspots\": \"Top methods by CPU time. Optional: ?top=N (default 10)\",\n");
            json.append("      \"GET /profiler/method\": \"Method details with callers/callees. Required: ?name=ClassName.methodName\"\n");
            json.append("    },\n");

            // CPU sampling
            json.append("    \"cpu_sampling\": {\n");
            json.append("      \"description\": \"Stack trace sampling for CPU profiling\",\n");
            json.append("      \"GET /profiler/cpu\": \"CPU sampling results with top methods and packages\",\n");
            json.append("      \"POST /profiler/cpu/start\": \"Start CPU sampling. Optional: ?interval=N (ms, default 50)\",\n");
            json.append("      \"POST /profiler/cpu/stop\": \"Stop CPU sampling\",\n");
            json.append("      \"POST /profiler/cpu/clear\": \"Clear CPU sampling data\"\n");
            json.append("    },\n");

            // Memory sampling
            json.append("    \"memory_sampling\": {\n");
            json.append("      \"description\": \"Heap and GC tracking\",\n");
            json.append("      \"GET /profiler/memory\": \"Memory sampling results with allocation rates and GC stats\",\n");
            json.append("      \"GET /profiler/memory/histogram\": \"Heap histogram (top classes by size). Optional: ?top=N (default 50)\",\n");
            json.append("      \"POST /profiler/memory/start\": \"Start memory sampling. Optional: ?interval=N (ms, default 1000)\",\n");
            json.append("      \"POST /profiler/memory/stop\": \"Stop memory sampling\"\n");
            json.append("    },\n");

            // Resources
            json.append("    \"resources\": {\n");
            json.append("      \"description\": \"Live JVM resource metrics\",\n");
            json.append("      \"GET /profiler/resources\": \"Current CPU, heap, metaspace, code cache, and thread stats\"\n");
            json.append("    },\n");

            // Threads
            json.append("    \"threads\": {\n");
            json.append("      \"description\": \"Thread information and stack traces\",\n");
            json.append("      \"GET /profiler/threads\": \"All threads with state info. Optional: ?stacks=true&depth=N\"\n");
            json.append("    },\n");

            // GC control
            json.append("    \"gc\": {\n");
            json.append("      \"description\": \"Garbage collection control\",\n");
            json.append("      \"POST /profiler/gc\": \"Trigger full GC and report memory freed\"\n");
            json.append("    }\n");
            json.append("  },\n");

            // Quick start
            json.append("  \"quick_start\": {\n");
            json.append("    \"1_check_status\": \"GET /profiler/status\",\n");
            json.append("    \"2a_exact_timing\": [\n");
            json.append("      \"POST /profiler/exact/enable\",\n");
            json.append("      \"(run your code with MethodProfiler instrumentation)\",\n");
            json.append("      \"GET /profiler/exact\"\n");
            json.append("    ],\n");
            json.append("    \"2b_jfr_sampling\": [\n");
            json.append("      \"POST /profiler/jfr/start?period=10\",\n");
            json.append("      \"(run your code for a few seconds)\",\n");
            json.append("      \"POST /profiler/jfr/stop\",\n");
            json.append("      \"GET /profiler/hotspots?top=10\"\n");
            json.append("    ]\n");
            json.append("  },\n");

            // Tips for LLMs
            json.append("  \"llm_tips\": {\n");
            json.append("    \"workflow\": \"Use /profiler/status first to check what data is available\",\n");
            json.append("    \"exact_vs_jfr\": \"Exact timing is precise but requires code instrumentation. JFR sampling works on any code with statistical accuracy.\",\n");
            json.append("    \"hotspots\": \"Start with /profiler/hotspots to find slow methods, then use /profiler/method?name=X for caller/callee analysis\",\n");
            json.append("    \"confidence\": \"JFR results include confidence levels: HIGH (100+ samples), MEDIUM (30+), LOW (10+), VERY_LOW (<10)\"\n");
            json.append("  }\n");
            json.append("}\n");

            sendJson(exchange, 200, json.toString());
            return;
        }
        // For other paths under /profiler that don't match, return 404
        sendJson(exchange, 404, "{\"error\":\"Endpoint not found\",\"hint\":\"GET /profiler for API documentation\"}");
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"server\":{");
        json.append("\"running\":true,");
        json.append("\"port\":").append(port);
        json.append("},");
        json.append("\"exact\":{");
        json.append("\"enabled\":").append(MethodProfiler.isEnabled()).append(",");
        json.append("\"methodCount\":").append(MethodProfiler.getMethodCount());
        json.append("},");
        json.append("\"jfr\":{");
        json.append("\"available\":").append(JFRMethodRecorder.isAvailable()).append(",");
        json.append("\"recording\":").append(jfrRecorder != null && isJFRRecording()).append(",");
        json.append("\"hasResults\":").append(lastJFRResults != null);
        json.append("},");
        json.append("\"cpu\":{");
        json.append("\"running\":").append(cpuSampler.isRunning()).append(",");
        json.append("\"totalSamples\":").append(cpuSampler.getTotalSamples());
        json.append("},");
        json.append("\"memory\":{");
        json.append("\"running\":").append(memorySampler.isRunning()).append(",");
        json.append("\"totalSamples\":").append(memorySampler.getTotalSamples());
        json.append("}}");

        sendJson(exchange, 200, json.toString());
    }

    private void handleExact(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        List<MethodProfiler.MethodTiming> timings = MethodProfiler.getAllTimings();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"timestamp\":").append(System.currentTimeMillis()).append(",");
        json.append("\"enabled\":").append(MethodProfiler.isEnabled()).append(",");
        json.append("\"methods\":[");

        boolean first = true;
        for (MethodProfiler.MethodTiming t : timings) {
            if (!first) json.append(",");
            first = false;
            json.append("{");
            json.append("\"label\":\"").append(escapeJson(t.getLabel())).append("\",");
            json.append("\"calls\":").append(t.getCallCount()).append(",");
            json.append("\"totalMs\":").append(String.format("%.3f", t.getTotalMs())).append(",");
            json.append("\"avgMs\":").append(String.format("%.6f", t.getAverageMs())).append(",");
            json.append("\"minMs\":").append(String.format("%.6f", t.getMinMs())).append(",");
            json.append("\"maxMs\":").append(String.format("%.6f", t.getMaxMs()));
            json.append("}");
        }

        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    private void handleExactCSV(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        String csv = MethodProfiler.generateCSVReport();
        sendText(exchange, 200, csv, "text/csv");
    }

    private void handleExactEnable(HttpExchange exchange) throws IOException {
        if (!checkPost(exchange)) return;
        MethodProfiler.setEnabled(true);
        sendJson(exchange, 200, "{\"success\":true,\"enabled\":true}");
    }

    private void handleExactDisable(HttpExchange exchange) throws IOException {
        if (!checkPost(exchange)) return;
        MethodProfiler.setEnabled(false);
        sendJson(exchange, 200, "{\"success\":true,\"enabled\":false}");
    }

    private void handleExactClear(HttpExchange exchange) throws IOException {
        if (!checkPost(exchange)) return;
        MethodProfiler.clear();
        sendJson(exchange, 200, "{\"success\":true,\"cleared\":true}");
    }

    private void handleJFR(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        if (lastJFRResults == null) {
            sendJson(exchange, 200, "{\"hasResults\":false,\"message\":\"No JFR recording results available. Start and stop a recording first.\"}");
            return;
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"hasResults\":true,");
        json.append("\"recordingDurationMs\":").append(lastJFRResults.recordingEndTime - lastJFRResults.recordingStartTime).append(",");
        json.append("\"samplePeriodMs\":").append(lastJFRResults.samplePeriodMs).append(",");
        json.append("\"totalSamples\":").append(lastJFRResults.totalSamples).append(",");
        json.append("\"methodCount\":").append(lastJFRResults.getMethodCount()).append(",");
        json.append("\"methods\":[");

        List<MethodStats> methods = lastJFRResults.getAllMethodsBySelfTime();
        boolean first = true;
        for (MethodStats m : methods) {
            if (!first) json.append(",");
            first = false;
            appendMethodStats(json, m, lastJFRResults.totalSamples, lastJFRResults.samplePeriodMs);
        }

        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    private void handleJFRStart(HttpExchange exchange) throws IOException {
        if (!checkPost(exchange)) return;

        if (!JFRMethodRecorder.isAvailable()) {
            sendJson(exchange, 500, "{\"success\":false,\"error\":\"JFR not available on this JVM\"}");
            return;
        }

        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        int period = 10;
        try {
            if (params.containsKey("period")) {
                period = Integer.parseInt(params.get("period"));
            }
        } catch (NumberFormatException e) {
            // Use default
        }

        try {
            jfrRecorder = new JFRMethodRecorder();
            jfrRecorder.setSamplePeriodMs(period);
            jfrRecorder.startRecording();
            sendJson(exchange, 200, "{\"success\":true,\"recording\":true,\"periodMs\":" + period + "}");
        } catch (Exception e) {
            sendJson(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleJFRStop(HttpExchange exchange) throws IOException {
        if (!checkPost(exchange)) return;

        if (jfrRecorder == null) {
            sendJson(exchange, 400, "{\"success\":false,\"error\":\"No recording in progress\"}");
            return;
        }

        try {
            jfrRecorder.stopRecording();
            lastJFRResults = jfrRecorder.analyze();

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,\"recording\":false,");
            json.append("\"totalSamples\":").append(lastJFRResults.totalSamples).append(",");
            json.append("\"methodCount\":").append(lastJFRResults.getMethodCount()).append(",");
            json.append("\"durationMs\":").append(lastJFRResults.recordingEndTime - lastJFRResults.recordingStartTime);
            json.append("}");

            sendJson(exchange, 200, json.toString());
        } catch (Exception e) {
            sendJson(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleHotspots(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        if (lastJFRResults == null) {
            sendJson(exchange, 200, "{\"hasResults\":false,\"hotspots\":[]}");
            return;
        }

        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        int top = 10;
        try {
            if (params.containsKey("top")) {
                top = Integer.parseInt(params.get("top"));
            }
        } catch (NumberFormatException e) {
            // Use default
        }

        List<MethodStats> hotspots = lastJFRResults.getTopMethodsBySelfTime(top);

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"hasResults\":true,");
        json.append("\"recordingDurationMs\":").append(lastJFRResults.recordingEndTime - lastJFRResults.recordingStartTime).append(",");
        json.append("\"samplePeriodMs\":").append(lastJFRResults.samplePeriodMs).append(",");
        json.append("\"totalSamples\":").append(lastJFRResults.totalSamples).append(",");
        json.append("\"hotspots\":[");

        boolean first = true;
        for (MethodStats m : hotspots) {
            if (!first) json.append(",");
            first = false;
            appendMethodStats(json, m, lastJFRResults.totalSamples, lastJFRResults.samplePeriodMs);
        }

        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    private void handleMethod(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String methodName = params.get("name");

        if (methodName == null || methodName.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Missing 'name' parameter\"}");
            return;
        }

        if (lastJFRResults == null) {
            sendJson(exchange, 200, "{\"found\":false,\"message\":\"No JFR results available\"}");
            return;
        }

        CallTreeNode node = lastJFRResults.getMethodNode(methodName);
        if (node == null) {
            sendJson(exchange, 200, "{\"found\":false,\"method\":\"" + escapeJson(methodName) + "\"}");
            return;
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"found\":true,");
        json.append("\"method\":\"").append(escapeJson(node.getMethodKey())).append("\",");
        json.append("\"className\":\"").append(escapeJson(node.getClassName())).append("\",");
        json.append("\"methodName\":\"").append(escapeJson(node.getMethodName())).append("\",");
        json.append("\"selfSamples\":").append(node.getSelfSamples()).append(",");
        json.append("\"totalSamples\":").append(node.getTotalSamples()).append(",");
        json.append("\"selfPercent\":").append(String.format("%.2f", node.getSelfPercent(lastJFRResults.totalSamples))).append(",");
        json.append("\"totalPercent\":").append(String.format("%.2f", node.getTotalPercent(lastJFRResults.totalSamples))).append(",");

        // Callers
        json.append("\"callers\":[");
        List<Map.Entry<String, Integer>> callers = node.getCallersSorted();
        boolean first = true;
        for (Map.Entry<String, Integer> caller : callers) {
            if (!first) json.append(",");
            first = false;
            json.append("{\"method\":\"").append(escapeJson(caller.getKey())).append("\",");
            json.append("\"count\":").append(caller.getValue()).append("}");
        }
        json.append("],");

        // Callees
        json.append("\"callees\":[");
        List<Map.Entry<String, Integer>> callees = node.getCalleesSorted();
        first = true;
        for (Map.Entry<String, Integer> callee : callees) {
            if (!first) json.append(",");
            first = false;
            json.append("{\"method\":\"").append(escapeJson(callee.getKey())).append("\",");
            json.append("\"count\":").append(callee.getValue()).append("}");
        }
        json.append("]");

        json.append("}");
        sendJson(exchange, 200, json.toString());
    }

    // ==================== CPU SAMPLING HANDLERS ====================

    private void handleCPU(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        CPUSampler.SamplingStats stats = cpuSampler.getStats();
        SampleAnalyzer.CPUAnalysisResults analysis = sampleAnalyzer.analyzeCPU(cpuSampler.getStackSamples());

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"running\":").append(stats.running).append(",");
        json.append("\"totalSamples\":").append(stats.totalSamples).append(",");
        json.append("\"currentSamples\":").append(stats.currentSamples).append(",");
        json.append("\"maxSamples\":").append(stats.maxSamples).append(",");
        json.append("\"droppedSamples\":").append(stats.droppedSamples).append(",");
        json.append("\"intervalMs\":").append(stats.intervalMs).append(",");

        // Top methods
        json.append("\"topMethods\":[");
        List<SampleAnalyzer.MethodStats> topMethods = analysis.getTopMethods(20);
        boolean first = true;
        for (SampleAnalyzer.MethodStats m : topMethods) {
            if (!first) json.append(",");
            first = false;
            json.append("{");
            json.append("\"method\":\"").append(escapeJson(m.method.getFullSignature())).append("\",");
            json.append("\"selfPercent\":").append(String.format("%.2f", m.selfTimePercent)).append(",");
            json.append("\"totalPercent\":").append(String.format("%.2f", m.totalTimePercent)).append(",");
            json.append("\"selfSamples\":").append(m.selfSamples).append(",");
            json.append("\"totalSamples\":").append(m.totalSamples);
            json.append("}");
        }
        json.append("],");

        // Top packages
        json.append("\"topPackages\":[");
        List<SampleAnalyzer.PackageStats> topPkgs = analysis.getTopPackages(10);
        first = true;
        for (SampleAnalyzer.PackageStats p : topPkgs) {
            if (!first) json.append(",");
            first = false;
            json.append("{");
            json.append("\"package\":\"").append(escapeJson(p.packageName)).append("\",");
            json.append("\"totalSamples\":").append(p.totalSamples).append(",");
            json.append("\"selfSamples\":").append(p.selfSamples).append(",");
            json.append("\"methodCount\":").append(p.methodCount);
            json.append("}");
        }
        json.append("]");

        json.append("}");
        sendJson(exchange, 200, json.toString());
    }

    private void handleCPUStart(HttpExchange exchange) throws IOException {
        if (!checkPost(exchange)) return;

        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        int interval = 50;
        try {
            if (params.containsKey("interval")) {
                interval = Integer.parseInt(params.get("interval"));
            }
        } catch (NumberFormatException e) {
            // Use default
        }

        cpuSampler.setSamplingInterval(interval);
        cpuSampler.start();
        sendJson(exchange, 200, "{\"success\":true,\"running\":true,\"intervalMs\":" + interval + "}");
    }

    private void handleCPUStop(HttpExchange exchange) throws IOException {
        if (!checkPost(exchange)) return;
        cpuSampler.stop();
        sendJson(exchange, 200, "{\"success\":true,\"running\":false,\"totalSamples\":" + cpuSampler.getTotalSamples() + "}");
    }

    private void handleCPUClear(HttpExchange exchange) throws IOException {
        if (!checkPost(exchange)) return;
        cpuSampler.clear();
        sendJson(exchange, 200, "{\"success\":true,\"cleared\":true}");
    }

    // ==================== MEMORY SAMPLING HANDLERS ====================

    private void handleMemory(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        MemorySampler.SamplingStats stats = memorySampler.getStats();
        SampleAnalyzer.MemoryAnalysisResults analysis = sampleAnalyzer.analyzeMemory(
            memorySampler.getHeapSamples(),
            memorySampler.getGCSamples()
        );

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"running\":").append(stats.running).append(",");
        json.append("\"totalSamples\":").append(stats.totalSamples).append(",");
        json.append("\"currentHeapSamples\":").append(stats.currentHeapSamples).append(",");
        json.append("\"gcEventCount\":").append(stats.gcEventCount).append(",");
        json.append("\"intervalMs\":").append(stats.intervalMs).append(",");
        json.append("\"analysis\":{");
        json.append("\"averageAllocationRateMBps\":").append(String.format("%.2f", analysis.averageAllocationRate)).append(",");
        json.append("\"peakAllocationRateMBps\":").append(String.format("%.2f", analysis.peakAllocationRate)).append(",");
        json.append("\"totalEstimatedAllocationMB\":").append(String.format("%.2f", analysis.totalEstimatedAllocation)).append(",");
        json.append("\"heapGrowthTrend\":\"").append(analysis.heapGrowthTrend).append("\",");
        json.append("\"totalGCEvents\":").append(analysis.totalGCEvents).append(",");
        json.append("\"averageGCOverheadPercent\":").append(String.format("%.2f", analysis.averageGCOverhead)).append(",");
        json.append("\"fullGCCount\":").append(analysis.fullGCCount);
        json.append("}}");

        sendJson(exchange, 200, json.toString());
    }

    private void handleMemoryStart(HttpExchange exchange) throws IOException {
        if (!checkPost(exchange)) return;

        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        int interval = 1000;
        try {
            if (params.containsKey("interval")) {
                interval = Integer.parseInt(params.get("interval"));
            }
        } catch (NumberFormatException e) {
            // Use default
        }

        memorySampler.setSamplingInterval(interval);
        memorySampler.start();
        sendJson(exchange, 200, "{\"success\":true,\"running\":true,\"intervalMs\":" + interval + "}");
    }

    private void handleMemoryStop(HttpExchange exchange) throws IOException {
        if (!checkPost(exchange)) return;
        memorySampler.stop();
        sendJson(exchange, 200, "{\"success\":true,\"running\":false,\"totalSamples\":" + memorySampler.getTotalSamples() + "}");
    }

    private void handleHeapHistogram(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        int top = 50;
        try {
            if (params.containsKey("top")) {
                top = Integer.parseInt(params.get("top"));
            }
        } catch (NumberFormatException e) {
            // Use default
        }

        List<HeapHistogramSample> histogram = memorySampler.captureHeapHistogram();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"timestamp\":").append(System.currentTimeMillis()).append(",");
        json.append("\"classCount\":").append(histogram.size()).append(",");
        json.append("\"classes\":[");

        boolean first = true;
        int count = 0;
        for (HeapHistogramSample h : histogram) {
            if (count++ >= top) break;
            if (!first) json.append(",");
            first = false;
            json.append("{");
            json.append("\"className\":\"").append(escapeJson(h.className)).append("\",");
            json.append("\"instanceCount\":").append(h.instanceCount).append(",");
            json.append("\"totalBytes\":").append(h.totalBytes).append(",");
            json.append("\"totalMB\":").append(String.format("%.2f", h.totalBytes / (1024.0 * 1024.0)));
            json.append("}");
        }

        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    // ==================== RESOURCE MONITOR HANDLER ====================

    private void handleResources(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        MetricSnapshot snapshot = metricsCollector.collect();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"timestamp\":").append(snapshot.timestamp).append(",");

        // CPU & GC
        json.append("\"cpu\":{");
        json.append("\"percent\":").append(String.format("%.2f", snapshot.cpuPercent));
        json.append("},");

        json.append("\"gc\":{");
        json.append("\"percent\":").append(String.format("%.2f", snapshot.gcPercent)).append(",");
        json.append("\"count\":").append(snapshot.gcCount).append(",");
        json.append("\"totalTimeMs\":").append(snapshot.gcTime);
        json.append("},");

        // Heap
        json.append("\"heap\":{");
        json.append("\"usedBytes\":").append(snapshot.heapUsed).append(",");
        json.append("\"usedMB\":").append(String.format("%.2f", snapshot.heapUsed / (1024.0 * 1024.0))).append(",");
        json.append("\"committedBytes\":").append(snapshot.heapCommitted).append(",");
        json.append("\"maxBytes\":").append(snapshot.heapMax).append(",");
        json.append("\"utilizationPercent\":").append(String.format("%.2f", snapshot.getHeapUtilization()));
        json.append("},");

        // Metaspace
        json.append("\"metaspace\":{");
        json.append("\"usedBytes\":").append(snapshot.metaspaceUsed).append(",");
        json.append("\"usedMB\":").append(String.format("%.2f", snapshot.metaspaceUsed / (1024.0 * 1024.0))).append(",");
        json.append("\"committedBytes\":").append(snapshot.metaspaceCommitted).append(",");
        json.append("\"maxBytes\":").append(snapshot.metaspaceMax);
        json.append("},");

        // Code Cache
        json.append("\"codeCache\":{");
        json.append("\"usedBytes\":").append(snapshot.codeCacheUsed).append(",");
        json.append("\"maxBytes\":").append(snapshot.codeCacheMax);
        json.append("},");

        // Threads
        json.append("\"threads\":{");
        json.append("\"count\":").append(snapshot.threadCount).append(",");
        json.append("\"daemonCount\":").append(snapshot.daemonThreadCount).append(",");
        json.append("\"peakCount\":").append(snapshot.peakThreadCount).append(",");
        json.append("\"userCount\":").append(snapshot.getUserThreadCount());
        json.append("}");

        json.append("}");
        sendJson(exchange, 200, json.toString());
    }

    // ==================== THREADS HANDLER ====================

    private void handleThreads(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        boolean includeStacks = "true".equalsIgnoreCase(params.get("stacks"));
        int maxDepth = 32;
        try {
            if (params.containsKey("depth")) {
                maxDepth = Integer.parseInt(params.get("depth"));
            }
        } catch (NumberFormatException e) {
            // Use default
        }

        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threads = threadBean.dumpAllThreads(false, false);

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"timestamp\":").append(System.currentTimeMillis()).append(",");
        json.append("\"threadCount\":").append(threads.length).append(",");
        json.append("\"threads\":[");

        boolean first = true;
        for (ThreadInfo thread : threads) {
            if (thread == null) continue;
            if (!first) json.append(",");
            first = false;

            json.append("{");
            json.append("\"id\":").append(thread.getThreadId()).append(",");
            json.append("\"name\":\"").append(escapeJson(thread.getThreadName())).append("\",");
            json.append("\"state\":\"").append(thread.getThreadState().name()).append("\",");
            json.append("\"blockedCount\":").append(thread.getBlockedCount()).append(",");
            json.append("\"blockedTimeMs\":").append(thread.getBlockedTime()).append(",");
            json.append("\"waitedCount\":").append(thread.getWaitedCount()).append(",");
            json.append("\"waitedTimeMs\":").append(thread.getWaitedTime());

            if (includeStacks) {
                StackTraceElement[] stack = thread.getStackTrace();
                json.append(",\"stackTrace\":[");
                boolean firstFrame = true;
                int frameCount = 0;
                for (StackTraceElement frame : stack) {
                    if (frameCount++ >= maxDepth) break;
                    if (!firstFrame) json.append(",");
                    firstFrame = false;
                    json.append("\"").append(escapeJson(frame.toString())).append("\"");
                }
                json.append("]");
            }

            json.append("}");
        }

        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    // ==================== GC CONTROL HANDLER ====================

    private void handleGC(HttpExchange exchange) throws IOException {
        if (!checkPost(exchange)) return;

        long beforeHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startTime = System.currentTimeMillis();

        System.gc();

        long afterHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long duration = System.currentTimeMillis() - startTime;
        long freedBytes = beforeHeap - afterHeap;

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\":true,");
        json.append("\"durationMs\":").append(duration).append(",");
        json.append("\"beforeHeapBytes\":").append(beforeHeap).append(",");
        json.append("\"afterHeapBytes\":").append(afterHeap).append(",");
        json.append("\"freedBytes\":").append(freedBytes).append(",");
        json.append("\"freedMB\":").append(String.format("%.2f", freedBytes / (1024.0 * 1024.0)));
        json.append("}");

        sendJson(exchange, 200, json.toString());
    }

    // ==================== HELPERS ====================

    private void appendMethodStats(StringBuilder json, MethodStats m, int totalSamples, int samplePeriodMs) {
        json.append("{");
        json.append("\"method\":\"").append(escapeJson(m.methodKey)).append("\",");
        json.append("\"className\":\"").append(escapeJson(m.className)).append("\",");
        json.append("\"methodName\":\"").append(escapeJson(m.methodName)).append("\",");
        json.append("\"selfSamples\":").append(m.getSelfSamples()).append(",");
        json.append("\"totalSamples\":").append(m.getTotalSamples()).append(",");
        json.append("\"selfPercent\":").append(String.format("%.2f", m.getSelfPercent(totalSamples))).append(",");
        json.append("\"totalPercent\":").append(String.format("%.2f", m.getTotalPercent(totalSamples))).append(",");
        json.append("\"selfTimeMs\":").append(m.getEstimatedSelfMs(samplePeriodMs)).append(",");
        json.append("\"totalTimeMs\":").append(m.getEstimatedTotalMs(samplePeriodMs)).append(",");
        json.append("\"confidence\":\"").append(getConfidence(m.getSelfSamples())).append("\"");
        json.append("}");
    }

    private String getConfidence(int samples) {
        if (samples >= 100) return "HIGH";
        if (samples >= 30) return "MEDIUM";
        if (samples >= 10) return "LOW";
        return "VERY_LOW";
    }

    private boolean isJFRRecording() {
        try {
            java.lang.reflect.Field f = JFRMethodRecorder.class.getDeclaredField("isRecording");
            f.setAccessible(true);
            return (boolean) f.get(jfrRecorder);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkGet(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed. Use GET.\"}");
            return false;
        }
        return true;
    }

    private boolean checkPost(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed. Use POST.\"}");
            return false;
        }
        return true;
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        sendText(exchange, status, json, "application/json");
    }

    private void sendText(HttpExchange exchange, int status, String text, String contentType) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
