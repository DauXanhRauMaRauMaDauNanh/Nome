#ifndef NOME_CRAFTING_HPP
#define NOME_CRAFTING_HPP

#include <cstdint>
#include <vector>
#include <string>

#ifdef _WIN32
  #define NOME_CRAFT_API __declspec(dllexport)
#else
  #define NOME_CRAFT_API __attribute__((visibility("default")))
#endif

namespace nome {

struct ShapedRecipe {
    uint8_t width;
    uint8_t height;
    std::vector<uint16_t> pattern; // Row-major: width * height
    uint16_t result_item;
    uint8_t result_count;
    bool mirrored;
};

struct ShapelessRecipe {
    std::vector<uint16_t> ingredients; // Sorted list of ingredient IDs
    uint16_t result_item;
    uint8_t result_count;
};

class NOME_CRAFT_API CraftingManager {
public:
    CraftingManager();
    void RegisterShaped(uint8_t w, uint8_t h, const std::vector<uint16_t>& pattern, uint16_t result, uint8_t count, bool mirrored = true);
    void RegisterShapeless(const std::vector<uint16_t>& ingredients, uint16_t result, uint8_t count);
    
    bool MatchGrid(const uint16_t* grid, uint8_t grid_dim, uint16_t* out_item, uint8_t* out_count) const;

    static CraftingManager& Instance();

private:
    std::vector<ShapedRecipe> shaped_recipes;
    std::vector<ShapelessRecipe> shapeless_recipes;
    void InitVanillaRecipes();
};

} // namespace nome

#ifdef __cplusplus
extern "C" {
#endif

NOME_CRAFT_API bool nome_crafting_solve(const uint16_t* grid, uint8_t grid_dim, uint16_t* out_item, uint8_t* out_count);

#ifdef __cplusplus
}
#endif

#endif // NOME_CRAFTING_HPP
