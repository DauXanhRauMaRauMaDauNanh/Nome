#include "../include/nome_crafting.hpp"
#include <algorithm>

namespace nome {

CraftingManager::CraftingManager() {
    InitVanillaRecipes();
}

CraftingManager& CraftingManager::Instance() {
    static CraftingManager instance;
    return instance;
}

void CraftingManager::RegisterShaped(uint8_t w, uint8_t h, const std::vector<uint16_t>& pattern, uint16_t result, uint8_t count, bool mirrored) {
    shaped_recipes.push_back({w, h, pattern, result, count, mirrored});
}

void CraftingManager::RegisterShapeless(const std::vector<uint16_t>& ingredients, uint16_t result, uint8_t count) {
    std::vector<uint16_t> sorted_ing = ingredients;
    std::sort(sorted_ing.begin(), sorted_ing.end());
    shapeless_recipes.push_back({sorted_ing, result, count});
}

void CraftingManager::InitVanillaRecipes() {
    // ----------------------------------------------------
    // 1. SHAPELESS RECIPES
    // ----------------------------------------------------
    // 1 Wood Log (17) -> 4 Wood Planks (5)
    RegisterShapeless({17}, 5, 4);
    // 1 Wood Log alt (162) -> 4 Planks (5)
    RegisterShapeless({162}, 5, 4);

    // Blocks <-> Items (Compact conversion)
    // 1 Iron Block (42) -> 9 Iron Ingots (265)
    RegisterShapeless({42}, 265, 9);
    // 1 Gold Block (41) -> 9 Gold Ingots (266)
    RegisterShapeless({41}, 266, 9);
    // 1 Diamond Block (57) -> 9 Diamonds (264)
    RegisterShapeless({57}, 264, 9);

    // Flint and Steel (259): 1 Iron Ingot (265) + 1 Flint (318)
    RegisterShapeless({265, 318}, 259, 1);

    // Book (340): 3 Paper (339) + 1 Leather (334)
    RegisterShapeless({339, 339, 339, 334}, 340, 1);

    // ----------------------------------------------------
    // 2. BASIC CRAFTING & STRUCTURE BLOCKS
    // ----------------------------------------------------
    // 2 Planks (5) -> 4 Sticks (280) [Vertical 1x2]
    RegisterShaped(1, 2, {5, 5}, 280, 4);

    // 4 Planks (5) -> 1 Crafting Table (58) [2x2]
    RegisterShaped(2, 2, {5, 5, 5, 5}, 58, 1);

    // 1 Coal (263) + 1 Stick (280) -> 4 Torches (50) [1x2]
    RegisterShaped(1, 2, {263, 280}, 50, 4);

    // 8 Cobblestone (4) -> 1 Furnace (61) [3x3 ring]
    RegisterShaped(3, 3, {4, 4, 4,
                          4, 0, 4,
                          4, 4, 4}, 61, 1);

    // 8 Planks (5) -> 1 Chest (54) [3x3 ring]
    RegisterShaped(3, 3, {5, 5, 5,
                          5, 0, 5,
                          5, 5, 5}, 54, 1);

    // 9 Iron Ingots (265) -> 1 Iron Block (42) [3x3]
    RegisterShaped(3, 3, {265, 265, 265,
                          265, 265, 265,
                          265, 265, 265}, 42, 1);

    // 9 Gold Ingots (266) -> 1 Gold Block (41) [3x3]
    RegisterShaped(3, 3, {266, 266, 266,
                          266, 266, 266,
                          266, 266, 266}, 41, 1);

    // 9 Diamonds (264) -> 1 Diamond Block (57) [3x3]
    RegisterShaped(3, 3, {264, 264, 264,
                          264, 264, 264,
                          264, 264, 264}, 57, 1);

    // ----------------------------------------------------
    // 3. TOOLS (Pickaxes, Axes, Shovels, Hoes)
    // ----------------------------------------------------
    // Wooden Pickaxe (270)
    RegisterShaped(3, 3, {5, 5, 5,
                          0, 280, 0,
                          0, 280, 0}, 270, 1);

    // Stone Pickaxe (274)
    RegisterShaped(3, 3, {4, 4, 4,
                          0, 280, 0,
                          0, 280, 0}, 274, 1);

    // Iron Pickaxe (257)
    RegisterShaped(3, 3, {265, 265, 265,
                          0, 280, 0,
                          0, 280, 0}, 257, 1);

    // Diamond Pickaxe (278)
    RegisterShaped(3, 3, {264, 264, 264,
                          0, 280, 0,
                          0, 280, 0}, 278, 1);

    // Wooden Axe (271)
    RegisterShaped(2, 3, {5, 5,
                          5, 280,
                          0, 280}, 271, 1, true);

    // Stone Axe (275)
    RegisterShaped(2, 3, {4, 4,
                          4, 280,
                          0, 280}, 275, 1, true);

    // Iron Axe (258)
    RegisterShaped(2, 3, {265, 265,
                          265, 280,
                          0, 280}, 258, 1, true);

    // Diamond Axe (279)
    RegisterShaped(2, 3, {264, 264,
                          264, 280,
                          0, 280}, 279, 1, true);

    // Shovels (1x3)
    RegisterShaped(1, 3, {5, 280, 280}, 269, 1);   // Wooden Shovel
    RegisterShaped(1, 3, {4, 280, 280}, 273, 1);   // Stone Shovel
    RegisterShaped(1, 3, {265, 280, 280}, 256, 1); // Iron Shovel
    RegisterShaped(1, 3, {264, 280, 280}, 277, 1); // Diamond Shovel

    // Hoes (2x3)
    RegisterShaped(2, 3, {5, 5, 0, 280, 0, 280}, 290, 1, true);   // Wooden Hoe
    RegisterShaped(2, 3, {4, 4, 0, 280, 0, 280}, 291, 1, true);   // Stone Hoe
    RegisterShaped(2, 3, {265, 265, 0, 280, 0, 280}, 292, 1, true);// Iron Hoe
    RegisterShaped(2, 3, {264, 264, 0, 280, 0, 280}, 293, 1, true);// Diamond Hoe

    // ----------------------------------------------------
    // 4. WEAPONS & COMBAT
    // ----------------------------------------------------
    // Swords (1x3)
    RegisterShaped(1, 3, {5, 5, 280}, 268, 1);     // Wooden Sword
    RegisterShaped(1, 3, {4, 4, 280}, 272, 1);     // Stone Sword
    RegisterShaped(1, 3, {265, 265, 280}, 267, 1); // Iron Sword
    RegisterShaped(1, 3, {264, 264, 280}, 276, 1); // Diamond Sword

    // Bow (261): 3 Sticks (280) + 3 Strings (287)
    RegisterShaped(3, 3, {0, 280, 287,
                          280, 0, 287,
                          0, 280, 287}, 261, 1, true);

    // Arrow (262): Flint (318) + Stick (280) + Feather (288) -> 4
    RegisterShaped(1, 3, {318, 280, 288}, 262, 4);

    // ----------------------------------------------------
    // 5. ARMOR
    // ----------------------------------------------------
    // Iron Helmet (306)
    RegisterShaped(3, 2, {265, 265, 265,
                          265, 0, 265}, 306, 1);
    // Iron Chestplate (307)
    RegisterShaped(3, 3, {265, 0, 265,
                          265, 265, 265,
                          265, 265, 265}, 307, 1);
    // Iron Leggings (308)
    RegisterShaped(3, 3, {265, 265, 265,
                          265, 0, 265,
                          265, 0, 265}, 308, 1);
    // Iron Boots (309)
    RegisterShaped(3, 2, {265, 0, 265,
                          265, 0, 265}, 309, 1);

    // Diamond Helmet (310)
    RegisterShaped(3, 2, {264, 264, 264,
                          264, 0, 264}, 310, 1);
    // Diamond Chestplate (311)
    RegisterShaped(3, 3, {264, 0, 264,
                          264, 264, 264,
                          264, 264, 264}, 311, 1);
    // Diamond Leggings (312)
    RegisterShaped(3, 3, {264, 264, 264,
                          264, 0, 264,
                          264, 0, 264}, 312, 1);
    // Diamond Boots (313)
    RegisterShaped(3, 2, {264, 0, 264,
                          264, 0, 264}, 313, 1);

    // ----------------------------------------------------
    // 6. UTILITIES, FOOD & CONSTRUCTION
    // ----------------------------------------------------
    // Bread (297): 3 Wheat (296) [3x1]
    RegisterShaped(3, 1, {296, 296, 296}, 297, 1);

    // Bucket (325): 3 Iron Ingots (265) [3x2 V-shape]
    RegisterShaped(3, 2, {265, 0, 265,
                          0, 265, 0}, 325, 1);

    // Shears (359): 2 Iron Ingots (265) [2x2 diagonal]
    RegisterShaped(2, 2, {0, 265,
                          265, 0}, 359, 1, true);

    // Paper (339): 3 Sugar Canes (338) [3x1]
    RegisterShaped(3, 1, {338, 338, 338}, 339, 3);

    // Glass Bottles (374): 3 Glass (20) [3x2 V-shape]
    RegisterShaped(3, 2, {20, 0, 20,
                          0, 20, 0}, 374, 3);

    // Wooden Door (324): 6 Planks (5) [2x3]
    RegisterShaped(2, 3, {5, 5,
                          5, 5,
                          5, 5}, 324, 3);

    // Wooden Trapdoor (96): 6 Planks (5) [3x2]
    RegisterShaped(3, 2, {5, 5, 5,
                          5, 5, 5}, 96, 2);

    // Wooden Slabs (126): 3 Planks (5) [3x1]
    RegisterShaped(3, 1, {5, 5, 5}, 126, 6);

    // Wooden Stairs (53): 6 Planks (5) [3x3 stair]
    RegisterShaped(3, 3, {5, 0, 0,
                          5, 5, 0,
                          5, 5, 5}, 53, 4, true);

    // Cobblestone Stairs (67): 6 Cobblestone (4) [3x3 stair]
    RegisterShaped(3, 3, {4, 0, 0,
                          4, 4, 0,
                          4, 4, 4}, 67, 4, true);

    // Fence (85): 4 Planks (5) + 2 Sticks (280) [3x2]
    RegisterShaped(3, 2, {5, 280, 5,
                          5, 280, 5}, 85, 3);

    // Fence Gate (107): 2 Planks (5) + 4 Sticks (280) [3x2]
    RegisterShaped(3, 2, {280, 5, 280,
                          280, 5, 280}, 107, 1);

    // Ladder (65): 7 Sticks (280) [3x3 H-shape]
    RegisterShaped(3, 3, {280, 0, 280,
                          280, 280, 280,
                          280, 0, 280}, 65, 3);

    // Bed (355): 3 Wool (35) + 3 Planks (5) [3x2]
    RegisterShaped(3, 2, {35, 35, 35,
                          5, 5, 5}, 355, 1);

    // Boat (333): 5 Planks (5) [3x2 U-shape]
    RegisterShaped(3, 2, {5, 0, 5,
                          5, 5, 5}, 333, 1);

    // Minecart (328): 5 Iron Ingots (265) [3x2 U-shape]
    RegisterShaped(3, 2, {265, 0, 265,
                          265, 265, 265}, 328, 1);

    // Rails (66): 6 Iron Ingots (265) + 1 Stick (280) [3x3]
    RegisterShaped(3, 3, {265, 0, 265,
                          265, 280, 265,
                          265, 0, 265}, 66, 16);
}

bool CraftingManager::MatchGrid(const uint16_t* grid, uint8_t grid_dim, uint16_t* out_item, uint8_t* out_count) const {
    if (!grid || (grid_dim != 2 && grid_dim != 3)) return false;

    // Collect all non-empty items in grid
    std::vector<uint16_t> present_items;
    int min_x = grid_dim, max_x = -1;
    int min_y = grid_dim, max_y = -1;

    for (int y = 0; y < grid_dim; y++) {
        for (int x = 0; x < grid_dim; x++) {
            uint16_t item = grid[y * grid_dim + x];
            if (item != 0) {
                present_items.push_back(item);
                if (x < min_x) min_x = x;
                if (x > max_x) max_x = x;
                if (y < min_y) min_y = y;
                if (y > max_y) max_y = y;
            }
        }
    }

    if (max_x == -1 || present_items.empty()) return false; // Entire grid is empty

    // 1. Try Shapeless Recipes First
    std::sort(present_items.begin(), present_items.end());
    for (const auto& sr : shapeless_recipes) {
        if (sr.ingredients == present_items) {
            *out_item = sr.result_item;
            *out_count = sr.result_count;
            return true;
        }
    }

    // 2. Try Shaped Recipes within bounding box
    int sub_w = max_x - min_x + 1;
    int sub_h = max_y - min_y + 1;

    for (const auto& r : shaped_recipes) {
        if (r.width == sub_w && r.height == sub_h) {
            // Direct match check
            bool direct_match = true;
            for (int ry = 0; ry < sub_h && direct_match; ry++) {
                for (int rx = 0; rx < sub_w && direct_match; rx++) {
                    uint16_t grid_val = grid[(min_y + ry) * grid_dim + (min_x + rx)];
                    uint16_t recipe_val = r.pattern[ry * sub_w + rx];
                    if (grid_val != recipe_val) {
                        direct_match = false;
                    }
                }
            }

            if (direct_match) {
                *out_item = r.result_item;
                *out_count = r.result_count;
                return true;
            }

            // Mirror match check (horizontal flip)
            if (r.mirrored) {
                bool mirror_match = true;
                for (int ry = 0; ry < sub_h && mirror_match; ry++) {
                    for (int rx = 0; rx < sub_w && mirror_match; rx++) {
                        uint16_t grid_val = grid[(min_y + ry) * grid_dim + (min_x + rx)];
                        uint16_t recipe_val = r.pattern[ry * sub_w + (sub_w - 1 - rx)];
                        if (grid_val != recipe_val) {
                            mirror_match = false;
                        }
                    }
                }
                if (mirror_match) {
                    *out_item = r.result_item;
                    *out_count = r.result_count;
                    return true;
                }
            }
        }
    }

    return false;
}

} // namespace nome

extern "C" {

bool nome_crafting_solve(const uint16_t* grid, uint8_t grid_dim, uint16_t* out_item, uint8_t* out_count) {
    return nome::CraftingManager::Instance().MatchGrid(grid, grid_dim, out_item, out_count);
}

}
