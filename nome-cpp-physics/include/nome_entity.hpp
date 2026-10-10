#ifndef NOME_ENTITY_HPP
#define NOME_ENTITY_HPP

#include <cstdint>
#include <cstddef>
#include <vector>

#ifdef _WIN32
  #define NOME_ENTITY_API __declspec(dllexport)
#else
  #define NOME_ENTITY_API __attribute__((visibility("default")))
#endif

namespace nome {

enum NativeEntityType : uint16_t {
    ENTITY_ITEM = 1,
    ENTITY_XP_ORB = 2,
    ENTITY_ARROW = 3,
    ENTITY_TNT = 4,
    ENTITY_FALLING_BLOCK = 5,

    // Hostile Mobs
    ENTITY_ZOMBIE = 10,
    ENTITY_SKELETON = 11,
    ENTITY_CREEPER = 12,
    ENTITY_SPIDER = 13,
    ENTITY_ENDERMAN = 14,

    // Passive & Neutral Mobs
    ENTITY_COW = 20,
    ENTITY_PIG = 21,
    ENTITY_SHEEP = 22,
    ENTITY_CHICKEN = 23,
    ENTITY_VILLAGER = 24,
    ENTITY_IRON_GOLEM = 25
};

struct NativePlayerTarget {
    uint64_t player_id;
    double x, y, z;
};

struct NativeEntity {
    int32_t entity_id;
    uint64_t uuid_most;
    uint64_t uuid_least;
    uint16_t type;
    double x, y, z;
    double vx, vy, vz;
    float yaw, pitch;
    bool on_ground;
    float width, height;
    int32_t health;
    int32_t max_health;
    bool is_alive;
    int32_t ai_state;
    int32_t ai_timer;
    uint64_t target_player_id;
};

class NOME_ENTITY_API EntityManager {
public:
    EntityManager();
    static EntityManager& Instance();

    int32_t Spawn(uint16_t type, double x, double y, double z);
    bool Remove(int32_t entity_id);
    void Clear();

    NativeEntity* Get(int32_t entity_id);
    size_t GetCount() const;
    const std::vector<NativeEntity>& GetAll() const;

    void Tick(const uint16_t* world_blocks, const std::vector<NativePlayerTarget>& players);
    bool Damage(int32_t entity_id, int32_t damage_amount);

private:
    int32_t next_entity_id{2000};
    std::vector<NativeEntity> entities;
};

} // namespace nome

#ifdef __cplusplus
extern "C" {
#endif

NOME_ENTITY_API int32_t nome_entity_spawn(uint16_t type, double x, double y, double z);
NOME_ENTITY_API bool nome_entity_remove(int32_t entity_id);
NOME_ENTITY_API size_t nome_entity_count();
NOME_ENTITY_API bool nome_entity_get_pos(int32_t entity_id, double* x, double* y, double* z, float* yaw, float* pitch);
NOME_ENTITY_API int32_t nome_entity_get_health(int32_t entity_id);
NOME_ENTITY_API bool nome_entity_damage(int32_t entity_id, int32_t damage);

#ifdef __cplusplus
}
#endif

#endif // NOME_ENTITY_HPP
