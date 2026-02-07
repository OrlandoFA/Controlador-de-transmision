package com.pvz.controller.memory;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * JNA interface to Windows Kernel32 API for memory manipulation and code injection
 */
public interface Kernel32Interface extends StdCallLibrary {

    Kernel32Interface INSTANCE = Native.load("kernel32", Kernel32Interface.class, W32APIOptions.DEFAULT_OPTIONS);

    // Process access flags
    int PROCESS_VM_READ = 0x0010;
    int PROCESS_VM_WRITE = 0x0020;
    int PROCESS_VM_OPERATION = 0x0008;
    int PROCESS_CREATE_THREAD = 0x0002;
    int PROCESS_QUERY_INFORMATION = 0x0400;
    int PROCESS_ALL_ACCESS = 0x001F0FFF;

    // Memory allocation types
    int MEM_COMMIT = 0x1000;
    int MEM_RESERVE = 0x2000;
    int MEM_RELEASE = 0x8000;

    // Memory protection flags
    int PAGE_READWRITE = 0x04;
    int PAGE_EXECUTE_READWRITE = 0x40;

    WinNT.HANDLE OpenProcess(int dwDesiredAccess, boolean bInheritHandle, int dwProcessId);

    boolean ReadProcessMemory(WinNT.HANDLE hProcess, Pointer lpBaseAddress,
                              Pointer lpBuffer, int nSize, IntByReference lpNumberOfBytesRead);

    boolean WriteProcessMemory(WinNT.HANDLE hProcess, Pointer lpBaseAddress,
                               Pointer lpBuffer, int nSize, IntByReference lpNumberOfBytesWritten);

    Pointer VirtualAllocEx(WinNT.HANDLE hProcess, Pointer lpAddress,
                           int dwSize, int flAllocationType, int flProtect);

    boolean VirtualFreeEx(WinNT.HANDLE hProcess, Pointer lpAddress,
                          int dwSize, int dwFreeType);

    WinNT.HANDLE CreateRemoteThread(WinNT.HANDLE hProcess, Pointer lpThreadAttributes,
                                    int dwStackSize, Pointer lpStartAddress,
                                    Pointer lpParameter, int dwCreationFlags,
                                    IntByReference lpThreadId);

    int WaitForSingleObject(WinNT.HANDLE hHandle, int dwMilliseconds);

    boolean CloseHandle(WinNT.HANDLE hObject);

    // Wait constants
    int WAIT_OBJECT_0 = 0x00000000;
    int WAIT_TIMEOUT = 0x00000102;
    int WAIT_FAILED = 0xFFFFFFFF;
    int INFINITE = 0xFFFFFFFF;
}