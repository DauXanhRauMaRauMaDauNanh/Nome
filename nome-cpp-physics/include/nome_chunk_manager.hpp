#ifndef NOME_CHUNK_MANAGER_HPP
#define NOME_CHUNK_MANAGER_HPP

#include "nome_worldgen.hpp"
#include <cstdint>
#include <vector>
#include <unordered_map>
#include <memory>
#include <mutex>
#include <atomic>
#include <chrono>
#include <algorithm>

#ifdef _WIN32
  #define NOME_CHUNK_API __declspec(dllexport)
#else
  #define NOME_CHUNK_API __attribute__((visibility("default")))
#endif

namespace nome {

struct ChunkPos {
    int x;
    int z;

    bool operator==(const ChunkPos& other) const {
        return x == other.x && z == other.z;
    }
};

struct ChunkPosHash {
    size_t operator()(const ChunkPos& pos) const {
        return ((size_t)(uint32_t)pos.x << 32) | (size_t)(uint32_t)pos.z;
    }
};

class NOME_CHUNK_API Chunk {
public:
    int x;
    int z;
    uint16_t blocks[16 * 16 * 384]; // 98,304 voxels off-heap
    std::atomic<int> ticket_count{0};
    std::chrono::steady_clock::time_point last_access;
    bool is_modified{false};
    std::mutex chunk_mutex;

    Chunk(int cx, int cz) : x(cx), z(cz) {
        last_access = std::chrono::steady_clock::now();
    }

    uint16_t GetBlock(int lx, int ly, int lz) {
        if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16 || ly < -64 || ly >= 320) return 0;
        int idx = ((ly + 64) * 256) + (lz * 16) + lx;
        return blocks[idx];
    }

    void SetBlock(int lx, int ly, int lz, uint16_t block_id) {
        if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16 || ly < -64 || ly >= 320) return;
        int idx = ((ly + 64) * 256) + (lz * 16) + lx;
        blocks[idx] = block_id;
        is_modified = true;
        last_access = std::chrono::steady_clock::now();
    }
};

class NOME_CHUNK_API ChunkManager {
public:
    static ChunkManager& Instance() {
        static ChunkManager instance;
        return instance;
    }

    void Init(int64_t seed);
    std::shared_ptr<Chunk> GetOrGenerateChunk(int cx, int cz);
    std::shared_ptr<Chunk> GetLoadedChunk(int cx, int cz);
    bool IsChunkLoaded(int cx, int cz);
    bool UnloadChunk(int cx, int cz, bool force = false);

    uint16_t GetBlock(int x, int y, int z);
    void SetBlock(int x, int y, int z, uint16_t block_id);

    void AddTicket(int cx, int cz);
    void RemoveTicket(int cx, int cz);

    size_t SerializeChunk(int cx, int cz, uint8_t* out_buf, size_t max_buf);

    // Smart Spiral Chunk Loading Order Generator
    std::vector<ChunkPos> GenerateSpiralChunkList(int center_x, int center_z, int view_distance);

    // Garbage collector for unreferenced idle chunks
    size_t CollectGarbage(int max_idle_seconds);

    size_t GetLoadedChunkCount();
    void GetLoadedChunkCoordinates(std::vector<ChunkPos>& out_coords);

private:
    ChunkManager() = default;
    int64_t world_seed{1337};
    std::unique_ptr<ChunkGenerator> generator;
    std::unordered_map<ChunkPos, std::shared_ptr<Chunk>, ChunkPosHash> loaded_chunks;
    std::mutex map_mutex;
};

} // namespace nome

#ifdef __cplusplus
extern "C" {
#endif

NOME_CHUNK_API void nome_chunk_init(int64_t seed);
NOME_CHUNK_API bool nome_chunk_is_loaded(int cx, int cz);
NOME_CHUNK_API void nome_chunk_load(int cx, int cz);
NOME_CHUNK_API bool nome_chunk_unload(int cx, int cz, bool force);
NOME_CHUNK_API uint16_t nome_chunk_get_block(int x, int y, int z);
NOME_CHUNK_API void nome_chunk_set_block(int x, int y, int z, uint16_t block);
NOME_CHUNK_API size_t nome_chunk_serialize(int cx, int cz, uint8_t* out_buf, size_t max_buf);
NOME_CHUNK_API size_t nome_chunk_get_loaded_count();
NOME_CHUNK_API size_t nome_chunk_gc(int max_idle_seconds);

#ifdef __cplusplus
}
#endif

#endif // NOME_CHUNK_MANAGER_HPP
