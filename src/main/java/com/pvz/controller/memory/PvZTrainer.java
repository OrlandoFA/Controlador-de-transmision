package com.pvz.controller.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * PvZ Trainer para Plants vs Zombies 1.2.0.1096 GOTY Steam
 *
 * Funcionalidades:
 * - Modificar sol
 * - Pausar/Resumir juego
 * - Spawn de zombies via code injection
 * - Plantar plantas via code injection
 * - Detección de escena y filas válidas
 */
public class PvZTrainer {

    private static final Logger logger = LoggerFactory.getLogger(PvZTrainer.class);

    private ProcessMemory memory;
    private CodeInjector codeInjector;
    private long baseAddress;
    private long gameBase;
    private long boardAddress;
    private boolean connected;
    private boolean codeInjectionTested;

    public PvZTrainer() {
        this.connected = false;
        this.codeInjectionTested = false;
    }

    public boolean connect() {
        int pid = PvZProcess.findProcessId();

        if (pid == -1) {
            logger.error("PvZ is not running!");
            return false;
        }

        try {
            memory = new ProcessMemory(pid);
            codeInjector = new CodeInjector(memory);

            boolean found = false;
            for (long tryBase : PvZOffsets.BASE_ADDRESSES) {
                logger.info("Trying base address: 0x{}", Long.toHexString(tryBase));

                long gamePtr = memory.readPointer(tryBase);

                if (gamePtr != 0 && gamePtr > 0x10000 && gamePtr < 0x7FFFFFFF) {
                    long boardPtr = memory.readPointer(gamePtr + PvZOffsets.BOARD);

                    logger.info("  Game pointer: 0x{}, Board pointer: 0x{}",
                            Long.toHexString(gamePtr), Long.toHexString(boardPtr));

                    if (boardPtr > 0x10000 && boardPtr < 0x7FFFFFFF) {
                        int sun = memory.readInt(boardPtr + PvZOffsets.SUN_COUNT);

                        if (sun >= 0 && sun < 100000) {
                            baseAddress = tryBase;
                            gameBase = gamePtr;
                            boardAddress = boardPtr;
                            found = true;
                            logger.info("SUCCESS! Found valid base at 0x{}", Long.toHexString(tryBase));
                            logger.info("  Sun value: {}", sun);
                            break;
                        }
                    }
                }
            }

            if (!found) {
                logger.error("Could not find valid game base address");
                logger.error("Make sure you are IN A LEVEL (not main menu)");
                return false;
            }

            connected = true;
            logger.info("Trainer connected successfully!");

            // Log scene info
            int scene = getScene();
            int rowCount = getRowCount();
            logger.info("Scene: {} ({}), Rows: {}", PvZOffsets.getSceneName(scene), scene, rowCount);

            // Test code injection capability
            if (!codeInjectionTested) {
                logger.info("Testing code injection capability...");
                codeInjectionTested = codeInjector.testInjection();
                if (!codeInjectionTested) {
                    logger.warn("Code injection test failed - spawn may not work");
                }
            }

            return true;

        } catch (Exception e) {
            logger.error("Failed to connect: {}", e.getMessage(), e);
            return false;
        }
    }

    public void disconnect() {
        if (memory != null) {
            memory.close();
        }
        connected = false;
        codeInjectionTested = false;
        logger.info("Trainer disconnected");
    }

    public boolean isConnected() {
        return connected && memory != null && memory.isValid();
    }

    private boolean refreshBoardAddress() {
        if (!isConnected()) return false;

        gameBase = memory.readPointer(baseAddress);
        if (gameBase == 0) return false;

        boardAddress = memory.readPointer(gameBase + PvZOffsets.BOARD);
        return boardAddress != 0;
    }

    // ==================== SCENE & ROW DETECTION ====================

    /**
     * Obtiene el tipo de escena actual
     * 0=Day, 1=Night, 2=Pool, 3=Fog, 4=Roof, 5=Moon
     */
    public int getScene() {
        if (!refreshBoardAddress()) return -1;
        return memory.readInt(boardAddress + PvZOffsets.SCENE);
    }

    /**
     * Obtiene el nombre de la escena actual
     */
    public String getSceneName() {
        int scene = getScene();
        return PvZOffsets.getSceneName(scene);
    }

