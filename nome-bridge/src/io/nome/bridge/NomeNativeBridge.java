package io.nome.bridge;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.*;
import com.comphenix.protocol.ProtocolLibrary;
import io.nome.bridge.util.MinecraftConsole;

/**
 * NomeNativeBridge binds the Java Runtime to the Native C/C++/Rust Core
 * using modern Java 22/25 Project Panama Foreign Function & Memory (FFM) API.
 * All state, worldgen, smart chunk manager, entity ECS, crafting, and networking
 * live 100% in Native C/C++ with zero Java tick overhead.
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
    private static MethodHandle hGetHealth;
    private static MethodHandle hSetHealth;
    private static MethodHandle hGetItem;
    private static MethodHandle hSetItem;
    private static MethodHandle hCraft2x2;
    private static MethodHandle hCraft3x3;
    private static MethodHandle hEntitySpawn;
    private static MethodHandle hEntityRemove;
    private static MethodHandle hEntityCount;
    private static MethodHandle hEntityGetPos;
    private static MethodHandle hEntityGetHealth;
    private static MethodHandle hEntityDamage;
    private static MethodHandle hLocateStructure;
    private static MethodHandle hSetAntiXray;
    private static MethodHandle hGetAntiXray;
    private static MethodHandle hRegisterCallbacks;

    // Smart Chunk Manager & Raw Packet MethodHandles
    private static MethodHandle hSendRaw;
    private static MethodHandle hChunkLoaded;
    private static MethodHandle hChunkLoad;
    private static MethodHandle hChunkUnload;
    private static MethodHandle hChunkCount;
    private static MethodHandle hChunkGC;
    private static MethodHandle hChunkGetAllBlocks;
    private static MethodHandle hChunkGetHeightmap;
    private static MethodHandle hChunkGetSection;

    public static synchronized boolean loadNativeEngine(String nativeDir) {
        if (initialized) return true;

        try {
            Path dllPath = Paths.get(nativeDir, System.mapLibraryName("nome_core")).toAbsolutePath();
            if (!dllPath.toFile().exists()) {
                dllPath = Paths.get("bin", "native", System.mapLibraryName("nome_core")).toAbsolutePath();
            }

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

            hGetHealth = linker.downcallHandle(lookup.find("nome_native_get_player_health").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));

            hSetHealth = linker.downcallHandle(lookup.find("nome_native_set_player_health").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));

            hGetItem = linker.downcallHandle(lookup.find("nome_native_get_player_item").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            hSetItem = linker.downcallHandle(lookup.find("nome_native_set_player_item").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_SHORT, ValueLayout.JAVA_BYTE));

            hCraft2x2 = linker.downcallHandle(lookup.find("nome_native_craft_2x2").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_SHORT, ValueLayout.JAVA_SHORT, ValueLayout.JAVA_SHORT, ValueLayout.JAVA_SHORT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            hCraft3x3 = linker.downcallHandle(lookup.find("nome_native_craft_3x3").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            hEntitySpawn = linker.downcallHandle(lookup.find("nome_native_entity_spawn").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_SHORT, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));

            hEntityRemove = linker.downcallHandle(lookup.find("nome_native_entity_remove").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT));

            hEntityCount = linker.downcallHandle(lookup.find("nome_native_entity_count").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG));

            hEntityGetPos = linker.downcallHandle(lookup.find("nome_native_entity_get_pos").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            hEntityGetHealth = linker.downcallHandle(lookup.find("nome_native_entity_get_health").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

            hEntityDamage = linker.downcallHandle(lookup.find("nome_native_entity_damage").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

            hLocateStructure = linker.downcallHandle(lookup.find("nome_native_locate_structure").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            hSetAntiXray = linker.downcallHandle(lookup.find("nome_native_set_antixray").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_BOOLEAN));

            hGetAntiXray = linker.downcallHandle(lookup.find("nome_native_get_antixray").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN));

            // Chunk & Raw Packet Downcalls
            hSendRaw = linker.downcallHandle(lookup.find("nome_native_send_raw_packet").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

            hChunkLoaded = linker.downcallHandle(lookup.find("nome_native_chunk_is_loaded").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

            hChunkLoad = linker.downcallHandle(lookup.find("nome_native_chunk_load").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

            hChunkUnload = linker.downcallHandle(lookup.find("nome_native_chunk_unload").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_BOOLEAN));

            hChunkCount = linker.downcallHandle(lookup.find("nome_native_chunk_get_loaded_count").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG));

            hChunkGC = linker.downcallHandle(lookup.find("nome_native_chunk_gc").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));

            hChunkGetAllBlocks = linker.downcallHandle(lookup.find("nome_native_chunk_get_all_blocks").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

            hChunkGetHeightmap = linker.downcallHandle(lookup.find("nome_native_chunk_get_heightmap").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

            hChunkGetSection = linker.downcallHandle(lookup.find("nome_native_chunk_get_section").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

            // Register Java EventBridge Upcalls into C++ Engine (8 stubs)
            hRegisterCallbacks = linker.downcallHandle(lookup.find("nome_register_java_callbacks").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            registerUpcalls(linker);

            initialized = true;
            return true;
        } catch (Throwable t) {
            System.err.println("[Nome-Bridge] Failed to load native library via Panama: " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    private static void registerUpcalls(Linker linker) {
        try {
            MethodHandles.Lookup lk = MethodHandles.lookup();

            MethodHandle mhJoin = lk.findStatic(NomeNativeBridge.class, "nativeCallbackOnJoin",
                    MethodType.methodType(void.class, long.class, MemorySegment.class));
            MemorySegment stubJoin = linker.upcallStub(mhJoin,
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS), Arena.global());

            MethodHandle mhQuit = lk.findStatic(NomeNativeBridge.class, "nativeCallbackOnQuit",
                    MethodType.methodType(void.class, long.class));
            MemorySegment stubQuit = linker.upcallStub(mhQuit,
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG), Arena.global());

            MethodHandle mhChat = lk.findStatic(NomeNativeBridge.class, "nativeCallbackOnChat",
                    MethodType.methodType(void.class, long.class, MemorySegment.class));
            MemorySegment stubChat = linker.upcallStub(mhChat,
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS), Arena.global());

            MethodHandle mhBreak = lk.findStatic(NomeNativeBridge.class, "nativeCallbackOnBlockBreak",
                    MethodType.methodType(void.class, long.class, int.class, int.class, int.class));
            MemorySegment stubBreak = linker.upcallStub(mhBreak,
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT), Arena.global());

            MethodHandle mhMove = lk.findStatic(NomeNativeBridge.class, "nativeCallbackOnPlayerMove",
                    MethodType.methodType(boolean.class, long.class, double.class, double.class, double.class, double.class, double.class, double.class, boolean.class));
            MemorySegment stubMove = linker.upcallStub(mhMove,
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_LONG, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_BOOLEAN), Arena.global());

            MethodHandle mhRecv = lk.findStatic(NomeNativeBridge.class, "nativeCallbackOnPacketReceive",
                    MethodType.methodType(boolean.class, long.class, int.class, MemorySegment.class, long.class));
            MemorySegment stubRecv = linker.upcallStub(mhRecv,
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Arena.global());

            MethodHandle mhSend = lk.findStatic(NomeNativeBridge.class, "nativeCallbackOnPacketSend",
                    MethodType.methodType(boolean.class, long.class, int.class, MemorySegment.class, long.class));
            MemorySegment stubSend = linker.upcallStub(mhSend,
                    FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), Arena.global());

            MethodHandle mhTick = lk.findStatic(NomeNativeBridge.class, "nativeCallbackOnTick",
                    MethodType.methodType(void.class, long.class, double.class, double.class));
            MemorySegment stubTick = linker.upcallStub(mhTick,
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE), Arena.global());

            hRegisterCallbacks.invokeExact(stubJoin, stubQuit, stubChat, stubBreak, stubMove, stubRecv, stubSend, stubTick);
        } catch (Throwable t) {
            System.err.println("[Nome-Bridge] Warning: Failed to register native upcall callbacks: " + t.getMessage());
        }
    }

    // ==========================================
    // Panama FFM Native -> Java Upcall Handlers
    // ==========================================

    public static void nativeCallbackOnJoin(long playerId, MemorySegment usernameSeg) {
        try {
            String username = usernameSeg.reinterpret(256).getString(0);
            NomeServer server = NomeServer.getInstance();
            if (server != null) {
                World w = server.getWorlds().isEmpty() ? null : server.getWorlds().get(0);
                UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
                Player p = new Player((int) playerId, username, uuid, w, new Location(w, 0.5, 64.0, 0.5), server);
                server.registerPlayer(p);

                MinecraftConsole.info("UUID of player " + username + " is " + uuid);
                MinecraftConsole.info(username + "[/127.0.0.1:" + playerId + "] logged in with entity id " + p.getEntityId() + " at ([world]0.5, 64.0, 0.5)");
                MinecraftConsole.info(username + " joined the game");

                server.getPluginManager().callEvent(new PlayerJoinEvent(p, "§e" + username + " joined the game"));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void nativeCallbackOnQuit(long playerId) {
        try {
            NomeServer server = NomeServer.getInstance();
            if (server != null) {
                Player p = server.getPlayerById((int) playerId);
                if (p != null) {
                    MinecraftConsole.info(p.getName() + " lost connection: Disconnected");
                    MinecraftConsole.info(p.getName() + " left the game");
                    server.getPluginManager().callEvent(new PlayerQuitEvent(p, "§e" + p.getName() + " left the game"));
                    server.unregisterPlayer(p);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void nativeCallbackOnChat(long playerId, MemorySegment msgSeg) {
        try {
            String msg = msgSeg.reinterpret(1024).getString(0);
            NomeServer server = NomeServer.getInstance();
            if (server != null) {
                Player p = server.getPlayerById((int) playerId);
                if (p != null) {
                    if (msg.startsWith("/")) {
                        MinecraftConsole.info(p.getName() + " issued server command: " + msg);
                    } else {
                        MinecraftConsole.info("<" + p.getName() + "> " + msg);
                    }
                    server.getPluginManager().callEvent(new AsyncPlayerChatEvent(false, p, msg, java.util.Collections.emptySet()));
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void nativeCallbackOnBlockBreak(long playerId, int x, int y, int z) {
        try {
            NomeServer server = NomeServer.getInstance();
            if (server != null) {
                Player p = server.getPlayerById((int) playerId);
                World w = p != null ? p.getWorld() : (server.getWorlds().isEmpty() ? null : server.getWorlds().get(0));
                if (w != null) {
                    BlockBreakEvent event = new BlockBreakEvent(w.getBlockAt(x, y, z), p);
                    server.getPluginManager().callEvent(event);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean nativeCallbackOnPlayerMove(long playerId, double fx, double fy, double fz, double tx, double ty, double tz, boolean onGround) {
        try {
            NomeServer server = NomeServer.getInstance();
            if (server == null) return true;
            Player p = server.getPlayerById((int) playerId);
            if (p == null) return true;

            World w = p.getWorld();
            Location from = new Location(w, fx, fy, fz);
            Location to = new Location(w, tx, ty, tz);

            PlayerMoveEvent event = new PlayerMoveEvent(p, from, to);
            server.getPluginManager().callEvent(event);

            p.setOnGround(onGround);

            if (event.isCancelled()) {
                // Anticheat cancelled movement: Native C++ rubberband setback!
                return false;
            }

            p.setLocationInternal(to);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return true;
        }
    }

    public static void nativeCallbackOnTick(long tick, double tps, double mspt) {
        try {
            NomeServer server = NomeServer.getInstance();
            if (server != null) {
                server.onServerTick(tick, tps, mspt);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean nativeCallbackOnPacketReceive(long playerId, int packetId, MemorySegment payloadSeg, long payloadLen) {
        try {
            NomeServer server = NomeServer.getInstance();
            if (server == null) return true;
            Player p = server.getPlayerById((int) playerId);
            if (p == null) return true;

            boolean hasProtoListeners = ProtocolLibrary.getProtocolManager().hasReceivingListeners(packetId);
            boolean hasBusListeners = server.getPluginManager().hasListeners(PlayerReceivePacketEvent.class);
            if (!hasProtoListeners && !hasBusListeners) {
                return true; // Zero GC allocation!
            }

            byte[] bytes = new byte[(int) payloadLen];
            if (payloadLen > 0 && payloadSeg != null && !payloadSeg.equals(MemorySegment.NULL)) {
                MemorySegment sized = payloadSeg.reinterpret(payloadLen);
                MemorySegment.copy(sized, ValueLayout.JAVA_BYTE, 0, bytes, 0, (int) payloadLen);
            }

            if (hasProtoListeners) {
                boolean allow = ProtocolLibrary.getProtocolManager().onPacketReceive(p, packetId, bytes);
                if (!allow) return false;
            }

            if (hasBusListeners) {
                PlayerReceivePacketEvent event = new PlayerReceivePacketEvent(p, packetId, bytes);
                server.getPluginManager().callEvent(event);
                return !event.isCancelled();
            }

            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return true;
        }
    }

    public static boolean nativeCallbackOnPacketSend(long playerId, int packetId, MemorySegment payloadSeg, long payloadLen) {
        try {
            NomeServer server = NomeServer.getInstance();
            if (server == null) return true;
            Player p = server.getPlayerById((int) playerId);
            if (p == null) return true;

            boolean hasProtoListeners = ProtocolLibrary.getProtocolManager().hasSendingListeners(packetId);
            boolean hasBusListeners = server.getPluginManager().hasListeners(PlayerSendPacketEvent.class);
            if (!hasProtoListeners && !hasBusListeners) {
                return true; // Zero GC allocation!
            }

            byte[] bytes = new byte[(int) payloadLen];
            if (payloadLen > 0 && payloadSeg != null && !payloadSeg.equals(MemorySegment.NULL)) {
                MemorySegment sized = payloadSeg.reinterpret(payloadLen);
                MemorySegment.copy(sized, ValueLayout.JAVA_BYTE, 0, bytes, 0, (int) payloadLen);
            }

            if (hasProtoListeners) {
                boolean allow = ProtocolLibrary.getProtocolManager().onPacketSend(p, packetId, bytes);
                if (!allow) return false;
            }

            if (hasBusListeners) {
                PlayerSendPacketEvent event = new PlayerSendPacketEvent(p, packetId, bytes);
                server.getPluginManager().callEvent(event);
                return !event.isCancelled();
            }

            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return true;
        }
    }

    // ==========================================
    // Java -> Native Engine Downcall Wrappers
    // ==========================================

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

    public static int getPlayerHealth(long playerId) {
        if (!initialized) return 20;
        try {
            return (int) hGetHealth.invokeExact(playerId);
        } catch (Throwable t) {
            return 20;
        }
    }

    public static void setPlayerHealth(long playerId, int health) {
        if (!initialized) return;
        try {
            hSetHealth.invokeExact(playerId, health);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void setPlayerItem(long playerId, int slot, short itemId, byte count) {
        if (!initialized) return;
        try {
            hSetItem.invokeExact(playerId, slot, itemId, count);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void setAntiXray(boolean enabled) {
        if (!initialized) return;
        try {
            hSetAntiXray.invokeExact(enabled);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean isAntiXray() {
        if (!initialized) return false;
        try {
            return (boolean) hGetAntiXray.invokeExact();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void sendRawPacket(long playerId, int packetId, byte[] payload) {
        if (!initialized) return;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pSeg = payload != null ? arena.allocateFrom(ValueLayout.JAVA_BYTE, payload) : MemorySegment.NULL;
            long len = payload != null ? payload.length : 0;
            hSendRaw.invokeExact(playerId, packetId, pSeg, len);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean isChunkLoaded(int cx, int cz) {
        if (!initialized) return false;
        try {
            return (boolean) hChunkLoaded.invokeExact(cx, cz);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void loadChunk(int cx, int cz) {
        if (!initialized) return;
        try {
            hChunkLoad.invokeExact(cx, cz);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean unloadChunk(int cx, int cz, boolean force) {
        if (!initialized) return false;
        try {
            return (boolean) hChunkUnload.invokeExact(cx, cz, force);
        } catch (Throwable t) {
            return false;
        }
    }

    public static long getLoadedChunkCount() {
        if (!initialized) return 0;
        try {
            return (long) hChunkCount.invokeExact();
        } catch (Throwable t) {
            return 0;
        }
    }

    public static long collectChunkGarbage(int maxIdleSeconds) {
        if (!initialized) return 0;
        try {
            return (long) hChunkGC.invokeExact(maxIdleSeconds);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void getChunkAllBlocks(int cx, int cz, short[] outBlocks) {
        if (!initialized || outBlocks == null || outBlocks.length < 16 * 16 * 384 || hChunkGetAllBlocks == null) return;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(ValueLayout.JAVA_SHORT, 16 * 16 * 384);
            hChunkGetAllBlocks.invokeExact(cx, cz, seg);
            MemorySegment.copy(seg, ValueLayout.JAVA_SHORT, 0, outBlocks, 0, 16 * 16 * 384);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void getChunkHeightmap(int cx, int cz, short[] outHeightmap) {
        if (!initialized || outHeightmap == null || outHeightmap.length < 256 || hChunkGetHeightmap == null) return;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(ValueLayout.JAVA_SHORT, 256);
            hChunkGetHeightmap.invokeExact(cx, cz, seg);
            MemorySegment.copy(seg, ValueLayout.JAVA_SHORT, 0, outHeightmap, 0, 256);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void getChunkSection(int cx, int cz, int sectionY, short[] outSection) {
        if (!initialized || outSection == null || outSection.length < 4096 || hChunkGetSection == null) return;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(ValueLayout.JAVA_SHORT, 4096);
            hChunkGetSection.invokeExact(cx, cz, sectionY, seg);
            MemorySegment.copy(seg, ValueLayout.JAVA_SHORT, 0, outSection, 0, 4096);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean craft2x2(short in0, short in1, short in2, short in3, short[] outResult) {
        if (!initialized || outResult == null || outResult.length < 2) return false;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pItem = arena.allocate(ValueLayout.JAVA_SHORT);
            MemorySegment pCount = arena.allocate(ValueLayout.JAVA_BYTE);
            boolean matched = (boolean) hCraft2x2.invokeExact(in0, in1, in2, in3, pItem, pCount);
            if (matched) {
                outResult[0] = pItem.get(ValueLayout.JAVA_SHORT, 0);
                outResult[1] = pCount.get(ValueLayout.JAVA_BYTE, 0);
            }
            return matched;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static boolean craft3x3(short[] grid9, short[] outResult) {
        if (!initialized || grid9 == null || grid9.length != 9 || outResult == null || outResult.length < 2) return false;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment gridSeg = arena.allocate(ValueLayout.JAVA_SHORT, 9);
            for (int i = 0; i < 9; i++) gridSeg.setAtIndex(ValueLayout.JAVA_SHORT, i, grid9[i]);
            MemorySegment pItem = arena.allocate(ValueLayout.JAVA_SHORT);
            MemorySegment pCount = arena.allocate(ValueLayout.JAVA_BYTE);
            boolean matched = (boolean) hCraft3x3.invokeExact(gridSeg, pItem, pCount);
            if (matched) {
                outResult[0] = pItem.get(ValueLayout.JAVA_SHORT, 0);
                outResult[1] = pCount.get(ValueLayout.JAVA_BYTE, 0);
            }
            return matched;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static int spawnEntity(short type, double x, double y, double z) {
        if (!initialized) return -1;
        try {
            return (int) hEntitySpawn.invokeExact(type, x, y, z);
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    public static boolean removeEntity(int entityId) {
        if (!initialized) return false;
        try {
            return (boolean) hEntityRemove.invokeExact(entityId);
        } catch (Throwable t) {
            return false;
        }
    }

    public static long getEntityCount() {
        if (!initialized) return 0;
        try {
            return (long) hEntityCount.invokeExact();
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getEntityHealth(int entityId) {
        if (!initialized) return 0;
        try {
            return (int) hEntityGetHealth.invokeExact(entityId);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static boolean damageEntity(int entityId, int damage) {
        if (!initialized) return false;
        try {
            return (boolean) hEntityDamage.invokeExact(entityId, damage);
        } catch (Throwable t) {
            return false;
        }
    }

    public static int[] locateStructure(String structureName, int originChunkX, int originChunkZ) {
        if (!initialized || structureName == null) return null;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameSeg = arena.allocateFrom(structureName);
            MemorySegment pOutX = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment pOutZ = arena.allocate(ValueLayout.JAVA_INT);
            boolean found = (boolean) hLocateStructure.invokeExact(nameSeg, originChunkX, originChunkZ, pOutX, pOutZ);
            if (found) {
                return new int[] { pOutX.get(ValueLayout.JAVA_INT, 0), pOutZ.get(ValueLayout.JAVA_INT, 0) };
            }
            return null;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
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
