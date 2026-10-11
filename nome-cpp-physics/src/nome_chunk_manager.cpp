#include "nome_chunk_manager.hpp"
#include <iostream>
#include <cmath>
#include <filesystem>
#include <cstdio>

namespace nome {

void ChunkManager::Init(int64_t seed) {
    std::lock_guard<std::mutex> lock(map_mutex);
    this->world_seed = seed;
    this->generator = std::make_unique<ChunkGenerator>(seed);
    this->loaded_chunks.clear();

    try {
        std::filesystem::create_directories("world/region");
    } catch (...) {}
}

std::shared_ptr<Chunk> ChunkManager::GetOrGenerateChunk(int cx, int cz) {
    ChunkPos pos{cx, cz};
    {
        std::lock_guard<std::mutex> lock(map_mutex);
        auto it = loaded_chunks.find(pos);
        if (it != loaded_chunks.end()) {
            it->second->last_access = std::chrono::steady_clock::now();
            return it->second;
        }
    }

    auto chunk = std::make_shared<Chunk>(cx, cz);

    char filepath[256];
    std::snprintf(filepath, sizeof(filepath), "world/region/c.%d.%d.bin", cx, cz);

    bool loaded_from_disk = false;
    FILE* f = std::fopen(filepath, "rb");
    if (f) {
        size_t read_count = std::fread(chunk->blocks, sizeof(uint16_t), 16 * 16 * 384, f);
        std::fclose(f);
        if (read_count == 16 * 16 * 384) {
            chunk->is_modified = false;
            loaded_from_disk = true;
        }
    }

    if (!loaded_from_disk) {
        if (!generator) {
            generator = std::make_unique<ChunkGenerator>(world_seed);
        }
        generator->GenerateChunk(cx, cz, chunk->blocks);
        chunk->is_modified = false;

        // Persist new chunk to disk
        FILE* out_f = std::fopen(filepath, "wb");
        if (out_f) {
            std::fwrite(chunk->blocks, sizeof(uint16_t), 16 * 16 * 384, out_f);
            std::fclose(out_f);
        }
    }

    {
        std::lock_guard<std::mutex> lock(map_mutex);
        loaded_chunks[pos] = chunk;
    }

    return chunk;
}

std::shared_ptr<Chunk> ChunkManager::GetLoadedChunk(int cx, int cz) {
    ChunkPos pos{cx, cz};
    std::lock_guard<std::mutex> lock(map_mutex);
    auto it = loaded_chunks.find(pos);
    if (it != loaded_chunks.end()) {
        it->second->last_access = std::chrono::steady_clock::now();
        return it->second;
    }
    return nullptr;
}

bool ChunkManager::IsChunkLoaded(int cx, int cz) {
    ChunkPos pos{cx, cz};
    std::lock_guard<std::mutex> lock(map_mutex);
    return loaded_chunks.find(pos) != loaded_chunks.end();
}

bool ChunkManager::UnloadChunk(int cx, int cz, bool force) {
    ChunkPos pos{cx, cz};
    std::lock_guard<std::mutex> lock(map_mutex);
    auto it = loaded_chunks.find(pos);
    if (it != loaded_chunks.end()) {
        if (!force && it->second->ticket_count.load() > 0) {
            return false; // Still held by players or plugins
        }
        loaded_chunks.erase(it);
        return true;
    }
    return false;
}

uint16_t ChunkManager::GetBlock(int x, int y, int z) {
    int cx = x >> 4;
    int cz = z >> 4;
    int lx = x & 15;
    int lz = z & 15;

    auto chunk = GetLoadedChunk(cx, cz);
    if (!chunk) {
        chunk = GetOrGenerateChunk(cx, cz);
    }
    return chunk ? chunk->GetBlock(lx, y, lz) : 0;
}

void ChunkManager::SetBlock(int x, int y, int z, uint16_t block_id) {
    int cx = x >> 4;
    int cz = z >> 4;
    int lx = x & 15;
    int lz = z & 15;

    auto chunk = GetOrGenerateChunk(cx, cz);
    if (chunk) {
        chunk->SetBlock(lx, y, lz, block_id);
    }
}

void ChunkManager::AddTicket(int cx, int cz) {
    auto chunk = GetOrGenerateChunk(cx, cz);
    if (chunk) {
        chunk->ticket_count.fetch_add(1);
    }
}

void ChunkManager::RemoveTicket(int cx, int cz) {
    auto chunk = GetLoadedChunk(cx, cz);
    if (chunk) {
        int old = chunk->ticket_count.load();
        if (old > 0) chunk->ticket_count.fetch_sub(1);
    }
}

size_t ChunkManager::SerializeChunk(int cx, int cz, uint8_t* out_buf, size_t max_buf, bool modern) {
    auto chunk = GetOrGenerateChunk(cx, cz);
    if (!chunk) return 0;
    return nome_worldgen_serialize_chunk(cx, cz, chunk->blocks, out_buf, max_buf, modern);
}

std::vector<ChunkPos> ChunkManager::GenerateSpiralChunkList(int center_x, int center_z, int view_distance) {
    std::vector<ChunkPos> result;
    int r = std::clamp(view_distance, 1, 16);

    for (int dz = -r; dz <= r; dz++) {
        for (int dx = -r; dx <= r; dx++) {
            if (dx * dx + dz * dz <= r * r) {
                result.push_back({center_x + dx, center_z + dz});
            }
        }
    }

    // Sort in ascending order of squared distance (Spiral outward from player center)
    std::sort(result.begin(), result.end(), [center_x, center_z](const ChunkPos& a, const ChunkPos& b) {
        int dist_a = (a.x - center_x) * (a.x - center_x) + (a.z - center_z) * (a.z - center_z);
        int dist_b = (b.x - center_x) * (b.x - center_x) + (b.z - center_z) * (b.z - center_z);
        return dist_a < dist_b;
    });

    return result;
}

size_t ChunkManager::CollectGarbage(int max_idle_seconds) {
    std::lock_guard<std::mutex> lock(map_mutex);
    auto now = std::chrono::steady_clock::now();
    size_t removed = 0;

    for (auto it = loaded_chunks.begin(); it != loaded_chunks.end(); ) {
        if (it->second->ticket_count.load() <= 0) {
            auto elapsed = std::chrono::duration_cast<std::chrono::seconds>(now - it->second->last_access).count();
            if (elapsed >= max_idle_seconds) {
                it = loaded_chunks.erase(it);
                removed++;
                continue;
            }
        }
        ++it;
    }
    return removed;
}

size_t ChunkManager::GetLoadedChunkCount() {
    std::lock_guard<std::mutex> lock(map_mutex);
    return loaded_chunks.size();
}

void ChunkManager::GetLoadedChunkCoordinates(std::vector<ChunkPos>& out_coords) {
    std::lock_guard<std::mutex> lock(map_mutex);
    out_coords.clear();
    for (const auto& pair : loaded_chunks) {
        out_coords.push_back(pair.first);
    }
}

void ChunkManager::GetChunkHeightmap(int cx, int cz, int16_t* out_heightmap) {
    auto chunk = GetOrGenerateChunk(cx, cz);
    if (chunk) {
        chunk->GetHeightmap(out_heightmap);
    } else if (out_heightmap) {
        std::fill_n(out_heightmap, 256, (int16_t)-64);
    }
}

void ChunkManager::GetChunkSection(int cx, int cz, int section_y, uint16_t* out_blocks) {
    auto chunk = GetOrGenerateChunk(cx, cz);
    if (chunk) {
        chunk->GetSection(section_y, out_blocks);
    } else if (out_blocks) {
        std::fill_n(out_blocks, 4096, (uint16_t)0);
    }
}

void ChunkManager::GetChunkAllBlocks(int cx, int cz, uint16_t* out_blocks) {
    auto chunk = GetOrGenerateChunk(cx, cz);
    if (chunk) {
        chunk->GetAllBlocks(out_blocks);
    } else if (out_blocks) {
        std::fill_n(out_blocks, 16 * 16 * 384, (uint16_t)0);
    }
}

bool ChunkManager::SaveChunk(int cx, int cz) {
    auto chunk = GetLoadedChunk(cx, cz);
    if (!chunk) return false;

    char filepath[256];
    std::snprintf(filepath, sizeof(filepath), "world/region/c.%d.%d.bin", cx, cz);
    FILE* f = std::fopen(filepath, "wb");
    if (!f) return false;
    std::fwrite(chunk->blocks, sizeof(uint16_t), 16 * 16 * 384, f);
    std::fclose(f);
    chunk->is_modified = false;
    return true;
}

size_t ChunkManager::SaveAllChunks() {
    std::lock_guard<std::mutex> lock(map_mutex);
    size_t count = 0;
    try {
        std::filesystem::create_directories("world/region");
    } catch (...) {}

    for (auto& pair : loaded_chunks) {
        auto chunk = pair.second;
        if (chunk) {
            char filepath[256];
            std::snprintf(filepath, sizeof(filepath), "world/region/c.%d.%d.bin", chunk->x, chunk->z);
            FILE* f = std::fopen(filepath, "wb");
            if (f) {
                std::fwrite(chunk->blocks, sizeof(uint16_t), 16 * 16 * 384, f);
                std::fclose(f);
                chunk->is_modified = false;
                count++;
            }
        }
    }
    return count;
}

} // namespace nome

