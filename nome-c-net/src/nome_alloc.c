#include "../include/nome_c_net.h"
#include <stdlib.h>
#include <string.h>

NomePacketBuffer* nome_buffer_create(size_t initial_capacity) {
    if (initial_capacity < 64) initial_capacity = 64;
    NomePacketBuffer *buf = (NomePacketBuffer*)malloc(sizeof(NomePacketBuffer));
    if (!buf) return NULL;

    buf->data = (uint8_t*)malloc(initial_capacity);
    if (!buf->data) {
        free(buf);
        return NULL;
    }
    buf->capacity = initial_capacity;
    buf->read_pos = 0;
    buf->write_pos = 0;
    return buf;
}

void nome_buffer_free(NomePacketBuffer *buf) {
    if (!buf) return;
    if (buf->data) free(buf->data);
    free(buf);
}

void nome_buffer_reset(NomePacketBuffer *buf) {
    if (!buf) return;
    buf->read_pos = 0;
    buf->write_pos = 0;
}

bool nome_buffer_write(NomePacketBuffer *buf, const uint8_t *src, size_t len) {
    if (!buf || !src) return false;
    if (buf->write_pos + len > buf->capacity) {
        size_t new_cap = buf->capacity * 2;
        while (new_cap < buf->write_pos + len) {
            new_cap *= 2;
        }
        uint8_t *new_data = (uint8_t*)realloc(buf->data, new_cap);
        if (!new_data) return false;
        buf->data = new_data;
        buf->capacity = new_cap;
    }
    memcpy(buf->data + buf->write_pos, src, len);
    buf->write_pos += len;
    return true;
}

bool nome_buffer_read(NomePacketBuffer *buf, uint8_t *dst, size_t len) {
    if (!buf || !dst) return false;
    if (buf->read_pos + len > buf->write_pos) return false;
    memcpy(dst, buf->data + buf->read_pos, len);
    buf->read_pos += len;
    return true;
}

bool nome_buffer_write_varint(NomePacketBuffer *buf, int32_t val) {
    uint8_t enc[5];
    int len = nome_varint_encode(val, enc);
    return nome_buffer_write(buf, enc, (size_t)len);
}

bool nome_buffer_write_string(NomePacketBuffer *buf, const char *str) {
    if (!str) return false;
    int32_t slen = (int32_t)strlen(str);
    if (!nome_buffer_write_varint(buf, slen)) return false;
    return nome_buffer_write(buf, (const uint8_t*)str, (size_t)slen);
}
