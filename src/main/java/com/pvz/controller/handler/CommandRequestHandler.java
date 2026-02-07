package com.pvz.controller.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.pvz.controller.config.ControllerConfig;
import com.pvz.controller.memory.TrainerExecutor;
import com.pvz.controller.model.GameCommand;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CommandRequestHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(CommandRequestHandler.class);
    private final Gson gson = new Gson();
    private final TrainerExecutor trainer = new TrainerExecutor();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String remoteAddress = exchange.getRemoteAddress().getAddress().getHostAddress();
        logger.debug("Request from: {}", remoteAddress);

        // Check localhost only
        if (ControllerConfig.isLocalhostOnly() && !isLocalhost(remoteAddress)) {
            logger.warn("Rejected non-localhost request from: {}", remoteAddress);
            sendResponse(exchange, 403, createErrorResponse("Access denied: localhost only"));
            return;
        }

        // Only accept POST
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, createErrorResponse("Method not allowed"));
            return;
        }

        try {
            // Read request body
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            logger.debug("Request body: {}", body);

            // Parse command
            GameCommand command = gson.fromJson(body, GameCommand.class);

            if (command == null) {
                sendResponse(exchange, 400, createErrorResponse("Invalid JSON"));
                return;
            }

            // Validate command
            if (!command.isValid()) {
                logger.warn("Invalid command received: {}", command);
                sendResponse(exchange, 400, createErrorResponse("Invalid command"));
                return;
            }

            logger.info("Executing command: {} from user: {}", command.getCommand(), command.getUser());

            // Execute command using memory trainer
            TrainerExecutor.ExecutionResult result = trainer.execute(command);

            if (result.isSuccess()) {
                sendResponse(exchange, 200, createSuccessResponse(result.getMessage()));
            } else {
                sendResponse(exchange, 500, createErrorResponse(result.getMessage()));
            }

        } catch (JsonSyntaxException e) {
            logger.error("JSON parse error: {}", e.getMessage());
            sendResponse(exchange, 400, createErrorResponse("Invalid JSON format"));
        } catch (Exception e) {
            logger.error("Error handling request: {}", e.getMessage(), e);
            sendResponse(exchange, 500, createErrorResponse("Internal server error"));
        }
    }

    private boolean isLocalhost(String address) {
        return "127.0.0.1".equals(address) ||
                "0:0:0:0:0:0:0:1".equals(address) ||
                "::1".equals(address);
    }

    private String createSuccessResponse(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("success", true);
        json.addProperty("message", message);
        return gson.toJson(json);
    }

    private String createErrorResponse(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("success", false);
        json.addProperty("message", message);
        return gson.toJson(json);
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