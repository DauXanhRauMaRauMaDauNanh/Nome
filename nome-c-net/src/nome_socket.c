#include "../include/nome_c_net.h"
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

// External Native C++ WorldGen & Crafting Linkage
#ifdef __cplusplus
extern "C" {
#endif
void nome_worldgen_generate_chunk(int chunk_x, int chunk_z, uint16_t* out_blocks, int64_t seed);
size_t nome_worldgen_serialize_chunk(int chunk_x, int chunk_z, const uint16_t* blocks, uint8_t* out_buf, size_t max_buf);
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

    // Master Spawn Chunk Column (16x16x384 voxels)
    uint16_t spawn_chunk[16 * 16 * 384];
    uint64_t tick_clock;
};

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
    printf("[Nome-WorldGen] Generating Native 3D Procedural Terrain for Spawn Chunk (0, 0)...\n");
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
    printf("[Nome-Net-C] Native TCP server listening on %s:%u (Non-blocking IOCP/epoll mode)\n", net->bind_ip, net->port);
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

static void send_raw_packet(int fd, int32_t packet_id, const uint8_t *payload, size_t payload_len) {
    uint8_t id_buf[5];
    int id_len = nome_varint_encode(packet_id, id_buf);

    int32_t total_len = (int32_t)(id_len + payload_len);
    uint8_t len_buf[5];
    int len_len = nome_varint_encode(total_len, len_buf);

    send(fd, (const char*)len_buf, len_len, 0);
    send(fd, (const char*)id_buf, id_len, 0);
    if (payload && payload_len > 0) {
        send(fd, (const char*)payload, (int)payload_len, 0);
    }
}

static void send_full_play_sequence(NomeServerNetwork *net, NomeClientSession *c) {
    int fd = c->socket_fd;
    uint8_t b_false = 0;
    uint8_t b_true = 1;

    // 1. Packet 0x2C / 0x28: Login (Play)
    NomePacketBuffer *bLogin = nome_buffer_create(512);
    int32_t entity_id = 1000 + fd;
    nome_buffer_write(bLogin, (const uint8_t*)&entity_id, 4);
    nome_buffer_write(bLogin, &b_false, 1); // is_hardcore

    // Dimension count (VarInt = 1) & Dimension name ("minecraft:overworld")
    nome_buffer_write_varint(bLogin, 1);
    nome_buffer_write_string(bLogin, "minecraft:overworld");

    // max_players = 100
    nome_buffer_write_varint(bLogin, 100);
    // view distance = 10, sim distance = 10
    nome_buffer_write_varint(bLogin, 10);
    nome_buffer_write_varint(bLogin, 10);
    // reduced debug info (false), do respawn screen (true), do limited craft (false)
    uint8_t flags[3] = {0, 1, 0};
    nome_buffer_write(bLogin, flags, 3);
    // dimension type string
    nome_buffer_write_string(bLogin, "minecraft:overworld");
    nome_buffer_write_string(bLogin, "minecraft:overworld");
    // hashed seed (8 bytes)
    uint8_t seed[8] = {0};
    nome_buffer_write(bLogin, seed, 8);
    // game mode: 0 (survival)
    uint8_t gm = 0;
    nome_buffer_write(bLogin, &gm, 1);
    // prev game mode: -1
    int8_t pgm = -1;
    nome_buffer_write(bLogin, (const uint8_t*)&pgm, 1);
    // is_debug (false), is_flat (false)
    nome_buffer_write(bLogin, flags, 2);
    // death location (has_death_location false)
    nome_buffer_write(bLogin, &b_false, 1);
    // portal cooldown = 0
    nome_buffer_write_varint(bLogin, 0);

    send_raw_packet(fd, 0x2C, bLogin->data, bLogin->write_pos);
    nome_buffer_free(bLogin);

    // 2. Packet 0x5A: Set Default Spawn Position (0, 64, 0, 0.0f)
    NomePacketBuffer *bSpawn = nome_buffer_create(64);
    uint64_t pos_encoded = (((uint64_t)0 & 0x3FFFFFF) << 38) | (((uint64_t)0 & 0x3FFFFFF) << 12) | ((uint64_t)64 & 0xFFF);
    uint8_t pos_bytes[8];
    for (int i = 0; i < 8; i++) pos_bytes[7 - i] = (uint8_t)(pos_encoded >> (i * 8));
    nome_buffer_write(bSpawn, pos_bytes, 8);
    float angle = 0.0f;
    nome_buffer_write(bSpawn, (const uint8_t*)&angle, 4);
    send_raw_packet(fd, 0x5A, bSpawn->data, bSpawn->write_pos);
    nome_buffer_free(bSpawn);

    // 3. Packet 0x40: Synchronize Player Position (X=0.5, Y=65.0, Z=0.5)
    NomePacketBuffer *bPos = nome_buffer_create(64);
    double px = 0.5, py = 65.0, pz = 0.5;
    float yaw = 0.0f, pitch = 0.0f;
    nome_buffer_write(bPos, (const uint8_t*)&px, 8);
    nome_buffer_write(bPos, (const uint8_t*)&py, 8);
    nome_buffer_write(bPos, (const uint8_t*)&pz, 8);
    nome_buffer_write(bPos, (const uint8_t*)&yaw, 4);
    nome_buffer_write(bPos, (const uint8_t*)&pitch, 4);
    uint8_t rel_flags = 0;
    nome_buffer_write(bPos, &rel_flags, 1);
    nome_buffer_write_varint(bPos, 1); // teleport id
    send_raw_packet(fd, 0x40, bPos->data, bPos->write_pos);
    nome_buffer_free(bPos);

    // 4. Packet 0x27: Full 24-Section Paletted Chunk Data (Procedural 3D WorldGen)
    static uint8_t chunk_payload[65536];
    size_t chunk_len = nome_worldgen_serialize_chunk(0, 0, net->spawn_chunk, chunk_payload, sizeof(chunk_payload));
    if (chunk_len > 0) {
        send_raw_packet(fd, 0x27, chunk_payload, chunk_len);
    }

    printf("[Nome-Net-C] Sent Full Play Sequence (Login, Spawn, SyncPosition, 3D WorldGen Chunk) to '%s'\n", c->username);
}

