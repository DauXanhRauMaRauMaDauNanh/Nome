#include "../include/nome_physics.hpp"

namespace nome {

bool CollisionEngine::CollidesWithVoxel(const AABB& box, int32_t block_x, int32_t block_y, int32_t block_z, uint16_t block_id) {
    if (block_id == 0) { // Air has no collision
        return false;
    }

    // Standard 1x1x1 solid cube
    AABB block_box = {
        (double)block_x, (double)block_y, (double)block_z,
        (double)(block_x + 1), (double)(block_y + 1), (double)(block_z + 1)
    };

    return box.intersects(block_box);
}

Vec3 CollisionEngine::CollideBoundingBox(const AABB& box, const Vec3& movement, const std::vector<AABB>& boxes) {
    double dx = movement.x;
    double dy = movement.y;
    double dz = movement.z;

    // Y collision pass
    for (const auto& b : boxes) {
        if (box.min_x < b.max_x && box.max_x > b.min_x && box.min_z < b.max_z && box.max_z > b.min_z) {
            if (dy > 0 && box.max_y <= b.min_y) {
                double diff = b.min_y - box.max_y;
                if (diff < dy) dy = diff;
            } else if (dy < 0 && box.min_y >= b.max_y) {
                double diff = b.max_y - box.min_y;
                if (diff > dy) dy = diff;
            }
        }
    }
    AABB y_box = {box.min_x, box.min_y + dy, box.min_z, box.max_x, box.max_y + dy, box.max_z};

    // X collision pass
    for (const auto& b : boxes) {
        if (y_box.min_y < b.max_y && y_box.max_y > b.min_y && y_box.min_z < b.max_z && y_box.max_z > b.min_z) {
            if (dx > 0 && y_box.max_x <= b.min_x) {
                double diff = b.min_x - y_box.max_x;
                if (diff < dx) dx = diff;
            } else if (dx < 0 && y_box.min_x >= b.max_x) {
                double diff = b.max_x - y_box.min_x;
                if (diff > dx) dx = diff;
            }
        }
    }
    AABB x_box = {y_box.min_x + dx, y_box.min_y, y_box.min_z, y_box.max_x + dx, y_box.max_y, y_box.max_z};

    // Z collision pass
    for (const auto& b : boxes) {
        if (x_box.min_x < b.max_x && x_box.max_x > b.min_x && x_box.min_y < b.max_y && x_box.max_y > b.min_y) {
            if (dz > 0 && x_box.max_z <= b.min_z) {
                double diff = b.min_z - x_box.max_z;
                if (diff < dz) dz = diff;
            } else if (dz < 0 && x_box.min_z >= b.max_z) {
                double diff = b.max_z - x_box.min_z;
                if (diff > dz) dz = diff;
            }
        }
    }

    return {dx, dy, dz};
}

} // namespace nome

extern "C" {

bool nome_physics_check_collision(double min_x, double min_y, double min_z,
                                  double max_x, double max_y, double max_z,
                                  int32_t bx, int32_t by, int32_t bz, uint16_t block_id) {
    nome::AABB box = {min_x, min_y, min_z, max_x, max_y, max_z};
    return nome::CollisionEngine::CollidesWithVoxel(box, bx, by, bz, block_id);
}

}
