package com.tonic.services.profiler.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tonic.services.profiler.MetricSnapshot;
import com.tonic.services.profiler.ResourceMetricsCollector;
import com.tonic.services.profiler.heap.HeapSnapshot;
import com.tonic.services.profiler.recording.CallTreeNode;
import com.tonic.services.profiler.recording.JFRMethodRecorder;
import com.tonic.services.profiler.recording.MethodProfiler;
import com.tonic.services.profiler.recording.MethodStats;
import com.tonic.services.profiler.recording.MethodTimingResults;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A local HTTP API over the profiler for scripts and LLM agents: status, JFR recordings and their hotspots, native
 * waits and allocations, exact timings, heap histograms, resources, threads and GC. GET / lists every endpoint.
 */
public class ProfilerServer {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
    private static final Gson PRETTY = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static ProfilerServer instance;

    private final ResourceMetricsCollector metricsCollector = new ResourceMetricsCollector();
    private final JFRMethodRecorder jfrRecorder = new JFRMethodRecorder();
    private HttpServer server;
    private ExecutorService executor;
    @Getter
    private int port = 8787;
    @Getter
    private volatile boolean running;
    private volatile MethodTimingResults lastJFRResults;

    private ProfilerServer() {
    }

    /**
     * Gets the server
     * @return the one server instance
     */
    public static synchronized ProfilerServer getInstance() {
        if (instance == null) {
            instance = new ProfilerServer();
        }
        return instance;
    }

    /**
     * Starts the server on its current port, doing nothing when it already runs
     * @throws IOException if the port cannot be bound
     */
    public void start() throws IOException {
        start(port);
    }

