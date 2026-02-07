package com.pvz.controller.config;

public class ControllerConfig {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_SCRIPTS_DIR = "./scripts";
    private static final String DEFAULT_AHK_EXECUTABLE = "C:\\Program Files\\AutoHotkey\\v2\\AutoHotkey64.exe";
    private static final int DEFAULT_SCRIPT_TIMEOUT = 10;
    private static final boolean DEFAULT_LOCALHOST_ONLY = true;

    public static int getPort() {
        String port = System.getenv("CONTROLLER_PORT");
        if (port != null) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                return DEFAULT_PORT;
            }
        }
        return DEFAULT_PORT;
    }

    public static String getScriptsDir() {
        String dir = System.getenv("SCRIPTS_DIR");
        return (dir != null && !dir.isEmpty()) ? dir : DEFAULT_SCRIPTS_DIR;
    }

    public static String getAhkExecutable() {
        String exe = System.getenv("AHK_EXECUTABLE");
        return (exe != null && !exe.isEmpty()) ? exe : DEFAULT_AHK_EXECUTABLE;
    }

    public static int getScriptTimeout() {
        String timeout = System.getenv("SCRIPT_TIMEOUT");
        if (timeout != null) {
            try {
                return Integer.parseInt(timeout);
            } catch (NumberFormatException e) {
                return DEFAULT_SCRIPT_TIMEOUT;
            }
        }
        return DEFAULT_SCRIPT_TIMEOUT;
    }

    public static boolean isLocalhostOnly() {
        String localhost = System.getenv("LOCALHOST_ONLY");
        if (localhost != null) {
            return Boolean.parseBoolean(localhost);
        }
        return DEFAULT_LOCALHOST_ONLY;
    }
}