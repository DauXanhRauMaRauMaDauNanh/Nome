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

// Voxel IDs matching Minecraft 26.3 / 1.21
enum BlockId : uint16_t {
    BLOCK_AIR = 0,
    BLOCK_STONE = 1,
    BLOCK_GRASS_BLOCK = 2,
    BLOCK_DIRT = 3,
    BLOCK_COBBLESTONE = 4,
    BLOCK_OAK_PLANKS = 5,
    BLOCK_BEDROCK = 7,
    BLOCK_WATER = 8,
    BLOCK_SAND = 12,
    BLOCK_GRAVEL = 13,
    BLOCK_OAK_LOG = 17,
    BLOCK_OAK_LEAVES = 18,
    BLOCK_GLASS = 20,
    BLOCK_DEEPSLATE = 100,
    BLOCK_IRON_ORE = 15,
    BLOCK_COAL_ORE = 16,
    BLOCK_DIAMOND_ORE = 56
};

// Full 16x16x384 Chunk Generator (Height -64 to 319 = 384 blocks total)
class NOME_WORLDGEN_API ChunkGenerator {
public:
    explicit ChunkGenerator(int64_t seed = 42);
    void GenerateChunk(int chunk_x, int chunk_z, uint16_t* block_data_384);

private:
    FastNoise terrain_noise;
    FastNoise cave_noise;
    FastNoise biome_noise;
};

} // namespace nome

#ifdef __cplusplus
extern "C" {
#endif

NOME_WORLDGEN_API void nome_worldgen_generate_chunk(int chunk_x, int chunk_z, uint16_t* out_blocks, int64_t seed);
NOME_WORLDGEN_API size_t nome_worldgen_serialize_chunk(int chunk_x, int chunk_z, const uint16_t* blocks, uint8_t* out_buf, size_t max_buf);

#ifdef __cplusplus
}
#endif

#endif // NOME_WORLDGEN_HPP
