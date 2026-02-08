package com.pvz.controller.tiktok;

import com.pvz.controller.games.GameController;
import com.pvz.controller.games.GameController.ActionResult;
import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.LiveClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio principal de conexión TikTok LIVE.
 *
 * Flujo GRATIS (chat):
 *   - Viewer escribe "plantas" o "zombies" → se registra
 *   - Team Plantas escribe "A3" → lanzaguisantes gratis (cooldown 5s por equipo)
 *   - Team Zombies escribe "C"  → zombie normal desde la entrada (cooldown 5s por equipo)
 *
 * Flujo REGALOS (gifts):
 *   - Viewer registrado escribe posición + envía regalo
 *   - Planta/zombie fuerte según regalo (cooldown individual 3s)
 *
 * Evento de LIKES (cada 5 minutos):
 *   - 50% planta aleatoria en posición aleatoria
 *   - 50% zombie aleatorio en fila aleatoria (normal/cono/cubeta/dancer)
 *
 * Follow/Share: bonus de sol instantáneo
 */
public class TikTokService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokService.class);

    private final String tiktokUsername;
    private final GameController gameController;
    private final TeamManager teamManager;
    private final Random rng = new Random();

    private LiveClient client;
    private volatile boolean running = false;

    // Cache último mensaje por usuario (para vincular posición con gift)
    private final Map<String, CachedMessage> lastMessages = new ConcurrentHashMap<>();

    // Acumulador de likes para evento
    private final AtomicInteger pendingLikes = new AtomicInteger(0);

    // Cooldown individual por usuario (para gifts)
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    // Cooldown global separado por equipo
    private volatile long lastPlantasAction = 0;
    private volatile long lastZombiesAction = 0;

    // ═══════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════

    // Likes → evento aleatorio cada 5 minutos
    private static final long LIKE_FLUSH_MS = 300_000;
    private static final int MIN_LIKES_FOR_EVENT = 5;

    // Pool de plantas aleatorias (evento de likes)
    private static final String[] RANDOM_PLANTS = {
            "pea", "snow", "repeater", "wallnut", "sunflower", "cherry", "squash"
    };

    // Pool de zombies aleatorios con probabilidad ponderada
    // 40% normal, 30% cono, 20% cubeta, 10% dancer (Michael Jackson)
    private static final String[] RANDOM_ZOMBIES = {
            "normal", "normal", "normal", "normal",
            "conehead", "conehead", "conehead",
            "buckethead", "buckethead",
            "dancer"
    };

    // Follow / Share bonus
    private static final int SUN_BONUS_FOLLOW = 100;
    private static final int SUN_BONUS_SHARE = 200;

    // Cooldown individual (gifts)
    private static final long ACTION_COOLDOWN_MS = 3000;

    // Cooldown global por equipo (acciones gratis por chat)
    private static final long GLOBAL_COOLDOWN_MS = 5_000;

    // Acción gratis por chat
    private static final String FREE_PLANT_TYPE = "pea";
    private static final String FREE_ZOMBIE_TYPE = "normal";

    // Cache de mensajes TTL
    private static final long MSG_CACHE_TTL_MS = 30_000;

    record CachedMessage(String text, long timestamp) {}

    // ═══════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════

    public TikTokService(String tiktokUsername, GameController gameController) {
        this.tiktokUsername = tiktokUsername;
        this.gameController = gameController;
        this.teamManager = new TeamManager();
    }

    // ═══════════════════════════════════════════════════════════
    // CONEXIÓN
    // ═══════════════════════════════════════════════════════════

    public void start() {
        logger.info("🔌 Conectando a TikTok LIVE de @{}...", tiktokUsername);

        client = TikTokLive.newClient(tiktokUsername)
                .configure(settings -> {
                    settings.setClientLanguage("es");
                    settings.setRetryOnConnectionFailure(true);
                    settings.setRetryConnectionTimeout(Duration.ofSeconds(5));
                    settings.setPrintToConsole(false);
                })

                // ── CONECTADO ──
                .onConnected((liveClient, event) -> {
                    running = true;
                    logger.info("🔴 ¡Conectado a TikTok LIVE de @{}!", tiktokUsername);
                    logger.info("🎮 Juego activo: {}", gameController.getGameName());
                    logger.info("📋 Viewers: escriban 'plantas' o 'zombies' para unirse");
                    logger.info("🆓 Plantas: escribe 'A3' para plantar gratis | Zombies: escribe 'C' para zombie gratis");
                    logger.info("🎁 Regalos → plantas/zombies fuertes según el regalo");
                    logger.info("❤️ Likes → cada 5 min se activa un evento aleatorio");
                    logger.info(teamManager.getStats());
                    startLikeFlushTimer();
                })

                // ── DESCONECTADO ──
                .onDisconnected((liveClient, event) -> {
                    running = false;
                    logger.warn("📴 Desconectado de TikTok LIVE");
                })

                // ── STREAM TERMINADO ──
                .onLiveEnded((liveClient, event) -> {
                    running = false;
                    logger.info("🛑 El stream ha terminado");
                })

                // ── CHAT (registro + acciones gratis) ──
                .onComment((liveClient, event) -> {
                    try {
                        handleComment(event);
                    } catch (Exception e) {
                        logger.error("Error en chat: {}", e.getMessage());
                    }
                })

                // ── GIFT (acción premium) ──
                .onGift((liveClient, event) -> {
                    try {
                        handleGift(event);
                    } catch (Exception e) {
                        logger.error("Error procesando gift: {}", e.getMessage(), e);
                    }
                })

                // ── GIFT COMBO (durante streak, solo log) ──
                .onGiftCombo((liveClient, event) -> {
                    logger.debug("🎁 Combo: {} x{}", event.getGift().getName(), event.getCombo());
                })

                // ── FOLLOW ──
                .onFollow((liveClient, event) -> {
                    try {
                        String uniqueId = event.getUser().getName();
                        String nickname = event.getUser().getProfileName();
                        logger.info("👤 {} hizo follow! → +{}☀️", nickname, SUN_BONUS_FOLLOW);
                        gameController.onLikeBonus(SUN_BONUS_FOLLOW, uniqueId);
                    } catch (Exception e) {
                        logger.error("Error en follow: {}", e.getMessage());
                    }
                })

                // ── SHARE ──
                .onShare((liveClient, event) -> {
                    try {
                        String uniqueId = event.getUser().getName();
                        String nickname = event.getUser().getProfileName();
                        logger.info("📢 {} compartió! → +{}☀️", nickname, SUN_BONUS_SHARE);
                        gameController.onLikeBonus(SUN_BONUS_SHARE, uniqueId);
                    } catch (Exception e) {
                        logger.error("Error en share: {}", e.getMessage());
                    }
                })

                // ── ROOM INFO ──
                .onRoomInfo((liveClient, event) -> {
                    logger.trace("👥 Viewers: {} | ❤️ Likes: {}",
                            event.getRoomInfo().getViewersCount(),
                            event.getRoomInfo().getLikesCount());
                })

                // ── JOIN ──
                .onJoin((liveClient, event) -> {
                    logger.trace("👋 {} entró al stream", event.getUser().getProfileName());
                })

                // ── ERROR ──
                .onError((liveClient, event) -> {
                    logger.error("❌ Error TikTok: {}", event.getException().getMessage());
                })

                .buildAndConnect();

        logger.info("✅ Cliente TikTok iniciado");
    }

    // ═══════════════════════════════════════════════════════════
    // CHAT HANDLER (registro + acciones gratis)
    // ═══════════════════════════════════════════════════════════

    private void handleComment(io.github.jwdeveloper.tiktok.data.events.TikTokCommentEvent event) {
        String uniqueId = event.getUser().getName();
        String nickname = event.getUser().getProfileName();
        String comment = event.getText();

        if (uniqueId == null || comment == null) return;

        // Cachear mensaje (para vincular posición con gift)
        lastMessages.put(uniqueId, new CachedMessage(comment, System.currentTimeMillis()));

        // Intentar registro de equipo
        String regResult = teamManager.tryRegister(uniqueId, nickname, comment);
        if (regResult != null) {
            logger.info("💬 {}", regResult);
            return;
        }

        // ── ACCIÓN GRATIS por chat ──
        TeamManager.Team team = teamManager.getTeam(uniqueId);
        if (team == null) {
            logger.debug("💬 {} (@{}): {}", nickname, uniqueId, comment);
            return;
        }

        if (team == TeamManager.Team.PLANTAS) {
            // Plantas necesitan posición completa: "A3"
            PositionParser.Position pos = PositionParser.parse(comment);
            if (pos == null) {
                logger.debug("💬 {} (@{}): {}", nickname, uniqueId, comment);
                return;
            }

            if (!checkGlobalCooldown(TeamManager.Team.PLANTAS)) {
                logger.debug("⏳ Cooldown Plantas ({})", nickname);
                return;
            }

            logger.info("🌱🆓 {} → lanzaguisantes GRATIS en {}", nickname, pos);
            ActionResult result = gameController.onTeamAAction(FREE_PLANT_TYPE, pos.row(), pos.col(), uniqueId);

            if (result.success()) {
                markGlobalCooldown(TeamManager.Team.PLANTAS);
                teamManager.incrementActions(uniqueId);
                logger.info("✅ {}", result.message());
            } else {
                logger.warn("⚠️ {}", result.message());
            }

        } else {
            // Zombies solo necesitan fila: "A", "B", "C", "D", "E", "F"
            // También acepta "A3" pero ignora la columna (siempre entran por la derecha)
            String trimmed = comment.trim().toUpperCase();
            int rowIndex = -1;

            if (trimmed.length() == 1 && trimmed.charAt(0) >= 'A' && trimmed.charAt(0) <= 'F') {
                rowIndex = trimmed.charAt(0) - 'A';
            } else {
                PositionParser.Position pos = PositionParser.parse(comment);
                if (pos != null) {
                    rowIndex = pos.rowIndex();
                }
            }

            if (rowIndex < 0) {
                logger.debug("💬 {} (@{}): {}", nickname, uniqueId, comment);
                return;
            }

            if (!checkGlobalCooldown(TeamManager.Team.ZOMBIES)) {
                logger.debug("⏳ Cooldown Zombies ({})", nickname);
                return;
            }

            char rowLetter = (char) ('A' + rowIndex);
            logger.info("🧟🆓 {} → zombie normal GRATIS en fila {} (entrada derecha)", nickname, rowLetter);
            ActionResult result = gameController.onTeamBAction(FREE_ZOMBIE_TYPE, 1, String.valueOf(rowIndex), uniqueId);

            if (result.success()) {
                markGlobalCooldown(TeamManager.Team.ZOMBIES);
                teamManager.incrementActions(uniqueId);
                logger.info("✅ {}", result.message());
            } else {
                logger.warn("⚠️ {}", result.message());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GIFT HANDLER (acciones premium)
    // ═══════════════════════════════════════════════════════════

    private void handleGift(io.github.jwdeveloper.tiktok.data.events.gift.TikTokGiftEvent event) {
        var gift = event.getGift();
        var user = event.getUser();

        String uniqueId = user.getName();
        String nickname = user.getProfileName();
        String giftName = gift.getName();
        int giftId = gift.getId();
        int diamondCost = gift.getDiamondCost();
        int combo = event.getCombo();

        logger.info("🎁 {} envió {} x{} (id:{}, {}💎)",
                nickname, giftName, Math.max(combo, 1), giftId, diamondCost);

        // Verificar equipo
        TeamManager.Team team = teamManager.getTeam(uniqueId);
        if (team == null) {
            logger.info("⚠️ {} no está en ningún equipo. Escribir 'plantas' o 'zombies'", nickname);
            return;
        }

        // Cooldown individual
        if (!checkCooldown(uniqueId)) {
            logger.debug("⏳ {} en cooldown individual", nickname);
            return;
        }

        // Mapear gift → acción
        String actionType = GiftMapper.resolve(giftId, diamondCost, team);
        if (actionType == null) {
            logger.warn("❓ Gift '{}' (id:{}) sin mapeo", giftName, giftId);
            return;
        }

        // Buscar posición del mensaje cacheado
        PositionParser.Position position = resolvePosition(uniqueId);

        // Ejecutar según equipo
        ActionResult result;

        if (team == TeamManager.Team.PLANTAS) {
            if (position == null) {
                logger.info("🌱 {} envió {} pero no indicó posición. Escribir ej: 'A1'", nickname, giftName);
                return;
            }
            logger.info("🌱🎁 {} → {} en {} (gift: {})", nickname, actionType, position, giftName);
            result = gameController.onTeamAAction(actionType, position.row(), position.col(), uniqueId);
        } else {
            String row = position != null ? String.valueOf(position.rowIndex()) : null;
            int count = Math.min(Math.max(combo, 1), 5);
            logger.info("🧟🎁 {} → {} x{} (gift: {}){}", nickname, actionType, count, giftName,
                    position != null ? " fila " + (char) ('A' + position.rowIndex()) : " (fila aleatoria)");
            result = gameController.onTeamBAction(actionType, count, row, uniqueId);
        }

        if (result.success()) {
            markCooldown(uniqueId);
            teamManager.incrementActions(uniqueId);
            logger.info("✅ {}", result.message());
        } else {
            logger.warn("⚠️ {}", result.message());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // POSICIÓN (cache de mensajes)
    // ═══════════════════════════════════════════════════════════

    private PositionParser.Position resolvePosition(String uniqueId) {
        CachedMessage cached = lastMessages.get(uniqueId);
        if (cached == null) return null;

        if (System.currentTimeMillis() - cached.timestamp > MSG_CACHE_TTL_MS) {
            lastMessages.remove(uniqueId);
            return null;
        }

        return PositionParser.parse(cached.text);
    }

    // ═══════════════════════════════════════════════════════════
    // COOLDOWN INDIVIDUAL (gifts)
    // ═══════════════════════════════════════════════════════════

    private boolean checkCooldown(String uniqueId) {
        Long last = cooldowns.get(uniqueId);
        return last == null || System.currentTimeMillis() - last >= ACTION_COOLDOWN_MS;
    }

    private void markCooldown(String uniqueId) {
        cooldowns.put(uniqueId, System.currentTimeMillis());
    }

    // ═══════════════════════════════════════════════════════════
    // COOLDOWN GLOBAL POR EQUIPO (acciones gratis)
    // ═══════════════════════════════════════════════════════════

    private boolean checkGlobalCooldown(TeamManager.Team team) {
        long last = team == TeamManager.Team.PLANTAS ? lastPlantasAction : lastZombiesAction;
        return System.currentTimeMillis() - last >= GLOBAL_COOLDOWN_MS;
    }

    private void markGlobalCooldown(TeamManager.Team team) {
        if (team == TeamManager.Team.PLANTAS) {
            lastPlantasAction = System.currentTimeMillis();
        } else {
            lastZombiesAction = System.currentTimeMillis();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // EVENTO DE LIKES (cada 5 minutos)
    // ═══════════════════════════════════════════════════════════

    private void startLikeFlushTimer() {
        Thread thread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(LIKE_FLUSH_MS);
                    flushLikes();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "like-flush");
        thread.setDaemon(true);
        thread.start();
    }

    private void flushLikes() {
        int likes = pendingLikes.getAndSet(0);
        if (likes < MIN_LIKES_FOR_EVENT) return;

        logger.info("═══════════════════════════════════════════");
        logger.info("🎉 ¡EVENTO DE LIKES! {} likes acumulados", likes);
        logger.info("═══════════════════════════════════════════");

        int maxRows = 5;
        boolean isPlantEvent = rng.nextBoolean();

        if (isPlantEvent) {
            String plant = RANDOM_PLANTS[rng.nextInt(RANDOM_PLANTS.length)];
            int plantRow = rng.nextInt(maxRows);
            int plantCol = rng.nextInt(7) + 1;
            String rowLetter = String.valueOf((char) ('A' + plantRow));

            logger.info("🌱 Evento → {} en {}{}", plant, rowLetter, plantCol);
            ActionResult result = gameController.onTeamAAction(plant, rowLetter, plantCol, "likes_event");

            if (result.success()) {
                logger.info("✅ {}", result.message());
            } else {
                logger.warn("⚠️ {}", result.message());
            }

        } else {
            String zombie = RANDOM_ZOMBIES[rng.nextInt(RANDOM_ZOMBIES.length)];
            int zombieRow = rng.nextInt(maxRows);
            char zombieRowLetter = (char) ('A' + zombieRow);

            String emoji = switch (zombie) {
                case "dancer" -> "🕺💀";
                case "buckethead" -> "🪣🧟";
                case "conehead" -> "🔺🧟";
                default -> "🧟";
            };

            logger.info("{} Evento → {} en fila {} (entrada derecha)", emoji, zombie, zombieRowLetter);
            ActionResult result = gameController.onTeamBAction(zombie, 1, String.valueOf(zombieRow), "likes_event");

            if (result.success()) {
                logger.info("✅ {}", result.message());
            } else {
                logger.warn("⚠️ {}", result.message());
            }
        }

        logger.info("═══════════════════════════════════════════");
    }

    // ═══════════════════════════════════════════════════════════
    // CONTROL
    // ═══════════════════════════════════════════════════════════

    public void stop() {
        running = false;
        flushLikes();
        if (client != null) {
            client.disconnect();
        }
        logger.info("TikTok service detenido");
    }

    public boolean isRunning() {
        return running;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }
}