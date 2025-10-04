package com.tonic.plugins.profiles.jagex;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JagexHttpServer {
    private final Map<Integer, CompletableFuture<OAuth2Response>> futures = new ConcurrentHashMap<>();
    private final ScheduledExecutorService shutdownScheduler = Executors.newScheduledThreadPool(1);

    private HttpServer server;


    public boolean isOnline() {
        return server != null;
    }

    public void start() throws IOException {
        if (server != null) {
            return;
        }

        server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/capture", new CaptureHandler());
        server.setExecutor(Executors.newFixedThreadPool(2));
        server.start();

        shutdownScheduler.schedule(this::stop, 5, TimeUnit.MINUTES);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }

        futures.values().forEach(future ->
                future.completeExceptionally(new IllegalStateException("Server stopped"))
        );
        futures.clear();
        shutdownScheduler.shutdown();
    }

    public CompletableFuture<OAuth2Response> waitForResponse(int state) {
        CompletableFuture<OAuth2Response> future = new CompletableFuture<>();
        futures.put(state, future);
        return future;
    }

    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendErrorResponse(exchange, 405);
                return;
            }

            String htmlContent = createBasicHtmlPage(
                    "const url = window.location.href;" +
                            "if (url.includes(\"localhost/#\")) {" +
                            "    window.location.href = url.replace(\"localhost/#\", \"localhost/capture?\");" +
                            "} else {" +
                            "    alert(\"Something went wrong\");" +
                            "}",
                    "Redirecting.."
            );
            sendHtmlResponse(exchange, htmlContent);
        }
    }

    private class CaptureHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendErrorResponse(exchange, 405);
                return;
            }

            URI requestUri = exchange.getRequestURI();
            Map<String, String> params = parseQueryParams(requestUri.getQuery());

            String error = params.get("error");
            String description = params.get("error_description");
            if (error != null) {
                System.err.println("Received error from OAuth2 server: " + error + ": " + description);
                sendHtmlResponse(exchange, "Error: " + error);
                return;
            }

            String code = params.get("code");
            String idToken = params.get("id_token");
            String state = params.get("state");

            if (code == null || idToken == null || state == null) {
                sendErrorResponse(exchange, 400);
                return;
            }

            OAuth2Response response = new OAuth2Response(code, idToken);
            CompletableFuture<OAuth2Response> future = futures.get(Integer.parseInt(state));
            if (future != null) {
                future.complete(response);
            }

            String htmlResponse = createBasicHtmlPage("",
                    "Everything is complete. You can close the window now.");

            sendHtmlResponse(exchange, htmlResponse);
        }
    }

    @Getter
    public static class OAuth2Response {
        private final String code;
        private final String idToken;

        public OAuth2Response(String code, String idToken) {
            this.code = code;
            this.idToken = idToken;
        }
    }

    private static void sendHtmlResponse(HttpExchange exchange,
                                         String content) throws IOException {
        byte[] responseBytes = content.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private static void sendErrorResponse(HttpExchange exchange,
                                          int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }

    private static String createBasicHtmlPage(String js,
                                              String body) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><head><script>");
        builder.append(js);
        builder.append("</script></head><body>");
        builder.append(body);
        builder.append("</body></html>");

        return builder.toString();
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null) return params;

        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2) {
                params.put(pair[0], pair[1]);
            }
        }
        return params;
    }
}
