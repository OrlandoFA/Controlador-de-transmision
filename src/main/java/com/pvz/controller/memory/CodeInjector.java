package com.pvz.controller.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Code Injector para Plants vs Zombies 1.2.0.1096 GOTY Steam
 */
public class CodeInjector {

    private static final Logger logger = LoggerFactory.getLogger(CodeInjector.class);

    private static final int LAWN_ADDRESS = 0x731C50;
    private static final int BOARD_OFFSET = 0x868;
    private static final int CHALLENGE_OFFSET = 0x178;
    private static final int CALL_PUT_ZOMBIE = 0x0042DCE0;
    private static final int CALL_PUT_PLANT = 0x004105A0;

    private static final int BLOCK_MAIN_LOOP_ADDR = 0x005DD25E;
    private static final byte BLOCK_MAIN_LOOP_ON = (byte) 0xFE;
    private static final byte BLOCK_MAIN_LOOP_OFF = (byte) 0xC8;

    private final ProcessMemory memory;

    public CodeInjector(ProcessMemory memory) {
        this.memory = memory;
    }

    private void blockMainLoop(boolean block) {
        byte value = block ? BLOCK_MAIN_LOOP_ON : BLOCK_MAIN_LOOP_OFF;
        memory.writeBytes(BLOCK_MAIN_LOOP_ADDR, new byte[]{value});
        logger.debug("Main loop {}", block ? "BLOCKED" : "UNBLOCKED");
    }

    private boolean safeExecuteShellcode(byte[] shellcode) {
        try {
            blockMainLoop(true);
            Thread.sleep(20);
            boolean success = executeShellcode(shellcode);
            blockMainLoop(false);
            return success;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            blockMainLoop(false);
            return false;
        } catch (Exception e) {
            logger.error("Error in safe execute: {}", e.getMessage());
            blockMainLoop(false);
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ZOMBIE SPAWN
    // ═══════════════════════════════════════════════════════════════════

    public boolean spawnZombie(int zombieType, int row, int col) {
        logger.info("SpawnZombie: type={}, row={}, col={}", zombieType, row, col);
        try {
            byte[] shellcode = buildPutZombieShellcode(zombieType, row, col);
            return safeExecuteShellcode(shellcode);
        } catch (Exception e) {
            logger.error("Failed to spawn zombie: {}", e.getMessage(), e);
            return false;
        }
    }

    private byte[] buildPutZombieShellcode(int zombieType, int row, int col) throws IOException {
        ByteArrayOutputStream code = new ByteArrayOutputStream();

        code.write(0x60); // pushad

        code.write(0x68); // push col
        writeInt32LE(code, col);

        code.write(0x68); // push zombieType
        writeInt32LE(code, zombieType);

        code.write(0xB8); // mov eax, row
        writeInt32LE(code, row);

        code.write(0x8B); code.write(0x0D); // mov ecx, [lawn]
        writeInt32LE(code, LAWN_ADDRESS);

        code.write(0x8B); code.write(0x89); // mov ecx, [ecx+board]
        writeInt32LE(code, BOARD_OFFSET);

        code.write(0x8B); code.write(0x89); // mov ecx, [ecx+challenge]
        writeInt32LE(code, CHALLENGE_OFFSET);

        code.write(0xBA); // mov edx, call_put_zombie
        writeInt32LE(code, CALL_PUT_ZOMBIE);

        code.write(0xFF); code.write(0xD2); // call edx
        code.write(0x61); // popad
        code.write(0xC3); // ret

        return code.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════════
    // PLANT PLACEMENT
    // ═══════════════════════════════════════════════════════════════════

    public boolean plantPlant(int plantType, int row, int col, boolean imitater) {
        logger.info("PlantPlant: type={}, row={}, col={}, imitater={}", plantType, row, col, imitater);
        try {
            byte[] shellcode = buildPutPlantShellcode(plantType, row, col, imitater);
            return safeExecuteShellcode(shellcode);
        } catch (Exception e) {
            logger.error("Failed to plant: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean plantPlant(int plantType, int row, int col) {
        return plantPlant(plantType, row, col, false);
    }

    private byte[] buildPutPlantShellcode(int plantType, int row, int col, boolean imitater) throws IOException {
        ByteArrayOutputStream code = new ByteArrayOutputStream();

        code.write(0x60); // pushad

        if (imitater) {
            code.write(0x68);
            writeInt32LE(code, plantType);
            code.write(0x68);
            writeInt32LE(code, 48);
        } else {
            code.write(0x68); // push -1
            writeInt32LE(code, -1);
            code.write(0x68); // push plantType
            writeInt32LE(code, plantType);
        }

        code.write(0xB8); // mov eax, row
        writeInt32LE(code, row);

        code.write(0x68); // push col
        writeInt32LE(code, col);

        code.write(0x8B); code.write(0x2D); // mov ebp, [lawn]
        writeInt32LE(code, LAWN_ADDRESS);

        code.write(0x8B); code.write(0xAD); // mov ebp, [ebp+board]
        writeInt32LE(code, BOARD_OFFSET);

        code.write(0x55); // push ebp

        code.write(0xBA); // mov edx, call_put_plant
        writeInt32LE(code, CALL_PUT_PLANT);

        code.write(0xFF); code.write(0xD2); // call edx
        code.write(0x61); // popad
        code.write(0xC3); // ret

        byte[] result = code.toByteArray();
        logger.debug("Plant shellcode built: {} bytes", result.length);
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════

    private boolean executeShellcode(byte[] shellcode) {
        long codeAddress = memory.allocateMemory(shellcode.length + 16, true);
        if (codeAddress == 0) {
            logger.error("Failed to allocate memory for shellcode");
            return false;
        }

        try {
            if (!memory.writeBytes(codeAddress, shellcode)) {
                logger.error("Failed to write shellcode");
                return false;
            }

            boolean success = memory.executeRemoteThread(codeAddress, 0, 5000);
            if (success) {
                logger.info("Shellcode executed successfully");
            } else {
                logger.error("Shellcode execution failed or timed out");
            }
            return success;

        } finally {
            memory.freeMemory(codeAddress);
        }
    }

    private void writeInt32LE(ByteArrayOutputStream stream, int value) {
        stream.write(value & 0xFF);
        stream.write((value >> 8) & 0xFF);
        stream.write((value >> 16) & 0xFF);
        stream.write((value >> 24) & 0xFF);
    }

    public boolean testInjection() {
        logger.info("Testing code injection...");
        byte[] testCode = {
                (byte) 0x60, (byte) 0x90, (byte) 0x90, (byte) 0x61, (byte) 0xC3
        };

        long codeAddress = memory.allocateMemory(16, true);
        if (codeAddress == 0) {
            logger.error("Test failed: Could not allocate memory");
            return false;
        }

        try {
            if (!memory.writeBytes(codeAddress, testCode)) {
                logger.error("Test failed: Could not write test code");
                return false;
            }

            boolean success = memory.executeRemoteThread(codeAddress, 0, 1000);
            logger.info("Code injection test {}", success ? "PASSED" : "FAILED");
            return success;

        } finally {
            memory.freeMemory(codeAddress);
        }
    }
}
