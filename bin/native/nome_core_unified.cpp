#include "nome_c_net.h"
#include "nome_physics.hpp"
#include "nome_worldgen.hpp"
#include "nome_chunk_manager.hpp"
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
  #define WIN32_LEAN_AND_MEAN
  #include <windows.h>
  #define NOME_EXPORT __declspec(dllexport)
#else
  #define NOME_EXPORT __attribute__((visibility("default")))
#endif

// Master Native World State (All in Native C++, ZERO JVM Heap)
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
    NomeOnPlayerMoveFn java_on_move{nullptr};
    NomeOnPacketReceiveFn java_on_packet_recv{nullptr};
    NomeOnPacketSendFn java_on_packet_send{nullptr};
};

// Rust Native Engine bindings
extern "C" {
    bool nome_rust_log_init(const char* log_file_path);
    void nome_rust_log(int32_t level, const char* tag, const char* msg);
    void nome_rust_log_flush();
    const char* nome_rust_core_version();

    void nome_rust_antixray_process_section(uint16_t* blocks, int32_t section_y, uint8_t mode, uint64_t seed);

    void nome_rust_scheduler_init();
    uint64_t nome_rust_schedule_region_task(int32_t rx, int32_t rz, uint64_t delay_ticks, uint64_t interval_ticks, uint64_t callback_id, uint64_t current_tick);
    uint64_t nome_rust_schedule_entity_task(uint64_t entity_id, uint64_t delay_ticks, uint64_t interval_ticks, uint64_t callback_id, uint64_t current_tick);
    bool nome_rust_cancel_task(uint64_t task_id);
    size_t nome_rust_poll_ready_tasks(uint64_t current_tick, uint64_t* out_callbacks, size_t max_callbacks);

    void nome_rust_tickets_init();
    void nome_rust_add_player_tickets(int32_t center_cx, int32_t center_cz, int32_t radius, uint64_t current_tick);
    uint8_t nome_rust_get_chunk_level(int32_t cx, int32_t cz, uint64_t current_tick);
    bool nome_rust_save_world(const char* world_dir);

    uint8_t nome_rust_alternate_current_update(int32_t x, int32_t y, int32_t z, uint8_t source_power);
    bool nome_rust_check_aabb_collision(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z, const uint16_t* block_grid, int32_t grid_w, int32_t grid_h, int32_t grid_d);

    void nome_rust_radius_iteration_init();
    int32_t nome_rust_get_radius_count(int32_t radius);
    int32_t nome_rust_get_radius_iteration(int32_t radius, int32_t* out_dx, int32_t* out_dz, int32_t max_len);
    void nome_rust_region_init(const char* world_dir);
    bool nome_rust_region_save_chunk(int32_t cx, int32_t cz, const uint8_t* data, size_t data_len);
}

static NativeServerState* g_server = nullptr;

static NomeOnPlayerJoinFn g_cb_on_join = nullptr;
static NomeOnPlayerQuitFn g_cb_on_quit = nullptr;
static NomeOnPlayerChatFn g_cb_on_chat = nullptr;
static NomeOnBlockBreakFn g_cb_on_break = nullptr;
static NomeOnPlayerMoveFn g_cb_on_move = nullptr;
static NomeOnPacketReceiveFn g_cb_on_recv = nullptr;
static NomeOnPacketSendFn g_cb_on_send = nullptr;
typedef void (*NomeOnTickFn)(uint64_t tick, double tps, double mspt);
static NomeOnTickFn g_cb_on_tick = nullptr;

static void internal_on_player_join(uint64_t player_id, const char *username) {
    if (g_cb_on_join) {
        g_cb_on_join(player_id, username);
    }
}

static void internal_on_player_quit(uint64_t player_id) {
    if (g_cb_on_quit) {
        g_cb_on_quit(player_id);
    }
}

static void internal_on_player_chat(uint64_t player_id, const char *message) {
    if (g_cb_on_chat) {
        g_cb_on_chat(player_id, message);
    }
}

static void internal_on_block_break(uint64_t player_id, int32_t x, int32_t y, int32_t z) {
    if (g_cb_on_break) {
        g_cb_on_break(player_id, x, y, z);
    }
}

static bool internal_on_player_move(uint64_t player_id, double from_x, double from_y, double from_z, double to_x, double to_y, double to_z, bool on_ground) {
    if (g_cb_on_move) {
        return g_cb_on_move(player_id, from_x, from_y, from_z, to_x, to_y, to_z, on_ground);
    }
    return true;
}

static bool internal_on_packet_receive(uint64_t player_id, int32_t packet_id, const uint8_t *payload, size_t len) {
    if (g_cb_on_recv) {
        return g_cb_on_recv(player_id, packet_id, payload, len);
    }
    return true;
}

