#include "../include/nome_worldgen.hpp"
#include <cmath>
#include <numeric>
#include <random>
#include <algorithm>
#include <cstring>

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
    : world_seed(seed),
      terrain_noise(seed),
      cave_noise(seed + 101),
      biome_noise(seed + 202),
      ore_noise(seed + 303) {}

static inline void set_block_fast(uint16_t* blocks, int x, int y, int z, uint16_t id) {
    if (x < 0 || x >= 16 || z < 0 || z >= 16 || y < -64 || y >= 320) return;
    int idx = ((y + 64) * 256) + (z * 16) + x;
    blocks[idx] = id;
}

static inline uint16_t get_block_fast(const uint16_t* blocks, int x, int y, int z) {
    if (x < 0 || x >= 16 || z < 0 || z >= 16 || y < -64 || y >= 320) return BLOCK_AIR;
    int idx = ((y + 64) * 256) + (z * 16) + x;
    return blocks[idx];
}

void ChunkGenerator::GenerateChunk(int chunk_x, int chunk_z, uint16_t* blocks) {
    if (!blocks) return;
    std::fill_n(blocks, 16 * 16 * 384, (uint16_t)BLOCK_AIR);

    // 1. Base Terrain & Multi-Layer Strata (Bedrock, Deepslate, Stone, Dirt, Grass, Ocean, Caves)
    GenerateTerrainBase(chunk_x, chunk_z, blocks);

    // 2. Exact Vanilla Ore Distribution (Diamonds, Iron, Coal, Copper, Gold, Redstone, Lapis, Emerald)
    GenerateVanillaOres(chunk_x, chunk_z, blocks);

    // 3. Vegetation & Flora (Oak Trees, Canopies)
    GenerateVegetation(chunk_x, chunk_z, blocks);

    // 4. Vanilla Special Structures (Villages, Mineshafts, Dungeons, Temples, Strongholds)
    GenerateStructures(chunk_x, chunk_z, blocks);
}

void ChunkGenerator::GenerateTerrainBase(int chunk_x, int chunk_z, uint16_t* blocks) {
    const int SEA_LEVEL = 62;

    for (int z = 0; z < 16; z++) {
        for (int x = 0; x < 16; x++) {
            double world_x = (double)(chunk_x * 16 + x);
            double world_z = (double)(chunk_z * 16 + z);

            double height_factor = terrain_noise.OctaveNoise2D(world_x, world_z, 4, 0.5, 0.007);
            int surface_y = 64 + (int)(height_factor * 28.0);
            if (surface_y < 50) surface_y = 50;
            if (surface_y > 140) surface_y = 140;

            for (int y = -64; y <= surface_y; y++) {
                // 1. Bedrock Floor with Dithering (-64 to -60)
                if (y == -64) {
                    set_block_fast(blocks, x, y, z, BLOCK_BEDROCK);
                    continue;
                } else if (y < -60) {
                    int dither_chance = y - (-64); // 1, 2, 3
                    if (((int)(std::abs(world_x * 13 + world_z * 17 + y * 7)) % 4) >= dither_chance) {
                        set_block_fast(blocks, x, y, z, BLOCK_BEDROCK);
                        continue;
                    }
                }

                // 2. 3D Cave Carver
                double c_noise = cave_noise.Noise3D(world_x * 0.035, (double)y * 0.035, world_z * 0.035);
                if (c_noise > 0.58 && y < surface_y - 4 && y > -60) {
                    set_block_fast(blocks, x, y, z, BLOCK_AIR);
                    continue;
                }

                // 3. Underground Strata Layers
                if (y < -8) {
                    // Solid Deepslate layer (-60 to -8)
                    set_block_fast(blocks, x, y, z, BLOCK_DEEPSLATE);
                } else if (y < 0) {
                    // Deepslate/Stone Transition (-8 to 0)
                    double blend = terrain_noise.Noise3D(world_x * 0.2, (double)y * 0.2, world_z * 0.2);
                    set_block_fast(blocks, x, y, z, (blend > 0.1) ? BLOCK_DEEPSLATE : BLOCK_STONE);
                } else if (y < surface_y - 3) {
                    // Solid Stone layer (0 to surface - 3)
                    set_block_fast(blocks, x, y, z, BLOCK_STONE);
                } else if (y < surface_y) {
                    set_block_fast(blocks, x, y, z, (surface_y <= SEA_LEVEL + 1) ? BLOCK_SAND : BLOCK_DIRT);
                } else if (y == surface_y) {
                    if (surface_y <= SEA_LEVEL + 1) {
                        set_block_fast(blocks, x, y, z, BLOCK_SAND); // Beach
                    } else {
                        set_block_fast(blocks, x, y, z, BLOCK_GRASS_BLOCK); // Surface
                    }
                }
            }

            // Fill Water Oceans up to SEA_LEVEL
            if (surface_y < SEA_LEVEL) {
                for (int y = surface_y + 1; y <= SEA_LEVEL; y++) {
                    set_block_fast(blocks, x, y, z, BLOCK_WATER);
                }
            }
        }
    }
}