    /**
     * Starts the server on 127.0.0.1, doing nothing when it already runs
     * @param port the port to listen on
     * @throws IOException if the port cannot be bound
     */
    public synchronized void start(int port) throws IOException {
        if (running) {
            return;
        }
        this.port = port;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        executor = Executors.newFixedThreadPool(2, task -> {
            Thread thread = new Thread(task, "Profiler API");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        route("/", this::handleRoot);
        route("/profiler", this::handleRoot);
        route("/profiler/status", this::handleStatus);
        route("/profiler/exact", this::handleExact);
        route("/profiler/exact/csv", this::handleExactCsv);
        route("/profiler/exact/enable", exchange -> setExact(exchange, true));
        route("/profiler/exact/disable", exchange -> setExact(exchange, false));
        route("/profiler/exact/clear", this::handleExactClear);
        route("/profiler/jfr", this::handleJfr);
        route("/profiler/jfr/start", this::handleJfrStart);
        route("/profiler/jfr/stop", this::handleJfrStop);
        route("/profiler/hotspots", this::handleHotspots);
        route("/profiler/method", this::handleMethod);
        route("/profiler/native", this::handleNative);
        route("/profiler/allocations", this::handleAllocations);
        route("/profiler/histogram", this::handleHistogram);
        route("/profiler/resources", this::handleResources);
        route("/profiler/threads", this::handleThreads);
        route("/profiler/gc", this::handleGc);
        server.start();
        running = true;
        System.out.println("[ProfilerServer] Started on http://127.0.0.1:" + port);
    }

    /**
     * Stops the server and its request threads; safe to call more than once
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        server.stop(0);
        executor.shutdownNow();
        running = false;
        System.out.println("[ProfilerServer] Stopped");
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!path.equals("/") && !path.equals("/profiler") && !path.equals("/profiler/")) {
            send(exchange, 404, map("error", "Unknown endpoint " + path + ". GET / lists them."));
            return;
        }
        Map<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("GET /profiler/status", "Server, exact timing and recording status");
        endpoints.put("POST /profiler/jfr/start", "Start a JFR recording. Optional: ?period=N (ms, default 10), ?allocations=true to record allocations too, ?package=com.example or ?class=com.example.Foo to show only those methods; allocations are then credited to the first matching frame");
        endpoints.put("POST /profiler/jfr/stop", "Stop the recording and analyze it");
        endpoints.put("GET /profiler/jfr", "Every method of the last recording, Java execution samples only");
        endpoints.put("GET /profiler/hotspots", "Methods that ran the most. Optional: ?top=N (default 10)");
        endpoints.put("GET /profiler/method", "A method's samples, callers and callees. Required: ?name=fully.qualified.Class.method");
        endpoints.put("GET /profiler/native", "Methods threads were blocked in (I/O, locks, window system, GPU); not CPU use. Optional: ?top=N");
        endpoints.put("GET /profiler/allocations", "Allocation bytes by class and by allocating method, when the recording had allocations=true. Optional: ?top=N");
        endpoints.put("GET /profiler/exact", "Exact timings from MethodProfiler.begin/end instrumentation");
        endpoints.put("GET /profiler/exact/csv", "Exact timings as CSV");
        endpoints.put("POST /profiler/exact/enable", "Start collecting exact timings");
        endpoints.put("POST /profiler/exact/disable", "Stop collecting exact timings");
        endpoints.put("POST /profiler/exact/clear", "Clear exact timings");
        endpoints.put("POST /profiler/histogram", "Live heap class histogram; runs a full GC first. Optional: ?top=N (default 50)");
        endpoints.put("GET /profiler/resources", "CPU, GC, heap, metaspace, code cache and thread counts now");
        endpoints.put("GET /profiler/threads", "Every thread with state and blocked/waited counts. Optional: ?stacks=true&depth=N");
        endpoints.put("POST /profiler/gc", "Run a full GC and report what it freed");
        Map<String, Object> help = new LinkedHashMap<>();
        help.put("name", "VitaLite Profiler API");
        help.put("version", "2");
        help.put("endpoints", endpoints);
        help.put("workflow", "POST /profiler/jfr/start, run the code for a few seconds, POST /profiler/jfr/stop, then GET /profiler/hotspots and GET /profiler/method?name=... for callers and callees");
        help.put("notes", "Sample times are samples × period, not measured time. Confidence is HIGH at 100+ samples, MEDIUM at 30+, LOW at 10+, else VERY_LOW.");
        sendText(exchange, 200, PRETTY.toJson(help), "application/json");
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "GET")) {
            return;
        }
        MethodTimingResults results = lastJFRResults;
        send(exchange, 200, map(
                "server", map("running", true, "port", port),
                "exact", map("enabled", MethodProfiler.isEnabled(), "methodCount", MethodProfiler.getMethodCount()),
                "jfr", map("available", JFRMethodRecorder.isAvailable(), "recording", jfrRecorder.isRecording(), "hasResults", results != null, "hasAllocations", results != null && results.allocations != null)));
    }

    private void handleExact(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "GET")) {
            return;
        }
        List<Map<String, Object>> methods = new ArrayList<>();
        for (MethodProfiler.MethodTiming timing : MethodProfiler.getAllTimings()) {
            methods.add(map("label", timing.getLabel(), "calls", timing.getCallCount(), "totalMs", round(timing.getTotalMs(), 3), "avgMs", round(timing.getAverageMs(), 6), "minMs", round(timing.getMinMs(), 6), "maxMs", round(timing.getMaxMs(), 6)));
        }
        send(exchange, 200, map("timestamp", System.currentTimeMillis(), "enabled", MethodProfiler.isEnabled(), "methods", methods));
    }

    private void handleExactCsv(HttpExchange exchange) throws IOException {
        if (allow(exchange, "GET")) {
            sendText(exchange, 200, MethodProfiler.generateCSVReport(), "text/csv");
        }
    }

    private void setExact(HttpExchange exchange, boolean enabled) throws IOException {
        if (allow(exchange, "POST")) {
            MethodProfiler.setEnabled(enabled);
            send(exchange, 200, map("success", true, "enabled", enabled));
        }
    }

    private void handleExactClear(HttpExchange exchange) throws IOException {
        if (allow(exchange, "POST")) {
            MethodProfiler.clear();
            send(exchange, 200, map("success", true, "cleared", true));
        }
    }

    private void handleJfr(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "GET") || !hasResults(exchange)) {
            return;
        }
        MethodTimingResults results = lastJFRResults;
        Map<String, Object> body = recordingSummary(results);
        body.put("methodCount", results.getMethodCount());
        body.put("methods", methods(results.getAllMethodsBySelfTime(), results));
        send(exchange, 200, body);
    }

    private void handleJfrStart(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "POST")) {
            return;
        }
        if (!JFRMethodRecorder.isAvailable()) {
            send(exchange, 500, map("success", false, "error", "JFR is not available in this JVM"));
            return;
        }
        Map<String, String> query = query(exchange);
        int period = intParam(query, "period", 10);
        boolean allocations = "true".equalsIgnoreCase(query.get("allocations"));
        String packageFilter = query.getOrDefault("package", "").trim();
        String classFilter = query.getOrDefault("class", "").trim();
        JFRMethodRecorder.FilterMode filter = !classFilter.isEmpty() ? JFRMethodRecorder.FilterMode.CLASS : !packageFilter.isEmpty() ? JFRMethodRecorder.FilterMode.PACKAGE : JFRMethodRecorder.FilterMode.ALL;
        synchronized (jfrRecorder) {
            if (jfrRecorder.isRecording()) {
                send(exchange, 409, map("success", false, "error", "A recording is already running. POST /profiler/jfr/stop first."));
                return;
            }
            try {
                jfrRecorder.setSamplePeriodMs(period);
                jfrRecorder.setRecordAllocations(allocations);
                jfrRecorder.setFilterMode(filter);
                jfrRecorder.setPackageFilter(packageFilter);
                jfrRecorder.setClassFilter(classFilter);
                jfrRecorder.startRecording();
            } catch (Exception e) {
                send(exchange, 500, map("success", false, "error", String.valueOf(e.getMessage())));
                return;
            }
        }
        send(exchange, 200, map("success", true, "recording", true, "periodMs", jfrRecorder.getSamplePeriodMs(), "allocations", allocations, "filter", filter == JFRMethodRecorder.FilterMode.CLASS ? "class " + classFilter : filter == JFRMethodRecorder.FilterMode.PACKAGE ? "package " + packageFilter : "all"));
    }

    private void handleJfrStop(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "POST")) {
            return;
        }
        MethodTimingResults results;
        synchronized (jfrRecorder) {
            if (!jfrRecorder.isRecording()) {
                send(exchange, 400, map("success", false, "error", "No recording in progress"));
                return;
            }
            try {
                jfrRecorder.stopRecording();
                results = jfrRecorder.analyze();
            } catch (Exception e) {
                send(exchange, 500, map("success", false, "error", String.valueOf(e.getMessage())));
                return;
            }
        }
        lastJFRResults = results;
        send(exchange, 200, map("success", true, "recording", false, "totalSamples", results.totalSamples, "nativeSamples", results.nativeSamples, "methodCount", results.getMethodCount(), "durationMs", results.recordingDurationMs, "allocations", results.allocations != null));
    }

    private void handleHotspots(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "GET") || !hasResults(exchange)) {
            return;
        }
        MethodTimingResults results = lastJFRResults;
        Map<String, Object> body = recordingSummary(results);
        body.put("hotspots", methods(results.getTopMethodsBySelfTime(intParam(query(exchange), "top", 10)), results));
        send(exchange, 200, body);
    }

    private void handleMethod(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "GET")) {
            return;
        }
        String name = query(exchange).get("name");
        if (name == null || name.isEmpty()) {
            send(exchange, 400, map("error", "Missing 'name' parameter"));
            return;
        }
        if (!hasResults(exchange)) {
            return;
        }
        MethodTimingResults results = lastJFRResults;
        CallTreeNode node = results.getMethodNode(name);
        if (node == null) {
            send(exchange, 200, map("found", false, "method", name));
            return;
        }
        List<Map<String, Object>> callers = new ArrayList<>();
        for (Map.Entry<String, Integer> caller : node.getCallersSorted()) {
            callers.add(map("method", caller.getKey(), "count", caller.getValue()));
        }
        List<Map<String, Object>> callees = new ArrayList<>();
        for (Map.Entry<String, Integer> callee : node.getCalleesSorted()) {
            callees.add(map("method", callee.getKey(), "count", callee.getValue()));
        }
        send(exchange, 200, map("found", true, "method", node.getMethodKey(), "className", node.getClassName(), "methodName", node.getMethodName(), "selfSamples", node.getSelfSamples(), "totalSamples", node.getTotalSamples(), "selfPercent", round(node.getSelfPercent(results.totalSamples), 2), "totalPercent", round(node.getTotalPercent(results.totalSamples), 2), "callers", callers, "callees", callees));
    }

    private void handleNative(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "GET") || !hasResults(exchange)) {
            return;
        }
        MethodTimingResults results = lastJFRResults;
        List<Map<String, Object>> methods = new ArrayList<>();
        for (MethodStats method : limit(results.getNativeMethodsBySelfTime(), intParam(query(exchange), "top", 20))) {
            methods.add(map("method", method.methodKey, "selfSamples", method.getSelfSamples(), "sharePercent", round(percent(method.getSelfSamples(), results.nativeSamples), 2), "totalSamples", method.getTotalSamples()));
        }
        send(exchange, 200, map("hasResults", true, "nativeSamples", results.nativeSamples, "note", "Threads inside native code, mostly waiting on I/O, locks, the window system or the GPU; not CPU use", "methods", methods));
    }

    private void handleAllocations(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "GET") || !hasResults(exchange)) {
            return;
        }
        MethodTimingResults.Allocations allocations = lastJFRResults.allocations;
        if (allocations == null) {
            send(exchange, 200, map("hasResults", false, "message", "The last recording had no allocations. Start one with POST /profiler/jfr/start?allocations=true."));
            return;
        }
        int top = intParam(query(exchange), "top", 20);
        send(exchange, 200, map("hasResults", true, "totalBytes", allocations.totalBytes, "note", "JFR samples allocations by TLAB, so bytes are estimates", "byClass", allocationRows(allocations.byClass, allocations.totalBytes, top, "className", true), "byMethod", allocationRows(allocations.bySite, allocations.totalBytes, top, "method", false)));
    }

    private void handleHistogram(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "POST")) {
            return;
        }
        HeapSnapshot snapshot = HeapSnapshot.capture();
        List<Map<String, Object>> classes = new ArrayList<>();
        for (HeapSnapshot.ClassCount count : limit(snapshot.classes, intParam(query(exchange), "top", 50))) {
            classes.add(map("className", count.displayName(), "module", count.module, "instanceCount", count.instances, "totalBytes", count.bytes));
        }
        send(exchange, 200, map("timestamp", snapshot.time, "classCount", snapshot.classes.size(), "totalInstances", snapshot.totalInstances, "totalBytes", snapshot.totalBytes, "classes", classes));
    }

    private void handleResources(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "GET")) {
            return;
        }
        MetricSnapshot snapshot = metricsCollector.collect();
        send(exchange, 200, map(
                "timestamp", snapshot.timestamp,
                "cpu", map("percent", round(snapshot.cpuPercent, 2)),
                "gc", map("percent", round(snapshot.gcPercent, 2), "count", snapshot.gcCount, "totalTimeMs", snapshot.gcTime),
                "heap", map("usedBytes", snapshot.heapUsed, "committedBytes", snapshot.heapCommitted, "maxBytes", snapshot.heapMax, "utilizationPercent", round(snapshot.getHeapUtilization(), 2)),
                "metaspace", map("usedBytes", snapshot.metaspaceUsed, "committedBytes", snapshot.metaspaceCommitted, "maxBytes", snapshot.metaspaceMax),
                "codeCache", map("usedBytes", snapshot.codeCacheUsed, "maxBytes", snapshot.codeCacheMax),
                "threads", map("count", snapshot.threadCount, "daemonCount", snapshot.daemonThreadCount, "peakCount", snapshot.peakThreadCount, "userCount", snapshot.getUserThreadCount())));
    }

    private void handleThreads(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "GET")) {
            return;
        }
        Map<String, String> query = query(exchange);
        boolean stacks = "true".equalsIgnoreCase(query.get("stacks"));
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos = bean.getThreadInfo(bean.getAllThreadIds(), stacks ? Math.max(0, intParam(query, "depth", 32)) : 0);
        List<Map<String, Object>> threads = new ArrayList<>();
        for (ThreadInfo info : infos) {
            if (info == null) {
                continue;
            }
            Map<String, Object> thread = map("id", info.getThreadId(), "name", info.getThreadName(), "state", info.getThreadState().name(), "daemon", info.isDaemon(), "blockedCount", info.getBlockedCount(), "blockedTimeMs", info.getBlockedTime(), "waitedCount", info.getWaitedCount(), "waitedTimeMs", info.getWaitedTime());
            if (stacks) {
                List<String> frames = new ArrayList<>();
                for (StackTraceElement frame : info.getStackTrace()) {
                    frames.add(frame.toString());
                }
                thread.put("stackTrace", frames);
            }
            threads.add(thread);
        }
        send(exchange, 200, map("timestamp", System.currentTimeMillis(), "threadCount", threads.size(), "threads", threads));
    }

    private void handleGc(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "POST")) {
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        long before = runtime.totalMemory() - runtime.freeMemory();
        long start = System.currentTimeMillis();
        System.gc();
        long after = runtime.totalMemory() - runtime.freeMemory();
        send(exchange, 200, map("success", true, "durationMs", System.currentTimeMillis() - start, "beforeHeapBytes", before, "afterHeapBytes", after, "freedBytes", before - after));
    }

    private Map<String, Object> recordingSummary(MethodTimingResults results) {
        return map("hasResults", true, "recordingDurationMs", results.recordingDurationMs, "samplePeriodMs", results.samplePeriodMs, "totalSamples", results.totalSamples, "truncatedSamples", results.truncatedSamples, "nativeSamples", results.nativeSamples);
    }

    private static List<Map<String, Object>> methods(List<MethodStats> stats, MethodTimingResults results) {
        List<Map<String, Object>> methods = new ArrayList<>();
        for (MethodStats method : stats) {
            methods.add(map("method", method.methodKey, "className", method.className, "methodName", method.methodName, "selfSamples", method.getSelfSamples(), "totalSamples", method.getTotalSamples(), "selfPercent", round(method.getSelfPercent(results.totalSamples), 2), "totalPercent", round(method.getTotalPercent(results.totalSamples), 2), "selfTimeMs", method.getEstimatedSelfMs(results.samplePeriodMs), "totalTimeMs", method.getEstimatedTotalMs(results.samplePeriodMs), "confidence", confidence(method.getSelfSamples())));
        }
        return methods;
    }

    private static List<Map<String, Object>> allocationRows(Map<String, long[]> totals, long all, int top, String nameKey, boolean className) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : limit(MethodTimingResults.Allocations.sorted(totals), top)) {
            rows.add(map(nameKey, className ? HeapSnapshot.readable(entry.getKey()) : entry.getKey(), "bytes", entry.getValue()[0], "sharePercent", round(percent(entry.getValue()[0], all), 2), "events", entry.getValue()[1]));
        }
        return rows;
    }

    private boolean hasResults(HttpExchange exchange) throws IOException {
        if (lastJFRResults != null) {
            return true;
        }
        send(exchange, 200, map("hasResults", false, "message", "No recording results yet. POST /profiler/jfr/start, then POST /profiler/jfr/stop."));
        return false;
    }

    private void route(String path, Handler handler) {
        server.createContext(path, exchange -> {
            try {
                handler.handle(exchange);
            } catch (Exception e) {
                send(exchange, 500, map("error", String.valueOf(e.getMessage())));
            } finally {
                exchange.close();
            }
        });
    }

    private static boolean allow(HttpExchange exchange, String method) throws IOException {
        if (method.equalsIgnoreCase(exchange.getRequestMethod())) {
            return true;
        }
        send(exchange, 405, map("error", "Method not allowed. Use " + method + "."));
        return false;
    }

    private static void send(HttpExchange exchange, int status, Map<String, Object> body) throws IOException {
        sendText(exchange, status, GSON.toJson(body), "application/json");
    }

    private static void sendText(HttpExchange exchange, int status, String text, String contentType) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream body = exchange.getResponseBody()) {
            body.write(bytes);
        }
    }

    private static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            if (equals > 0) {
                params.put(URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    private static int intParam(Map<String, String> query, String name, int fallback) {
        try {
            return query.containsKey(name) ? Integer.parseInt(query.get(name).trim()) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index + 1 < keysAndValues.length; index += 2) {
            map.put((String) keysAndValues[index], keysAndValues[index + 1]);
        }
        return map;
    }

    private static <T> List<T> limit(List<T> list, int top) {
        return list.subList(0, Math.max(0, Math.min(top, list.size())));
    }

    private static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    private static double percent(long part, long total) {
        return total == 0 ? 0 : part * 100.0 / total;
    }

    private static String confidence(int samples) {
        return samples >= 100 ? "HIGH" : samples >= 30 ? "MEDIUM" : samples >= 10 ? "LOW" : "VERY_LOW";
    }

    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