static bool internal_on_packet_send(uint64_t player_id, int32_t packet_id, const uint8_t *payload, size_t len) {
    if (g_cb_on_send) {
        return g_cb_on_send(player_id, packet_id, payload, len);
    }
    return true;
}

static void native_tick_loop() {
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
        uint64_t cur_tick = g_server->tick_counter.fetch_add(1);

        // 4. Main Thread Tick Upcall into Java Runtime & Bukkit Scheduler
        if (g_cb_on_tick) {
            g_cb_on_tick(cur_tick, g_server->rolling_tps, g_server->current_mspt);
        }

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
}

extern "C" {


#ifdef _WIN32
static void enable_windows_ansi() {
    HANDLE hOut = GetStdHandle(STD_OUTPUT_HANDLE);
    if (hOut != INVALID_HANDLE_VALUE) {
        DWORD dwMode = 0;
        if (GetConsoleMode(hOut, &dwMode)) {
            SetConsoleMode(hOut, dwMode | 0x0004); // ENABLE_VIRTUAL_TERMINAL_PROCESSING
        }
    }
    SetConsoleOutputCP(CP_UTF8);
}
#endif

NOME_EXPORT bool nome_native_init() {
    if (!g_server) {
#ifdef _WIN32
        enable_windows_ansi();
#endif
        g_server = new NativeServerState();
        
        // Initialize Rust high-performance subsystems (Logger, Folia Scheduler, Moonrise Tickets & Radius Iteration)
        nome_rust_log_init("logs/latest.log");
        nome_rust_scheduler_init();
        nome_rust_tickets_init();
        nome_rust_radius_iteration_init();
        nome_rust_region_init("world");

        // Initialize multi-chunk manager and procedural terrain generator
        nome_chunk_init(1337);
        // Preload and pin 49 spawn chunks (-3 to +3 in X and Z)
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                nome_chunk_load(dx, dz);
                nome::ChunkManager::Instance().AddTicket(dx, dz);
            }
        }

        // Spawn initial vanilla mobs (Villager, Cow, Sheep, Zombie) into native ECS
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
        nome_net_set_move_callback(g_server->network, internal_on_player_move);
        nome_net_set_packet_listener(g_server->network, internal_on_packet_receive, internal_on_packet_send);
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
        // Flush all chunks to disk on server stop
        nome_chunk_save_all();
        nome_rust_save_world("world");
        nome_rust_log_flush();
    }
}

NOME_EXPORT bool nome_native_chunk_save(int cx, int cz) {
    return nome_chunk_save(cx, cz);
}

NOME_EXPORT size_t nome_native_chunk_save_all() {
    return nome_chunk_save_all();
}

NOME_EXPORT void nome_register_java_callbacks(NomeOnPlayerJoinFn on_join,
                                              NomeOnPlayerQuitFn on_quit,
                                              NomeOnPlayerChatFn on_chat,
                                              NomeOnBlockBreakFn on_break,
                                              NomeOnPlayerMoveFn on_move,
                                              NomeOnPacketReceiveFn on_recv,
                                              NomeOnPacketSendFn on_send,
                                              NomeOnTickFn on_tick) {
    g_cb_on_join = on_join;
    g_cb_on_quit = on_quit;
    g_cb_on_chat = on_chat;
    g_cb_on_break = on_break;
    g_cb_on_move = on_move;
    g_cb_on_recv = on_recv;
    g_cb_on_send = on_send;
    g_cb_on_tick = on_tick;
}

NOME_EXPORT void nome_native_send_raw_packet(uint64_t player_id, int32_t packet_id, const uint8_t* payload, size_t len) {
    if (g_server && g_server->network) {
        nome_net_send_raw(g_server->network, player_id, packet_id, payload, len);
    }
}

NOME_EXPORT bool nome_native_chunk_is_loaded(int cx, int cz) {
    return nome_chunk_is_loaded(cx, cz);
}

NOME_EXPORT void nome_native_chunk_load(int cx, int cz) {
    nome_chunk_load(cx, cz);
}

NOME_EXPORT void nome_native_chunk_get_heightmap(int cx, int cz, int16_t* out_heightmap) {
    nome_chunk_get_heightmap(cx, cz, out_heightmap);
}

NOME_EXPORT void nome_native_chunk_get_section(int cx, int cz, int section_y, uint16_t* out_blocks) {
    nome_chunk_get_section(cx, cz, section_y, out_blocks);
}

NOME_EXPORT void nome_native_chunk_get_all_blocks(int cx, int cz, uint16_t* out_blocks) {
    nome_chunk_get_all_blocks(cx, cz, out_blocks);
}

NOME_EXPORT bool nome_native_chunk_unload(int cx, int cz, bool force) {
    return nome_chunk_unload(cx, cz, force);
}

NOME_EXPORT size_t nome_native_chunk_get_loaded_count() {
    return nome_chunk_get_loaded_count();
}