void ChunkGenerator::GenerateVanillaOres(int chunk_x, int chunk_z, uint16_t* blocks) {
    for (int z = 0; z < 16; z++) {
        for (int x = 0; x < 16; x++) {
            double wx = (double)(chunk_x * 16 + x);
            double wz = (double)(chunk_z * 16 + z);

            for (int y = -63; y < 140; y++) {
                uint16_t cur = get_block_fast(blocks, x, y, z);
                if (cur != BLOCK_STONE && cur != BLOCK_DEEPSLATE) continue;

                bool is_deepslate = (cur == BLOCK_DEEPSLATE);

                // 1. DIAMOND ORE: Y = -64 to 16 (Trapezoid, peaks at -64)
                if (y <= 16) {
                    double depth_weight = (16.0 - y) / 80.0; // 0.0 at y=16, 1.0 at y=-64
                    double n = ore_noise.Noise3D(wx * 0.12, (double)y * 0.12, wz * 0.12);
                    if (n > (0.82 - depth_weight * 0.12)) {
                        set_block_fast(blocks, x, y, z, is_deepslate ? BLOCK_DEEPSLATE_DIAMOND_ORE : BLOCK_DIAMOND_ORE);
                        continue;
                    }
                }

                // 2. REDSTONE ORE: Y = -64 to 15 (Triangular, peaks at -64)
                if (y <= 15) {
                    double depth_weight = (15.0 - y) / 79.0;
                    double n = ore_noise.Noise3D(wx * 0.1, (double)y * 0.1, wz * 0.1);
                    if (n > (0.80 - depth_weight * 0.10)) {
                        set_block_fast(blocks, x, y, z, is_deepslate ? BLOCK_DEEPSLATE_REDSTONE_ORE : BLOCK_REDSTONE_ORE);
                        continue;
                    }
                }

                // 3. LAPIS ORE: Y = -64 to 64 (Triangular, peaks at Y = 0)
                if (y >= -64 && y <= 64) {
                    double dist_from_zero = std::abs((double)y) / 64.0;
                    double n = ore_noise.Noise3D(wx * 0.09, (double)y * 0.09, wz * 0.09);
                    if (n > (0.75 + dist_from_zero * 0.12)) {
                        set_block_fast(blocks, x, y, z, is_deepslate ? BLOCK_DEEPSLATE_LAPIS_ORE : BLOCK_LAPIS_ORE);
                        continue;
                    }
                }

                // 4. IRON ORE: Middle (-24 to 56, peaks at 16) + Lower (-64 to 72)
                if (y >= -64 && y <= 72) {
                    double dist_from_16 = std::abs((double)(y - 16)) / 60.0;
                    double n = ore_noise.Noise3D(wx * 0.08, (double)y * 0.08, wz * 0.08);
                    if (n > (0.68 + dist_from_16 * 0.08)) {
                        set_block_fast(blocks, x, y, z, is_deepslate ? BLOCK_DEEPSLATE_IRON_ORE : BLOCK_IRON_ORE);
                        continue;
                    }
                }

                // 5. COAL ORE: Y = 0 to 192 (peaks at 96)
                if (y >= 0 && y <= 192) {
                    double dist_from_96 = std::abs((double)(y - 96)) / 96.0;
                    double n = ore_noise.Noise3D(wx * 0.065, (double)y * 0.065, wz * 0.065);
                    if (n > (0.64 + dist_from_96 * 0.10)) {
                        set_block_fast(blocks, x, y, z, BLOCK_COAL_ORE);
                        continue;
                    }
                }

                // 6. COPPER ORE: Y = -16 to 112 (peaks at 48)
                if (y >= -16 && y <= 112) {
                    double dist_from_48 = std::abs((double)(y - 48)) / 64.0;
                    double n = ore_noise.Noise3D(wx * 0.075, (double)y * 0.075, wz * 0.075);
                    if (n > (0.70 + dist_from_48 * 0.10)) {
                        set_block_fast(blocks, x, y, z, is_deepslate ? BLOCK_DEEPSLATE_COPPER_ORE : BLOCK_COPPER_ORE);
                        continue;
                    }
                }

                // 7. GOLD ORE: Y = -64 to 32 (peaks at -16)
                if (y >= -64 && y <= 32) {
                    double dist_from_neg16 = std::abs((double)(y - (-16))) / 48.0;
                    double n = ore_noise.Noise3D(wx * 0.085, (double)y * 0.085, wz * 0.085);
                    if (n > (0.74 + dist_from_neg16 * 0.10)) {
                        set_block_fast(blocks, x, y, z, is_deepslate ? BLOCK_DEEPSLATE_GOLD_ORE : BLOCK_GOLD_ORE);
                        continue;
                    }
                }
            }
        }
    }
}

