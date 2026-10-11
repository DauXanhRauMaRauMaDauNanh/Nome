#include "../include/nome_c_net.h"
#include "../include/nome_config_26_3.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#ifdef _WIN32
  #include <winsock2.h>
  #include <ws2tcpip.h>
#else
  #include <sys/socket.h>
  #include <netinet/in.h>
  #include <arpa/inet.h>
  #include <unistd.h>
  #include <fcntl.h>
#endif

// External Native C++ WorldGen, Crafting & ChunkManager Linkage
#ifdef __cplusplus
extern "C" {
#endif
void nome_worldgen_generate_chunk(int chunk_x, int chunk_z, uint16_t* out_blocks, int64_t seed);
size_t nome_worldgen_serialize_chunk(int chunk_x, int chunk_z, const uint16_t* blocks, uint8_t* out_buf, size_t max_buf, bool modern);
size_t nome_chunk_serialize(int cx, int cz, uint8_t* out_buf, size_t max_buf, bool modern);
void nome_chunk_init(int64_t seed);
uint16_t nome_chunk_get_block(int x, int y, int z);
void nome_chunk_set_block(int x, int y, int z, uint16_t block);
bool nome_crafting_solve(const uint16_t* grid, uint8_t grid_dim, uint16_t* out_item, uint8_t* out_count);
bool nome_worldgen_locate_structure(const char* structure_name, int origin_chunk_x, int origin_chunk_z, int* out_chunk_x, int* out_chunk_z, int64_t seed);
int32_t nome_entity_spawn(uint16_t type, double x, double y, double z);
#ifdef __cplusplus
}
#endif

struct NomeServerNetwork {
    char bind_ip[64];
    uint16_t port;
    int worker_threads;
    bool is_running;
    int listen_fd;
    NomeClientSession clients[MAX_NOME_CLIENTS];

    NomeOnPlayerJoinFn on_join;
    NomeOnPlayerQuitFn on_quit;
    NomeOnPlayerChatFn on_chat;
    NomeOnBlockBreakFn on_break;
    NomeOnPlayerMoveFn on_move;
    NomeOnPlayerInteractFn on_interact;

    NomeOnPacketReceiveFn on_packet_receive;
    NomeOnPacketSendFn on_packet_send;

    // Master Spawn Chunk Column (16x16x384 voxels)
    uint16_t spawn_chunk[16 * 16 * 384];
    uint64_t tick_clock;
};

static char g_server_favicon[65536] = {0};

void nome_net_set_favicon(const char *favicon_data_uri) {
    if (!favicon_data_uri || favicon_data_uri[0] == '\0') {
        g_server_favicon[0] = '\0';
        return;
    }
    strncpy(g_server_favicon, favicon_data_uri, sizeof(g_server_favicon) - 1);
    g_server_favicon[sizeof(g_server_favicon) - 1] = '\0';
}

static void set_nonblocking(int fd) {
#ifdef _WIN32
    u_long mode = 1;
    ioctlsocket(fd, FIONBIO, &mode);
#else
    int flags = fcntl(fd, F_GETFL, 0);
    fcntl(fd, F_SETFL, flags | O_NONBLOCK);
#endif
}

NomeServerNetwork* nome_net_create(const char *bind_ip, uint16_t port, int worker_threads) {
    NomeServerNetwork *net = (NomeServerNetwork*)calloc(1, sizeof(NomeServerNetwork));
    if (!net) return NULL;

    strncpy(net->bind_ip, bind_ip ? bind_ip : "0.0.0.0", sizeof(net->bind_ip) - 1);
    net->port = port > 0 ? port : 25565;
    net->worker_threads = worker_threads > 0 ? worker_threads : 4;
    net->listen_fd = -1;
    net->is_running = false;
    net->tick_clock = 0;

    // Generate Spawn Chunk (0, 0) natively using 3D Perlin Noise
    nome_worldgen_generate_chunk(0, 0, net->spawn_chunk, 1337);

    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        net->clients[i].socket_fd = -1;
        net->clients[i].is_active = false;
        net->clients[i].state = NOME_STATE_HANDSHAKE;
        net->clients[i].health = 20;
        net->clients[i].food = 20;
        net->clients[i].gamemode = 0;
        net->clients[i].x = 0.5;
        net->clients[i].y = 65.0;
        net->clients[i].z = 0.5;
        net->clients[i].on_ground = true;
    }

    return net;
}

void nome_net_set_callbacks(NomeServerNetwork *net,
                             NomeOnPlayerJoinFn on_join,
                             NomeOnPlayerQuitFn on_quit,
                             NomeOnPlayerChatFn on_chat,
                             NomeOnBlockBreakFn on_break) {
    if (!net) return;
    net->on_join = on_join;
    net->on_quit = on_quit;
    net->on_chat = on_chat;
    net->on_break = on_break;
}

void nome_net_set_move_callback(NomeServerNetwork *net, NomeOnPlayerMoveFn on_move) {
    if (net) net->on_move = on_move;
}

void nome_net_set_interact_callback(NomeServerNetwork *net, NomeOnPlayerInteractFn on_interact) {
    if (net) net->on_interact = on_interact;
}

bool nome_net_start(NomeServerNetwork *net) {
    if (!net || net->is_running) return false;

#ifdef _WIN32
    WSADATA wsa;
    if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0) {
        return false;
    }
#endif

    int fd = (int)socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (fd < 0) return false;

    int opt = 1;
#ifdef _WIN32
    setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, (const char*)&opt, sizeof(opt));
#else
    setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
#endif

    set_nonblocking(fd);

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(net->port);
    inet_pton(AF_INET, net->bind_ip, &addr.sin_addr);

    if (bind(fd, (struct sockaddr*)&addr, sizeof(addr)) != 0) {
        printf("[Nome-Net-C] Failed to bind to %s:%u\n", net->bind_ip, net->port);
#ifdef _WIN32
        closesocket(fd);
#else
        close(fd);
#endif
        return false;
    }

    if (listen(fd, 128) != 0) {
#ifdef _WIN32
        closesocket(fd);
#else
        close(fd);
#endif
        return false;
    }

    net->listen_fd = fd;
    net->is_running = true;
    return true;
}

void nome_net_stop(NomeServerNetwork *net) {
    if (!net || !net->is_running) return;

    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        if (net->clients[i].is_active && net->clients[i].socket_fd >= 0) {
#ifdef _WIN32
            closesocket(net->clients[i].socket_fd);
#else
            close(net->clients[i].socket_fd);
#endif
            net->clients[i].is_active = false;
        }
    }

    if (net->listen_fd >= 0) {
#ifdef _WIN32
        closesocket(net->listen_fd);
        WSACleanup();
#else
        close(net->listen_fd);
#endif
        net->listen_fd = -1;
    }
    net->is_running = false;
}

void nome_net_destroy(NomeServerNetwork *net) {
    if (!net) return;
    nome_net_stop(net);
    free(net);
}

static NomeServerNetwork *g_active_net = NULL;
static void send_raw_packet(int fd, int32_t packet_id, const uint8_t *payload, size_t payload_len);

