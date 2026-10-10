#include "../include/nome_physics.hpp"
#include <queue>

namespace nome {

struct LightNode {
    int x, y, z;
    uint8_t level;
};

static inline int GetLightIndex(int x, int y, int z) {
    return (y & 15) * 256 + (z & 15) * 16 + (x & 15);
}

static inline uint8_t GetNibble(const uint8_t* array, int index) {
    int byte_idx = index >> 1;
    if ((index & 1) == 0) {
        return array[byte_idx] & 0x0F;
    } else {
        return (array[byte_idx] >> 4) & 0x0F;
    }
}

static inline void SetNibble(uint8_t* array, int index, uint8_t val) {
    int byte_idx = index >> 1;
    if ((index & 1) == 0) {
        array[byte_idx] = (array[byte_idx] & 0xF0) | (val & 0x0F);
    } else {
        array[byte_idx] = (array[byte_idx] & 0x0F) | ((val & 0x0F) << 4);
    }
}

void LightEngine::PropagateBlockLight(uint8_t* light_array, const uint8_t* opacity_array,
                                     int source_x, int source_y, int source_z, uint8_t source_level) {
    if (!light_array || source_level == 0) return;

    std::queue<LightNode> q;
    int src_idx = GetLightIndex(source_x, source_y, source_z);
    SetNibble(light_array, src_idx, source_level);
    q.push({source_x, source_y, source_z, source_level});

    const int dx[6] = {1, -1, 0, 0, 0, 0};
    const int dy[6] = {0, 0, 1, -1, 0, 0};
    const int dz[6] = {0, 0, 0, 0, 1, -1};

    while (!q.empty()) {
        LightNode cur = q.front();
        q.pop();

        if (cur.level <= 1) continue;

        for (int i = 0; i < 6; i++) {
            int nx = cur.x + dx[i];
            int ny = cur.y + dy[i];
            int nz = cur.z + dz[i];

            if (nx < 0 || nx >= 16 || ny < 0 || ny >= 16 || nz < 0 || nz >= 16) {
                continue; // Stays inside section boundary for simple BFS
            }

            int n_idx = GetLightIndex(nx, ny, nz);
            uint8_t opacity = opacity_array ? opacity_array[n_idx] : 0;
            if (opacity >= 15) continue; // Solid opaque block

            uint8_t new_level = (cur.level > (opacity + 1)) ? (cur.level - opacity - 1) : 0;
            uint8_t cur_neighbor_level = GetNibble(light_array, n_idx);

            if (new_level > cur_neighbor_level) {
                SetNibble(light_array, n_idx, new_level);
                q.push({nx, ny, nz, new_level});
            }
        }
    }
}

void LightEngine::PropagateSkyLight(uint8_t* light_array, const uint8_t* opacity_array) {
    // Top-down direct sunlight column propagation
    for (int z = 0; z < 16; z++) {
        for (int x = 0; x < 16; x++) {
            uint8_t current_light = 15;
            for (int y = 15; y >= 0; y--) {
                int idx = GetLightIndex(x, y, z);
                uint8_t opacity = opacity_array ? opacity_array[idx] : 0;
                if (opacity >= 15) {
                    current_light = 0;
                } else if (current_light > opacity) {
                    current_light -= opacity;
                } else {
                    current_light = 0;
                }
                SetNibble(light_array, idx, current_light);
            }
        }
    }
}

} // namespace nome

extern "C" {

void nome_lighting_propagate(uint8_t* light_data, const uint8_t* opacity_data,
                              int x, int y, int z, uint8_t light_level) {
    nome::LightEngine::PropagateBlockLight(light_data, opacity_data, x, y, z, light_level);
}

}