NOME_EXPORT size_t nome_native_chunk_gc(int max_idle_seconds) {
    return nome_chunk_gc(max_idle_seconds);
}

NOME_EXPORT void nome_native_set_antixray(bool enabled) {
    nome_worldgen_set_antixray(enabled);
}

NOME_EXPORT bool nome_native_get_antixray() {
    return nome_worldgen_get_antixray();
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
    nome_chunk_set_block(x, y, z, block_id);
    if (g_server->network) {
        nome_net_send_block_change(g_server->network, x, y, z, block_id);
    }
}

NOME_EXPORT uint16_t nome_native_get_block(int32_t x, int32_t y, int32_t z) {
    if (!g_server) return 0;
    return nome_chunk_get_block(x, y, z);
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

// ==========================================
// Rust Bridge Native Exports
// ==========================================
NOME_EXPORT bool nome_native_log_init(const char* log_file_path) {
    return nome_rust_log_init(log_file_path);
}

NOME_EXPORT void nome_native_log(int32_t level, const char* tag, const char* msg) {
    nome_rust_log(level, tag, msg);
}

NOME_EXPORT void nome_native_log_flush() {
    nome_rust_log_flush();
}

NOME_EXPORT void nome_native_antixray_process_section(uint16_t* blocks, int32_t section_y, uint8_t mode, uint64_t seed) {
    nome_rust_antixray_process_section(blocks, section_y, mode, seed);
}

NOME_EXPORT void nome_native_tickets_init() {
    nome_rust_tickets_init();
}

NOME_EXPORT void nome_native_add_player_tickets(int32_t center_cx, int32_t center_cz, int32_t radius, uint64_t current_tick) {
    nome_rust_add_player_tickets(center_cx, center_cz, radius, current_tick);
}

NOME_EXPORT uint8_t nome_native_get_chunk_level(int32_t cx, int32_t cz, uint64_t current_tick) {
    return nome_rust_get_chunk_level(cx, cz, current_tick);
}

NOME_EXPORT uint64_t nome_native_schedule_region_task(int32_t rx, int32_t rz, uint64_t delay_ticks, uint64_t interval_ticks, uint64_t callback_id, uint64_t current_tick) {
    return nome_rust_schedule_region_task(rx, rz, delay_ticks, interval_ticks, callback_id, current_tick);
}

NOME_EXPORT uint64_t nome_native_schedule_entity_task(uint64_t entity_id, uint64_t delay_ticks, uint64_t interval_ticks, uint64_t callback_id, uint64_t current_tick) {
    return nome_rust_schedule_entity_task(entity_id, delay_ticks, interval_ticks, callback_id, current_tick);
}

NOME_EXPORT bool nome_native_cancel_task(uint64_t task_id) {
    return nome_rust_cancel_task(task_id);
}

NOME_EXPORT size_t nome_native_poll_ready_tasks(uint64_t current_tick, uint64_t* out_callbacks, size_t max_callbacks) {
    return nome_rust_poll_ready_tasks(current_tick, out_callbacks, max_callbacks);
}

NOME_EXPORT bool nome_native_save_world(const char* world_dir) {
    nome_chunk_save_all();
    return nome_rust_save_world(world_dir);
}

NOME_EXPORT uint8_t nome_native_alternate_current_update(int32_t x, int32_t y, int32_t z, uint8_t source_power) {
    return nome_rust_alternate_current_update(x, y, z, source_power);
}

NOME_EXPORT bool nome_native_check_aabb_collision(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z, const uint16_t* block_grid, int32_t grid_w, int32_t grid_h, int32_t grid_d) {
    return nome_rust_check_aabb_collision(min_x, min_y, min_z, max_x, max_y, max_z, block_grid, grid_w, grid_h, grid_d);
}

NOME_EXPORT void nome_native_radius_iteration_init() {
    nome_rust_radius_iteration_init();
}

NOME_EXPORT int32_t nome_native_get_radius_count(int32_t radius) {
    return nome_rust_get_radius_count(radius);
}

NOME_EXPORT int32_t nome_native_get_radius_iteration(int32_t radius, int32_t* out_dx, int32_t* out_dz, int32_t max_len) {
    return nome_rust_get_radius_iteration(radius, out_dx, out_dz, max_len);
}

NOME_EXPORT void nome_native_region_init(const char* world_dir) {
    nome_rust_region_init(world_dir);
}

NOME_EXPORT bool nome_native_region_save_chunk(int32_t cx, int32_t cz, const uint8_t* data, size_t data_len) {
    return nome_rust_region_save_chunk(cx, cz, data, data_len);
}

NOME_EXPORT const char* nome_rust_version() {
    return nome_rust_core_version();
}

NOME_EXPORT void nome_native_set_favicon(const char* favicon) {
    nome_net_set_favicon(favicon);
}

}

