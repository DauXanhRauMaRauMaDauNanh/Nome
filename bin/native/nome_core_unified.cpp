#include "nome_c_net.h"
#include "nome_physics.hpp"
#include <chrono>
#include <thread>
#include <mutex>
#include <atomic>
#include <vector>
#include <iostream>
#include <cstring>

#ifdef _WIN32
  #define NOME_EXPORT __declspec(dllexport)
#else
  #define NOME_EXPORT __attribute__((visibility("default")))
#endif

// Internal High-Performance Engine State
struct ChunkSection {
    uint16_t blocks[4096];
    uint8_t block_light[2048];
    uint8_t sky_light[2048];
};

struct NativeServerState {
    std::atomic<bool> is_running{false};
    std::atomic<uint64_t> tick_counter{0};
    double rolling_tps{20.0};
    double current_mspt{0.05};
    std::mutex state_mutex;

    NomeServerNetwork *network{nullptr};

    // Spawn chunk section (16x16x16)
    ChunkSection spawn_section{};

    // Upcall function pointers to Java Plugin EventBus
    NomeOnPlayerJoinFn java_on_join{nullptr};
    NomeOnPlayerQuitFn java_on_quit{nullptr};
    NomeOnPlayerChatFn java_on_chat{nullptr};
    NomeOnBlockBreakFn java_on_break{nullptr};
};

static NativeServerState* g_server = nullptr;

static void internal_on_player_join(uint64_t player_id, const char *username) {
    if (g_server && g_server->java_on_join) {
        g_server->java_on_join(player_id, username);
    }
}

static void internal_on_player_quit(uint64_t player_id) {
    if (g_server && g_server->java_on_quit) {
        g_server->java_on_quit(player_id);
    }
}

static void internal_on_player_chat(uint64_t player_id, const char *message) {
    if (g_server && g_server->java_on_chat) {
        g_server->java_on_chat(player_id, message);
    }
}

static void internal_on_block_break(uint64_t player_id, int32_t x, int32_t y, int32_t z) {
    if (g_server && g_server->java_on_break) {
        g_server->java_on_break(player_id, x, y, z);
    }
}

extern "C" {

NOME_EXPORT const char* nome_rust_version() {
    return "Nome-Core v0.2.0-native [C (IOCP Net) + C++ (Physics/Lighting) + Rust (Safe Architecture)]";
}

NOME_EXPORT bool nome_native_init() {
    if (!g_server) {
        g_server = new NativeServerState();
        std::memset(&g_server->spawn_section, 0, sizeof(ChunkSection));
        g_server->network = nome_net_create("0.0.0.0", 25565, 4);
        nome_net_set_callbacks(g_server->network,
                               internal_on_player_join,
                               internal_on_player_quit,
                               internal_on_player_chat,
                               internal_on_block_break);
    }
    return true;
}

NOME_EXPORT bool nome_native_start() {
    if (!g_server) return false;
    if (g_server->network) {
        nome_net_start(g_server->network);
    }
    g_server->is_running.store(true);
    return true;
}

NOME_EXPORT void nome_native_stop() {
    if (g_server) {
        g_server->is_running.store(false);
        if (g_server->network) {
            nome_net_stop(g_server->network);
        }
    }
}

NOME_EXPORT void nome_register_java_callbacks(NomeOnPlayerJoinFn on_join,
                                              NomeOnPlayerQuitFn on_quit,
                                              NomeOnPlayerChatFn on_chat,
                                              NomeOnBlockBreakFn on_break) {
    if (g_server) {
        g_server->java_on_join = on_join;
        g_server->java_on_quit = on_quit;
        g_server->java_on_chat = on_chat;
        g_server->java_on_break = on_break;
    }
}

NOME_EXPORT double nome_native_tick_once() {
    if (!g_server || !g_server->is_running.load()) return 20.0;

    auto start_time = std::chrono::high_resolution_clock::now();

    // 1. Tick networking (poll C non-blocking sockets for packets)
    if (g_server->network) {
        nome_net_poll_events(g_server->network, 0);
    }

    // 2. Physics & collision pass (C++ AABB checks)
    // 3. Starlight light calculations

    g_server->tick_counter.fetch_add(1);

    auto end_time = std::chrono::high_resolution_clock::now();
    double ms = std::chrono::duration<double, std::milli>(end_time - start_time).count();
    g_server->current_mspt = ms;

    // Target 50ms tick (20 TPS)
    if (ms < 50.0) {
        std::this_thread::sleep_for(std::chrono::duration<double, std::milli>(50.0 - ms));
        g_server->rolling_tps = 20.0;
    } else {
        g_server->rolling_tps = (ms > 0.0) ? (1000.0 / ms) : 20.0;
    }

    return g_server->rolling_tps;
}

NOME_EXPORT double nome_native_get_mspt() {
    return g_server ? g_server->current_mspt : 0.0;
}

NOME_EXPORT void nome_native_set_block(int32_t x, int32_t y, int32_t z, uint16_t block_id) {
    if (!g_server) return;
    int idx = (y & 15) * 256 + (z & 15) * 16 + (x & 15);
    {
        std::lock_guard<std::mutex> lock(g_server->state_mutex);
        g_server->spawn_section.blocks[idx] = block_id;
    }
    if (g_server->network) {
        nome_net_send_block_change(g_server->network, x, y, z, block_id);
    }
}

NOME_EXPORT uint16_t nome_native_get_block(int32_t x, int32_t y, int32_t z) {
    if (!g_server) return 0;
    int idx = (y & 15) * 256 + (z & 15) * 16 + (x & 15);
    std::lock_guard<std::mutex> lock(g_server->state_mutex);
    return g_server->spawn_section.blocks[idx];
}

NOME_EXPORT void nome_native_broadcast_message(const char *message) {
    if (g_server && g_server->network) {
        nome_net_broadcast_chat(g_server->network, message);
    }
}

NOME_EXPORT void nome_native_teleport_player(uint64_t player_id, double x, double y, double z, float yaw, float pitch) {
    if (g_server && g_server->network) {
        nome_net_send_teleport(g_server->network, player_id, x, y, z, yaw, pitch);
    }
}

}