    /**
     * Obtiene el número total de filas del nivel actual
     */
    public int getRowCount() {
        int scene = getScene();
        if (scene < 0) return 5;
        return PvZOffsets.getRowCountForScene(scene);
    }

    /**
     * Obtiene el tipo de cada fila
     * 0=Normal, 1=Pool, 2=High (techo)
     */
    public int[] getRowTypes() {
        if (!refreshBoardAddress()) return new int[]{0, 0, 0, 0, 0};

        int rowCount = getRowCount();
        int[] rowTypes = new int[rowCount];

        for (int i = 0; i < rowCount; i++) {
            rowTypes[i] = memory.readInt(boardAddress + PvZOffsets.ROW_TYPE + (i * 4));
        }

        return rowTypes;
    }

    /**
     * Obtiene las filas activas según el nivel de aventura (tutorial)
     * Los primeros niveles tienen filas limitadas
     */
    private List<Integer> getActiveRowsForLevel() {
        int level = getAdventureLevel();
        int scene = getScene();
        int maxRows = getRowCount();

        List<Integer> activeRows = new ArrayList<>();

        // Niveles de tutorial (1-1 a 1-3) tienen filas limitadas
        if (level == 1) {
            // Nivel 1-1: Solo fila 2 (la del medio con pasto)
            activeRows.add(2);
        } else if (level == 2 || level == 3) {
            // Nivel 1-2 y 1-3: Filas 1, 2, 3
            activeRows.add(1);
            activeRows.add(2);
            activeRows.add(3);
        } else {
            // Nivel 1-4 en adelante: Todas las filas disponibles
            for (int i = 0; i < maxRows; i++) {
                activeRows.add(i);
            }
        }

        logger.debug("Adventure level: {}, Active rows: {}", level, activeRows);
        return activeRows;
    }

    /**
     * Obtiene las filas válidas para spawnear zombies terrestres
     * (excluye filas de agua en niveles de piscina Y respeta niveles de tutorial)
     */
    public List<Integer> getValidLandRows() {
        List<Integer> validRows = new ArrayList<>();
        int[] rowTypes = getRowTypes();
        List<Integer> activeRows = getActiveRowsForLevel();

        for (int i = 0; i < rowTypes.length; i++) {
            // Verificar que la fila esté activa para este nivel
            if (!activeRows.contains(i)) {
                continue;
            }

            // ROW_NORMAL (0) = tierra/pasto, válido para zombies terrestres
            // ROW_HIGH (2) = techo, también válido
            if (rowTypes[i] == PvZOffsets.ROW_NORMAL || rowTypes[i] == PvZOffsets.ROW_HIGH) {
                validRows.add(i);
            }
        }

        // Si no encontró filas válidas por tipo, usar las filas activas directamente
        if (validRows.isEmpty()) {
            validRows.addAll(activeRows);
        }

        return validRows;
    }

    /**
     * Obtiene las filas de agua (para zombies acuáticos)
     * Solo retorna filas si estamos en un nivel con piscina
     */
    public List<Integer> getPoolRows() {
        List<Integer> poolRows = new ArrayList<>();

        // Solo los niveles de piscina/niebla tienen agua
        if (!hasPool()) {
            return poolRows;  // Lista vacía si no hay piscina
        }

        int[] rowTypes = getRowTypes();
        List<Integer> activeRows = getActiveRowsForLevel();

        for (int i = 0; i < rowTypes.length; i++) {
            // Verificar que la fila esté activa para este nivel
            if (!activeRows.contains(i)) {
                continue;
            }

            if (rowTypes[i] == PvZOffsets.ROW_POOL) {
                poolRows.add(i);
            }
        }

        return poolRows;
    }

    /**
     * Verifica si el nivel actual tiene piscina
     */
    public boolean hasPool() {
        int scene = getScene();
        return PvZOffsets.hasPool(scene);
    }

    /**
     * Obtiene el nivel de aventura actual (1-50)
     */
    public int getAdventureLevel() {
        if (!refreshBoardAddress()) return -1;
        return memory.readInt(boardAddress + PvZOffsets.ADVENTURE_LEVEL);
    }

    // ==================== SUN FUNCTIONS ====================

