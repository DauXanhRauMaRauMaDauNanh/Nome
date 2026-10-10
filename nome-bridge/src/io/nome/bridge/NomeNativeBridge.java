package io.nome.bridge;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * NomeNativeBridge binds the Java Runtime to the Native C/C++/Rust Core
 * using modern Java 22/25 Project Panama Foreign Function & Memory (FFM) API.
 */
public final class NomeNativeBridge {

    private static boolean initialized = false;
    private static MethodHandle hInit;
    private static MethodHandle hStart;
    private static MethodHandle hStop;
    private static MethodHandle hTickOnce;
    private static MethodHandle hGetMspt;
    private static MethodHandle hSetBlock;
    private static MethodHandle hGetBlock;
    private static MethodHandle hBroadcast;
    private static MethodHandle hTeleport;
    private static MethodHandle hVersion;

    public static synchronized boolean loadNativeEngine(String nativeDir) {
        if (initialized) return true;

        try {
            Path dllPath = Paths.get(nativeDir, System.mapLibraryName("nome_core")).toAbsolutePath();
            if (!dllPath.toFile().exists()) {
                dllPath = Paths.get("bin", "native", System.mapLibraryName("nome_core")).toAbsolutePath();
            }

            System.out.println("[Nome-Bridge] Loading native engine from: " + dllPath);
            SymbolLookup lookup = SymbolLookup.libraryLookup(dllPath, Arena.global());
            Linker linker = Linker.nativeLinker();

            hInit = linker.downcallHandle(lookup.find("nome_native_init").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN));

            hStart = linker.downcallHandle(lookup.find("nome_native_start").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN));

            hStop = linker.downcallHandle(lookup.find("nome_native_stop").orElseThrow(),
                    FunctionDescriptor.ofVoid());

            hTickOnce = linker.downcallHandle(lookup.find("nome_native_tick_once").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE));

            hGetMspt = linker.downcallHandle(lookup.find("nome_native_get_mspt").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE));

            hSetBlock = linker.downcallHandle(lookup.find("nome_native_set_block").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_SHORT));

            hGetBlock = linker.downcallHandle(lookup.find("nome_native_get_block").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

            hBroadcast = linker.downcallHandle(lookup.find("nome_native_broadcast_message").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

            hTeleport = linker.downcallHandle(lookup.find("nome_native_teleport_player").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));

            hVersion = linker.downcallHandle(lookup.find("nome_rust_version").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS));

            initialized = true;
            System.out.println("[Nome-Bridge] Native engine symbols bound successfully via Project Panama FFM.");
            return true;
        } catch (Throwable t) {
            System.err.println("[Nome-Bridge] Failed to load native library via Panama: " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    public static boolean init() {
        if (!initialized) return false;
        try {
            return (boolean) hInit.invokeExact();
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static boolean start() {
        if (!initialized) return false;
        try {
            return (boolean) hStart.invokeExact();
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static void stop() {
        if (!initialized) return;
        try {
            hStop.invokeExact();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static double tickOnce() {
        if (!initialized) return 20.0;
        try {
            return (double) hTickOnce.invokeExact();
        } catch (Throwable t) {
            return 20.0;
        }
    }

    public static double getMspt() {
        if (!initialized) return 0.0;
        try {
            return (double) hGetMspt.invokeExact();
        } catch (Throwable t) {
            return 0.0;
        }
    }

    public static void setBlock(int x, int y, int z, short blockId) {
        if (!initialized) return;
        try {
            hSetBlock.invokeExact(x, y, z, blockId);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static short getBlock(int x, int y, int z) {
        if (!initialized) return 0;
        try {
            return (short) hGetBlock.invokeExact(x, y, z);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void broadcastMessage(String message) {
        if (!initialized || message == null) return;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment strSeg = arena.allocateFrom(message);
            hBroadcast.invokeExact(strSeg);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void teleportPlayer(long playerId, double x, double y, double z, float yaw, float pitch) {
        if (!initialized) return;
        try {
            hTeleport.invokeExact(playerId, x, y, z, yaw, pitch);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static String getVersion() {
        if (!initialized) return "Nome (Native Unloaded)";
        try {
            MemorySegment addr = (MemorySegment) hVersion.invokeExact();
            if (addr == null || addr.equals(MemorySegment.NULL)) return "Unknown";
            return addr.reinterpret(256).getString(0);
        } catch (Throwable t) {
            return "Nome Native Core Error: " + t.getMessage();
        }
    }
}
