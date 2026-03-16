package com.eci.arep.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpWebServer {

    private final ControllerRegistry registry;
    private final int port;

    public HttpWebServer(ControllerRegistry registry, int port) {
        this.registry = registry;
        this.port = port;
    }

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/", new MainHandler(registry));
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();
            System.out.println("Server running on http://0.0.0.0:" + port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                server.stop(5);
                System.out.println("Server stopped.");
            }));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start HTTP server", e);
        }
    }

    private static class MainHandler implements HttpHandler {

        private final ControllerRegistry registry;

        private MainHandler(ControllerRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            RouteHandler handler = registry.getHandler(path);
            if (handler != null) {
                Request request = new Request(path, exchange.getRequestURI().getRawQuery());
                String response = handler.invoke(request);
                sendText(exchange, 200, response);
                return;
            }

            if (serveStatic(exchange, path)) {
                return;
            }

            sendText(exchange, 404, "Not Found");
        }

        private boolean serveStatic(HttpExchange exchange, String path) throws IOException {
            String normalizedPath = "/".equals(path) ? "/index.html" : path;
            String resourcePath = "static" + normalizedPath;

            InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
            if (resource == null) {
                return false;
            }

            byte[] content;
            try (InputStream input = resource) {
                content = input.readAllBytes();
            }

            exchange.getResponseHeaders().set("Content-Type", contentType(normalizedPath));
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
            return true;
        }

        private String contentType(String path) {
            if (path.endsWith(".html")) {
                return "text/html; charset=UTF-8";
            }
            if (path.endsWith(".png")) {
                return "image/png";
            }
            return "application/octet-stream";
        }

        private void sendText(HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }
}
