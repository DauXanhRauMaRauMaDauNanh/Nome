#include "../include/nome_worldgen.hpp"
#include <cmath>
#include <numeric>
#include <random>
#include <algorithm>

namespace nome {

FastNoise::FastNoise(int64_t seed) {
    std::mt19937_64 rng((uint64_t)seed);
    std::vector<int> permutation(256);
    std::iota(permutation.begin(), permutation.end(), 0);
    std::shuffle(permutation.begin(), permutation.end(), rng);

    for (int i = 0; i < 256; i++) {
        p[i] = permutation[i];
        p[256 + i] = permutation[i];
    }
}

double FastNoise::Fade(double t) {
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}

double FastNoise::Lerp(double t, double a, double b) {
    return a + t * (b - a);
}

double FastNoise::Grad(int hash, double x, double y) {
    int h = hash & 7;
    double u = h < 4 ? x : y;
    double v = h < 4 ? y : x;
    return ((h & 1) ? -u : u) + ((h & 2) ? -2.0 * v : 2.0 * v);
}

double FastNoise::Grad3D(int hash, double x, double y, double z) {
    int h = hash & 15;
    double u = h < 8 ? x : y;
    double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
    return ((h & 1) ? -u : u) + ((h & 2) ? -v : v);
}

double FastNoise::Noise2D(double x, double z) const {
    int X = (int)std::floor(x) & 255;
    int Z = (int)std::floor(z) & 255;

    x -= std::floor(x);
    z -= std::floor(z);

    double u = Fade(x);
    double v = Fade(z);

    int A = p[X] + Z;
    int B = p[X + 1] + Z;

    return Lerp(v, Lerp(u, Grad(p[A], x, z), Grad(p[B], x - 1, z)),
                   Lerp(u, Grad(p[A + 1], x, z - 1), Grad(p[B + 1], x - 1, z - 1)));
}

double FastNoise::OctaveNoise2D(double x, double z, int octaves, double persistence, double scale) const {
    double total = 0.0;
    double frequency = scale;
    double amplitude = 1.0;
    double max_value = 0.0;

    for (int i = 0; i < octaves; i++) {
        total += Noise2D(x * frequency, z * frequency) * amplitude;
        max_value += amplitude;
        amplitude *= persistence;
        frequency *= 2.0;
    }

    return total / max_value;
}

double FastNoise::Noise3D(double x, double y, double z) const {
    int X = (int)std::floor(x) & 255;
    int Y = (int)std::floor(y) & 255;
    int Z = (int)std::floor(z) & 255;

    x -= std::floor(x);
    y -= std::floor(y);
    z -= std::floor(z);

    double u = Fade(x);
    double v = Fade(y);
    double w = Fade(z);

    int A = p[X] + Y;
    int AA = p[A] + Z;
    int AB = p[A + 1] + Z;
    int B = p[X + 1] + Y;
    int BA = p[B] + Z;
    int BB = p[B + 1] + Z;

    return Lerp(w, Lerp(v, Lerp(u, Grad3D(p[AA], x, y, z), Grad3D(p[BA], x - 1, y, z)),
                           Lerp(u, Grad3D(p[AB], x, y - 1, z), Grad3D(p[BB], x - 1, y - 1, z))),
                   Lerp(v, Lerp(u, Grad3D(p[AA + 1], x, y, z - 1), Grad3D(p[BA + 1], x - 1, y, z - 1)),
                           Lerp(u, Grad3D(p[AB + 1], x, y - 1, z - 1), Grad3D(p[BB + 1], x - 1, y - 1, z - 1))));
}

ChunkGenerator::ChunkGenerator(int64_t seed)
    : terrain_noise(seed), cave_noise(seed + 101), biome_noise(seed + 202) {}

void ChunkGenerator::GenerateChunk(int chunk_x, int chunk_z, uint16_t* block_data) {
    if (!block_data) return;

    // Clear buffer (16 * 16 * 384 = 98304 blocks = AIR)
    std::fill_n(block_data, 16 * 16 * 384, (uint16_t)BLOCK_AIR);

    const int SEA_LEVEL = 62;

    for (int z = 0; z < 16; z++) {
        for (int x = 0; x < 16; x++) {
            double world_x = (double)(chunk_x * 16 + x);
            double world_z = (double)(chunk_z * 16 + z);

            // Compute surface height: baseline 64 + mountain/valley elevation
            double height_factor = terrain_noise.OctaveNoise2D(world_x, world_z, 4, 0.5, 0.008);
            int surface_y = 64 + (int)(height_factor * 24.0);
            if (surface_y < 50) surface_y = 50;
            if (surface_y > 120) surface_y = 120;

            for (int y = -64; y <= surface_y; y++) {
                int index = ((y + 64) * 256) + (z * 16) + x;

                // 1. Bedrock Layer
                if (y == -64) {
                    block_data[index] = BLOCK_BEDROCK;
                    continue;
                }

                // 2. 3D Cave Carver
                double c_noise = cave_noise.Noise3D(world_x * 0.04, (double)y * 0.04, world_z * 0.04);
                if (c_noise > 0.55 && y < surface_y - 4) {
                    block_data[index] = BLOCK_AIR; // Hollow out cave
                    continue;
                }

                // 3. Underground Strata (Deepslate vs Stone vs Ores)
                if (y < 0) {
                    // Deepslate with Diamond / Iron
                    if (y < -30 && terrain_noise.Noise3D(world_x * 0.1, y * 0.1, world_z * 0.1) > 0.7) {
                        block_data[index] = BLOCK_DIAMOND_ORE;
                    } else {
                        block_data[index] = BLOCK_DEEPSLATE;
                    }
                } else if (y < surface_y - 3) {
                    // Stone with Coal / Iron
                    if (terrain_noise.Noise3D(world_x * 0.08, y * 0.08, world_z * 0.08) > 0.6) {
                        block_data[index] = BLOCK_IRON_ORE;
                    } else if (terrain_noise.Noise3D(world_x * 0.06, y * 0.06, world_z * 0.06) > 0.65) {
                        block_data[index] = BLOCK_COAL_ORE;
                    } else {
                        block_data[index] = BLOCK_STONE;
                    }
                } else if (y < surface_y) {
                    block_data[index] = BLOCK_DIRT;
                } else if (y == surface_y) {
                    if (surface_y <= SEA_LEVEL + 1) {
                        block_data[index] = BLOCK_SAND; // Beach
                    } else {
                        block_data[index] = BLOCK_GRASS_BLOCK; // Lush Grass
                    }
                }
            }

            // 4. Fill Oceans / Water up to SEA_LEVEL
            if (surface_y < SEA_LEVEL) {
                for (int y = surface_y + 1; y <= SEA_LEVEL; y++) {
                    int index = ((y + 64) * 256) + (z * 16) + x;
                    block_data[index] = BLOCK_WATER;
                }
            }

            // 5. Tree Placement (Procedural Flora)
            if (surface_y > SEA_LEVEL && x > 2 && x < 13 && z > 2 && z < 13) {
                double tree_val = terrain_noise.Noise2D(world_x * 0.3, world_z * 0.3);
                if (tree_val > 0.72) {
                    // Grow an Oak Tree (4 logs high + 3x3 leaves)
                    for (int ty = 1; ty <= 4; ty++) {
                        int t_idx = ((surface_y + ty + 64) * 256) + (z * 16) + x;
                        block_data[t_idx] = BLOCK_OAK_LOG;
                    }
                    // Leaves foliage
                    for (int lx = -1; lx <= 1; lx++) {
                        for (int lz = -1; lz <= 1; lz++) {
                            for (int ly = 3; ly <= 5; ly++) {
                                if (lx == 0 && lz == 0 && ly < 5) continue; // tree trunk
                                int l_idx = ((surface_y + ly + 64) * 256) + ((z + lz) * 16) + (x + lx);
                                block_data[l_idx] = BLOCK_OAK_LEAVES;
                            }
                        }
                    }
                }
            }
        }
    }
}

} // namespace nome