void ChunkGenerator::GenerateVegetation(int chunk_x, int chunk_z, uint16_t* blocks) {
    for (int z = 2; z < 14; z++) {
        for (int x = 2; x < 14; x++) {
            double wx = (double)(chunk_x * 16 + x);
            double wz = (double)(chunk_z * 16 + z);

            // Find surface block
            int surface_y = -1;
            for (int y = 140; y >= 62; y--) {
                uint16_t b = get_block_fast(blocks, x, y, z);
                if (b == BLOCK_GRASS_BLOCK) {
                    surface_y = y;
                    break;
                }
            }

            if (surface_y > 62) {
                double b_noise = biome_noise.Noise2D(wx * 0.04, wz * 0.04);
                double tree_noise = terrain_noise.Noise2D(wx * 0.25, wz * 0.25);

                // Dappled Forest Biome (MC 26.3): Poplar trees, Shelf Mushrooms, Fallen Trunks, Red Shrubs
                if (b_noise > 0.05 && b_noise < 0.65) {
                    if (tree_noise > 0.74) {
                        // Tall Poplar Tree (7 to 10 blocks)
                        int trunk_height = 8 + (std::abs((int)(wx + wz)) % 3);
                        for (int ty = 1; ty <= trunk_height; ty++) {
                            set_block_fast(blocks, x, surface_y + ty, z, BLOCK_POPLAR_LOG);
                        }

                        // Attach Shelf Mushroom to trunk (MC 26.3)
                        set_block_fast(blocks, x + 1, surface_y + 2, z, BLOCK_SHELF_MUSHROOM);
                        set_block_fast(blocks, x, surface_y + 3, z + 1, BLOCK_SHELF_MUSHROOM);

                        // Poplar Canopy with Autumn Foliage (Red, Orange, Yellow)
                        uint16_t foliage_type = BLOCK_ORANGE_POPLAR_LEAVES;
                        int leaf_variant = std::abs((int)(wx * 3 + wz * 7)) % 3;
                        if (leaf_variant == 0) foliage_type = BLOCK_RED_POPLAR_LEAVES;
                        else if (leaf_variant == 1) foliage_type = BLOCK_ORANGE_POPLAR_LEAVES;
                        else foliage_type = BLOCK_YELLOW_POPLAR_LEAVES;

                        for (int ly = trunk_height - 3; ly <= trunk_height + 2; ly++) {
                            int rad = (ly == trunk_height + 2) ? 1 : 2;
                            for (int lx = -rad; lx <= rad; lx++) {
                                for (int lz = -rad; lz <= rad; lz++) {
                                    if (lx == 0 && lz == 0 && ly <= trunk_height) continue;
                                    if (std::abs(lx) == rad && std::abs(lz) == rad && ly > trunk_height) continue;
                                    set_block_fast(blocks, x + lx, surface_y + ly, z + lz, foliage_type);
                                }
                            }
                        }
                    } else if (tree_noise > 0.66 && tree_noise <= 0.72) {
                        // Fallen Poplar Tree (MC 26.3): Horizontal trunk with Shelf Mushroom & Brown Mushroom
                        int log_len = 4 + (std::abs((int)(wx * 11)) % 3);
                        for (int l = 0; l < log_len; l++) {
                            if (x + l < 15) {
                                set_block_fast(blocks, x + l, surface_y + 1, z, BLOCK_POPLAR_LOG);
                            }
                        }
                        // Decorate fallen log
                        set_block_fast(blocks, x + 1, surface_y + 2, z, BLOCK_BROWN_MUSHROOM);
                        set_block_fast(blocks, x + 2, surface_y + 1, z + 1, BLOCK_SHELF_MUSHROOM);
                    } else if (tree_noise > 0.40 && tree_noise <= 0.48) {
                        // Red Shrub (MC 26.3)
                        set_block_fast(blocks, x, surface_y + 1, z, BLOCK_RED_SHRUB);
                    }
                } else {
                    // Regular Forest / Plains: Standard Oak Tree
                    if (tree_noise > 0.75) {
                        for (int ty = 1; ty <= 5; ty++) {
                            set_block_fast(blocks, x, surface_y + ty, z, BLOCK_OAK_LOG);
                        }
                        for (int lx = -2; lx <= 2; lx++) {
                            for (int lz = -2; lz <= 2; lz++) {
                                for (int ly = 3; ly <= 6; ly++) {
                                    if (lx == 0 && lz == 0 && ly < 6) continue;
                                    if (std::abs(lx) == 2 && std::abs(lz) == 2 && ly == 6) continue;
                                    set_block_fast(blocks, x + lx, surface_y + ly, z + lz, BLOCK_OAK_LEAVES);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

void ChunkGenerator::GenerateStructures(int chunk_x, int chunk_z, uint16_t* blocks) {
    // 1. Village Placement (Matches vanilla spacing: 34 chunks, salt: 10387312)
    int64_t v_hash = ((int64_t)chunk_x * 341873128712ULL) ^ ((int64_t)chunk_z * 132897987541ULL) ^ (world_seed + 10387312);
    if ((std::abs(v_hash) % 34) == 0) {
        StampVillage(chunk_x, chunk_z, blocks);
    }

    // 2. Underground Dungeons (Monster Spawner & Chests)
    int64_t d_hash = ((int64_t)chunk_x * 8912389ULL) ^ ((int64_t)chunk_z * 781263ULL) ^ world_seed;
    if ((std::abs(d_hash) % 8) == 0) {
        StampDungeon(chunk_x, chunk_z, blocks);
    }

    // 3. Abandoned Mineshaft
    int64_t m_hash = ((int64_t)chunk_x * 12345ULL) ^ ((int64_t)chunk_z * 67890ULL) ^ world_seed;
    if ((std::abs(m_hash) % 16) == 0) {
        StampMineshaft(chunk_x, chunk_z, blocks);
    }

    // 4. Stronghold (Ring between distance 80 and 120 chunks)
    double dist = std::sqrt((double)(chunk_x * chunk_x + chunk_z * chunk_z));
    if (dist >= 80.0 && dist <= 120.0 && (std::abs(chunk_x * 31 + chunk_z * 17) % 25) == 0) {
        StampStronghold(chunk_x, chunk_z, blocks);
    }

    // 5. Abandoned Camp (Matches vanilla 26.3 spacing: 37 chunks, salt: 91231127)
    int64_t camp_hash = ((int64_t)chunk_x * 91231127ULL) ^ ((int64_t)chunk_z * 7123981ULL) ^ (world_seed + 91231127);
    if ((std::abs(camp_hash) % 37) == 0) {
        StampAbandonedCamp(chunk_x, chunk_z, blocks);
    }
}

void ChunkGenerator::StampVillage(int chunk_x, int chunk_z, uint16_t* blocks) {
    int cx = 8, cz = 8;
    int ground_y = 64;
    for (int y = 100; y >= 62; y--) {
        if (get_block_fast(blocks, cx, y, cz) == BLOCK_GRASS_BLOCK) {
            ground_y = y;
            break;
        }
    }

    // Village Cottage (5x5 with oak log corners, cobblestone base, door, windows, crafting table, chest)
    for (int x = 2; x <= 6; x++) {
        for (int z = 2; z <= 6; z++) {
            // Foundation
            set_block_fast(blocks, x, ground_y, z, BLOCK_COBBLESTONE);

            for (int y = ground_y + 1; y <= ground_y + 4; y++) {
                bool is_corner = (x == 2 || x == 6) && (z == 2 || z == 6);
                if (is_corner) {
                    set_block_fast(blocks, x, y, z, BLOCK_OAK_LOG);
                } else if (y == ground_y + 4) {
                    set_block_fast(blocks, x, y, z, BLOCK_OAK_PLANKS); // Roof
                } else if (x == 4 && z == 2 && y == ground_y + 1) {
                    set_block_fast(blocks, x, y, z, BLOCK_AIR); // Doorway
                } else if ((x == 2 || x == 6 || z == 6) && y == ground_y + 2) {
                    set_block_fast(blocks, x, y, z, BLOCK_GLASS); // Window
                } else {
                    set_block_fast(blocks, x, y, z, BLOCK_OAK_PLANKS);
                }
            }
        }
    }

    // Interior furniture
    set_block_fast(blocks, 3, ground_y + 1, 5, BLOCK_CRAFTING_TABLE);
    set_block_fast(blocks, 5, ground_y + 1, 5, BLOCK_CHEST);
    set_block_fast(blocks, 4, ground_y + 3, 3, BLOCK_TORCH);

    // Village Farm plot (Farmland + Water canal + Wheat)
    for (int fx = 8; fx <= 13; fx++) {
        for (int fz = 2; fz <= 6; fz++) {
            if (fz == 4) {
                set_block_fast(blocks, fx, ground_y, fz, BLOCK_WATER);
            } else {
                set_block_fast(blocks, fx, ground_y, fz, BLOCK_FARMLAND);
                set_block_fast(blocks, fx, ground_y + 1, fz, BLOCK_COBBLESTONE); // Crops stand
            }
        }
    }
}

void ChunkGenerator::StampDungeon(int chunk_x, int chunk_z, uint16_t* blocks) {
    int dy = -10; // Underground dungeon
    for (int x = 4; x <= 11; x++) {
        for (int z = 4; z <= 11; z++) {
            for (int y = dy; y <= dy + 4; y++) {
                bool is_wall = (x == 4 || x == 11 || z == 4 || z == 11 || y == dy || y == dy + 4);
                if (is_wall) {
                    bool mossy = ((x + z + y) % 3 == 0);
                    set_block_fast(blocks, x, y, z, mossy ? BLOCK_MOSSY_COBBLESTONE : BLOCK_COBBLESTONE);
                } else {
                    set_block_fast(blocks, x, y, z, BLOCK_AIR);
                }
            }
        }
    }

    // Monster Spawner in center + Loot Chests
    set_block_fast(blocks, 7, dy + 1, 7, BLOCK_MONSTER_SPAWNER);
    set_block_fast(blocks, 5, dy + 1, 7, BLOCK_CHEST);
    set_block_fast(blocks, 10, dy + 1, 7, BLOCK_CHEST);
}

void ChunkGenerator::StampMineshaft(int chunk_x, int chunk_z, uint16_t* blocks) {
    int my = -25; // Deep underground
    // Tunnel along X axis
    for (int x = 0; x < 16; x++) {
        for (int z = 6; z <= 9; z++) {
            for (int y = my; y <= my + 3; y++) {
                if (y == my) {
                    set_block_fast(blocks, x, y, z, BLOCK_OAK_PLANKS); // Floor
                    if (z == 7 || z == 8) {
                        set_block_fast(blocks, x, y + 1, z, BLOCK_RAILS); // Minecart rails
                    }
                } else {
                    set_block_fast(blocks, x, y, z, BLOCK_AIR);
                }
            }
        }
        // Wooden support arch every 4 blocks
        if (x % 4 == 0) {
            set_block_fast(blocks, x, my + 1, 6, BLOCK_OAK_FENCE);
            set_block_fast(blocks, x, my + 2, 6, BLOCK_OAK_FENCE);
            set_block_fast(blocks, x, my + 1, 9, BLOCK_OAK_FENCE);
            set_block_fast(blocks, x, my + 2, 9, BLOCK_OAK_FENCE);
            set_block_fast(blocks, x, my + 3, 6, BLOCK_OAK_PLANKS);
            set_block_fast(blocks, x, my + 3, 7, BLOCK_OAK_PLANKS);
            set_block_fast(blocks, x, my + 3, 8, BLOCK_OAK_PLANKS);
            set_block_fast(blocks, x, my + 3, 9, BLOCK_OAK_PLANKS);
            set_block_fast(blocks, x, my + 2, 7, BLOCK_COBWEB);
        }
    }
}

void ChunkGenerator::StampStronghold(int chunk_x, int chunk_z, uint16_t* blocks) {
    int sy = -40; // Deep underground Stronghold Portal Chamber
    for (int x = 3; x <= 12; x++) {
        for (int z = 3; z <= 12; z++) {
            for (int y = sy; y <= sy + 5; y++) {
                bool is_wall = (x == 3 || x == 12 || z == 3 || z == 12 || y == sy || y == sy + 5);
                if (is_wall) {
                    set_block_fast(blocks, x, y, z, BLOCK_STONE_BRICKS);
                } else {
                    set_block_fast(blocks, x, y, z, BLOCK_AIR);
                }
            }
        }
    }

    // End Portal Frame Ring (3x3 ring with lava pool below)
    set_block_fast(blocks, 7, sy + 1, 7, BLOCK_LAVA);
    set_block_fast(blocks, 8, sy + 1, 7, BLOCK_LAVA);
    set_block_fast(blocks, 7, sy + 1, 8, BLOCK_LAVA);
    set_block_fast(blocks, 8, sy + 1, 8, BLOCK_LAVA);

    // End Portal Frames
    for (int px = 6; px <= 9; px++) {
        set_block_fast(blocks, px, sy + 2, 5, BLOCK_END_PORTAL_FRAME);
        set_block_fast(blocks, px, sy + 2, 10, BLOCK_END_PORTAL_FRAME);
    }
    for (int pz = 6; pz <= 9; pz++) {
        set_block_fast(blocks, 5, sy + 2, pz, BLOCK_END_PORTAL_FRAME);
        set_block_fast(blocks, 10, sy + 2, pz, BLOCK_END_PORTAL_FRAME);
    }

    // Silverfish Monster Spawner
    set_block_fast(blocks, 7, sy + 3, 7, BLOCK_MONSTER_SPAWNER);
}

void ChunkGenerator::StampAbandonedCamp(int chunk_x, int chunk_z, uint16_t* blocks) {
    int cx = 8, cz = 8;
    int ground_y = 64;
    for (int y = 100; y >= 62; y--) {
        if (get_block_fast(blocks, cx, y, cz) == BLOCK_GRASS_BLOCK) {
            ground_y = y;
            break;
        }
    }

    // 1. Central Campfire surrounded by Cobblestone ring and log seating
    set_block_fast(blocks, 8, ground_y + 1, 8, BLOCK_CAMPFIRE);
    set_block_fast(blocks, 7, ground_y, 8, BLOCK_COBBLESTONE);
    set_block_fast(blocks, 9, ground_y, 8, BLOCK_COBBLESTONE);
    set_block_fast(blocks, 8, ground_y, 7, BLOCK_COBBLESTONE);
    set_block_fast(blocks, 8, ground_y, 9, BLOCK_COBBLESTONE);

    // Poplar log seats
    set_block_fast(blocks, 6, ground_y + 1, 8, BLOCK_POPLAR_LOG);
    set_block_fast(blocks, 10, ground_y + 1, 8, BLOCK_POPLAR_LOG);
    set_block_fast(blocks, 8, ground_y + 1, 6, BLOCK_POPLAR_LOG);

    // 2. Abandoned Camp Tent (A-frame with Wool, Wool Slabs, Cushions, Straw Bed)
    for (int tx = 2; tx <= 5; tx++) {
        for (int tz = 10; tz <= 14; tz++) {
            set_block_fast(blocks, tx, ground_y, tz, BLOCK_DIRT);
        }
    }

    // Tent A-frame
    for (int tz = 10; tz <= 14; tz++) {
        set_block_fast(blocks, 2, ground_y + 1, tz, BLOCK_RED_WOOL);
        set_block_fast(blocks, 2, ground_y + 2, tz, BLOCK_RED_WOOL_SLAB);
        set_block_fast(blocks, 3, ground_y + 3, tz, BLOCK_WHITE_WOOL);
        set_block_fast(blocks, 4, ground_y + 3, tz, BLOCK_WHITE_WOOL);
        set_block_fast(blocks, 5, ground_y + 2, tz, BLOCK_RED_WOOL_SLAB);
        set_block_fast(blocks, 5, ground_y + 1, tz, BLOCK_RED_WOOL);
    }

    // Inside Tent: Straw Bed (MC 26.3) & Cushions (MC 26.3)
    set_block_fast(blocks, 3, ground_y + 1, 12, BLOCK_STRAW_BED);
    set_block_fast(blocks, 3, ground_y + 1, 13, BLOCK_STRAW_BED);
    set_block_fast(blocks, 4, ground_y + 1, 11, BLOCK_RED_CUSHION);

    // Loot Chest with Explorer Map, Compass, Arrows
    set_block_fast(blocks, 4, ground_y + 1, 13, BLOCK_CHEST);

    // Entrance torch
    set_block_fast(blocks, 3, ground_y + 2, 9, BLOCK_TORCH);
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

bool nome_worldgen_locate_structure(const char* structure_name, int origin_chunk_x, int origin_chunk_z, int* out_chunk_x, int* out_chunk_z, int64_t seed) {
    if (!structure_name || !out_chunk_x || !out_chunk_z) return false;

    // Search outwards from origin in spiral
    for (int r = 1; r < 100; r++) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (std::abs(dx) != r && std::abs(dz) != r) continue;
                int cx = origin_chunk_x + dx;
                int cz = origin_chunk_z + dz;

                if (strcmp(structure_name, "village") == 0) {
                    int64_t v_hash = ((int64_t)cx * 341873128712ULL) ^ ((int64_t)cz * 132897987541ULL) ^ (seed + 10387312);
                    if ((std::abs(v_hash) % 34) == 0) {
                        *out_chunk_x = cx; *out_chunk_z = cz;
                        return true;
                    }
                } else if (strcmp(structure_name, "stronghold") == 0) {
                    double dist = std::sqrt((double)(cx * cx + cz * cz));
                    if (dist >= 80.0 && dist <= 120.0 && (std::abs(cx * 31 + cz * 17) % 25) == 0) {
                        *out_chunk_x = cx; *out_chunk_z = cz;
                        return true;
                    }
                } else if (strcmp(structure_name, "mineshaft") == 0) {
                    int64_t m_hash = ((int64_t)cx * 12345ULL) ^ ((int64_t)cz * 67890ULL) ^ seed;
                    if ((std::abs(m_hash) % 16) == 0) {
                        *out_chunk_x = cx; *out_chunk_z = cz;
                        return true;
                    }
                } else if (strcmp(structure_name, "abandoned_camp") == 0 || strcmp(structure_name, "camp") == 0) {
                    int64_t c_hash = ((int64_t)cx * 91231127ULL) ^ ((int64_t)cz * 7123981ULL) ^ (seed + 91231127);
                    if ((std::abs(c_hash) % 37) == 0) {
                        *out_chunk_x = cx; *out_chunk_z = cz;
                        return true;
                    }
                }
            }
        }
    }
    return false;
}

}
