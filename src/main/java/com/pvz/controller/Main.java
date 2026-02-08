package com.pvz.controller;

import com.pvz.controller.config.ControllerConfig;
import com.pvz.controller.games.GameController;
import com.pvz.controller.games.pvz.PvZGameController;
import com.pvz.controller.server.HttpCommandServer;
import com.pvz.controller.tiktok.GiftMapper;
import com.pvz.controller.tiktok.TikTokService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final List<GameController> GAMES = List.of(
            new PvZGameController()
    );

    private static HttpCommandServer httpServer;
    private static TikTokService tikTokService;

    public static void main(String[] args) {
        printBanner();

        Scanner scanner = new Scanner(System.in);

        // ── Paso 1: Iniciar HTTP Server ──
        try {
            httpServer = new HttpCommandServer();
            httpServer.start();
            logger.info("✅ HTTP Server en puerto {}", ControllerConfig.getPort());
        } catch (Exception e) {
            logger.error("❌ No se pudo iniciar HTTP Server: {}", e.getMessage());
            System.exit(1);
        }

        // ── Paso 2: Elegir juego ──
        GameController game = selectGame(scanner);
        logger.info("🎮 Juego seleccionado: {}", game.getGameName());

        // ── Paso 3: Pedir usuario de TikTok ──
        System.out.print("\n📱 Usuario de TikTok (sin @): ");
        String tiktokUser = scanner.nextLine().trim();

        if (tiktokUser.startsWith("@")) {
            tiktokUser = tiktokUser.substring(1);
        }

        if (tiktokUser.isEmpty()) {
            logger.warn("⚠️ No se ingresó usuario. Modo solo HTTP (sin TikTok).");
            waitForCommands(scanner, game);
            return;
        }

        // ── Paso 4: Conectar a TikTok ──
        tikTokService = new TikTokService(tiktokUser, game);

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("👋 Cerrando...");
            if (tikTokService != null) tikTokService.stop();
            if (httpServer != null) httpServer.stop();
        }));

        // ── Abrir overlay en 2 ventanas Chrome separadas ──
        openOverlayWindows();

        // Iniciar TikTok en otro hilo
        String finalUser = tiktokUser;
        Thread tiktokThread = new Thread(() -> {
            try {
                tikTokService.start();
            } catch (Exception e) {
                logger.error("❌ Error conectando a TikTok: {}", e.getMessage());
                logger.info("💡 ¿@{} está en vivo?", finalUser);
            }
        }, "tiktok-main");
        tiktokThread.setDaemon(true);
        tiktokThread.start();

        // ── Paso 5: Consola interactiva ──
        waitForCommands(scanner, game);
    }

    private static void openOverlayWindows() {
        try {
            String baseUrl = "http://localhost:" + ControllerConfig.getPort() + "/overlay";
            String chromePath = findChrome();

            if (chromePath == null) {
                logger.warn("⚠️ Chrome no encontrado. Abre manualmente:");
                logger.info("  Ticker: {}/ticker", baseUrl);
                logger.info("  Teams:  {}/teams", baseUrl);
                return;
            }

            // Carpetas temporales separadas para que Chrome no fusione ventanas
            String tempDir = System.getProperty("java.io.tmpdir");
            String tickerProfile = tempDir + "pvz-ticker-profile";
            String teamsProfile = tempDir + "pvz-teams-profile";

            // Ventana 1: TICKER (barra de instrucciones arriba)
            new ProcessBuilder(
                    chromePath,
                    "--app=" + baseUrl + "/ticker",
                    "--user-data-dir=" + tickerProfile,
                    "--window-size=960,82",
                    "--window-position=0,0"
            ).start();

            Thread.sleep(800);

            // Ventana 2: TEAMS (equipos a la derecha)
            new ProcessBuilder(
                    chromePath,
                    "--app=" + baseUrl + "/teams",
                    "--user-data-dir=" + teamsProfile,
                    "--window-size=250,550",
                    "--window-position=710,52"
            ).start();

            logger.info("🖥️ Overlay: Ticker (arriba) + Teams (derecha)");
            logger.info("💡 Puedes minimizar ambas ventanas");

        } catch (Exception e) {
            logger.warn("⚠️ No se pudo abrir overlay: {}", e.getMessage());
        }
    }

    private static String findChrome() {
        String[] paths = {
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe"
        };

        for (String p : paths) {
            if (p != null && new java.io.File(p).exists()) return p;
        }

        try {
            Process test = new ProcessBuilder("chrome", "--version").start();
            test.waitFor();
            if (test.exitValue() == 0) return "chrome";
        } catch (Exception ignored) {}

        return null;
    }

    private static GameController selectGame(Scanner scanner) {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║     🎮 Selecciona el juego           ║");
        System.out.println("╠══════════════════════════════════════╣");

        for (int i = 0; i < GAMES.size(); i++) {
            System.out.printf("║  %d. %-33s║%n", i + 1, GAMES.get(i).getGameName());
        }

        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("Elige (número): ");

        while (true) {
            try {
                String input = scanner.nextLine().trim();
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= GAMES.size()) {
                    return GAMES.get(choice - 1);
                }
                System.out.print("Opción inválida. Elige (1-" + GAMES.size() + "): ");
            } catch (NumberFormatException e) {
                System.out.print("Ingresa un número: ");
            }
        }
    }

    private static void waitForCommands(Scanner scanner, GameController game) {
        System.out.println("\n📌 Comandos: stats | gifts | reset | status | help | exit\n");

        while (true) {
            try {
                String input = scanner.nextLine().trim().toLowerCase();

                switch (input) {
                    case "stats" -> {
                        if (tikTokService != null) {
                            System.out.println(tikTokService.getTeamManager().getStats());
                        } else {
                            System.out.println("⚠️ TikTok no conectado");
                        }
                    }
                    case "gifts" -> System.out.println(GiftMapper.getGuide());
                    case "reset" -> {
                        if (tikTokService != null) {
                            tikTokService.getTeamManager().reset();
                            System.out.println("🔄 Equipos reseteados");
                        }
                    }
                    case "status" -> System.out.println(game.getStatusInfo());
                    case "help" -> printHelp();
                    case "exit", "quit" -> {
                        System.out.println("👋 Cerrando...");
                        if (tikTokService != null) tikTokService.stop();
                        if (httpServer != null) httpServer.stop();
                        System.exit(0);
                    }
                    default -> {
                        if (!input.isEmpty()) {
                            System.out.println("❓ Comando no reconocido. Escribe 'help'");
                        }
                    }
                }
            } catch (Exception e) {
                break;
            }
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║     🎮 Game Controller + TikTok LIVE v2.0       ║");
        System.out.println("║     🌱 Team Plantas  vs  Team Zombies 🧟        ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static void printHelp() {
        System.out.println("""
                
                📌 Comandos disponibles:
                  stats  → Estadísticas de equipos
                  gifts  → Guía de mapeo de regalos
                  reset  → Resetear equipos (nuevo juego)
                  status → Estado del juego
                  help   → Esta ayuda
                  exit   → Cerrar todo
                """);
    }
}
