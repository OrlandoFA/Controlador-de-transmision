package com.pvz.controller.tiktok;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de equipos Plantas vs Zombies.
 * Los viewers escriben en el chat para registrarse.
 * Datos persistidos en data/teams.json
 *
 * Comandos rápidos: p, z, 🌱, 🧟
 */
public class TeamManager {

    private static final Logger logger = LoggerFactory.getLogger(TeamManager.class);
    private static final String DATA_FILE = "data/teams.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public enum Team {
        PLANTAS, ZOMBIES
    }

    private static final Map<String, Team> ALIASES = Map.ofEntries(
            // Team Plantas
            Map.entry("plantas", Team.PLANTAS),
            Map.entry("planta", Team.PLANTAS),
            Map.entry("plants", Team.PLANTAS),
            Map.entry("plant", Team.PLANTAS),
            Map.entry("p", Team.PLANTAS),
            Map.entry("🌱", Team.PLANTAS),
            Map.entry("🌻", Team.PLANTAS),

            // Team Zombies
            Map.entry("zombies", Team.ZOMBIES),
            Map.entry("zombie", Team.ZOMBIES),
            Map.entry("zombis", Team.ZOMBIES),
            Map.entry("zombi", Team.ZOMBIES),
            Map.entry("z", Team.ZOMBIES),
            Map.entry("🧟", Team.ZOMBIES),
            Map.entry("💀", Team.ZOMBIES)
    );

    public static class PlayerInfo {
        public Team team;
        public String nickname;
        public long joinedAt;
        public int actions;

        public PlayerInfo() {}

        public PlayerInfo(Team team, String nickname) {
            this.team = team;
            this.nickname = nickname;
            this.joinedAt = System.currentTimeMillis();
            this.actions = 0;
        }
    }

    private final ConcurrentHashMap<String, PlayerInfo> players = new ConcurrentHashMap<>();

    public TeamManager() {
        load();
    }

    /**
     * Intenta registrar un usuario basándose en su mensaje de chat.
     * @return mensaje para log, o null si no era un registro
     */
    public String tryRegister(String uniqueId, String nickname, String message) {
        String normalized = message.trim().toLowerCase();
        Team team = ALIASES.get(normalized);

        if (team == null) return null;

        PlayerInfo existing = players.get(uniqueId);
        if (existing != null && existing.team == team) return null;

        boolean switched = existing != null;
        PlayerInfo info = new PlayerInfo(team, nickname);
        info.actions = (existing != null) ? existing.actions : 0;
        players.put(uniqueId, info);
        save();

        String emoji = team == Team.PLANTAS ? "🌱" : "🧟";
        String action = switched ? "cambió a" : "se unió a";
        String teamName = team == Team.PLANTAS ? "Plantas" : "Zombies";

        return String.format("%s %s %s Team %s!", emoji, nickname, action, teamName);
    }

    public Team getTeam(String uniqueId) {
        PlayerInfo info = players.get(uniqueId);
        return info != null ? info.team : null;
    }

    public boolean isRegistered(String uniqueId) {
        return players.containsKey(uniqueId);
    }

    public void incrementActions(String uniqueId) {
        PlayerInfo info = players.get(uniqueId);
        if (info != null) {
            info.actions++;
            save();
        }
    }

    public String getStats() {
        int pCount = 0, zCount = 0, pActions = 0, zActions = 0;
        for (PlayerInfo info : players.values()) {
            if (info.team == Team.PLANTAS) { pCount++; pActions += info.actions; }
            else { zCount++; zActions += info.actions; }
        }
        return String.format("📊 🌱 %d (%d acciones) | 🧟 %d (%d acciones) | Total: %d",
                pCount, pActions, zCount, zActions, players.size());
    }
    public Map<String, PlayerInfo> getPlayersSnapshot() {
        return new java.util.HashMap<>(players);
    }

    public void reset() {
        players.clear();
        save();
        logger.info("🔄 Equipos reseteados");
    }

    private void save() {
        try {
            File file = new File(DATA_FILE);
            file.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(players, writer);
            }
        } catch (IOException e) {
            logger.error("Error guardando equipos: {}", e.getMessage());
        }
    }

    private void load() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<ConcurrentHashMap<String, PlayerInfo>>() {}.getType();
            ConcurrentHashMap<String, PlayerInfo> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                players.putAll(loaded);
                logger.info("📂 Cargados {} jugadores registrados", players.size());
            }
        } catch (IOException e) {
            logger.warn("No se pudieron cargar equipos: {}", e.getMessage());
        }
    }
}