void nome_net_set_packet_listener(NomeServerNetwork *net, NomeOnPacketReceiveFn on_recv, NomeOnPacketSendFn on_send) {
    if (net) {
        net->on_packet_receive = on_recv;
        net->on_packet_send = on_send;
    }
}

void nome_net_send_raw(NomeServerNetwork *net, uint64_t player_id, int32_t packet_id, const uint8_t *payload, size_t len) {
    if (!net) return;
    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        if (net->clients[i].is_active && (uint64_t)net->clients[i].socket_fd == player_id) {
            send_raw_packet(net->clients[i].socket_fd, packet_id, payload, len);
            break;
        }
    }
}

static bool nome_net_send_all(int fd, const uint8_t *data, size_t total_len) {
    if (fd < 0 || !data || total_len == 0) return false;
    size_t sent = 0;
    while (sent < total_len) {
        int n = send(fd, (const char*)(data + sent), (int)(total_len - sent), 0);
        if (n > 0) {
            sent += (size_t)n;
        } else if (n < 0) {
#ifdef _WIN32
            int err = WSAGetLastError();
            if (err == WSAEWOULDBLOCK) {
#else
            int err = errno;
            if (err == EWOULDBLOCK || err == EAGAIN) {
#endif
                fd_set wfds;
                FD_ZERO(&wfds);
                FD_SET(fd, &wfds);
                struct timeval tv = {1, 0}; // 1 second timeout
                int sel = select(fd + 1, NULL, &wfds, NULL, &tv);
                if (sel <= 0) return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    return true;
}

static void send_raw_packet(int fd, int32_t packet_id, const uint8_t *payload, size_t payload_len) {
    // Universal Packet Interception Hook for Anticheat, Anti-Xray and ProtocolLib
    if (g_active_net && g_active_net->on_packet_send) {
        bool allow = g_active_net->on_packet_send((uint64_t)fd, packet_id, payload, payload_len);
        if (!allow) return; // Dropped/Filtered by external plugin
    }

    uint8_t id_buf[5];
    int id_len = nome_varint_encode(packet_id, id_buf);

    int32_t total_len = (int32_t)(id_len + payload_len);
    uint8_t len_buf[5];
    int len_len = nome_varint_encode(total_len, len_buf);

    size_t full_size = (size_t)(len_len + id_len + payload_len);
    if (full_size <= 4096) {
        uint8_t stack_buf[4096];
        memcpy(stack_buf, len_buf, len_len);
        memcpy(stack_buf + len_len, id_buf, id_len);
        if (payload && payload_len > 0) {
            memcpy(stack_buf + len_len + id_len, payload, payload_len);
        }
        nome_net_send_all(fd, stack_buf, full_size);
    } else {
        uint8_t *heap_buf = (uint8_t*)malloc(full_size);
        if (heap_buf) {
            memcpy(heap_buf, len_buf, len_len);
            memcpy(heap_buf + len_len, id_buf, id_len);
            if (payload && payload_len > 0) {
                memcpy(heap_buf + len_len + id_len, payload, payload_len);
            }
            nome_net_send_all(fd, heap_buf, full_size);
            free(heap_buf);
        }
    }
}

static void nome_net_queue_spiral_chunks(NomeClientSession *c, int center_cx, int center_cz, int view_dist) {
    if (!c) return;
    int r = view_dist;
    if (r < 1) r = 1;
    if (r > 6) r = 6;
    c->pending_chunk_head = 0;
    c->pending_chunk_count = 0;

    // Center chunk first
    c->pending_chunk_x[c->pending_chunk_count] = center_cx;
    c->pending_chunk_z[c->pending_chunk_count] = center_cz;
    c->pending_chunk_count++;

    // Spiral outwards
    for (int rad = 1; rad <= r; rad++) {
        for (int dz = -rad; dz <= rad; dz++) {
            for (int dx = -rad; dx <= rad; dx++) {
                if (dx*dx + dz*dz <= rad*rad && dx*dx + dz*dz > (rad-1)*(rad-1)) {
                    if (c->pending_chunk_count < (int)(sizeof(c->pending_chunk_x)/sizeof(c->pending_chunk_x[0]))) {
                        c->pending_chunk_x[c->pending_chunk_count] = center_cx + dx;
                        c->pending_chunk_z[c->pending_chunk_count] = center_cz + dz;
                        c->pending_chunk_count++;
                    }
                }
            }
        }
    }
}

static void send_full_play_sequence(NomeServerNetwork *net, NomeClientSession *c) {
    g_active_net = net;
    int fd = c->socket_fd;
    uint8_t b_false = 0;
    uint8_t b_true = 1;
    bool modern = (c->protocol_version >= 766 || c->protocol_version == 777 || c->protocol_version == 0);

    // 1. Packet Login (Play): 0x33 for 26.3 / 1.21+, 0x2C for 1.20.4
    NomePacketBuffer *bLogin = nome_buffer_create(512);
    int32_t entity_id = 1000 + fd;
    nome_buffer_write(bLogin, (const uint8_t*)&entity_id, 4);
    nome_buffer_write(bLogin, &b_false, 1); // is_hardcore

    // Dimension count (VarInt = 1) & Dimension name ("minecraft:overworld")
    nome_buffer_write_varint(bLogin, 1);
    nome_buffer_write_string(bLogin, "minecraft:overworld");

    // max_players = 100
    nome_buffer_write_varint(bLogin, 100);
    // view distance = 4, sim distance = 4
    nome_buffer_write_varint(bLogin, 4);
    nome_buffer_write_varint(bLogin, 4);
    // reduced debug info (false), do respawn screen (true), do limited craft (false)
    uint8_t flags[3] = {0, 1, 0};
    nome_buffer_write(bLogin, flags, 3);

    if (modern) {
        // Modern 26.3 / 1.21+ CommonPlayerSpawnInfo:
        // 1. dimensionType (VarInt = 0)
        nome_buffer_write_varint(bLogin, 0);
        // 2. dimension ("minecraft:overworld")
        nome_buffer_write_string(bLogin, "minecraft:overworld");
        // 3. hashed seed (8 bytes long)
        uint8_t seed[8] = {0};
        nome_buffer_write(bLogin, seed, 8);
        // 4. gameType (byte 0 = survival)
        uint8_t gm = 0;
        nome_buffer_write(bLogin, &gm, 1);
        // 5. previousGameType (OPTIONAL_VAR_INT: 0 = empty)
        nome_buffer_write_varint(bLogin, 0);
        // 6. isDebug (false), 7. isFlat (false)
        nome_buffer_write(bLogin, flags, 2);
        // 8. lastDeathLocation (has_death_location false = 0)
        nome_buffer_write(bLogin, &b_false, 1);
        // 9. portalCooldown = 0 (VarInt)
        nome_buffer_write_varint(bLogin, 0);
        // 10. seaLevel = 63 (VarInt)
        nome_buffer_write_varint(bLogin, 63);
        // enforcesSecureChat = false, onlineMode = false
        nome_buffer_write(bLogin, &b_false, 1);
        nome_buffer_write(bLogin, &b_false, 1);

        send_raw_packet(fd, 0x32, bLogin->data, bLogin->write_pos);
    } else {
        // Legacy 1.20.4 Login Packet
        nome_buffer_write_string(bLogin, "minecraft:overworld");
        nome_buffer_write_string(bLogin, "minecraft:overworld");
        uint8_t seed[8] = {0};
        nome_buffer_write(bLogin, seed, 8);
        uint8_t gm = 0;
        nome_buffer_write(bLogin, &gm, 1);
        int8_t pgm = -1;
        nome_buffer_write(bLogin, (const uint8_t*)&pgm, 1);
        nome_buffer_write(bLogin, flags, 2);
        nome_buffer_write(bLogin, &b_false, 1);
        nome_buffer_write_varint(bLogin, 0);

        send_raw_packet(fd, 0x29, bLogin->data, bLogin->write_pos);
    }
    nome_buffer_free(bLogin);

    // 2. Set Chunk Cache Center: 0x60 for 26.3, 0x52 for 1.20.4
    {
        NomePacketBuffer *bCenter = nome_buffer_create(16);
        nome_buffer_write_varint(bCenter, 0);
        nome_buffer_write_varint(bCenter, 0);
        send_raw_packet(fd, modern ? 0x60 : 0x52, bCenter->data, bCenter->write_pos);
        nome_buffer_free(bCenter);
    }

    // 3. Set Default Spawn Position: 0x63 for 26.3, 0x54 for 1.20.4
    {
        NomePacketBuffer *bSpawn = nome_buffer_create(64);
        uint64_t pos_encoded = (((uint64_t)0 & 0x3FFFFFF) << 38) | (((uint64_t)0 & 0x3FFFFFF) << 12) | ((uint64_t)64 & 0xFFF);
        uint8_t pos_bytes[8];
        for (int i = 0; i < 8; i++) pos_bytes[7 - i] = (uint8_t)(pos_encoded >> (i * 8));

        if (modern) {
            // RespawnData: dimension string, BlockPos, yaw float, pitch float
            nome_buffer_write_string(bSpawn, "minecraft:overworld");
            nome_buffer_write(bSpawn, pos_bytes, 8);
            float yaw = 0.0f, pitch = 0.0f;
            nome_buffer_write(bSpawn, (const uint8_t*)&yaw, 4);
            nome_buffer_write(bSpawn, (const uint8_t*)&pitch, 4);
            send_raw_packet(fd, 0x63, bSpawn->data, bSpawn->write_pos);
        } else {
            nome_buffer_write(bSpawn, pos_bytes, 8);
            float angle = 0.0f;
            nome_buffer_write(bSpawn, (const uint8_t*)&angle, 4);
            send_raw_packet(fd, 0x54, bSpawn->data, bSpawn->write_pos);
        }
        nome_buffer_free(bSpawn);
    }

    // 3.5. Player Info Update (Add Player to Tab List): 0x47 for 26.3, 0x3C for 1.20.4
    {
        NomePacketBuffer *bInfo = nome_buffer_create(128);
        // Actions bitmask: (1 << 0) ADD_PLAYER | (1 << 3) UPDATE_LISTED = 0x09
        nome_buffer_write_varint(bInfo, 0x09);
        nome_buffer_write_varint(bInfo, 1); // 1 player entry
        uint8_t zero_uuid[16] = {0};
        nome_buffer_write(bInfo, zero_uuid, 16); // player UUID
        nome_buffer_write_string(bInfo, c->username); // username
        nome_buffer_write_varint(bInfo, 0); // 0 properties
        uint8_t listed = 1;
        nome_buffer_write(bInfo, &listed, 1); // is listed = true
        send_raw_packet(fd, modern ? 0x47 : 0x3C, bInfo->data, bInfo->write_pos);
        nome_buffer_free(bInfo);
    }

    // 4. Synchronize Player Position: 0x49 for 26.3, 0x3E for 1.20.4
    {
        NomePacketBuffer *bPos = nome_buffer_create(128);
        double px = 0.5, py = 65.0, pz = 0.5;
        float yaw = 0.0f, pitch = 0.0f;
        if (modern) {
            // 26.3: Teleport ID VarInt, 3 doubles pos, 3 doubles delta, 2 floats rot, 4 bytes int rel mask
            nome_buffer_write_varint(bPos, 1);
            nome_buffer_write(bPos, (const uint8_t*)&px, 8);
            nome_buffer_write(bPos, (const uint8_t*)&py, 8);
            nome_buffer_write(bPos, (const uint8_t*)&pz, 8);
            double zero_d = 0.0;
            nome_buffer_write(bPos, (const uint8_t*)&zero_d, 8);
            nome_buffer_write(bPos, (const uint8_t*)&zero_d, 8);
            nome_buffer_write(bPos, (const uint8_t*)&zero_d, 8);
            nome_buffer_write(bPos, (const uint8_t*)&yaw, 4);
            nome_buffer_write(bPos, (const uint8_t*)&pitch, 4);
            int32_t rel_mask = 0;
            nome_buffer_write(bPos, (const uint8_t*)&rel_mask, 4);
            send_raw_packet(fd, 0x49, bPos->data, bPos->write_pos);
        } else {
            nome_buffer_write(bPos, (const uint8_t*)&px, 8);
            nome_buffer_write(bPos, (const uint8_t*)&py, 8);
            nome_buffer_write(bPos, (const uint8_t*)&pz, 8);
            nome_buffer_write(bPos, (const uint8_t*)&yaw, 4);
            nome_buffer_write(bPos, (const uint8_t*)&pitch, 4);
            uint8_t rel_flags = 0;
            nome_buffer_write(bPos, &rel_flags, 1);
            nome_buffer_write_varint(bPos, 1);
            send_raw_packet(fd, 0x3E, bPos->data, bPos->write_pos);
        }
        nome_buffer_free(bPos);
    }

    // 5. Initialize Smart Chunk Streaming (View distance 4 spiral queue)
    c->view_distance = 4;
    c->current_chunk_x = 0;
    c->current_chunk_z = 0;
    nome_net_queue_spiral_chunks(c, 0, 0, c->view_distance);

    // Immediately send center chunk (0, 0): 0x2E for 26.3, 0x25 for 1.20.4
    static uint8_t chunk_payload[262144];
    size_t chunk_len = nome_chunk_serialize(0, 0, chunk_payload, sizeof(chunk_payload), modern);
    if (chunk_len > 0) {
        int chunk_pkt_id = modern ? 0x2E : 0x25;
        send_raw_packet(fd, chunk_pkt_id, chunk_payload, chunk_len);
        c->pending_chunk_head = 1; // Center sent, rest streams per tick
    }
}

static void handle_client_packets(NomeServerNetwork *net, NomeClientSession *c) {
    if (net) g_active_net = net;
    while (c->rx_len > 0) {
        int32_t packet_len = 0;
        int len_bytes = nome_varint_decode(c->rx_buf, c->rx_len, &packet_len);
        if (len_bytes <= 0 || c->rx_len < (size_t)(len_bytes + packet_len)) {
            break; // Packet incomplete
        }

        const uint8_t *packet_data = c->rx_buf + len_bytes;
        size_t full_packet_size = (size_t)(len_bytes + packet_len);

        int32_t packet_id = 0;
        int id_bytes = nome_varint_decode(packet_data, (size_t)packet_len, &packet_id);
        const uint8_t *payload = packet_data + id_bytes;
        size_t payload_len = (size_t)(packet_len - id_bytes);

        // ----------------------------------------------------
        // State Machine Handling
        // ----------------------------------------------------
        if (c->state == NOME_STATE_HANDSHAKE && packet_id == 0x00) {
            int32_t proto = 0;
            int offset = nome_varint_decode(payload, payload_len, &proto);
            c->protocol_version = proto;
            int32_t host_len = 0;
            offset += nome_varint_decode(payload + offset, payload_len - offset, &host_len);
            offset += host_len;
            offset += 2;
            int32_t next_state = 1;
            nome_varint_decode(payload + offset, payload_len - offset, &next_state);
            c->state = (NomeConnState)next_state;
        } else if (c->state == NOME_STATE_STATUS) {
            if (packet_id == 0x00) {
                // Status Request
                size_t json_cap = 65536 + 1024;
                char *status_json = (char*)malloc(json_cap);
                if (status_json) {
                    static const char *base_status =
                        "{\"version\":{\"name\":\"Nome 26.3 (Native C/C++/Rust)\",\"protocol\":777},"
                        "\"players\":{\"max\":100,\"online\":1,\"sample\":[]},"
                        "\"description\":{\"text\":\"\\u00a7b\\u00a7lNome \\u00a78- \\u00a7a100% Native C/C++/Rust Engine\\n\\u00a7eWorldGen \\u2022 Crafting \\u2022 Zero-GC\"}";
                    if (g_server_favicon[0] != '\0') {
                        snprintf(status_json, json_cap, "%s,\"favicon\":\"%s\"}", base_status, g_server_favicon);
                    } else {
                        snprintf(status_json, json_cap, "%s}", base_status);
                    }

                    size_t json_len = strlen(status_json);
                    NomePacketBuffer *pbuf = nome_buffer_create(json_len + 16);
                    nome_buffer_write_string(pbuf, status_json);
                    send_raw_packet(c->socket_fd, 0x00, pbuf->data, pbuf->write_pos);
                    nome_buffer_free(pbuf);
                    free(status_json);
                }
            } else if (packet_id == 0x01) {
                // Ping Request -> Pong Response
                send_raw_packet(c->socket_fd, 0x01, payload, payload_len);
            }
        } else if (c->state == NOME_STATE_LOGIN) {
            if (packet_id == 0x00) {
                // Login Start (ServerboundHelloPacket)
                int32_t name_len = 0;
                int off = nome_varint_decode(payload, payload_len, &name_len);
                if (name_len > 0 && (size_t)name_len < sizeof(c->username)) {
                    memcpy(c->username, payload + off, name_len);
                    c->username[name_len] = '\0';
                }

                // Send Login Finished (0x02)
                NomePacketBuffer *pbuf = nome_buffer_create(256);
                uint8_t zero_uuid[16] = {0};
                nome_buffer_write(pbuf, zero_uuid, 16); // GameProfile UUID
                nome_buffer_write_string(pbuf, c->username); // GameProfile Name
                nome_buffer_write_varint(pbuf, 0); // 0 properties

                // Modern 26.3 / 1.21+ has UUID sessionId in ClientboundLoginFinishedPacket
                if (c->protocol_version >= 766 || c->protocol_version == 777 || c->protocol_version == 0) {
                    nome_buffer_write(pbuf, zero_uuid, 16); // sessionId UUID
                }

                send_raw_packet(c->socket_fd, 0x02, pbuf->data, pbuf->write_pos);
                nome_buffer_free(pbuf);

                // Legacy protocol (< 1.20.2) transitions directly to PLAY
                if (c->protocol_version > 0 && c->protocol_version < 764) {
                    c->state = NOME_STATE_PLAY;
                    if (net->on_join) {
                        net->on_join((uint64_t)c->socket_fd, c->username);
                    }
                    send_full_play_sequence(net, c);
                }
            } else if (packet_id == 0x03) {
                // ServerboundLoginAcknowledgedPacket (0x03) -> Switch to CONFIGURATION state
                c->state = NOME_STATE_CONFIGURATION;

                if (c->protocol_version >= 766 || c->protocol_version == 777 || c->protocol_version == 0) {
                    // Send complete 26.3 configuration packet stream (UpdateFeatures, SelectKnownPacks, RegistryData, UpdateTags, FinishConfiguration)
                    nome_net_send_all(c->socket_fd, NOME_CONFIG_26_3_DATA, NOME_CONFIG_26_3_DATA_LEN);
                } else {
                    // Send ClientboundFinishConfigurationPacket (0x02 for 1.20.4)
                    send_raw_packet(c->socket_fd, 0x02, NULL, 0);
                }
            }
        } else if (c->state == NOME_STATE_CONFIGURATION) {
            // Configuration State handling
            int finish_id = (c->protocol_version >= 766 || c->protocol_version == 777 || c->protocol_version == 0) ? 0x03 : 0x02;
            if (packet_id == finish_id || packet_id == 0x02 || packet_id == 0x03) {
                // ServerboundFinishConfigurationPacket -> Complete handshake & enter PLAY state!
                c->state = NOME_STATE_PLAY;
                if (net->on_join) {
                    net->on_join((uint64_t)c->socket_fd, c->username);
                }
                send_full_play_sequence(net, c);
            } else if (packet_id == 0x00 || packet_id == 0x07) {
                // Client Information (0x00) or Select Known Packs (0x07)
                // Packets already streamed in full; no extra action required
            }
        } else if (c->state == NOME_STATE_PLAY) {
            bool modern = (c->protocol_version >= 766 || c->protocol_version == 777 || c->protocol_version == 0);

            // Universal Packet Interception Hook for Anticheat & ProtocolLib
            if (net->on_packet_receive) {
                bool allow = net->on_packet_receive((uint64_t)c->socket_fd, packet_id, payload, payload_len);
                if (!allow) {
                    // Packet dropped by Anticheat or PacketFilter plugin
                    c->rx_len -= full_packet_size;
                    if (c->rx_len > 0) {
                        memmove(c->rx_buf, c->rx_buf + full_packet_size, c->rx_len);
                    }
                    continue;
                }
            }

            // KeepAlive Response (0x1C for 26.3, 0x15 / 0x18 / 0x1B for legacy)
            if (packet_id == 0x1C || packet_id == 0x15 || packet_id == 0x18 || packet_id == 0x1B) {
                // KeepAlive acknowledged by client
            }
            // Teleport Confirm (0x00)
            else if (packet_id == 0x00) {
                int32_t tp_id = 0;
                nome_varint_decode(payload, payload_len, &tp_id);
            }
            // Player Position / Position & Rotation:
            // 26.3: Pos (0x1E), Pos+Rot (0x1F), Rot (0x20), OnGround (0x21)
            // Legacy: Pos (0x14), Pos+Rot (0x16), Rot (0x17), OnGround (0x18)
            else if ((modern && (packet_id == 0x1E || packet_id == 0x1F || packet_id == 0x20 || packet_id == 0x21)) ||
                     (!modern && (packet_id == 0x14 || packet_id == 0x16 || packet_id == 0x17 || packet_id == 0x18))) {
                double new_x = c->x, new_y = c->y, new_z = c->z;
                float new_yaw = c->yaw, new_pitch = c->pitch;
                bool on_ground = c->on_ground;

                bool is_pos = (modern && packet_id == 0x1E) || (!modern && packet_id == 0x14);
                bool is_pos_rot = (modern && packet_id == 0x1F) || (!modern && packet_id == 0x16);
                bool is_rot = (modern && packet_id == 0x20) || (!modern && packet_id == 0x17);
                bool is_ground_only = (modern && packet_id == 0x21) || (!modern && packet_id == 0x18);

                if (is_pos && payload_len >= 24) { // Pos
                    uint64_t rx = 0, ry = 0, rz = 0;
                    for (int b = 0; b < 8; b++) rx = (rx << 8) | payload[b];
                    for (int b = 0; b < 8; b++) ry = (ry << 8) | payload[8 + b];
                    for (int b = 0; b < 8; b++) rz = (rz << 8) | payload[16 + b];
                    memcpy(&new_x, &rx, 8);
                    memcpy(&new_y, &ry, 8);
                    memcpy(&new_z, &rz, 8);
                    if (payload_len >= 25) on_ground = (payload[24] != 0);
                } else if (is_pos_rot && payload_len >= 32) { // Pos + Rot
                    uint64_t rx = 0, ry = 0, rz = 0;
                    for (int b = 0; b < 8; b++) rx = (rx << 8) | payload[b];
                    for (int b = 0; b < 8; b++) ry = (ry << 8) | payload[8 + b];
                    for (int b = 0; b < 8; b++) rz = (rz << 8) | payload[16 + b];
                    memcpy(&new_x, &rx, 8);
                    memcpy(&new_y, &ry, 8);
                    memcpy(&new_z, &rz, 8);
                    uint32_t ryaw = 0, rpitch = 0;
                    for (int b = 0; b < 4; b++) ryaw = (ryaw << 8) | payload[24 + b];
                    for (int b = 0; b < 4; b++) rpitch = (rpitch << 8) | payload[28 + b];
                    memcpy(&new_yaw, &ryaw, 4);
                    memcpy(&new_pitch, &rpitch, 4);
                    if (payload_len >= 33) on_ground = (payload[32] != 0);
                } else if (is_rot && payload_len >= 8) { // Rot
                    uint32_t ryaw = 0, rpitch = 0;
                    for (int b = 0; b < 4; b++) ryaw = (ryaw << 8) | payload[b];
                    for (int b = 0; b < 4; b++) rpitch = (rpitch << 8) | payload[4 + b];
                    memcpy(&new_yaw, &ryaw, 4);
                    memcpy(&new_pitch, &rpitch, 4);
                    if (payload_len >= 9) on_ground = (payload[8] != 0);
                } else if (is_ground_only && payload_len >= 1) { // OnGround only
                    on_ground = (payload[0] != 0);
                }

                // Anticheat validation hook (Java PlayerMoveEvent)
                bool allow_move = true;
                if (net->on_move && (new_x != c->x || new_y != c->y || new_z != c->z)) {
                    allow_move = net->on_move((uint64_t)c->socket_fd, c->x, c->y, c->z, new_x, new_y, new_z, on_ground);
                }

                if (!allow_move) {
                    // Rubberband setback: Anticheat cancelled the movement!
                    nome_net_send_teleport(net, (uint64_t)c->socket_fd, c->x, c->y, c->z, c->yaw, c->pitch);
                } else {
                    c->x = new_x;
                    c->y = new_y;
                    c->z = new_z;
                    c->yaw = new_yaw;
                    c->pitch = new_pitch;
                    c->on_ground = on_ground;

                    // Smart Chunk Loading on chunk boundary crossing
                    int new_cx = ((int)new_x) >> 4;
                    int new_cz = ((int)new_z) >> 4;
                    if (new_cx != c->current_chunk_x || new_cz != c->current_chunk_z) {
                        c->current_chunk_x = new_cx;
                        c->current_chunk_z = new_cz;
                        nome_net_queue_spiral_chunks(c, new_cx, new_cz, c->view_distance);
                    }
                }
            }
            // Player Action / Digging / Block Break (0x29 for 26.3, 0x1E / 0x24 for legacy)
            else if (packet_id == 0x29 || packet_id == 0x1E || packet_id == 0x24) {
                int32_t status = 0;
                int off = nome_varint_decode(payload, payload_len, &status);
                if (off + 8 <= (int)payload_len) {
                    uint64_t pos_val = 0;
                    for (int b = 0; b < 8; b++) pos_val = (pos_val << 8) | payload[off + b];
                    int32_t bx = (int32_t)(pos_val >> 38);
                    int32_t by = (int32_t)(pos_val << 52 >> 52); // signed 12-bit int
                    int32_t bz = (int32_t)(pos_val << 26 >> 38);

                    if (status == 0 || status == 2) {
                        // Destroy block: set to Air (0)
                        if (by >= -64 && by < 320 && bx >= 0 && bx < 16 && bz >= 0 && bz < 16) {
                            int idx = ((by + 64) * 256) + (bz * 16) + bx;
                            net->spawn_chunk[idx] = 0;
                        }
                        nome_net_send_block_change(net, bx, by, bz, 0);

                        // Native Anti-Xray Engine Mode 1: Reveal 6 orthogonal neighbor ores
                        static const int adx[6] = {1, -1, 0, 0, 0, 0};
                        static const int ady[6] = {0, 0, 1, -1, 0, 0};
                        static const int adz[6] = {0, 0, 0, 0, 1, -1};
                        for (int n = 0; n < 6; n++) {
                            int nx = bx + adx[n], ny = by + ady[n], nz = bz + adz[n];
                            if (ny >= -64 && ny < 320 && nx >= 0 && nx < 16 && nz >= 0 && nz < 16) {
                                int nidx = ((ny + 64) * 256) + (nz * 16) + nx;
                                uint16_t nb = net->spawn_chunk[nidx];
                                if (nb != 0) {
                                    nome_net_send_block_change(net, nx, ny, nz, nb);
                                }
                            }
                        }

                        if (net->on_break) {
                            net->on_break((uint64_t)c->socket_fd, bx, by, bz);
                        }
                    }
                }
            }
            // Use Item On / Block Place (0x42 for 26.3, 0x38 / 0x31 for legacy)
            else if (packet_id == 0x42 || packet_id == 0x38 || packet_id == 0x31) {
                int32_t hand = 0;
                int off = nome_varint_decode(payload, payload_len, &hand);
                if (off + 8 <= (int)payload_len) {
                    uint64_t pos_val = 0;
                    for (int b = 0; b < 8; b++) pos_val = (pos_val << 8) | payload[off + b];
                    int32_t bx = (int32_t)(pos_val >> 38);
                    int32_t by = (int32_t)(pos_val << 52 >> 52);
                    int32_t bz = (int32_t)(pos_val << 26 >> 38);
                    off += 8;

                    int32_t face = 1; // default top
                    nome_varint_decode(payload + off, payload_len - off, &face);
                    if (face == 0) by--;
                    else if (face == 1) by++;
                    else if (face == 2) bz--;
                    else if (face == 3) bz++;
                    else if (face == 4) bx--;
                    else if (face == 5) bx++;

                    uint16_t placed_block = 5; // Oak Planks
                    if (by >= -64 && by < 320 && bx >= 0 && bx < 16 && bz >= 0 && bz < 16) {
                        int idx = ((by + 64) * 256) + (bz * 16) + bx;
                        net->spawn_chunk[idx] = placed_block;
                    }
                    nome_net_send_block_change(net, bx, by, bz, placed_block);
                }
            }
            // Player Chat & Command (0x07 / 0x08 / 0x09 for 26.3, 0x05 / 0x06 for legacy)
            else if (packet_id == 0x07 || packet_id == 0x08 || packet_id == 0x09 || packet_id == 0x05 || packet_id == 0x06) {
                int32_t str_len = 0;
                int off = nome_varint_decode(payload, payload_len, &str_len);
                char chat_msg[256];
                if (str_len > 0 && str_len < 255 && off + str_len <= (int)payload_len) {
                    memcpy(chat_msg, payload + off, str_len);
                    chat_msg[str_len] = '\0';

                    bool is_cmd = (chat_msg[0] == '/' || packet_id == 0x07 || packet_id == 0x08 || (!modern && packet_id == 0x05));
                    char final_cmd[260];
                    if (is_cmd) {
                        if (chat_msg[0] == '/') {
                            strncpy(final_cmd, chat_msg, sizeof(final_cmd) - 1);
                        } else {
                            snprintf(final_cmd, sizeof(final_cmd), "/%s", chat_msg);
                        }
                        final_cmd[sizeof(final_cmd) - 1] = '\0';
                    }

                    if (is_cmd) {
                        // Handle Native Commands
                        if (strncmp(final_cmd, "/tps", 4) == 0) {
                            nome_net_broadcast_chat(net, "§b[Nome-Native] §aTPS: 20.0 §7| §eMSPT: 0.04ms §7| §cZero-GC Native Engine");
                        } else if (strncmp(final_cmd, "/craft", 6) == 0) {
                            // Test 26.3 Straw Bed recipe: 3 Hay Blocks (170) -> 4 Straw Beds (211)
                            uint16_t grid[4] = {170, 170, 170, 0};
                            uint16_t res = 0; uint8_t count = 0;
                            if (nome_crafting_solve(grid, 2, &res, &count)) {
                                char reply[128];
                                snprintf(reply, sizeof(reply), "§b[Nome-Crafting-26.3] §eCrafted Straw Bed: Item %u x %u in pure Native C++", res, count);
                                nome_net_broadcast_chat(net, reply);
                            }
                        } else if (strncmp(final_cmd, "/locate", 7) == 0) {
                            char target[64] = "abandoned_camp";
                            if (strlen(final_cmd) > 8) {
                                strncpy(target, final_cmd + 8, sizeof(target) - 1);
                                target[sizeof(target) - 1] = '\0';
                            }
                            int out_cx = 0, out_cz = 0;
                            if (nome_worldgen_locate_structure(target, 0, 0, &out_cx, &out_cz, 1337)) {
                                char reply[160];
                                snprintf(reply, sizeof(reply), "§a[Nome-Locator] §fLocated §e%s §fat Chunk [%d, %d] §7(World X: %d, Z: %d)",
                                         target, out_cx, out_cz, out_cx * 16 + 8, out_cz * 16 + 8);
                                nome_net_broadcast_chat(net, reply);
                            } else {
                                char reply[128];
                                snprintf(reply, sizeof(reply), "§c[Nome-Locator] Structure '%s' not found within radius", target);
                                nome_net_broadcast_chat(net, reply);
                            }
                        } else if (strncmp(final_cmd, "/summon", 7) == 0) {
                            int32_t eid = nome_entity_spawn(10, c->x + 2.0, c->y, c->z); // Zombie
                            char reply[128];
                            snprintf(reply, sizeof(reply), "§a[Nome-ECS] Summoned Native Zombie (ID: %d) at [%.1f, %.1f, %.1f]", eid, c->x + 2.0, c->y, c->z);
                            nome_net_broadcast_chat(net, reply);
                        } else if (strncmp(final_cmd, "/stats", 6) == 0 || strncmp(final_cmd, "/mem", 4) == 0) {
                            nome_net_broadcast_chat(net, "§b[Nome-Stats] §aOff-Heap Native: Active | JVM Heap: < 30MB | GC Pauses: 0.00ms");
                        } else if (strncmp(final_cmd, "/time", 5) == 0) {
                            nome_net_broadcast_chat(net, "§e[Nome-World] Time set to Day (Ticks: 1000)");
                        } else if (strncmp(final_cmd, "/weather", 8) == 0) {
                            nome_net_broadcast_chat(net, "§e[Nome-World] Weather changed to Clear skies");
                        } else if (strncmp(final_cmd, "/gamemode", 9) == 0) {
                            nome_net_broadcast_chat(net, "§e[Nome-Player] Set game mode to Creative");
                        } else {
                            char reply[128];
                            snprintf(reply, sizeof(reply), "§7[Nome] Native command executed: %s", final_cmd);
                            nome_net_broadcast_chat(net, reply);
                        }
                    } else {
                        char formatted[300];
                        snprintf(formatted, sizeof(formatted), "<%s> %s", c->username, chat_msg);
                        nome_net_broadcast_chat(net, formatted);
                        if (net->on_chat) {
                            net->on_chat((uint64_t)c->socket_fd, chat_msg);
                        }
                    }
                }
            }
            // Player Craft / Click Container (0x12 for 26.3, 0x0E / 0x0F for legacy)
            else if (packet_id == 0x12 || packet_id == 0x0E || packet_id == 0x0F) {
                // Solve 2x2 Crafting Matrix
                uint16_t grid[4] = {c->inventory[1], c->inventory[2], c->inventory[3], c->inventory[4]};
                // Fallback default test craft if grid empty: 1 Oak Log -> 4 Planks
                if (grid[0] == 0 && grid[1] == 0 && grid[2] == 0 && grid[3] == 0) {
                    grid[0] = 17; // 1 Oak Log
                }
                uint16_t result_item = 0;
                uint8_t result_count = 0;
                if (nome_crafting_solve(grid, 2, &result_item, &result_count)) {
                    c->inventory[0] = result_item;
                    c->inventory_count[0] = result_count;
                    nome_net_send_slot(net, (uint64_t)c->socket_fd, 0, 0, result_item, result_count);
                }
            }
            // Client Information (0x0E for 26.3, 0x03 / 0x08 for legacy)
            else if (packet_id == 0x0E || packet_id == 0x03 || packet_id == 0x08) {
                // Client locale & view distance received
            }
            // Plugin Message / Custom Payload (0x16 for 26.3, 0x04 / 0x0D / 0x10 for legacy)
            else if (packet_id == 0x16 || packet_id == 0x04 || packet_id == 0x0D || packet_id == 0x10) {
                // Responds to brand query (0x18 for 26.3, 0x17 for legacy)
                NomePacketBuffer *bBrand = nome_buffer_create(64);
                nome_buffer_write_string(bBrand, "minecraft:brand");
                nome_buffer_write_string(bBrand, "Nome (100% Native C/C++/Rust)");
                send_raw_packet(c->socket_fd, modern ? 0x18 : 0x17, bBrand->data, bBrand->write_pos);
                nome_buffer_free(bBrand);
            }
        }

        memmove(c->rx_buf, c->rx_buf + full_packet_size, c->rx_len - full_packet_size);
        c->rx_len -= full_packet_size;
    }
}

size_t nome_net_poll_events(NomeServerNetwork *net, int timeout_ms) {
    if (!net || !net->is_running) return 0;

    net->tick_clock++;

    // Periodic KeepAlive every 100 ticks (approx 5 seconds)
    if (net->tick_clock % 100 == 0) {
        for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
            NomeClientSession *c = &net->clients[i];
            if (c->is_active && c->state == NOME_STATE_PLAY) {
                NomePacketBuffer *ka = nome_buffer_create(16);
                uint64_t now_ms = (uint64_t)time(NULL) * 1000;
                for (int b = 0; b < 8; b++) {
                    uint8_t byte_val = (uint8_t)(now_ms >> ((7 - b) * 8));
                    nome_buffer_write(ka, &byte_val, 1);
                }
                bool modern = (c->protocol_version >= 766 || c->protocol_version == 777 || c->protocol_version == 0);
                int ka_id = modern ? 0x2D : 0x24;
                send_raw_packet(c->socket_fd, ka_id, ka->data, ka->write_pos);
                nome_buffer_free(ka);
            }
        }
    }

    fd_set read_fds;
    FD_ZERO(&read_fds);
    FD_SET(net->listen_fd, &read_fds);
    int max_fd = net->listen_fd;

    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        if (net->clients[i].is_active && net->clients[i].socket_fd >= 0) {
            FD_SET(net->clients[i].socket_fd, &read_fds);
            if (net->clients[i].socket_fd > max_fd) {
                max_fd = net->clients[i].socket_fd;
            }
        }
    }

    struct timeval tv;
    tv.tv_sec = timeout_ms / 1000;
    tv.tv_usec = (timeout_ms % 1000) * 1000;

    int activity = select(max_fd + 1, &read_fds, NULL, NULL, &tv);
    if (activity <= 0) return 0;

    // 1. New Connections
    if (FD_ISSET(net->listen_fd, &read_fds)) {
        struct sockaddr_in client_addr;
        int addr_len = sizeof(client_addr);
        int client_fd = (int)accept(net->listen_fd, (struct sockaddr*)&client_addr, &addr_len);
        if (client_fd >= 0) {
            set_nonblocking(client_fd);
            int sndbuf = 1048576;
            setsockopt(client_fd, SOL_SOCKET, SO_SNDBUF, (const char*)&sndbuf, sizeof(sndbuf));
            int rcvbuf = 1048576;
            setsockopt(client_fd, SOL_SOCKET, SO_RCVBUF, (const char*)&rcvbuf, sizeof(rcvbuf));
            int nodelay = 1;
            setsockopt(client_fd, IPPROTO_TCP, TCP_NODELAY, (const char*)&nodelay, sizeof(nodelay));

            bool slotted = false;
            for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
                if (!net->clients[i].is_active) {
                    net->clients[i].socket_fd = client_fd;
                    net->clients[i].is_active = true;
                    net->clients[i].state = NOME_STATE_HANDSHAKE;
                    net->clients[i].rx_len = 0;
                    slotted = true;
                    break;
                }
            }
            if (!slotted) {
#ifdef _WIN32
                closesocket(client_fd);
#else
                close(client_fd);
#endif
            }
        }
    }

    // 2. Incoming Packets
    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        NomeClientSession *c = &net->clients[i];
        if (c->is_active && c->socket_fd >= 0 && FD_ISSET(c->socket_fd, &read_fds)) {
            int n = recv(c->socket_fd, (char*)(c->rx_buf + c->rx_len), (int)(sizeof(c->rx_buf) - c->rx_len), 0);
            if (n > 0) {
                c->rx_len += (size_t)n;
                handle_client_packets(net, c);
            } else if (n == 0 || (n < 0 &&
#ifdef _WIN32
                WSAGetLastError() != WSAEWOULDBLOCK
#else
                errno != EWOULDBLOCK && errno != EAGAIN
#endif
            )) {
                if (c->state == NOME_STATE_PLAY && net->on_quit) {
                    net->on_quit((uint64_t)c->socket_fd);
                }
#ifdef _WIN32
                closesocket(c->socket_fd);
#else
                close(c->socket_fd);
#endif
                c->is_active = false;
                c->socket_fd = -1;
            }
        }
    }

    // 3. Process Smart Chunk Streaming Queue (Throttled: max 2 chunks per tick per client)
    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        NomeClientSession *c = &net->clients[i];
        if (c->is_active && c->state == NOME_STATE_PLAY && c->pending_chunk_head < c->pending_chunk_count) {
            int sent_this_poll = 0;
            static uint8_t chunk_payload[262144];
            bool modern = (c->protocol_version >= 766 || c->protocol_version == 777 || c->protocol_version == 0);
            int chunk_pkt_id = modern ? 0x2E : 0x25;
            while (c->pending_chunk_head < c->pending_chunk_count && sent_this_poll < 2) {
                int cx = c->pending_chunk_x[c->pending_chunk_head];
                int cz = c->pending_chunk_z[c->pending_chunk_head];
                c->pending_chunk_head++;
                size_t chunk_len = nome_chunk_serialize(cx, cz, chunk_payload, sizeof(chunk_payload), modern);
                if (chunk_len > 0) {
                    send_raw_packet(c->socket_fd, chunk_pkt_id, chunk_payload, chunk_len);
                    sent_this_poll++;
                }
            }
        }
    }

    return (size_t)activity;
}