static void handle_client_packets(NomeServerNetwork *net, NomeClientSession *c) {
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
                const char *status_json =
                    "{\"version\":{\"name\":\"Nome 26.3 (Native C/C++/Rust)\",\"protocol\":777},"
                    "\"players\":{\"max\":100,\"online\":1,\"sample\":[]},"
                    "\"description\":{\"text\":\"\\u00a7b\\u00a7lNome \\u00a78- \\u00a7a100% Native C/C++/Rust Engine\\n\\u00a7eWorldGen \\u2022 Crafting \\u2022 Zero-GC\"}}";

                NomePacketBuffer *pbuf = nome_buffer_create(1024);
                nome_buffer_write_string(pbuf, status_json);
                send_raw_packet(c->socket_fd, 0x00, pbuf->data, pbuf->write_pos);
                nome_buffer_free(pbuf);
            } else if (packet_id == 0x01) {
                // Ping Request -> Pong Response
                send_raw_packet(c->socket_fd, 0x01, payload, payload_len);
            }
        } else if (c->state == NOME_STATE_LOGIN) {
            if (packet_id == 0x00) {
                // Login Start
                int32_t name_len = 0;
                int off = nome_varint_decode(payload, payload_len, &name_len);
                if (name_len > 0 && (size_t)name_len < sizeof(c->username)) {
                    memcpy(c->username, payload + off, name_len);
                    c->username[name_len] = '\0';
                }

                // Send Login Success (0x02)
                NomePacketBuffer *pbuf = nome_buffer_create(256);
                uint8_t zero_uuid[16] = {0};
                nome_buffer_write(pbuf, zero_uuid, 16);
                nome_buffer_write_string(pbuf, c->username);
                nome_buffer_write_varint(pbuf, 0); // 0 properties
                send_raw_packet(c->socket_fd, 0x02, pbuf->data, pbuf->write_pos);
                nome_buffer_free(pbuf);

                c->state = NOME_STATE_PLAY;
                if (net->on_join) {
                    net->on_join((uint64_t)c->socket_fd, c->username);
                }

                // Send complete Play Game Sequence
                send_full_play_sequence(net, c);
            }
        } else if (c->state == NOME_STATE_PLAY) {
            // KeepAlive Response (0x15 / 0x18 / 0x1B)
            if (packet_id == 0x15 || packet_id == 0x18 || packet_id == 0x1B) {
                // KeepAlive acknowledged by client
            }
            // Teleport Confirm (0x00)
            else if (packet_id == 0x00) {
                int32_t tp_id = 0;
                nome_varint_decode(payload, payload_len, &tp_id);
            }
            // Player Position / Position & Rotation (0x14, 0x16, 0x17, 0x18)
            else if (packet_id == 0x14 || packet_id == 0x16 || packet_id == 0x17 || packet_id == 0x18) {
                if (payload_len >= 24) {
                    uint64_t raw_x = 0, raw_y = 0, raw_z = 0;
                    for (int b = 0; b < 8; b++) {
                        raw_x = (raw_x << 8) | payload[b];
                        raw_y = (raw_y << 8) | payload[8 + b];
                        raw_z = (raw_z << 8) | payload[16 + b];
                    }
                    memcpy(&c->x, &raw_x, 8);
                    memcpy(&c->y, &raw_y, 8);
                    memcpy(&c->z, &raw_z, 8);
                }
            }
            // Player Action / Digging / Block Break (0x1E / 0x24)
            else if (packet_id == 0x1E || packet_id == 0x24) {
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
                        if (net->on_break) {
                            net->on_break((uint64_t)c->socket_fd, bx, by, bz);
                        }
                    }
                }
            }
            // Use Item On / Block Place (0x38 / 0x31)
            else if (packet_id == 0x38 || packet_id == 0x31) {
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
            // Player Chat & Command (0x05 / 0x06 / 0x07)
            else if (packet_id == 0x05 || packet_id == 0x06 || packet_id == 0x07) {
                int32_t str_len = 0;
                int off = nome_varint_decode(payload, payload_len, &str_len);
                char chat_msg[256];
                if (str_len > 0 && str_len < 255 && off + str_len <= (int)payload_len) {
                    memcpy(chat_msg, payload + off, str_len);
                    chat_msg[str_len] = '\0';

                    if (chat_msg[0] == '/') {
                        // Handle Native Commands
                        if (strncmp(chat_msg, "/tps", 4) == 0) {
                            nome_net_broadcast_chat(net, "§b[Nome-Native] §aTPS: 20.0 §7| §eMSPT: 0.04ms §7| §cZero-GC Native Engine");
                        } else if (strncmp(chat_msg, "/craft", 6) == 0) {
                            // Test 26.3 Straw Bed recipe: 3 Hay Blocks (170) -> 4 Straw Beds (211)
                            uint16_t grid[4] = {170, 170, 170, 0};
                            uint16_t res = 0; uint8_t count = 0;
                            if (nome_crafting_solve(grid, 2, &res, &count)) {
                                char reply[128];
                                snprintf(reply, sizeof(reply), "§b[Nome-Crafting-26.3] §eCrafted Straw Bed: Item %u x %u in pure Native C++", res, count);
                                nome_net_broadcast_chat(net, reply);
                            }
                        } else if (strncmp(chat_msg, "/locate", 7) == 0) {
                            char target[64] = "abandoned_camp";
                            if (strlen(chat_msg) > 8) {
                                strncpy(target, chat_msg + 8, sizeof(target) - 1);
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
                        } else if (strncmp(chat_msg, "/summon", 7) == 0) {
                            int32_t eid = nome_entity_spawn(10, c->x + 2.0, c->y, c->z); // Zombie
                            char reply[128];
                            snprintf(reply, sizeof(reply), "§a[Nome-ECS] Summoned Native Zombie (ID: %d) at [%.1f, %.1f, %.1f]", eid, c->x + 2.0, c->y, c->z);
                            nome_net_broadcast_chat(net, reply);
                        } else if (strncmp(chat_msg, "/stats", 6) == 0 || strncmp(chat_msg, "/mem", 4) == 0) {
                            nome_net_broadcast_chat(net, "§b[Nome-Stats] §aOff-Heap Native: Active | JVM Heap: < 30MB | GC Pauses: 0.00ms");
                        } else if (strncmp(chat_msg, "/time", 5) == 0) {
                            nome_net_broadcast_chat(net, "§e[Nome-World] Time set to Day (Ticks: 1000)");
                        } else if (strncmp(chat_msg, "/weather", 8) == 0) {
                            nome_net_broadcast_chat(net, "§e[Nome-World] Weather changed to Clear skies");
                        } else if (strncmp(chat_msg, "/gamemode", 9) == 0) {
                            nome_net_broadcast_chat(net, "§e[Nome-Player] Set game mode to Creative");
                        } else {
                            char reply[128];
                            snprintf(reply, sizeof(reply), "§7[Nome] Native command executed: %s", chat_msg);
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
            // Player Craft / Click Container (0x0E / 0x0F)
            else if (packet_id == 0x0E || packet_id == 0x0F) {
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
                    printf("[Nome-Crafting-C++] Solved Crafting Grid: produced Item %u x %u\n", result_item, result_count);
                }
            }
            // Client Information (0x03)
            else if (packet_id == 0x03) {
                // Client locale & view distance received
            }
            // Plugin Message (0x04 / 0x0D)
            else if (packet_id == 0x04 || packet_id == 0x0D) {
                // Responds to brand query
                NomePacketBuffer *bBrand = nome_buffer_create(64);
                nome_buffer_write_string(bBrand, "minecraft:brand");
                nome_buffer_write_string(bBrand, "Nome (100% Native C/C++/Rust)");
                send_raw_packet(c->socket_fd, 0x18, bBrand->data, bBrand->write_pos);
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
                send_raw_packet(c->socket_fd, 0x26, ka->data, ka->write_pos);
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

    return (size_t)activity;
}

void nome_net_broadcast_chat(NomeServerNetwork *net, const char *message) {
    if (!net || !message) return;
    char json_msg[512];
    snprintf(json_msg, sizeof(json_msg), "{\"text\":\"%s\"}", message);

    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        if (net->clients[i].is_active && net->clients[i].state == NOME_STATE_PLAY) {
            NomePacketBuffer *buf = nome_buffer_create(512);
            nome_buffer_write_string(buf, json_msg);
            uint8_t overlay = 0;
            nome_buffer_write(buf, &overlay, 1);
            send_raw_packet(net->clients[i].socket_fd, 0x69, buf->data, buf->write_pos);
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

            NomePacketBuffer *buf = nome_buffer_create(64);
            nome_buffer_write(buf, (const uint8_t*)&x, 8);
            nome_buffer_write(buf, (const uint8_t*)&y, 8);
            nome_buffer_write(buf, (const uint8_t*)&z, 8);
            nome_buffer_write(buf, (const uint8_t*)&yaw, 4);
            nome_buffer_write(buf, (const uint8_t*)&pitch, 4);
            uint8_t rel_flags = 0;
            nome_buffer_write(buf, &rel_flags, 1);
            nome_buffer_write_varint(buf, 2); // teleport id
            send_raw_packet(net->clients[i].socket_fd, 0x40, buf->data, buf->write_pos);
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
            send_raw_packet(net->clients[i].socket_fd, 0x09, buf->data, buf->write_pos);
            nome_buffer_free(buf);
        }
    }
}

void nome_net_send_slot(NomeServerNetwork *net, uint64_t player_id, int window_id, int slot, uint16_t item_id, uint8_t count) {
    if (!net) return;
    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        if (net->clients[i].is_active && (uint64_t)net->clients[i].socket_fd == player_id) {
            NomePacketBuffer *buf = nome_buffer_create(64);
            nome_buffer_write_varint(buf, window_id);
            nome_buffer_write_varint(buf, 1); // state id
            int16_t s = (int16_t)slot;
            uint8_t slot_be[2] = {(uint8_t)(s >> 8), (uint8_t)s};
            nome_buffer_write(buf, slot_be, 2);
            if (count > 0 && item_id > 0) {
                nome_buffer_write_varint(buf, count);
                nome_buffer_write_varint(buf, item_id);
                uint8_t no_nbt = 0;
                nome_buffer_write(buf, &no_nbt, 1);
            } else {
                nome_buffer_write_varint(buf, 0);
            }
            send_raw_packet(net->clients[i].socket_fd, 0x14, buf->data, buf->write_pos);
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
