#include "../include/nome_c_net.h"
#include <string.h>

int nome_varint_encode(int32_t value, uint8_t *out_buffer) {
    uint32_t uval = (uint32_t)value;
    int bytes = 0;
    while (true) {
        if ((uval & ~0x7Fu) == 0) {
            out_buffer[bytes++] = (uint8_t)uval;
            return bytes;
        } else {
            out_buffer[bytes++] = (uint8_t)((uval & 0x7Fu) | 0x80u);
            uval >>= 7;
        }
    }
}

int nome_varint_decode(const uint8_t *in_buffer, size_t max_len, int32_t *out_value) {
    int32_t result = 0;
    int shift = 0;
    size_t i = 0;

    for (i = 0; i < max_len && i < 5; i++) {
        uint8_t b = in_buffer[i];
        result |= (int32_t)(b & 0x7F) << shift;
        if ((b & 0x80) == 0) {
            *out_value = result;
            return (int)(i + 1);
        }
        shift += 7;
    }
    return -1; // Malformed VarInt or buffer overflow
}

int nome_varlong_encode(int64_t value, uint8_t *out_buffer) {
    uint64_t uval = (uint64_t)value;
    int bytes = 0;
    while (true) {
        if ((uval & ~0x7FULL) == 0) {
            out_buffer[bytes++] = (uint8_t)uval;
            return bytes;
        } else {
            out_buffer[bytes++] = (uint8_t)((uval & 0x7FULL) | 0x80u);
            uval >>= 7;
        }
    }
}

int nome_varlong_decode(const uint8_t *in_buffer, size_t max_len, int64_t *out_value) {
    int64_t result = 0;
    int shift = 0;
    size_t i = 0;

    for (i = 0; i < max_len && i < 10; i++) {
        uint8_t b = in_buffer[i];
        result |= (int64_t)(b & 0x7F) << shift;
        if ((b & 0x80) == 0) {
            *out_value = result;
            return (int)(i + 1);
        }
        shift += 7;
    }
    return -1;
}