    public int getSun() {
        if (!refreshBoardAddress()) {
            logger.warn("Not in a level");
            return -1;
        }
        return memory.readInt(boardAddress + PvZOffsets.SUN_COUNT);
    }

    /**
     * Redondea un valor al múltiplo de 25 más cercano
     * El sol en PvZ siempre va en incrementos de 25
     */
    private int roundToSunIncrement(int value) {
        if (value <= 0) return 0;
        return ((value + 12) / 25) * 25;  // Redondea al múltiplo de 25 más cercano
    }

    public boolean setSun(int amount) {
        if (!refreshBoardAddress()) {
            logger.warn("Not in a level");
            return false;
        }

        // Redondear al múltiplo de 25 más cercano
        int validAmount = roundToSunIncrement(amount);

        // Limitar entre 0 y 9999
        if (validAmount < 0) validAmount = 0;
        if (validAmount > 9999) validAmount = 9999;

        boolean success = memory.writeInt(boardAddress + PvZOffsets.SUN_COUNT, validAmount);
        if (success) {
            logger.info("Sun set to: {} (requested: {})", validAmount, amount);
        }
        return success;
    }

    public boolean addSun(int amount) {
        int current = getSun();
        if (current == -1) return false;

        // Calcular nuevo valor y redondear
        int newAmount = roundToSunIncrement(current + amount);
        return setSun(newAmount);
    }

    // ==================== GAME STATE ====================

    public boolean isGamePaused() {
        if (!refreshBoardAddress()) return false;
        return memory.readInt(boardAddress + PvZOffsets.GAME_PAUSED) != 0;
    }

    public boolean pauseGame() {
        if (!refreshBoardAddress()) {
            logger.warn("Not in a level");
            return false;
        }
        boolean success = memory.writeInt(boardAddress + PvZOffsets.GAME_PAUSED, 1);
        if (success) {
            logger.info("Game paused");
        }
        return success;
    }

    public boolean resumeGame() {
        if (!refreshBoardAddress()) {
            logger.warn("Not in a level");
            return false;
        }
        boolean success = memory.writeInt(boardAddress + PvZOffsets.GAME_PAUSED, 0);
        if (success) {
            logger.info("Game resumed");
        }
        return success;
    }

    public int getZombieCount() {
        if (!refreshBoardAddress()) return -1;
        return memory.readInt(boardAddress + PvZOffsets.ZOMBIE_COUNT);
    }

    // ==================== ZOMBIE SPAWN ====================

    /**
     * Verifica si un tipo de zombie es acuático
     */
    private boolean isAquaticZombie(int zombieType) {
        // Ducky Tube (10), Snorkel (11), Dolphin Rider (14)
        return zombieType == 10 || zombieType == 11 || zombieType == 14;
    }