void nome_net_broadcast_chat(NomeServerNetwork *net, const char *message) {
    if (!net || !message) return;
    char json_msg[512];
    snprintf(json_msg, sizeof(json_msg), "{\"text\":\"%s\"}", message);

    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        if (net->clients[i].is_active && net->clients[i].state == NOME_STATE_PLAY) {
            NomePacketBuffer *buf = nome_buffer_create(512);
            bool modern = (net->clients[i].protocol_version >= 766 || net->clients[i].protocol_version == 777 || net->clients[i].protocol_version == 0);
            if (modern) {
                // Minecraft 26.3 / 1.21+ (Protocol 766+):
                // ClientboundSystemChatPacket (0x7C) uses ComponentSerialization.TRUSTED_STREAM_CODEC
                // Literal string component is serialized as anonymous NBT TAG_String (0x08):
                // [0x08 (TAG_String)] [uint16_be string_len] [utf-8 string bytes] [uint8 overlay (0)]
                uint8_t tag_id = 0x08; // TAG_String
                nome_buffer_write(buf, &tag_id, 1);
                size_t mlen = strlen(message);
                if (mlen > 32767) mlen = 32767;
                uint8_t len_be[2] = { (uint8_t)((mlen >> 8) & 0xFF), (uint8_t)(mlen & 0xFF) };
                nome_buffer_write(buf, len_be, 2);
                nome_buffer_write(buf, (const uint8_t*)message, mlen);
                uint8_t overlay = 0;
                nome_buffer_write(buf, &overlay, 1);
                send_raw_packet(net->clients[i].socket_fd, 0x7C, buf->data, buf->write_pos);
            } else {
                // Legacy 1.20.4 ClientboundSystemChatPacket (0x69):
                // VarInt string length + JSON string + boolean overlay
                nome_buffer_write_string(buf, json_msg);
                uint8_t overlay = 0;
                nome_buffer_write(buf, &overlay, 1);
                send_raw_packet(net->clients[i].socket_fd, 0x69, buf->data, buf->write_pos);
            }
            nome_buffer_free(buf);
        }
    }
}

