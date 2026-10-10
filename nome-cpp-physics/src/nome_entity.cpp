#include "../include/nome_entity.hpp"
#include <cmath>
#include <algorithm>
#include <random>

namespace nome {

EntityManager::EntityManager() = default;

EntityManager& EntityManager::Instance() {
    static EntityManager instance;
    return instance;
}

int32_t EntityManager::Spawn(uint16_t type, double x, double y, double z) {
    NativeEntity e;
    e.entity_id = next_entity_id++;
    e.uuid_most = (uint64_t)e.entity_id * 1000003ULL;
    e.uuid_least = (uint64_t)type * 982451653ULL;
    e.type = type;
    e.x = x;
    e.y = y;
    e.z = z;
    e.vx = 0.0;
    e.vy = 0.0;
    e.vz = 0.0;
    e.yaw = 0.0f;
    e.pitch = 0.0f;
    e.on_ground = false;
    e.is_alive = true;
    e.ai_state = 0;
    e.ai_timer = 0;
    e.target_player_id = 0;

    // Set dimensions & health by type
    switch (type) {
        case ENTITY_ZOMBIE:
        case ENTITY_SKELETON:
        case ENTITY_VILLAGER:
            e.width = 0.6f;
            e.height = 1.95f;
            e.health = e.max_health = 20;
            break;
        case ENTITY_CREEPER:
            e.width = 0.6f;
            e.height = 1.7f;
            e.health = e.max_health = 20;
            break;
        case ENTITY_SPIDER:
            e.width = 1.4f;
            e.height = 0.9f;
            e.health = e.max_health = 16;
            break;
        case ENTITY_COW:
        case ENTITY_PIG:
        case ENTITY_SHEEP:
            e.width = 0.9f;
            e.height = 1.3f;
            e.health = e.max_health = 10;
            break;
        case ENTITY_CHICKEN:
            e.width = 0.4f;
            e.height = 0.7f;
            e.health = e.max_health = 4;
            break;
        case ENTITY_IRON_GOLEM:
            e.width = 1.4f;
            e.height = 2.7f;
            e.health = e.max_health = 100;
            break;
        case ENTITY_ITEM:
            e.width = 0.25f;
            e.height = 0.25f;
            e.health = e.max_health = 5;
            break;
        default:
            e.width = 0.5f;
            e.height = 0.5f;
            e.health = e.max_health = 10;
            break;
    }

    entities.push_back(e);
    return e.entity_id;
}

bool EntityManager::Remove(int32_t entity_id) {
    auto it = std::remove_if(entities.begin(), entities.end(), [entity_id](const NativeEntity& e) {
        return e.entity_id == entity_id;
    });
    if (it != entities.end()) {
        entities.erase(it, entities.end());
        return true;
    }
    return false;
}

void EntityManager::Clear() {
    entities.clear();
}

NativeEntity* EntityManager::Get(int32_t entity_id) {
    for (auto& e : entities) {
        if (e.entity_id == entity_id) return &e;
    }
    return nullptr;
}

size_t EntityManager::GetCount() const {
    return entities.size();
}

const std::vector<NativeEntity>& EntityManager::GetAll() const {
    return entities;
}

bool EntityManager::Damage(int32_t entity_id, int32_t damage_amount) {
    NativeEntity* e = Get(entity_id);
    if (!e || !e->is_alive) return false;
    e->health -= damage_amount;
    if (e->health <= 0) {
        e->health = 0;
        e->is_alive = false;
    }
    return true;
}

static inline bool is_solid_block(uint16_t b) {
    return b != 0 && b != 8 && b != 10 && b != 30; // Not air, water, lava, cobweb
}

void EntityManager::Tick(const uint16_t* world_blocks, const std::vector<NativePlayerTarget>& players) {
    const double GRAVITY = 0.08;
    const double DRAG_AIR = 0.98;
    const double DRAG_GROUND = 0.60;

    for (auto& e : entities) {
        if (!e.is_alive) continue;

        // 1. AI Logic & Pathing
        e.ai_timer++;

        // Hostile Mobs targeting players
        bool is_hostile = (e.type == ENTITY_ZOMBIE || e.type == ENTITY_SKELETON ||
                           e.type == ENTITY_CREEPER || e.type == ENTITY_SPIDER);

        if (is_hostile && !players.empty()) {
            // Find nearest player
            double min_dist_sq = 16.0 * 16.0; // 16 blocks detection radius
            const NativePlayerTarget* target = nullptr;

            for (const auto& p : players) {
                double dx = p.x - e.x;
                double dy = p.y - e.y;
                double dz = p.z - e.z;
                double d_sq = dx * dx + dy * dy + dz * dz;
                if (d_sq < min_dist_sq) {
                    min_dist_sq = d_sq;
                    target = &p;
                }
            }

            if (target) {
                double dx = target->x - e.x;
                double dz = target->z - e.z;
                double dist = std::sqrt(dx * dx + dz * dz);

                // Face the player
                e.yaw = (float)(std::atan2(dz, dx) * 180.0 / 3.141592653589793) - 90.0f;

                // Move towards player
                if (dist > 1.2) {
                    double speed = (e.type == ENTITY_SPIDER) ? 0.32 : 0.23;
                    e.vx = (dx / dist) * speed;
                    e.vz = (dz / dist) * speed;
                } else {
                    e.vx = 0.0;
                    e.vz = 0.0;
                }

                // Creeper Fuse Logic
                if (e.type == ENTITY_CREEPER && dist < 3.0) {
                    e.ai_state = 3; // Fusing
                }
            }
        } else {
            // Passive mobs: Random stroll every 40 ticks
            if (e.ai_timer % 40 == 0) {
                double angle = (std::abs(e.entity_id * 17 + e.ai_timer) % 360) * 3.14159265 / 180.0;
                double stroll_speed = 0.08;
                e.vx = std::cos(angle) * stroll_speed;
                e.vz = std::sin(angle) * stroll_speed;
                e.yaw = (float)(angle * 180.0 / 3.14159265);
            }
        }

        // 2. Physics & Gravity
        e.vy -= GRAVITY;
        e.vx *= (e.on_ground ? DRAG_GROUND : DRAG_AIR);
        e.vz *= (e.on_ground ? DRAG_GROUND : DRAG_AIR);
        e.vy *= DRAG_AIR;

        // 3. AABB Voxel Collision Detection against world blocks
        double new_x = e.x + e.vx;
        double new_y = e.y + e.vy;
        double new_z = e.z + e.vz;

        if (world_blocks) {
            // Check vertical collision (Floor / Ceiling)
            int bx = (int)std::floor(new_x);
            int by_below = (int)std::floor(new_y);
            int bz = (int)std::floor(new_z);

            if (bx >= 0 && bx < 16 && bz >= 0 && bz < 16 && by_below >= -64 && by_below < 320) {
                int block_idx = ((by_below + 64) * 256) + (bz * 16) + bx;
                uint16_t b_id = world_blocks[block_idx];

                if (is_solid_block(b_id)) {
                    // Stood on ground
                    new_y = (double)(by_below + 1);
                    e.vy = 0.0;
                    e.on_ground = true;
                } else {
                    e.on_ground = false;
                }
            }
        }

        e.x = new_x;
        e.y = new_y;
        e.z = new_z;
    }
}

} // namespace nome

extern "C" {

int32_t nome_entity_spawn(uint16_t type, double x, double y, double z) {
    return nome::EntityManager::Instance().Spawn(type, x, y, z);
}

bool nome_entity_remove(int32_t entity_id) {
    return nome::EntityManager::Instance().Remove(entity_id);
}

size_t nome_entity_count() {
    return nome::EntityManager::Instance().GetCount();
}

bool nome_entity_get_pos(int32_t entity_id, double* x, double* y, double* z, float* yaw, float* pitch) {
    nome::NativeEntity* e = nome::EntityManager::Instance().Get(entity_id);
    if (e) {
        if (x) *x = e->x;
        if (y) *y = e->y;
        if (z) *z = e->z;
        if (yaw) *yaw = e->yaw;
        if (pitch) *pitch = e->pitch;
        return true;
    }
    return false;
}

int32_t nome_entity_get_health(int32_t entity_id) {
    nome::NativeEntity* e = nome::EntityManager::Instance().Get(entity_id);
    return e ? e->health : 0;
}

bool nome_entity_damage(int32_t entity_id, int32_t damage) {
    return nome::EntityManager::Instance().Damage(entity_id, damage);
}

}
