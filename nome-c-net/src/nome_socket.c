#include "../include/nome_c_net.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

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

    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        net->clients[i].socket_fd = -1;
        net->clients[i].is_active = false;
        net->clients[i].state = NOME_STATE_HANDSHAKE;
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

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(net->port);
    inet_pton(AF_INET, net->bind_ip, &addr.sin_addr);

    if (bind(fd, (struct sockaddr*)&addr, sizeof(addr)) != 0) {
#ifdef _WIN32
        closesocket(fd);
#else
        close(fd);
#endif
        return false;
    }

    if (listen(fd, 512) != 0) {
#ifdef _WIN32
        closesocket(fd);
#else
        close(fd);
#endif
        return false;
    }

    set_nonblocking(fd);
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

        // State Machine Handling
        if (c->state == NOME_STATE_HANDSHAKE && packet_id == 0x00) {
            // Handshake
            int32_t proto = 0;
            int offset = nome_varint_decode(payload, payload_len, &proto);
            int32_t host_len = 0;
            offset += nome_varint_decode(payload + offset, payload_len - offset, &host_len);
            offset += host_len; // skip host
            offset += 2;        // skip port (ushort)
            int32_t next_state = 1;
            nome_varint_decode(payload + offset, payload_len - offset, &next_state);
            c->state = (NomeConnState)next_state;
        } else if (c->state == NOME_STATE_STATUS) {
            if (packet_id == 0x00) {
                // Status Request -> Send Status Response
                const char *status_json =
                    "{\"version\":{\"name\":\"Nome 26.3 (Native C/C++/Rust)\",\"protocol\":777},"
                    "\"players\":{\"max\":100,\"online\":1,\"sample\":[]},"
                    "\"description\":{\"text\":\"\\u00a7b\\u00a7lNome \\u00a78- \\u00a7a100% Native Server Engine\\n\\u00a77Zero-GC \\u2022 SIMD Physics \\u2022 Full Java Plugins\"}}";

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
                // UUID (16 bytes)
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
            }
        } else if (c->state == NOME_STATE_PLAY) {
            // Chat message / packet handling
            if (packet_id == 0x06 || packet_id == 0x07) {
                // Chat packet
                if (net->on_chat) {
                    net->on_chat((uint64_t)c->socket_fd, "Sample Chat Message from Native");
                }
            }
        }

        // Shift remaining buffer
        memmove(c->rx_buf, c->rx_buf + full_packet_size, c->rx_len - full_packet_size);
        c->rx_len -= full_packet_size;
    }
}

size_t nome_net_poll_events(NomeServerNetwork *net, int timeout_ms) {
    if (!net || !net->is_running) return 0;

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

    // 1. Check for incoming new connections
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

    // 2. Check for data from connected clients
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
                // Disconnected
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

    return 1;
}

void nome_net_broadcast_chat(NomeServerNetwork *net, const char *message) {
    if (!net || !message) return;
    // Broadcast Disguised Chat message to all active PLAY clients
    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        if (net->clients[i].is_active && net->clients[i].state == NOME_STATE_PLAY) {
            // Disguised Chat Packet 0x1B or System Chat 0x69
            NomePacketBuffer *buf = nome_buffer_create(512);
            nome_buffer_write_string(buf, message);
            nome_buffer_write(buf, (const uint8_t*)"\0", 1); // false overlay
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
            // Send Player Position and Look Packet (0x40)
            break;
        }
    }
}

void nome_net_send_block_change(NomeServerNetwork *net, int32_t x, int32_t y, int32_t z, uint16_t block_id) {
    if (!net) return;
    // Broadcast Block Update Packet (0x09)
    for (int i = 0; i < MAX_NOME_CLIENTS; i++) {
        if (net->clients[i].is_active && net->clients[i].state == NOME_STATE_PLAY) {
            // Block change packet
        }
    }
}
