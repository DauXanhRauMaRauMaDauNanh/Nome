#include "nome_c_net.h"
#include "nome_physics.hpp"
#include "nome_worldgen.hpp"
#include "nome_crafting.hpp"
#include "nome_entity.hpp"
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

// Master 16x16x384 Native World State (All in Native C++, ZERO JVM Heap)
struct NativeServerState {
    std::atomic<bool> is_running{false};
    std::atomic<uint64_t> tick_counter{0};
    double rolling_tps{20.0};
    double current_mspt{0.04};
    std::mutex state_mutex;
    std::thread* tick_thread{nullptr};

    NomeServerNetwork *network{nullptr};

    // Spawn chunk column: 16 * 16 * 384 = 98304 blocks
    uint16_t spawn_blocks[16 * 16 * 384];

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

static void native_tick_loop() {
    printf("[Nome-Core] Native High-Precision 20 TPS OS Thread Started.\n");
    while (g_server && g_server->is_running.load()) {
        auto start_time = std::chrono::high_resolution_clock::now();

        // 1. Tick networking (poll non-blocking sockets)
        if (g_server->network) {
            nome_net_poll_events(g_server->network, 0);
        }

        // 2. Collect players & Tick Native Entity System (AABB physics, Mob AI, Gravity)
        std::vector<nome::NativePlayerTarget> targets;
        if (g_server->network) {
            for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
                NomeClientSession* c = nome_net_get_client(g_server->network, (uint64_t)i);
                if (c && c->is_active && c->state == NOME_STATE_PLAY) {
                    targets.push_back({(uint64_t)c->socket_fd, c->x, c->y, c->z});
                }
            }
        }
        nome::EntityManager::Instance().Tick(g_server->spawn_blocks, targets);

        // 3. Physics & collision pass (C++ AABB checks)
        g_server->tick_counter.fetch_add(1);

        auto end_time = std::chrono::high_resolution_clock::now();
        double ms = std::chrono::duration<double, std::milli>(end_time - start_time).count();
        g_server->current_mspt = ms;

        // Pacing to exact 50.0ms tick (20.0 TPS)
        if (ms < 50.0) {
            std::this_thread::sleep_for(std::chrono::duration<double, std::milli>(50.0 - ms));
            g_server->rolling_tps = 20.0;
        } else {
            g_server->rolling_tps = (ms > 0.0) ? (1000.0 / ms) : 20.0;
        }
    }
    printf("[Nome-Core] Native Tick Loop Stopped.\n");
}

