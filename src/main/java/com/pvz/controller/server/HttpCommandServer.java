package com.pvz.controller.server;

import com.pvz.controller.config.ControllerConfig;
import com.pvz.controller.handler.CommandRequestHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;

public class HttpCommandServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpCommandServer.class);
    private HttpServer server;

    // Carpeta del build de React
    private static final Path OVERLAY_DIR = Path.of("overlay");

    // MIME types para archivos estáticos
    private static final Map<String, String> MIME_TYPES = Map.of(
            ".html", "text/html; charset=utf-8",
            ".css", "text/css; charset=utf-8",
            ".js", "application/javascript; charset=utf-8",
            ".json", "application/json; charset=utf-8",
            ".svg", "image/svg+xml",
            ".png", "image/png",
            ".ico", "image/x-icon",
            ".woff2", "font/woff2",
            ".woff", "font/woff"
    );

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(ControllerConfig.getPort()), 0);

        // Command endpoint
        server.createContext("/command", new CommandRequestHandler());

        // Health check
        server.createContext("/health", exchange -> {
            String response = "{\"status\":\"ok\"}";
            sendResponse(exchange, 200, response);
        });

        // Status endpoint
        server.createContext("/status", exchange -> {
            String response = String.format(
                    "{\"status\":\"running\",\"port\":%d,\"scriptsDir\":\"%s\",\"localhostOnly\":%b}",
                    ControllerConfig.getPort(),
                    ControllerConfig.getScriptsDir().replace("\\", "\\\\"),
                    ControllerConfig.isLocalhostOnly()
            );
            sendResponse(exchange, 200, response);
        });

        // ── TEAMS JSON ──
        server.createContext("/teams", exchange -> {
            try {
                Path teamsFile = Path.of("data/teams.json");
                if (Files.exists(teamsFile)) {
                    String json = Files.readString(teamsFile, StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } else {
                    sendResponse(exchange, 200, "{}");
                }
            } catch (Exception e) {
                logger.error("Error sirviendo teams: {}", e.getMessage());
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        // ── OVERLAY (React build) ──
        server.createContext("/overlay", exchange -> {
            try {
                String path = exchange.getRequestURI().getPath();

                // /overlay → index.html
                // /overlay/assets/xxx → assets/xxx
                String relativePath;
                if (path.equals("/overlay") || path.equals("/overlay/")) {
                    relativePath = "index.html";
                } else {
                    relativePath = path.substring("/overlay/".length());
                }

                Path filePath = OVERLAY_DIR.resolve(relativePath).normalize();

                // Seguridad: no salir del directorio
                if (!filePath.startsWith(OVERLAY_DIR)) {
                    sendResponse(exchange, 403, "{\"error\":\"forbidden\"}");
                    return;
                }

                if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                    byte[] bytes = Files.readAllBytes(filePath);
                    String contentType = getMimeType(filePath.toString());
                    exchange.getResponseHeaders().set("Content-Type", contentType);
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } else {
                    // SPA fallback → index.html
                    Path indexPath = OVERLAY_DIR.resolve("index.html");
                    if (Files.exists(indexPath)) {
                        byte[] bytes = Files.readAllBytes(indexPath);
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(bytes);
                        }
                    } else {
                        sendResponse(exchange, 404, "{\"error\":\"overlay not found. Run: npm run build\"}");
                    }
                }
            } catch (Exception e) {
                logger.error("Error sirviendo overlay: {}", e.getMessage());
                sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        // Root endpoint
        server.createContext("/", exchange -> {
            String response = "{\"name\":\"PvZ Controller\",\"version\":\"2.0.0\",\"endpoints\":[\"/command\",\"/health\",\"/status\",\"/teams\",\"/overlay\"]}";
            sendResponse(exchange, 200, response);
        });

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        // Verificar overlay
        if (Files.exists(OVERLAY_DIR.resolve("index.html"))) {
            logger.info("🖥️ Overlay disponible en http://localhost:{}/overlay", ControllerConfig.getPort());
        } else {
            logger.warn("⚠️ Carpeta overlay/ no encontrada. El overlay no estará disponible.");
        }

        logger.info("HTTP Server started on port {}", ControllerConfig.getPort());
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("HTTP Server stopped");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getMimeType(String path) {
        String lower = path.toLowerCase();
        for (var entry : MIME_TYPES.entrySet()) {
            if (lower.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "application/octet-stream";
    }
}