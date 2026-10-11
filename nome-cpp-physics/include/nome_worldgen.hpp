#ifndef NOME_WORLDGEN_HPP
#define NOME_WORLDGEN_HPP

#include <cstdint>
#include <vector>
#include <cstddef>

#ifdef _WIN32
  #define NOME_WORLDGEN_API __declspec(dllexport)
#else
  #define NOME_WORLDGEN_API __attribute__((visibility("default")))
#endif

namespace nome {

// Fast Perlin / Simplex Noise generator for Terrain & Biomes
class NOME_WORLDGEN_API FastNoise {
public:
    explicit FastNoise(int64_t seed = 1337);
    double Noise2D(double x, double z) const;
    double OctaveNoise2D(double x, double z, int octaves, double persistence, double scale) const;
    double Noise3D(double x, double y, double z) const;

private:
    int p[512];
    static double Fade(double t);
    static double Lerp(double t, double a, double b);
    static double Grad(int hash, double x, double y);
    static double Grad3D(int hash, double x, double y, double z);
};

// Voxel IDs matching Minecraft 26.3 / 1.21.x Vanilla
enum BlockId : uint16_t {
    BLOCK_AIR = 0,
    BLOCK_STONE = 1,
    BLOCK_GRASS_BLOCK = 2,
    BLOCK_DIRT = 3,
    BLOCK_COBBLESTONE = 4,
    BLOCK_OAK_PLANKS = 5,
    BLOCK_BEDROCK = 7,
    BLOCK_WATER = 8,
    BLOCK_LAVA = 10,
    BLOCK_SAND = 12,
    BLOCK_GRAVEL = 13,
    BLOCK_GOLD_ORE = 14,
    BLOCK_IRON_ORE = 15,
    BLOCK_COAL_ORE = 16,
    BLOCK_OAK_LOG = 17,
    BLOCK_OAK_LEAVES = 18,
    BLOCK_GLASS = 20,
    BLOCK_LAPIS_ORE = 21,
    BLOCK_SANDSTONE = 24,
    BLOCK_COBWEB = 30,
    BLOCK_TNT = 46,
    BLOCK_BOOKSHELF = 47,
    BLOCK_MOSSY_COBBLESTONE = 48,
    BLOCK_TORCH = 50,
    BLOCK_MONSTER_SPAWNER = 52,
    BLOCK_CHEST = 54,
    BLOCK_DIAMOND_ORE = 56,
    BLOCK_CRAFTING_TABLE = 58,
    BLOCK_FARMLAND = 60,
    BLOCK_FURNACE = 61,
    BLOCK_LADDER = 65,
    BLOCK_RAILS = 66,
    BLOCK_REDSTONE_ORE = 73,
    BLOCK_OAK_FENCE = 85,
    BLOCK_STONE_BRICKS = 98,
    BLOCK_DEEPSLATE = 100,
    BLOCK_COPPER_ORE = 110,
    BLOCK_END_PORTAL_FRAME = 120,
    BLOCK_EMERALD_ORE = 129,
    BLOCK_TERRACOTTA = 159,
    BLOCK_WHITE_WOOL = 35,
    BLOCK_RED_WOOL = 36,
    BLOCK_BROWN_MUSHROOM = 39,
    BLOCK_RED_MUSHROOM = 40,
    BLOCK_HAY_BLOCK = 170,
    
    // Deepslate Ore variants (Y < 0)
    BLOCK_DEEPSLATE_COAL_ORE = 201,
    BLOCK_DEEPSLATE_IRON_ORE = 202,
    BLOCK_DEEPSLATE_COPPER_ORE = 203,
    BLOCK_DEEPSLATE_GOLD_ORE = 204,
    BLOCK_DEEPSLATE_REDSTONE_ORE = 205,
    BLOCK_DEEPSLATE_LAPIS_ORE = 206,
    BLOCK_DEEPSLATE_DIAMOND_ORE = 207,
    BLOCK_DEEPSLATE_EMERALD_ORE = 208,

    // Minecraft 26.3 New Blocks & Features
    BLOCK_CAMPFIRE = 210,
    BLOCK_STRAW_BED = 211,
    BLOCK_CUSHION = 212,
    BLOCK_RED_CUSHION = 213,
    BLOCK_WOOL_SLAB = 214,
    BLOCK_RED_WOOL_SLAB = 215,
    BLOCK_WOOL_STAIRS = 216,
    BLOCK_RED_WOOL_STAIRS = 217,
    BLOCK_CONCRETE_SLAB = 218,
    BLOCK_CONCRETE_STAIRS = 219,
    BLOCK_POPLAR_LOG = 220,
    BLOCK_POPLAR_LEAVES = 221,
    BLOCK_RED_POPLAR_LEAVES = 222,
    BLOCK_ORANGE_POPLAR_LEAVES = 223,
    BLOCK_YELLOW_POPLAR_LEAVES = 224,
    BLOCK_SHELF_MUSHROOM = 225,
    BLOCK_RED_SHRUB = 226
};

// Full 16x16x384 Chunk Generator (Height -64 to 319 = 384 blocks total)
class NOME_WORLDGEN_API ChunkGenerator {
public:
    explicit ChunkGenerator(int64_t seed = 42);
    void GenerateChunk(int chunk_x, int chunk_z, uint16_t* block_data_384);

private:
    int64_t world_seed;
    FastNoise terrain_noise;
    FastNoise cave_noise;
    FastNoise biome_noise;
    FastNoise ore_noise;

    // Feature generation passes
    void GenerateTerrainBase(int chunk_x, int chunk_z, uint16_t* blocks);
    void GenerateVanillaOres(int chunk_x, int chunk_z, uint16_t* blocks);
    void GenerateVegetation(int chunk_x, int chunk_z, uint16_t* blocks);
    void GenerateStructures(int chunk_x, int chunk_z, uint16_t* blocks);

    // Structure Stamp helpers
    void StampVillage(int chunk_x, int chunk_z, uint16_t* blocks);
    void StampDungeon(int chunk_x, int chunk_z, uint16_t* blocks);
    void StampMineshaft(int chunk_x, int chunk_z, uint16_t* blocks);
    void StampDesertPyramid(int chunk_x, int chunk_z, uint16_t* blocks);
    void StampStronghold(int chunk_x, int chunk_z, uint16_t* blocks);
    void StampAbandonedCamp(int chunk_x, int chunk_z, uint16_t* blocks);
};

} // namespace nome

#ifdef __cplusplus
extern "C" {
#endif

NOME_WORLDGEN_API void nome_worldgen_generate_chunk(int chunk_x, int chunk_z, uint16_t* out_blocks, int64_t seed);
NOME_WORLDGEN_API size_t nome_worldgen_serialize_chunk(int chunk_x, int chunk_z, const uint16_t* blocks, uint8_t* out_buf, size_t max_buf, bool modern);
NOME_WORLDGEN_API bool nome_worldgen_locate_structure(const char* structure_name, int origin_chunk_x, int origin_chunk_z, int* out_chunk_x, int* out_chunk_z, int64_t seed);
NOME_WORLDGEN_API void nome_worldgen_set_antixray(bool enabled);
NOME_WORLDGEN_API bool nome_worldgen_get_antixray();

#ifdef __cplusplus
}
#endif

#endif // NOME_WORLDGEN_HPP
