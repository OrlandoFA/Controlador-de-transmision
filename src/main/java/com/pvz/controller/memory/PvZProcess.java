package com.pvz.controller.memory;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PvZProcess {

    private static final Logger logger = LoggerFactory.getLogger(PvZProcess.class);

    private static final String[] PROCESS_NAMES = {
            "popcapgame1.exe",
            "PlantsVsZombies.exe",
            "plantsvszombies.exe"
    };

    public static int findProcessId() {
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(
                Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));

        if (snapshot == WinNT.INVALID_HANDLE_VALUE) {
            logger.error("Failed to create process snapshot");
            return -1;
        }

        int[] foundPids = new int[PROCESS_NAMES.length];
        for (int i = 0; i < foundPids.length; i++) {
            foundPids[i] = -1;
        }

        try {
            Tlhelp32.PROCESSENTRY32.ByReference processEntry = new Tlhelp32.PROCESSENTRY32.ByReference();

            if (Kernel32.INSTANCE.Process32First(snapshot, processEntry)) {
                do {
                    String exeName = new String(processEntry.szExeFile).trim();
                    exeName = exeName.replace("\0", "");

                    for (int i = 0; i < PROCESS_NAMES.length; i++) {
                        if (exeName.equalsIgnoreCase(PROCESS_NAMES[i])) {
                            int pid = processEntry.th32ProcessID.intValue();
                            logger.info("Found process: {} (PID: {})", exeName, pid);
                            foundPids[i] = pid;
                        }
                    }
                } while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry));
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }

        for (int i = 0; i < foundPids.length; i++) {
            if (foundPids[i] != -1) {
                logger.info("Using process: {} (PID: {})", PROCESS_NAMES[i], foundPids[i]);
                return foundPids[i];
            }
        }

        logger.warn("PvZ process not found");
        return -1;
    }

    public static boolean isGameRunning() {
        return findProcessId() != -1;
    }
}