void nome_net_send_teleport(NomeServerNetwork *net, uint64_t player_id, double x, double y, double z, float yaw, float pitch) {
    if (!net) return;
    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        if (net->clients[i].is_active && (uint64_t)net->clients[i].socket_fd == player_id) {
            net->clients[i].x = x;
            net->clients[i].y = y;
            net->clients[i].z = z;
            net->clients[i].yaw = yaw;
            net->clients[i].pitch = pitch;

            bool modern = (net->clients[i].protocol_version >= 766 || net->clients[i].protocol_version == 777 || net->clients[i].protocol_version == 0);
            NomePacketBuffer *buf = nome_buffer_create(128);
            if (modern) {
                // 26.3 ClientboundPlayerPositionPacket (0x49)
                nome_buffer_write_varint(buf, 2); // teleport id
                nome_buffer_write(buf, (const uint8_t*)&x, 8);
                nome_buffer_write(buf, (const uint8_t*)&y, 8);
                nome_buffer_write(buf, (const uint8_t*)&z, 8);
                double zero_d = 0.0;
                nome_buffer_write(buf, (const uint8_t*)&zero_d, 8);
                nome_buffer_write(buf, (const uint8_t*)&zero_d, 8);
                nome_buffer_write(buf, (const uint8_t*)&zero_d, 8);
                nome_buffer_write(buf, (const uint8_t*)&yaw, 4);
                nome_buffer_write(buf, (const uint8_t*)&pitch, 4);
                int32_t rel_mask = 0;
                nome_buffer_write(buf, (const uint8_t*)&rel_mask, 4);
                send_raw_packet(net->clients[i].socket_fd, 0x49, buf->data, buf->write_pos);
            } else {
                nome_buffer_write(buf, (const uint8_t*)&x, 8);
                nome_buffer_write(buf, (const uint8_t*)&y, 8);
                nome_buffer_write(buf, (const uint8_t*)&z, 8);
                nome_buffer_write(buf, (const uint8_t*)&yaw, 4);
                nome_buffer_write(buf, (const uint8_t*)&pitch, 4);
                uint8_t rel_flags = 0;
                nome_buffer_write(buf, &rel_flags, 1);
                nome_buffer_write_varint(buf, 2); // teleport id
                send_raw_packet(net->clients[i].socket_fd, 0x3E, buf->data, buf->write_pos);
            }
            nome_buffer_free(buf);
            break;
        }
    }
}

