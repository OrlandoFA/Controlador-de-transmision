package com.pvz.controller.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TrainerExecutor V1.2 - Ejecuta comandos del juego via memory trainer
 * Soporta inyección de código para spawn de zombies y plantar plantas
 */
public class TrainerExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TrainerExecutor.class);
    private static PvZTrainer trainer = new PvZTrainer();
    private static PlantManager plantManager = null;

    public static ExecutionResult execute(com.pvz.controller.model.GameCommand command) {
        try {
            // Conectar si no está conectado
            if (!trainer.isConnected()) {
                logger.info("Conectando a PvZ...");
                if (!trainer.connect()) {
                    return new ExecutionResult(false, "❌ No se pudo conectar a PvZ. ¿Está el juego en un nivel?");
                }
                // Inicializar PlantManager después de conectar
                plantManager = new PlantManager(trainer, trainer.getCodeInjector());
            }

            String cmd = command.getCommand().toLowerCase();

            switch (cmd) {
                case "sun":
                    int amount = command.getCountOrDefault(0);

                    if (amount <= 0) {
                        int currentSun = trainer.getSun();
                        if (currentSun == -1) {
                            return new ExecutionResult(false, "❌ ¡No estás en un nivel!");
                        }
                        int newSun = ((currentSun + 25 + 12) / 25) * 25;
                        if (newSun > 9999) newSun = 9999;

                        boolean addSuccess = trainer.setSun(newSun);
                        return new ExecutionResult(addSuccess,
                                addSuccess ? "☀️ +25 sol! Total: " + newSun : "❌ Error al agregar sol");
                    } else {
                        int roundedAmount = ((amount + 12) / 25) * 25;
                        if (roundedAmount > 9999) roundedAmount = 9999;

                        boolean sunSuccess = trainer.setSun(roundedAmount);
                        return new ExecutionResult(sunSuccess,
                                sunSuccess ? "☀️ Sol establecido a " + roundedAmount : "❌ Error al establecer sol");
                    }

                case "pause":
                    boolean pauseSuccess = trainer.pauseGame();
                    return new ExecutionResult(pauseSuccess,
                            pauseSuccess ? "⏸️ Juego pausado" : "❌ Error al pausar");

                case "resume":
                    boolean resumeSuccess = trainer.resumeGame();
                    return new ExecutionResult(resumeSuccess,
                            resumeSuccess ? "▶️ Juego reanudado" : "❌ Error al reanudar");

                case "info":
                    String levelInfo = trainer.getLevelInfo();
                    if (levelInfo == null || levelInfo.isEmpty()) {
                        return new ExecutionResult(false, "❌ No estás en un nivel. ¡Entra a un nivel primero!");
                    }
                    return new ExecutionResult(true, levelInfo);

                case "spawn_zombie":
                    return handleSpawnZombie(command);

                case "plant":
                    return handlePlant(command);

                case "plants":
                case "plantmenu":
                    return new ExecutionResult(true, PvZOffsets.getPlantMenu());

                case "zombies":
                case "zombiemenu":
                    return new ExecutionResult(true, PvZOffsets.getZombieMenu());

                case "grid":
                    return handleGrid();

                case "wave":
                    trainer.addSun(500);
                    return new ExecutionResult(true, "🌊 ¡Bonus de oleada! +500 sol");

                case "test_injection":
                    if (!trainer.isConnected()) {
                        return new ExecutionResult(false, "❌ No conectado al juego");
                    }
                    boolean testResult = trainer.isCodeInjectionReady();
                    return new ExecutionResult(testResult,
                            testResult ? "✅ Inyección de código lista" : "❌ Inyección de código falló");

                default:
                    return new ExecutionResult(false, "❌ Comando desconocido: " + cmd);
            }

        } catch (Exception e) {
            logger.error("Error de ejecución: {}", e.getMessage(), e);
            trainer = new PvZTrainer();
            plantManager = null;
            return new ExecutionResult(false, "❌ Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ZOMBIE HANDLING
    // ═══════════════════════════════════════════════════════════════════

    private static ExecutionResult handleSpawnZombie(com.pvz.controller.model.GameCommand command) {
        String zombieType = command.getType() != null ? command.getType() : "normal";
        int count = command.getCountOrDefault(1);
        if (count <= 0) count = 1;
        if (count > 10) count = 10;

        if (!trainer.isCodeInjectionReady()) {
            return new ExecutionResult(false, "❌ Inyección no lista. Reconecta e intenta de nuevo.");
        }

        String zombieName = PvZOffsets.getZombieName(PvZOffsets.getZombieTypeId(zombieType));
        logger.info("Ejecutando spawn: {} x{}", zombieName, count);

        boolean success = trainer.spawnZombie(zombieType, count, -1);

        if (success) {
            return new ExecutionResult(true,
                    String.format("🧟 ¡%d %s invocado(s)!", count, zombieName));
        } else {
            return new ExecutionResult(false,
                    "❌ Error al invocar " + zombieName + ". Revisa la consola.");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // PLANT HANDLING
    // ═══════════════════════════════════════════════════════════════════

    private static ExecutionResult handlePlant(com.pvz.controller.model.GameCommand command) {
        if (plantManager == null) {
            return new ExecutionResult(false, "❌ PlantManager no inicializado. Reconecta.");
        }

        String plantType = command.getType();
        String row = command.getRow();
        Integer col = command.getCol();

        if (plantType == null || plantType.isEmpty()) {
            return new ExecutionResult(false, "❌ Especifica el tipo de planta. Usa `!plants` para ver opciones.");
        }

        if (row == null || row.isEmpty()) {
            return new ExecutionResult(false, "❌ Especifica la fila (A-F)");
        }

        if (col == null || col < 1 || col > 9) {
            return new ExecutionResult(false, "❌ Especifica la columna (1-9)");
        }

        PlantManager.PlantResult result = plantManager.plant(plantType, row, col);

        if (result.isSuccess()) {
            return new ExecutionResult(true, "🌱 " + result.getMessage());
        } else {
            return new ExecutionResult(false, "❌ " + result.getMessage());
        }
    }

    private static ExecutionResult handleGrid() {
        if (plantManager == null) {
            return new ExecutionResult(false, "❌ No conectado al juego");
        }
        return new ExecutionResult(true, plantManager.getPlantingGrid());
    }

    // ═══════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════

    public static void reconnect() {
        if (trainer != null) {
            trainer.disconnect();
        }
        trainer = new PvZTrainer();
        plantManager = null;
        logger.info("Trainer reiniciado - se reconectará en el siguiente comando");
    }

    public static void shutdown() {
        if (trainer != null) {
            trainer.disconnect();
        }
        plantManager = null;
    }

    // ═══════════════════════════════════════════════════════════════════
    // RESULT CLASS
    // ═══════════════════════════════════════════════════════════════════

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