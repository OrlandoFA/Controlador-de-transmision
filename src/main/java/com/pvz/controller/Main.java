package com.pvz.controller;

import com.pvz.controller.config.ControllerConfig;
import com.pvz.controller.server.HttpCommandServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting PvZ Controller...");
        logger.info("Port: {} | Scripts dir: {} | Localhost only: {}",
                ControllerConfig.getPort(),
                ControllerConfig.getScriptsDir(),
                ControllerConfig.isLocalhostOnly());

        try {
            HttpCommandServer server = new HttpCommandServer();
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down controller...");
                server.stop();
            }));

            logger.info("Controller is running on port {}", ControllerConfig.getPort());
            logger.info("Endpoints:");
            logger.info("  POST /command - Execute game command");
            logger.info("  GET  /health  - Health check");
            logger.info("  GET  /status  - Server status");

            Thread.currentThread().join();

        } catch (Exception e) {
            logger.error("Failed to start controller: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}