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
import java.util.concurrent.Executors;

public class HttpCommandServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpCommandServer.class);
    private HttpServer server;

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

        // Root endpoint
        server.createContext("/", exchange -> {
            String response = "{\"name\":\"PvZ Controller\",\"version\":\"1.0.0\",\"endpoints\":[\"/command\",\"/health\",\"/status\"]}";
            sendResponse(exchange, 200, response);
        });

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

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
}