    /**
     * Spawn zombies usando code injection con validación de filas
     *
     * @param type Tipo de zombie (nombre o ID)
     * @param count Cantidad a spawnear
     * @param row Fila específica (0-5), o -1 para aleatorio en filas válidas
     * @return true si al menos un zombie fue spawneado
     */
    public boolean spawnZombie(String type, int count, int row) {
        if (!refreshBoardAddress()) {
            logger.warn("Not in a level - cannot spawn zombies");
            return false;
        }

        if (!codeInjectionTested) {
            logger.error("Code injection not available");
            return false;
        }

        int zombieType = PvZOffsets.getZombieTypeId(type);
        String zombieName = PvZOffsets.getZombieName(zombieType);
        boolean isAquatic = isAquaticZombie(zombieType);

        // Get valid rows based on zombie type
        List<Integer> validRows;
        if (isAquatic && hasPool()) {
            validRows = getPoolRows();
            if (validRows.isEmpty()) {
                // Fallback to land if no pool (shouldn't happen)
                validRows = getValidLandRows();
            }
        } else {
            validRows = getValidLandRows();
        }

        if (validRows.isEmpty()) {
            logger.error("No valid rows available for spawning");
            return false;
        }

        logger.info("Spawning {} x{} (type ID: {})", zombieName, count, zombieType);
        logger.info("Valid rows: {}, Scene: {}", validRows, getSceneName());

        try {
            int successCount = 0;

            for (int i = 0; i < count; i++) {
                if (!refreshBoardAddress()) {
                    logger.error("Lost connection to game");
                    break;
                }

                // Determine row
                int spawnRow;
                if (row >= 0 && validRows.contains(row)) {
                    spawnRow = row;
                } else {
                    // Random row from valid rows
                    spawnRow = validRows.get((int) (Math.random() * validRows.size()));
                }

                // Column 9 = right side of screen
                int spawnCol = 9;

                boolean success = codeInjector.spawnZombie(zombieType, spawnRow, spawnCol);

                if (success) {
                    successCount++;
                    logger.info("Zombie {} spawned on row {}", i + 1, spawnRow);
                } else {
                    logger.error("Failed to spawn zombie {} of {}", i + 1, count);
                }

                if (i < count - 1) {
                    Thread.sleep(200);
                }
            }

            logger.info("Successfully spawned {} of {} {} zombie(s)", successCount, count, zombieName);
            return successCount > 0;

        } catch (Exception e) {
            logger.error("Error spawning zombies: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==================== UTILITY ====================

    public boolean isCodeInjectionReady() {
        return codeInjectionTested;
    }

    public long getBoardAddress() {
        return boardAddress;
    }

    public long getBaseAddress() {
        return baseAddress;
    }

    public CodeInjector getCodeInjector() {
        return codeInjector;
    }

    /**
     * Obtiene información completa del nivel actual en formato visual
     */
    public String getLevelInfo() {
        int sun = getSun();
        if (sun == -1) {
            return null;  // No estamos en un nivel
        }

        int scene = getScene();
        int level = getAdventureLevel();
        int zombies = getZombieCount();
        boolean paused = isGamePaused();
        List<Integer> landRows = getValidLandRows();
        List<Integer> poolRows = getPoolRows();

        StringBuilder sb = new StringBuilder();

        // Línea 1: Estado general
        sb.append("☀️ ").append(sun);
        sb.append(" | 🧟 ").append(zombies);
        sb.append(" | ").append(paused ? "⏸️ Pausado" : "▶️ Jugando");
        sb.append("\n");

        // Línea 2: Nivel y escena
        sb.append("📍 Nivel ").append(getLevelName(level));
        sb.append(" | ").append(getSceneEmoji(scene)).append(" ").append(getSceneNameSpanish(scene));
        sb.append("\n");

        // Separador
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // Filas visuales
        int totalRows = getRowCount();
        for (int row = 0; row < totalRows; row++) {
            boolean isActive = landRows.contains(row);
            boolean isPool = poolRows.contains(row);

            if (isPool) {
                sb.append("🌊 ");
            } else if (isActive) {
                sb.append("🌱 ");
            } else {
                sb.append("🚫 ");
            }

            sb.append(row + 1).append(" │");

            // Representación visual de la fila
            if (isActive || isPool) {
                sb.append("▓▓▓▓▓▓▓▓░░│");
            } else {
                sb.append("░░░░░░░░░░│ inactiva");
            }

            sb.append("\n");
        }

        // Separador final
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return sb.toString();
    }

    /**
     * Obtiene emoji para la escena
     */
    private String getSceneEmoji(int scene) {
        switch (scene) {
            case 0: return "🌅"; // Day
            case 1: return "🌙"; // Night
            case 2: return "🏊"; // Pool
            case 3: return "🌫️"; // Fog
            case 4: return "🏠"; // Roof
            case 5: return "🌑"; // Moon
            default: return "❓";
        }
    }

    /**
     * Obtiene nombre de escena en español
     */
    private String getSceneNameSpanish(int scene) {
        switch (scene) {
            case 0: return "Día";
            case 1: return "Noche";
            case 2: return "Piscina";
            case 3: return "Niebla";
            case 4: return "Techo";
            case 5: return "Luna";
            default: return "Desconocido";
        }
    }

    /**
     * Convierte el número de nivel de aventura a nombre legible
     * Niveles 1-10 = Mundo 1 (Day)
     * Niveles 11-20 = Mundo 2 (Night)
     * Niveles 21-30 = Mundo 3 (Pool)
     * Niveles 31-40 = Mundo 4 (Fog)
     * Niveles 41-50 = Mundo 5 (Roof)
     */
    private String getLevelName(int level) {
        if (level <= 0) return "???";

        int world = ((level - 1) / 10) + 1;
        int stage = ((level - 1) % 10) + 1;

        return world + "-" + stage;
    }
}