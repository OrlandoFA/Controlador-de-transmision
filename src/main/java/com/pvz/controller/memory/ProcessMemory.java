package com.pvz.controller.memory;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessMemory {

    private static final Logger logger = LoggerFactory.getLogger(ProcessMemory.class);
    private WinNT.HANDLE processHandle;
    private final int processId;

    public ProcessMemory(int processId) {
        this.processId = processId;
        openProcess();
    }

    private void openProcess() {
        processHandle = Kernel32Interface.INSTANCE.OpenProcess(
                Kernel32Interface.PROCESS_ALL_ACCESS,
                false,
                processId);

        if (processHandle == null) {
            logger.error("Failed to open process: {} - Make sure you run as Administrator!", processId);
            throw new RuntimeException("Cannot open process " + processId + ". Run as Administrator!");
        }
        logger.info("Process opened successfully. PID: {}", processId);
    }

    public int readInt(long address) {
        Memory buffer = new Memory(4);
        IntByReference bytesRead = new IntByReference();

        boolean success = Kernel32Interface.INSTANCE.ReadProcessMemory(
                processHandle,
                Pointer.createConstant(address),
                buffer,
                4,
                bytesRead
        );

        if (!success || bytesRead.getValue() != 4) {
            int error = com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError();
            logger.error("Failed to read memory at address: 0x{} (Error: {})",
                    Long.toHexString(address), error);
            return 0;
        }

        return buffer.getInt(0);
    }

    public boolean writeInt(long address, int value) {
        Memory buffer = new Memory(4);
        buffer.setInt(0, value);
        IntByReference bytesWritten = new IntByReference();

        boolean success = Kernel32Interface.INSTANCE.WriteProcessMemory(
                processHandle,
                Pointer.createConstant(address),
                buffer,
                4,
                bytesWritten
        );

        if (!success) {
            int error = com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError();
            logger.error("Failed to write memory at address: 0x{} (Error: {})",
                    Long.toHexString(address), error);
        }

        return success;
    }

    public long readPointer(long address) {
        return Integer.toUnsignedLong(readInt(address));
    }

    public long resolvePointerChain(long baseAddress, int... offsets) {
        long address = readPointer(baseAddress);

        for (int i = 0; i < offsets.length - 1; i++) {
            address = readPointer(address + offsets[i]);
        }

        if (offsets.length > 0) {
            address += offsets[offsets.length - 1];
        }

        return address;
    }

    public boolean writeBytes(long address, byte[] data) {
        Memory buffer = new Memory(data.length);
        buffer.write(0, data, 0, data.length);
        IntByReference bytesWritten = new IntByReference();

        boolean success = Kernel32Interface.INSTANCE.WriteProcessMemory(
                processHandle,
                Pointer.createConstant(address),
                buffer,
                data.length,
                bytesWritten
        );

        if (!success || bytesWritten.getValue() != data.length) {
            int error = com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError();
            logger.error("Failed to write {} bytes at address: 0x{} (Error: {})",
                    data.length, Long.toHexString(address), error);
            return false;
        }

        return true;
    }

    public long allocateMemory(int size, boolean executable) {
        int protection = executable ?
                Kernel32Interface.PAGE_EXECUTE_READWRITE :
                Kernel32Interface.PAGE_READWRITE;

        Pointer address = Kernel32Interface.INSTANCE.VirtualAllocEx(
                processHandle,
                null,
                size,
                Kernel32Interface.MEM_COMMIT | Kernel32Interface.MEM_RESERVE,
                protection
        );

        if (address == null) {
            int error = com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError();
            logger.error("Failed to allocate {} bytes (Error: {})", size, error);
            return 0;
        }

        long addr = Pointer.nativeValue(address);
        logger.debug("Allocated {} bytes at 0x{}", size, Long.toHexString(addr));
        return addr;
    }

    public boolean freeMemory(long address) {
        boolean success = Kernel32Interface.INSTANCE.VirtualFreeEx(
                processHandle,
                Pointer.createConstant(address),
                0,
                Kernel32Interface.MEM_RELEASE
        );

        if (!success) {
            int error = com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError();
            logger.error("Failed to free memory at 0x{} (Error: {})", Long.toHexString(address), error);
        }

        return success;
    }

    public boolean executeRemoteThread(long codeAddress, long paramAddress, int timeout) {
        IntByReference threadId = new IntByReference();

        WinNT.HANDLE threadHandle = Kernel32Interface.INSTANCE.CreateRemoteThread(
                processHandle,
                null,
                0,
                Pointer.createConstant(codeAddress),
                paramAddress != 0 ? Pointer.createConstant(paramAddress) : null,
                0,
                threadId
        );

        if (threadHandle == null) {
            int error = com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError();
            logger.error("Failed to create remote thread (Error: {})", error);
            return false;
        }

        logger.debug("Created remote thread ID: {}", threadId.getValue());

        int waitResult = Kernel32Interface.INSTANCE.WaitForSingleObject(threadHandle, timeout);

        Kernel32Interface.INSTANCE.CloseHandle(threadHandle);

        if (waitResult == Kernel32Interface.WAIT_OBJECT_0) {
            logger.debug("Remote thread completed successfully");
            return true;
        } else if (waitResult == Kernel32Interface.WAIT_TIMEOUT) {
            logger.warn("Remote thread timed out after {}ms", timeout);
            return false;
        } else {
            int error = com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError();
            logger.error("WaitForSingleObject failed (Error: {})", error);
            return false;
        }
    }

    public WinNT.HANDLE getProcessHandle() {
        return processHandle;
    }

    public void close() {
        if (processHandle != null) {
            Kernel32Interface.INSTANCE.CloseHandle(processHandle);
            logger.info("Process handle closed");
        }
    }

    public boolean isValid() {
        return processHandle != null;
    }
}