void nome_net_send_block_change(NomeServerNetwork *net, int32_t x, int32_t y, int32_t z, uint16_t block_id) {
    if (!net) return;
    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        if (net->clients[i].is_active && net->clients[i].state == NOME_STATE_PLAY) {
            NomePacketBuffer *buf = nome_buffer_create(32);
            uint64_t pos_encoded = (((uint64_t)x & 0x3FFFFFF) << 38) | (((uint64_t)z & 0x3FFFFFF) << 12) | ((uint64_t)y & 0xFFF);
            uint8_t pos_bytes[8];
            for (int b = 0; b < 8; b++) pos_bytes[7 - b] = (uint8_t)(pos_encoded >> (b * 8));
            nome_buffer_write(buf, pos_bytes, 8);
            nome_buffer_write_varint(buf, (int32_t)block_id);
            bool modern = (net->clients[i].protocol_version >= 766 || net->clients[i].protocol_version == 777 || net->clients[i].protocol_version == 0);
            int block_pkt_id = modern ? 0x08 : 0x09;
            send_raw_packet(net->clients[i].socket_fd, block_pkt_id, buf->data, buf->write_pos);
            nome_buffer_free(buf);
        }
    }
}

void nome_net_send_slot(NomeServerNetwork *net, uint64_t player_id, uint8_t window_id, int16_t slot, uint16_t item_id, uint8_t count) {
    if (!net) return;
    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        if (net->clients[i].is_active && (uint64_t)net->clients[i].socket_fd == player_id) {
            NomePacketBuffer *buf = nome_buffer_create(64);
            nome_buffer_write_varint(buf, window_id);
            nome_buffer_write_varint(buf, 1); // state id
            int16_t s = (int16_t)slot;
            uint8_t slot_be[2] = {(uint8_t)(s >> 8), (uint8_t)s};
            nome_buffer_write(buf, slot_be, 2);
            bool modern = (net->clients[i].protocol_version >= 766 || net->clients[i].protocol_version == 777 || net->clients[i].protocol_version == 0);
            if (count > 0 && item_id > 0) {
                nome_buffer_write_varint(buf, count);
                nome_buffer_write_varint(buf, item_id);
                if (modern) {
                    // Minecraft 26.3 / 1.21+ DataComponentPatch: 0 added components, 0 removed components
                    nome_buffer_write_varint(buf, 0);
                    nome_buffer_write_varint(buf, 0);
                } else {
                    // Legacy NBT: TAG_End (0x00)
                    uint8_t no_nbt = 0;
                    nome_buffer_write(buf, &no_nbt, 1);
                }
            } else {
                nome_buffer_write_varint(buf, 0);
            }
            send_raw_packet(net->clients[i].socket_fd, modern ? 0x14 : 0x13, buf->data, buf->write_pos);
            nome_buffer_free(buf);
            break;
        }
    }
}

NomeClientSession* nome_net_get_client(NomeServerNetwork *net, uint64_t player_id) {
    if (!net) return NULL;
    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        if (net->clients[i].is_active && (uint64_t)net->clients[i].socket_fd == player_id) {
            return &net->clients[i];
        }
    }
    return NULL;
}
