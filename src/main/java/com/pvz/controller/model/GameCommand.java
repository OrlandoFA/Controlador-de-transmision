package com.pvz.controller.model;

import java.util.Set;

public class GameCommand {

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "spawn_zombie", "wave", "pause", "resume", "sun", "info",
            "plant", "plants", "plantmenu", "grid",
            "zombies", "zombiemenu"
    );

    private static final Set<String> ALLOWED_ZOMBIE_TYPES = Set.of(
            "normal", "conehead", "buckethead", "flag",
            "pole", "newspaper", "football", "dancer", "gargantuar",
            "screendoor", "dolphin", "jack", "balloon", "digger",
            "pogo", "yeti", "bungee", "ladder", "catapult", "imp",
            "giga", "zomboni", "snorkel", "bobsled", "dancing"
    );

    private String command;
    private String type;
    private int count;
    private String user;
    private String row;
    private Integer col;

    public GameCommand() {
    }

    public boolean isValid() {
        if (command == null || !ALLOWED_COMMANDS.contains(command)) {
            return false;
        }
        if (isSpawnCommand()) {
            return isValidZombieType() && isValidCount();
        }
        if (isPlantCommand()) {
            return type != null && row != null && col != null;
        }
        return true;
    }

    public boolean isSpawnCommand() {
        return "spawn_zombie".equals(command);
    }

    public boolean isPlantCommand() {
        return "plant".equals(command);
    }

    public boolean isValidZombieType() {
        return type != null && ALLOWED_ZOMBIE_TYPES.contains(type.toLowerCase());
    }

    public boolean isValidCount() {
        return count >= 1 && count <= 10;
    }

    public int getCountOrDefault(int defaultValue) {
        return count > 0 ? count : defaultValue;
    }

    // Getters and Setters
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getRow() { return row; }
    public void setRow(String row) { this.row = row; }

    public Integer getCol() { return col; }
    public void setCol(Integer col) { this.col = col; }

    @Override
    public String toString() {
        return "GameCommand{" +
                "command='" + command + '\'' +
                ", type='" + type + '\'' +
                ", count=" + count +
                ", row='" + row + '\'' +
                ", col=" + col +
                ", user='" + user + '\'' +
                '}';
    }
}