extern "C" {

NOME_EXPORT const char* nome_rust_version() {
    return "Nome-Core v0.4.0-ultra [100% Native: C (IOCP) + C++ (WorldGen/Crafting/Entities/Lighting) + Rust Architecture]";
}

NOME_EXPORT bool nome_native_init() {
    if (!g_server) {
        g_server = new NativeServerState();
        
        // Procedurally generate the spawn world in pure Native C++ (Perlin 3D Noise)
        printf("[Nome-Core] Procedurally generating 3D Voxel World (Height: -64 to 319)...\n");
        nome_worldgen_generate_chunk(0, 0, g_server->spawn_blocks, 1337);

        // Spawn initial vanilla mobs (Villager, Cow, Sheep, Zombie) into native ECS
        printf("[Nome-Core] Spawning Initial Vanilla Entities (Villager, Cow, Sheep, Zombie)...\n");
        nome::EntityManager::Instance().Spawn(nome::ENTITY_VILLAGER, 8.5, 65.0, 8.5);
        nome::EntityManager::Instance().Spawn(nome::ENTITY_COW, 5.5, 65.0, 5.5);
        nome::EntityManager::Instance().Spawn(nome::ENTITY_SHEEP, 11.5, 65.0, 5.5);
        nome::EntityManager::Instance().Spawn(nome::ENTITY_ZOMBIE, 12.5, 65.0, 12.5);

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

    // Launch background native tick thread for 0% Java CPU overhead
    if (!g_server->tick_thread) {
        g_server->tick_thread = new std::thread(native_tick_loop);
    }

    return true;
}

NOME_EXPORT void nome_native_stop() {
    if (g_server) {
        g_server->is_running.store(false);
        if (g_server->tick_thread) {
            if (g_server->tick_thread->joinable()) {
                g_server->tick_thread->join();
            }
            delete g_server->tick_thread;
            g_server->tick_thread = nullptr;
        }
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
    if (!g_server) return 20.0;
    return g_server->rolling_tps;
}

NOME_EXPORT double nome_native_get_mspt() {
    return g_server ? g_server->current_mspt : 0.0;
}

NOME_EXPORT void nome_native_set_block(int32_t x, int32_t y, int32_t z, uint16_t block_id) {
    if (!g_server) return;
    int idx = ((y + 64) * 256) + ((z & 15) * 16) + (x & 15);
    if (idx >= 0 && idx < 16 * 16 * 384) {
        std::lock_guard<std::mutex> lock(g_server->state_mutex);
        g_server->spawn_blocks[idx] = block_id;
    }
    if (g_server->network) {
        nome_net_send_block_change(g_server->network, x, y, z, block_id);
    }
}

NOME_EXPORT uint16_t nome_native_get_block(int32_t x, int32_t y, int32_t z) {
    if (!g_server) return 0;
    int idx = ((y + 64) * 256) + ((z & 15) * 16) + (x & 15);
    if (idx >= 0 && idx < 16 * 16 * 384) {
        std::lock_guard<std::mutex> lock(g_server->state_mutex);
        return g_server->spawn_blocks[idx];
    }
    return 0;
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

// 100% Native Player State Queries (Off-Heap in C++, Zero Java Object Allocation)
NOME_EXPORT bool nome_native_get_player_pos(uint64_t player_id, double* out_x, double* out_y, double* out_z, float* out_yaw, float* out_pitch) {
    if (!g_server || !g_server->network) return false;
    NomeClientSession* c = nome_net_get_client(g_server->network, player_id);
    if (c) {
        if (out_x) *out_x = c->x;
        if (out_y) *out_y = c->y;
        if (out_z) *out_z = c->z;
        if (out_yaw) *out_yaw = c->yaw;
        if (out_pitch) *out_pitch = c->pitch;
        return true;
    }
    return false;
}

NOME_EXPORT int32_t nome_native_get_player_health(uint64_t player_id) {
    if (!g_server || !g_server->network) return 20;
    NomeClientSession* c = nome_net_get_client(g_server->network, player_id);
    return c ? c->health : 20;
}

NOME_EXPORT void nome_native_set_player_health(uint64_t player_id, int32_t health) {
    if (!g_server || !g_server->network) return;
    NomeClientSession* c = nome_net_get_client(g_server->network, player_id);
    if (c) c->health = health;
}

NOME_EXPORT bool nome_native_get_player_item(uint64_t player_id, int32_t slot, uint16_t* out_item, uint8_t* out_count) {
    if (!g_server || !g_server->network || slot < 0 || slot >= 46) return false;
    NomeClientSession* c = nome_net_get_client(g_server->network, player_id);
    if (c) {
        if (out_item) *out_item = c->inventory[slot];
        if (out_count) *out_count = c->inventory_count[slot];
        return true;
    }
    return false;
}

NOME_EXPORT void nome_native_set_player_item(uint64_t player_id, int32_t slot, uint16_t item, uint8_t count) {
    if (!g_server || !g_server->network || slot < 0 || slot >= 46) return;
    NomeClientSession* c = nome_net_get_client(g_server->network, player_id);
    if (c) {
        c->inventory[slot] = item;
        c->inventory_count[slot] = count;
        nome_net_send_slot(g_server->network, player_id, 0, slot, item, count);
    }
}

// 100% Native Crafting Solver (2x2 and 3x3)
NOME_EXPORT bool nome_native_craft_2x2(uint16_t in0, uint16_t in1, uint16_t in2, uint16_t in3,
                                      uint16_t* out_item, uint8_t* out_count) {
    uint16_t grid[4] = {in0, in1, in2, in3};
    return nome::CraftingManager::Instance().MatchGrid(grid, 2, out_item, out_count);
}

NOME_EXPORT bool nome_native_craft_3x3(const uint16_t* grid9, uint16_t* out_item, uint8_t* out_count) {
    return nome::CraftingManager::Instance().MatchGrid(grid9, 3, out_item, out_count);
}

// 100% Native Entity ECS Exports
NOME_EXPORT int32_t nome_native_entity_spawn(uint16_t type, double x, double y, double z) {
    return nome::EntityManager::Instance().Spawn(type, x, y, z);
}

NOME_EXPORT bool nome_native_entity_remove(int32_t entity_id) {
    return nome::EntityManager::Instance().Remove(entity_id);
}

NOME_EXPORT size_t nome_native_entity_count() {
    return nome::EntityManager::Instance().GetCount();
}

NOME_EXPORT bool nome_native_entity_get_pos(int32_t entity_id, double* x, double* y, double* z, float* yaw, float* pitch) {
    return nome_entity_get_pos(entity_id, x, y, z, yaw, pitch);
}

NOME_EXPORT int32_t nome_native_entity_get_health(int32_t entity_id) {
    return nome_entity_get_health(entity_id);
}

NOME_EXPORT bool nome_native_entity_damage(int32_t entity_id, int32_t damage) {
    return nome_entity_damage(entity_id, damage);
}

NOME_EXPORT bool nome_native_locate_structure(const char* name, int origin_cx, int origin_cz, int* out_cx, int* out_cz) {
    return nome_worldgen_locate_structure(name, origin_cx, origin_cz, out_cx, out_cz, 1337);
}

}
