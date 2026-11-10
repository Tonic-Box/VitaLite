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
import com.tonic.services.profiler.heap.HeapSnapshot;
import lombok.Getter;

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
    @Getter
    private int port = 8787;
    @Getter
    private volatile boolean running = false;

    // JFR state
    private JFRMethodRecorder jfrRecorder;
    private MethodTimingResults lastJFRResults;

    // Sampling state

    // Resource metrics
    private final ResourceMetricsCollector metricsCollector;

    private ProfilerServer() {
        jfrRecorder = new JFRMethodRecorder();
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

        // Memory sampling endpoints
        server.createContext("/profiler/memory/histogram", this::handleHeapHistogram);

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

    // ==================== HANDLERS ====================

    private void handleRoot(HttpExchange exchange) throws IOException {
        // Serve help/documentation for any request to root or /profiler without sub-path
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.equals("/profiler") || path.equals("/profiler/")) {

            String json = "{\n" +
                    "  \"name\": \"VitaLite Profiler API\",\n" +
                    "  \"version\": \"1.0\",\n" +
                    "  \"description\": \"REST API for accessing JVM profiler data. Designed for LLM integration.\",\n" +
                    "  \"endpoints\": {\n" +

                    // Status
                    "    \"status\": {\n" +
                    "      \"GET /profiler/status\": \"Server and profiler status overview\"\n" +
                    "    },\n" +

                    // Exact timing
                    "    \"exact_timing\": {\n" +
                    "      \"description\": \"Manual method timing via MethodProfiler.begin()/end() instrumentation\",\n" +
                    "      \"GET /profiler/exact\": \"All exact timing data as JSON\",\n" +
                    "      \"GET /profiler/exact/csv\": \"All exact timing data as CSV\",\n" +
                    "      \"POST /profiler/exact/enable\": \"Enable MethodProfiler (starts collecting data)\",\n" +
                    "      \"POST /profiler/exact/disable\": \"Disable MethodProfiler (zero overhead when disabled)\",\n" +
                    "      \"POST /profiler/exact/clear\": \"Clear all collected timing data\"\n" +
                    "    },\n" +

                    // JFR
                    "    \"jfr_sampling\": {\n" +
                    "      \"description\": \"JVM Flight Recorder sampling profiler for statistical CPU profiling\",\n" +
                    "      \"GET /profiler/jfr\": \"Results from last JFR recording session\",\n" +
                    "      \"POST /profiler/jfr/start\": \"Start JFR recording. Optional: ?period=N (sample period in ms, default 10)\",\n" +
                    "      \"POST /profiler/jfr/stop\": \"Stop JFR recording and analyze results\"\n" +
                    "    },\n" +

                    // Analysis
                    "    \"analysis\": {\n" +
                    "      \"description\": \"Higher-level analysis endpoints for JFR data\",\n" +
                    "      \"GET /profiler/hotspots\": \"Top methods by CPU time. Optional: ?top=N (default 10)\",\n" +
                    "      \"GET /profiler/method\": \"Method details with callers/callees. Required: ?name=ClassName.methodName\"\n" +
                    "    },\n" +

                    "    \"heap\": {\n" +
                    "      \"GET /profiler/memory/histogram\": \"Live heap histogram, top classes by size; runs a full GC. Optional: ?top=N (default 50)\"\n" +
                    "    },\n" +

                    // Resources
                    "    \"resources\": {\n" +
                    "      \"description\": \"Live JVM resource metrics\",\n" +
                    "      \"GET /profiler/resources\": \"Current CPU, heap, metaspace, code cache, and thread stats\"\n" +
                    "    },\n" +

                    // Threads
                    "    \"threads\": {\n" +
                    "      \"description\": \"Thread information and stack traces\",\n" +
                    "      \"GET /profiler/threads\": \"All threads with state info. Optional: ?stacks=true&depth=N\"\n" +
                    "    },\n" +

                    // GC control
                    "    \"gc\": {\n" +
                    "      \"description\": \"Garbage collection control\",\n" +
                    "      \"POST /profiler/gc\": \"Trigger full GC and report memory freed\"\n" +
                    "    }\n" +
                    "  },\n" +

                    // Quick start
                    "  \"quick_start\": {\n" +
                    "    \"1_check_status\": \"GET /profiler/status\",\n" +
                    "    \"2a_exact_timing\": [\n" +
                    "      \"POST /profiler/exact/enable\",\n" +
                    "      \"(run your code with MethodProfiler instrumentation)\",\n" +
                    "      \"GET /profiler/exact\"\n" +
                    "    ],\n" +
                    "    \"2b_jfr_sampling\": [\n" +
                    "      \"POST /profiler/jfr/start?period=10\",\n" +
                    "      \"(run your code for a few seconds)\",\n" +
                    "      \"POST /profiler/jfr/stop\",\n" +
                    "      \"GET /profiler/hotspots?top=10\"\n" +
                    "    ]\n" +
                    "  },\n" +

                    // Tips for LLMs
                    "  \"llm_tips\": {\n" +
                    "    \"workflow\": \"Use /profiler/status first to check what data is available\",\n" +
                    "    \"exact_vs_jfr\": \"Exact timing is precise but requires code instrumentation. JFR sampling works on any code with statistical accuracy.\",\n" +
                    "    \"hotspots\": \"Start with /profiler/hotspots to find slow methods, then use /profiler/method?name=X for caller/callee analysis\",\n" +
                    "    \"confidence\": \"JFR results include confidence levels: HIGH (100+ samples), MEDIUM (30+), LOW (10+), VERY_LOW (<10)\"\n" +
                    "  }\n" +
                    "}\n";

            sendJson(exchange, 200, json);
            return;
        }
        // For other paths under /profiler that don't match, return 404
        sendJson(exchange, 404, "{\"error\":\"Endpoint not found\",\"hint\":\"GET /profiler for API documentation\"}");
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        String json = "{" +
                "\"server\":{" +
                "\"running\":true," +
                "\"port\":" + port +
                "}," +
                "\"exact\":{" +
                "\"enabled\":" + MethodProfiler.isEnabled() + "," +
                "\"methodCount\":" + MethodProfiler.getMethodCount() +
                "}," +
                "\"jfr\":{" +
                "\"available\":" + JFRMethodRecorder.isAvailable() + "," +
                "\"recording\":" + (jfrRecorder != null && isJFRRecording()) + "," +
                "\"hasResults\":" + (lastJFRResults != null) +
                "}}";

        sendJson(exchange, 200, json);
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

            String json = "{\"success\":true,\"recording\":false," +
                    "\"totalSamples\":" + lastJFRResults.totalSamples + "," +
                    "\"methodCount\":" + lastJFRResults.getMethodCount() + "," +
                    "\"durationMs\":" + (lastJFRResults.recordingEndTime - lastJFRResults.recordingStartTime) +
                    "}";

            sendJson(exchange, 200, json);
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

        HeapSnapshot snapshot = HeapSnapshot.capture();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"timestamp\":").append(snapshot.time).append(",");
        json.append("\"classCount\":").append(snapshot.classes.size()).append(",");
        json.append("\"totalInstances\":").append(snapshot.totalInstances).append(",");
        json.append("\"totalBytes\":").append(snapshot.totalBytes).append(",");
        json.append("\"classes\":[");

        boolean first = true;
        int count = 0;
        for (HeapSnapshot.ClassCount h : snapshot.classes) {
            if (count++ >= top) break;
            if (!first) json.append(",");
            first = false;
            json.append("{");
            json.append("\"className\":\"").append(escapeJson(h.displayName())).append("\",");
            json.append("\"instanceCount\":").append(h.instances).append(",");
            json.append("\"totalBytes\":").append(h.bytes).append(",");
            json.append("\"totalMB\":").append(String.format(java.util.Locale.ROOT, "%.2f", h.bytes / (1024.0 * 1024.0)));
            json.append("}");
        }

        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    // ==================== RESOURCE MONITOR HANDLER ====================

    private void handleResources(HttpExchange exchange) throws IOException {
        if (!checkGet(exchange)) return;

        MetricSnapshot snapshot = metricsCollector.collect();

        String json = "{" +
                "\"timestamp\":" + snapshot.timestamp + "," +

                // CPU & GC
                "\"cpu\":{" +
                "\"percent\":" + String.format("%.2f", snapshot.cpuPercent) +
                "}," +
                "\"gc\":{" +
                "\"percent\":" + String.format("%.2f", snapshot.gcPercent) + "," +
                "\"count\":" + snapshot.gcCount + "," +
                "\"totalTimeMs\":" + snapshot.gcTime +
                "}," +

                // Heap
                "\"heap\":{" +
                "\"usedBytes\":" + snapshot.heapUsed + "," +
                "\"usedMB\":" + String.format("%.2f", snapshot.heapUsed / (1024.0 * 1024.0)) + "," +
                "\"committedBytes\":" + snapshot.heapCommitted + "," +
                "\"maxBytes\":" + snapshot.heapMax + "," +
                "\"utilizationPercent\":" + String.format("%.2f", snapshot.getHeapUtilization()) +
                "}," +

                // Metaspace
                "\"metaspace\":{" +
                "\"usedBytes\":" + snapshot.metaspaceUsed + "," +
                "\"usedMB\":" + String.format("%.2f", snapshot.metaspaceUsed / (1024.0 * 1024.0)) + "," +
                "\"committedBytes\":" + snapshot.metaspaceCommitted + "," +
                "\"maxBytes\":" + snapshot.metaspaceMax +
                "}," +

                // Code Cache
                "\"codeCache\":{" +
                "\"usedBytes\":" + snapshot.codeCacheUsed + "," +
                "\"maxBytes\":" + snapshot.codeCacheMax +
                "}," +

                // Threads
                "\"threads\":{" +
                "\"count\":" + snapshot.threadCount + "," +
                "\"daemonCount\":" + snapshot.daemonThreadCount + "," +
                "\"peakCount\":" + snapshot.peakThreadCount + "," +
                "\"userCount\":" + snapshot.getUserThreadCount() +
                "}" +
                "}";
        sendJson(exchange, 200, json);
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

        String json = "{" +
                "\"success\":true," +
                "\"durationMs\":" + duration + "," +
                "\"beforeHeapBytes\":" + beforeHeap + "," +
                "\"afterHeapBytes\":" + afterHeap + "," +
                "\"freedBytes\":" + freedBytes + "," +
                "\"freedMB\":" + String.format("%.2f", freedBytes / (1024.0 * 1024.0)) +
                "}";

        sendJson(exchange, 200, json);
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