extern "C" {

void nome_worldgen_generate_chunk(int chunk_x, int chunk_z, uint16_t* out_blocks, int64_t seed) {
    static nome::ChunkGenerator generator(seed);
    generator.GenerateChunk(chunk_x, chunk_z, out_blocks);
}

static size_t write_varint(uint8_t* dst, int32_t val) {
    uint32_t v = (uint32_t)val;
    size_t written = 0;
    while (v >= 0x80) {
        dst[written++] = (uint8_t)((v & 0x7F) | 0x80);
        v >>= 7;
    }
    dst[written++] = (uint8_t)v;
    return written;
}

static void write_be32(uint8_t* dst, int32_t val) {
    dst[0] = (uint8_t)(val >> 24);
    dst[1] = (uint8_t)(val >> 16);
    dst[2] = (uint8_t)(val >> 8);
    dst[3] = (uint8_t)(val);
}

static void write_be16(uint8_t* dst, int16_t val) {
    dst[0] = (uint8_t)(val >> 8);
    dst[1] = (uint8_t)(val);
}

static void write_be64(uint8_t* dst, uint64_t val) {
    for (int i = 0; i < 8; i++) {
        dst[7 - i] = (uint8_t)(val >> (i * 8));
    }
}

size_t nome_worldgen_serialize_chunk(int chunk_x, int chunk_z, const uint16_t* blocks, uint8_t* out_buf, size_t max_buf) {
    if (!blocks || !out_buf || max_buf < 65536) return 0;
    
    size_t pos = 0;

    // 1. Chunk X and Chunk Z (BE int32)
    write_be32(out_buf + pos, chunk_x); pos += 4;
    write_be32(out_buf + pos, chunk_z); pos += 4;

    // 2. Heightmaps (NBT Compound: Empty compound 0x0A 0x00)
    out_buf[pos++] = 0x0A;
    out_buf[pos++] = 0x00;

    // Temporary buffer to aggregate the 24 sections
    static uint8_t sec_buffer[65536];
    size_t sec_pos = 0;

    for (int s = 0; s < 24; s++) {
        int base_y = s * 16;
        int16_t non_air = 0;
        uint16_t sec_blocks[4096];
        std::vector<uint16_t> palette;

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int global_idx = ((base_y + y) * 256) + (z * 16) + x;
                    uint16_t b = blocks[global_idx];
                    int local_idx = (y * 16 + z) * 16 + x;
                    sec_blocks[local_idx] = b;
                    if (b != 0) non_air++;
                }
            }
        }

        // Section header: non-air block count (BE int16)
        write_be16(sec_buffer + sec_pos, non_air); sec_pos += 2;

        if (non_air == 0) {
            // Entire section is Air -> single-valued palette
            sec_buffer[sec_pos++] = 0; // bits_per_entry = 0
            sec_pos += write_varint(sec_buffer + sec_pos, 0); // block state = 0 (Air)
            sec_pos += write_varint(sec_buffer + sec_pos, 0); // data array len = 0
        } else {
            // Gather unique palette entries
            for (int i = 0; i < 4096; i++) {
                uint16_t b = sec_blocks[i];
                if (std::find(palette.begin(), palette.end(), b) == palette.end()) {
                    palette.push_back(b);
                }
            }

            if (palette.size() == 1) {
                // Single block type section
                sec_buffer[sec_pos++] = 0; // bits_per_entry = 0
                sec_pos += write_varint(sec_buffer + sec_pos, palette[0]);
                sec_pos += write_varint(sec_buffer + sec_pos, 0);
            } else {
                // Indirect palette: 4 bits per entry (up to 16 entries)
                sec_buffer[sec_pos++] = 4; // bits_per_entry = 4
                sec_pos += write_varint(sec_buffer + sec_pos, (int32_t)palette.size());
                for (uint16_t p : palette) {
                    sec_pos += write_varint(sec_buffer + sec_pos, p);
                }
                sec_pos += write_varint(sec_buffer + sec_pos, 256); // 256 longs

                // Pack 4096 entries (16 entries per 64-bit long)
                for (int l = 0; l < 256; l++) {
                    uint64_t val = 0;
                    for (int e = 0; e < 16; e++) {
                        int idx = l * 16 + e;
                        uint16_t b = sec_blocks[idx];
                        auto it = std::find(palette.begin(), palette.end(), b);
                        uint64_t p_idx = (it != palette.end()) ? (uint64_t)(it - palette.begin()) : 0;
                        val |= (p_idx << (e * 4));
                    }
                    write_be64(sec_buffer + sec_pos, val);
                    sec_pos += 8;
                }
            }
        }

        // Biomes PalettedContainer: single-valued Plains (39)
        sec_buffer[sec_pos++] = 0; // bits_per_entry = 0
        sec_pos += write_varint(sec_buffer + sec_pos, 39); // biome = plains
        sec_pos += write_varint(sec_buffer + sec_pos, 0); // data len = 0
    }

    // 3. Write Data Size (VarInt) + Section Payload
    pos += write_varint(out_buf + pos, (int32_t)sec_pos);
    memcpy(out_buf + pos, sec_buffer, sec_pos);
    pos += sec_pos;

    // 4. Block Entities Count (VarInt 0)
    pos += write_varint(out_buf + pos, 0);

    // 5. Light Data (Empty masks & zero light arrays)
    pos += write_varint(out_buf + pos, 0); // sky light mask len
    pos += write_varint(out_buf + pos, 0); // block light mask len
    pos += write_varint(out_buf + pos, 0); // empty sky light mask len
    pos += write_varint(out_buf + pos, 0); // empty block light mask len
    pos += write_varint(out_buf + pos, 0); // sky light arrays count
    pos += write_varint(out_buf + pos, 0); // block light arrays count

    return pos;
}

}
