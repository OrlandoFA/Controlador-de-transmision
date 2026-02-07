package com.pvz.controller.executor;

import com.pvz.controller.config.ControllerConfig;
import com.pvz.controller.model.GameCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ScriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ScriptExecutor.class);

    private static final Map<String, String> COMMAND_TO_SCRIPT = new HashMap<>();

    static {
        COMMAND_TO_SCRIPT.put("spawn_zombie", "spawn_zombie.ahk");
        COMMAND_TO_SCRIPT.put("wave", "next_wave.ahk");
        COMMAND_TO_SCRIPT.put("pause", "pause_game.ahk");
        COMMAND_TO_SCRIPT.put("resume", "resume_game.ahk");
    }

    public ExecutionResult execute(GameCommand command) {
        String scriptName = COMMAND_TO_SCRIPT.get(command.getCommand());

        if (scriptName == null) {
            return new ExecutionResult(false, "Unknown command: " + command.getCommand());
        }

        File scriptFile = new File(ControllerConfig.getScriptsDir(), scriptName);

        if (!scriptFile.exists()) {
            logger.error("Script not found: {}", scriptFile.getAbsolutePath());
            return new ExecutionResult(false, "Script not found: " + scriptName);
        }

        List<String> cmdList = buildCommand(scriptFile, command);

        logger.info("Executing: {}", String.join(" ", cmdList));

        try {
            ProcessBuilder pb = new ProcessBuilder(cmdList);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(
                    ControllerConfig.getScriptTimeout(), TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                logger.error("Script timed out: {}", scriptName);
                return new ExecutionResult(false, "Script timed out");
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                logger.info("Script executed successfully: {}", scriptName);
                return new ExecutionResult(true, "Command executed: " + command.getCommand());
            } else {
                logger.error("Script failed with exit code {}: {}", exitCode, output);
                return new ExecutionResult(false, "Script failed with exit code: " + exitCode);
            }

        } catch (Exception e) {
            logger.error("Failed to execute script: {}", e.getMessage(), e);
            return new ExecutionResult(false, "Execution error: " + e.getMessage());
        }
    }

    private List<String> buildCommand(File scriptFile, GameCommand command) {
        List<String> cmdList = new ArrayList<>();
        cmdList.add(ControllerConfig.getAhkExecutable());
        cmdList.add(scriptFile.getAbsolutePath());

        if (command.isSpawnCommand()) {
            cmdList.add(command.getType());
            cmdList.add(String.valueOf(command.getCount()));
        }

        return cmdList;
    }

    public static class ExecutionResult {
        private final boolean success;
        private final String message;

        public ExecutionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}