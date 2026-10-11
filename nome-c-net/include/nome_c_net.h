#ifndef NOME_C_NET_H
#define NOME_C_NET_H

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

#ifdef _WIN32
  #define NOME_API __declspec(dllexport)
#else
  #define NOME_API __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* =========================================================================
   1. Fast VarInt & VarLong Encoders / Decoders
   ========================================================================= */

NOME_API int nome_varint_encode(int32_t value, uint8_t *out_buf);
NOME_API int nome_varint_decode(const uint8_t *in_buf, size_t max_len, int32_t *out_val);

NOME_API int nome_varlong_encode(int64_t value, uint8_t *out_buf);
NOME_API int nome_varlong_decode(const uint8_t *in_buf, size_t max_len, int64_t *out_val);

/* =========================================================================
   2. Zero-Allocation Packet Buffer
   ========================================================================= */

typedef struct NomePacketBuffer {
    uint8_t *data;
    size_t capacity;
    size_t write_pos;
    size_t read_pos;
} NomePacketBuffer;

NOME_API NomePacketBuffer* nome_buffer_create(size_t capacity);
NOME_API void nome_buffer_free(NomePacketBuffer *buf);
NOME_API void nome_buffer_reset(NomePacketBuffer *buf);

NOME_API bool nome_buffer_write(NomePacketBuffer *buf, const uint8_t *data, size_t len);
NOME_API bool nome_buffer_write_varint(NomePacketBuffer *buf, int32_t value);
NOME_API bool nome_buffer_write_string(NomePacketBuffer *buf, const char *str);

/* =========================================================================
   3. Protocol Connection State Machine
   ========================================================================= */

typedef enum NomeConnState {
    NOME_STATE_HANDSHAKE = 0,
    NOME_STATE_STATUS = 1,
    NOME_STATE_LOGIN = 2,
    NOME_STATE_CONFIGURATION = 3,
    NOME_STATE_PLAY = 4
} NomeConnState;

typedef struct NomeClientSession {
    int socket_fd;
    NomeConnState state;
    int protocol_version;
    char username[32];
    uint64_t uuid_most;
    uint64_t uuid_least;
    double x, y, z;
    float yaw, pitch;
    int health;
    int food;
    int gamemode;
    uint16_t inventory[46];
    uint8_t inventory_count[46];
    bool on_ground;
    bool is_sneaking;
    bool is_sprinting;
    bool is_flying;
    float fall_distance;
    uint64_t last_keepalive_sent;
    uint8_t rx_buf[8192];
    size_t rx_len;
    bool is_active;

    // Smart Chunk Loading State
    int current_chunk_x;
    int current_chunk_z;
    int view_distance;
    int pending_chunk_x[512];
    int pending_chunk_z[512];
    int pending_chunk_count;
    int pending_chunk_head;
} NomeClientSession;

#define MAX_NOME_CLIENTS 256

/* =========================================================================
   4. Native C Network Engine (Port 25565 Async Server)
   ========================================================================= */

typedef struct NomeServerNetwork NomeServerNetwork;

// Callbacks to notify Native Core & Java Plugin Bridge
typedef void (*NomeOnPlayerJoinFn)(uint64_t player_id, const char *username);
typedef void (*NomeOnPlayerQuitFn)(uint64_t player_id);
typedef void (*NomeOnPlayerChatFn)(uint64_t player_id, const char *message);
typedef void (*NomeOnBlockBreakFn)(uint64_t player_id, int32_t x, int32_t y, int32_t z);
typedef bool (*NomeOnPlayerMoveFn)(uint64_t player_id, double from_x, double from_y, double from_z, double to_x, double to_y, double to_z, bool on_ground);
typedef void (*NomeOnPlayerInteractFn)(uint64_t player_id, int32_t x, int32_t y, int32_t z, int32_t action);

// Packet Interception callbacks for Anticheat, Anti-Xray & ProtocolLib
typedef bool (*NomeOnPacketReceiveFn)(uint64_t player_id, int32_t packet_id, const uint8_t *payload, size_t len);
typedef bool (*NomeOnPacketSendFn)(uint64_t player_id, int32_t packet_id, const uint8_t *payload, size_t len);

NOME_API NomeServerNetwork* nome_net_create(const char *bind_ip, uint16_t port, int worker_threads);
NOME_API bool nome_net_start(NomeServerNetwork *net);
NOME_API void nome_net_stop(NomeServerNetwork *net);
NOME_API void nome_net_destroy(NomeServerNetwork *net);
NOME_API size_t nome_net_poll_events(NomeServerNetwork *net, int timeout_ms);

NOME_API void nome_net_set_callbacks(NomeServerNetwork *net,
                                     NomeOnPlayerJoinFn on_join,
                                     NomeOnPlayerQuitFn on_quit,
                                     NomeOnPlayerChatFn on_chat,
                                     NomeOnBlockBreakFn on_break);
NOME_API void nome_net_set_move_callback(NomeServerNetwork *net, NomeOnPlayerMoveFn on_move);
NOME_API void nome_net_set_interact_callback(NomeServerNetwork *net, NomeOnPlayerInteractFn on_interact);
NOME_API void nome_net_set_packet_listener(NomeServerNetwork *net, NomeOnPacketReceiveFn on_recv, NomeOnPacketSendFn on_send);

NOME_API void nome_net_broadcast_chat(NomeServerNetwork *net, const char *msg);
NOME_API void nome_net_send_block_change(NomeServerNetwork *net, int32_t x, int32_t y, int32_t z, uint16_t block_id);
NOME_API void nome_net_send_teleport(NomeServerNetwork *net, uint64_t player_id, double x, double y, double z, float yaw, float pitch);
NOME_API void nome_net_send_slot(NomeServerNetwork *net, uint64_t player_id, uint8_t window_id, int16_t slot, uint16_t item_id, uint8_t count);
NOME_API void nome_net_send_raw(NomeServerNetwork *net, uint64_t player_id, int32_t packet_id, const uint8_t *payload, size_t len);
NOME_API void nome_net_queue_chunk(NomeServerNetwork *net, uint64_t player_id, int cx, int cz);
NOME_API NomeClientSession* nome_net_get_client(NomeServerNetwork *net, uint64_t player_id);
NOME_API void nome_net_set_favicon(const char *favicon_data_uri);

#ifdef __cplusplus
}
#endif

#endif // NOME_C_NET_H
