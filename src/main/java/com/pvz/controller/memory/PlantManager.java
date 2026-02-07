package com.pvz.controller.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PlantManager - Gestiona la colocación de plantas en PvZ
 *
 * Basado en el código de PvZ Toolkit (pvz.cpp)
 */
public class PlantManager {

    private static final Logger logger = LoggerFactory.getLogger(PlantManager.class);

    private final PvZTrainer trainer;
    private final CodeInjector codeInjector;

    public PlantManager(PvZTrainer trainer, CodeInjector codeInjector) {
        this.trainer = trainer;
        this.codeInjector = codeInjector;
    }

    // ═══════════════════════════════════════════════════════════════════
    // PLANT PLACEMENT
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Planta una planta en la posición especificada
     *
     * @param plantName Nombre o alias de la planta
     * @param row Fila (letra A-F o número 0-5)
     * @param col Columna (1-9)
     * @return PlantResult con el resultado
     */
    public PlantResult plant(String plantName, String row, int col) {
        // Validar conexión
        if (!trainer.isConnected()) {
            return PlantResult.failure("No conectado al juego");
        }

        if (!trainer.isCodeInjectionReady()) {
            return PlantResult.failure("Code injection no disponible");
        }

        // Obtener ID de la planta
        int plantId = PvZOffsets.getPlantTypeId(plantName);
        if (plantId < 0 || plantId > 47) {
            return PlantResult.failure("Planta no reconocida: " + plantName + ". Usa `!plants` para ver la lista.");
        }

        // Convertir fila
        int rowIndex = parseRow(row);
        if (rowIndex < 0) {
            return PlantResult.failure("Fila inválida: " + row + " (usa A-F o 0-5)");
        }

        // Validar fila según escena
        int maxRows = trainer.getRowCount();
        if (rowIndex >= maxRows) {
            return PlantResult.failure("Fila " + row + " no existe (máx: " + maxRows + ")");
        }

        // Convertir columna (de 1-based a 0-based)
        int colIndex = col - 1;

        // Validar columna (Cob Cannon necesita 2 columnas)
        int maxCol = (plantId == 46) ? 7 : 8;
        if (colIndex < 0 || colIndex > maxCol) {
            return PlantResult.failure("Columna inválida: " + col + " (usa 1-" + (maxCol + 1) + ")");
        }

        // Info para el log
        String plantDisplayName = PvZOffsets.getPlantName(plantId);
        String plantEmoji = PvZOffsets.getPlantEmoji(plantId);
        char rowLetter = PvZOffsets.rowIndexToLetter(rowIndex);

        logger.info("Plantando {} {} en {}{}", plantEmoji, plantDisplayName, rowLetter, col);

        // Ejecutar plantación
        try {
            boolean success = codeInjector.plantPlant(plantId, rowIndex, colIndex);

            if (success) {
                String msg = String.format("%s %s plantado en %s%d",
                        plantEmoji, plantDisplayName, rowLetter, col);
                return PlantResult.success(msg);
            } else {
                return PlantResult.failure("Error al plantar (code injection falló)");
            }

        } catch (Exception e) {
            logger.error("Excepción al plantar: {}", e.getMessage(), e);
            return PlantResult.failure("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // PLANT MENU
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Genera el menú de plantas disponibles
     * Los aliases aquí deben coincidir EXACTAMENTE con PvZOffsets
     */
    public String getPlantMenu() {
        return PvZOffsets.getPlantMenu();
    }

    /**
     * Genera grid visual del nivel
     */
    public String getPlantingGrid() {
        StringBuilder sb = new StringBuilder();

        int rows = trainer.getRowCount();
        int scene = trainer.getScene();

        sb.append("```\n");
        sb.append("    1   2   3   4   5   6   7   8   9\n");
        sb.append("  ┌───┬───┬───┬───┬───┬───┬───┬───┬───┐\n");

        for (int r = 0; r < rows; r++) {
            char rowLetter = (char) ('A' + r);
            sb.append(rowLetter).append(" │");

            for (int c = 0; c < 9; c++) {
                String cell = " · ";
                // Filas de agua en piscina/niebla
                if ((scene == 2 || scene == 3) && (r == 2 || r == 3)) {
                    cell = " ~ ";
                }
                sb.append(cell).append("│");
            }
            sb.append("\n");

            if (r < rows - 1) {
                sb.append("  ├───┼───┼───┼───┼───┼───┼───┼───┼───┤\n");
            }
        }

        sb.append("  └───┴───┴───┴───┴───┴───┴───┴───┴───┘\n");
        sb.append("```\n");
        sb.append("` · ` = tierra | ` ~ ` = agua");

        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Parsea la fila (letra A-F o número 0-5)
     */
    private int parseRow(String row) {
        if (row == null || row.isEmpty()) {
            return -1;
        }

        // Intentar como letra
        if (row.length() == 1 && Character.isLetter(row.charAt(0))) {
            return PvZOffsets.rowLetterToIndex(row.charAt(0));
        }

        // Intentar como número
        try {
            int rowNum = Integer.parseInt(row);
            if (rowNum >= 0 && rowNum <= 5) {
                return rowNum;
            }
            // Si pone 1-6, convertir a 0-5
            if (rowNum >= 1 && rowNum <= 6) {
                return rowNum - 1;
            }
        } catch (NumberFormatException e) {
            // No es número
        }

        return -1;
    }

    // ═══════════════════════════════════════════════════════════════════
    // RESULT CLASS
    // ═══════════════════════════════════════════════════════════

    public static class PlantResult {
        private final boolean success;
        private final String message;

        private PlantResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static PlantResult success(String message) {
            return new PlantResult(true, message);
        }

        public static PlantResult failure(String message) {
            return new PlantResult(false, message);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}