extern "C" {

void nome_chunk_init(int64_t seed) {
    nome::ChunkManager::Instance().Init(seed);
}

bool nome_chunk_is_loaded(int cx, int cz) {
    return nome::ChunkManager::Instance().IsChunkLoaded(cx, cz);
}

void nome_chunk_load(int cx, int cz) {
    nome::ChunkManager::Instance().GetOrGenerateChunk(cx, cz);
}

bool nome_chunk_unload(int cx, int cz, bool force) {
    return nome::ChunkManager::Instance().UnloadChunk(cx, cz, force);
}

uint16_t nome_chunk_get_block(int x, int y, int z) {
    return nome::ChunkManager::Instance().GetBlock(x, y, z);
}

void nome_chunk_set_block(int x, int y, int z, uint16_t block) {
    nome::ChunkManager::Instance().SetBlock(x, y, z, block);
}

void nome_chunk_get_heightmap(int cx, int cz, int16_t* out_heightmap) {
    nome::ChunkManager::Instance().GetChunkHeightmap(cx, cz, out_heightmap);
}

void nome_chunk_get_section(int cx, int cz, int section_y, uint16_t* out_blocks) {
    nome::ChunkManager::Instance().GetChunkSection(cx, cz, section_y, out_blocks);
}

void nome_chunk_get_all_blocks(int cx, int cz, uint16_t* out_blocks) {
    nome::ChunkManager::Instance().GetChunkAllBlocks(cx, cz, out_blocks);
}

size_t nome_chunk_serialize(int cx, int cz, uint8_t* out_buf, size_t max_buf, bool modern) {
    return nome::ChunkManager::Instance().SerializeChunk(cx, cz, out_buf, max_buf, modern);
}

size_t nome_chunk_get_loaded_count() {
    return nome::ChunkManager::Instance().GetLoadedChunkCount();
}

size_t nome_chunk_gc(int max_idle_seconds) {
    return nome::ChunkManager::Instance().CollectGarbage(max_idle_seconds);
}

bool nome_chunk_save(int cx, int cz) {
    return nome::ChunkManager::Instance().SaveChunk(cx, cz);
}

size_t nome_chunk_save_all() {
    return nome::ChunkManager::Instance().SaveAllChunks();
}

}
