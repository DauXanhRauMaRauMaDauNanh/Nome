#ifndef NOME_PHYSICS_HPP
#define NOME_PHYSICS_HPP

#include <cstdint>
#include <cmath>
#include <vector>
#include <algorithm>

#ifdef _WIN32
  #define NOME_CPP_API __declspec(dllexport)
#else
  #define NOME_CPP_API __attribute__((visibility("default")))
#endif

namespace nome {

struct Vec3 {
    double x, y, z;

    inline Vec3 operator+(const Vec3& o) const { return {x + o.x, y + o.y, z + o.z}; }
    inline Vec3 operator-(const Vec3& o) const { return {x - o.x, y - o.y, z - o.z}; }
    inline double length_sq() const { return x * x + y * y + z * z; }
    inline double length() const { return std::sqrt(length_sq()); }
};

struct AABB {
    double min_x, min_y, min_z;
    double max_x, max_y, max_z;

    inline bool intersects(const AABB& o) const {
        return (min_x < o.max_x && max_x > o.min_x) &&
               (min_y < o.max_y && max_y > o.min_y) &&
               (min_z < o.max_z && max_z > o.min_z);
    }

    inline AABB expand(double dx, double dy, double dz) const {
        return {
            dx < 0 ? min_x + dx : min_x,
            dy < 0 ? min_y + dy : min_y,
            dz < 0 ? min_z + dz : min_z,
            dx > 0 ? max_x + dx : max_x,
            dy > 0 ? max_y + dy : max_y,
            dz > 0 ? max_z + dz : max_z
        };
    }
};

/* =========================================================================
   Collision Detection Engine
   ========================================================================= */
class NOME_CPP_API CollisionEngine {
public:
    static bool CollidesWithVoxel(const AABB& box, int32_t block_x, int32_t block_y, int32_t block_z, uint16_t block_id);
    static Vec3 CollideBoundingBox(const AABB& entity_box, const Vec3& movement, const std::vector<AABB>& potential_boxes);
};

/* =========================================================================
   Native Starlight-Style Bitmask Lighting Engine (Chunk 16x16x16 Sections)
   ========================================================================= */
class NOME_CPP_API LightEngine {
public:
    // Nibble array (4 bits per block, 16x16x16 = 4096 blocks = 2048 bytes)
    static void PropagateBlockLight(uint8_t* light_array, const uint8_t* opacity_array, int source_x, int source_y, int source_z, uint8_t source_level);
    static void PropagateSkyLight(uint8_t* light_array, const uint8_t* opacity_array);
};

} // namespace nome

#ifdef __cplusplus
extern "C" {
#endif

NOME_CPP_API bool nome_physics_check_collision(double min_x, double min_y, double min_z,
                                              double max_x, double max_y, double max_z,
                                              int32_t bx, int32_t by, int32_t bz, uint16_t block_id);

NOME_CPP_API void nome_lighting_propagate(uint8_t* light_data, const uint8_t* opacity_data,
                                          int x, int y, int z, uint8_t light_level);

#ifdef __cplusplus
}
#endif

#endif // NOME_PHYSICS_HPP
