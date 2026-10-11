//! Paper 26.3 Native Anti-Xray (Engine Mode 1 & Engine Mode 2)
//! Directly ported and optimized from Paper's ChunkPacketBlockControllerAntiXray.
//! Obfuscates underground ores in chunk packets sent to clients with zero Java GC allocations.

pub const ID_AIR: u16 = 0;
pub const ID_STONE: u16 = 1;
pub const ID_DEEPSLATE: u16 = 847;

// Ore IDs
pub const ID_GOLD_ORE: u16 = 14;
pub const ID_IRON_ORE: u16 = 15;
pub const ID_COAL_ORE: u16 = 16;
pub const ID_LAPIS_ORE: u16 = 21;
pub const ID_DIAMOND_ORE: u16 = 56;
pub const ID_REDSTONE_ORE: u16 = 73;
pub const ID_EMERALD_ORE: u16 = 129;
pub const ID_DEEPSLATE_GOLD: u16 = 848;
pub const ID_DEEPSLATE_IRON: u16 = 849;
pub const ID_DEEPSLATE_LAPIS: u16 = 850;
pub const ID_DEEPSLATE_REDSTONE: u16 = 851;
pub const ID_DEEPSLATE_COAL: u16 = 852;
pub const ID_DEEPSLATE_DIAMOND: u16 = 853;
pub const ID_DEEPSLATE_EMERALD: u16 = 854;
pub const ID_NETHER_GOLD: u16 = 867;
pub const ID_ANCIENT_DEBRIS: u16 = 872;
pub const ID_SPAWNER: u16 = 52;
pub const ID_CHEST: u16 = 54;

#[inline(always)]
pub fn is_ore(id: u16) -> bool {
    matches!(
        id,
        ID_GOLD_ORE
            | ID_IRON_ORE
            | ID_COAL_ORE
            | ID_LAPIS_ORE
            | ID_DIAMOND_ORE
            | ID_REDSTONE_ORE
            | ID_EMERALD_ORE
            | ID_DEEPSLATE_GOLD
            | ID_DEEPSLATE_IRON
            | ID_DEEPSLATE_LAPIS
            | ID_DEEPSLATE_REDSTONE
            | ID_DEEPSLATE_COAL
            | ID_DEEPSLATE_DIAMOND
            | ID_DEEPSLATE_EMERALD
            | ID_NETHER_GOLD
            | ID_ANCIENT_DEBRIS
            | ID_SPAWNER
            | ID_CHEST
    )
}

#[inline(always)]
pub fn is_opaque_solid(id: u16) -> bool {
    // Air, fluids, torches, and transparent blocks are NOT opaque
    id != 0 && id != 8 && id != 9 && id != 10 && id != 11 && id != 50 && id != 76
}

/// Checks if an ore at (x, y, z) is exposed to an open block (Air, Water, Lava, Torch, etc.)
/// If ALL 6 adjacent blocks are opaque solid, the ore is completely hidden (not exposed).
#[inline]
pub fn is_ore_exposed(blocks: &[u16; 4096], x: usize, y: usize, z: usize) -> bool {
    // If on the outer boundary of section, assume potentially exposed to avoid false positives
    if x == 0 || x == 15 || y == 0 || y == 15 || z == 0 || z == 15 {
        return true;
    }

    let idx = y * 256 + z * 16 + x;
    let west = blocks[idx - 1];
    let east = blocks[idx + 1];
    let north = blocks[idx - 16];
    let south = blocks[idx + 16];
    let down = blocks[idx - 256];
    let up = blocks[idx + 256];

    !is_opaque_solid(west)
        || !is_opaque_solid(east)
        || !is_opaque_solid(north)
        || !is_opaque_solid(south)
        || !is_opaque_solid(down)
        || !is_opaque_solid(up)
}

/// Simple fast xorshift64 PRNG for Engine Mode 2 fake ore scrambling
#[inline(always)]
fn xorshift64(mut state: u64) -> u64 {
    state ^= state << 13;
    state ^= state >> 7;
    state ^= state << 17;
    state
}

/// Process a 4096-block subchunk section for Anti-Xray obfuscation:
/// - mode 0: Disabled (no-op)
/// - mode 1: Hide unexposed ores (replace with stone / deepslate)
/// - mode 2: Hide unexposed ores AND scramble solid underground stone with fake ores
pub fn process_section_antixray(
    blocks: &mut [u16; 4096],
    section_y: i32,
    mode: u8,
    seed: u64,
) {
    if mode == 0 {
        return;
    }

    let default_replacement = if section_y < 0 { ID_DEEPSLATE } else { ID_STONE };

    // Mode 1: Hide unexposed real ores
    for y in 0..16 {
        for z in 0..16 {
            for x in 0..16 {
                let idx = y * 256 + z * 16 + x;
                let id = blocks[idx];
                if is_ore(id) {
                    if !is_ore_exposed(blocks, x, y, z) {
                        blocks[idx] = default_replacement;
                    }
                }
            }
        }
    }

    // Mode 2: In addition, generate pseudo-random fake ores in deep underground solid blocks
    if mode >= 2 && section_y <= 2 {
        for y in 1..15 {
            for z in 1..15 {
                for x in 1..15 {
                    let idx = y * 256 + z * 16 + x;
                    let id = blocks[idx];
                    if id == ID_STONE || id == ID_DEEPSLATE {
                        // Seed based on coordinates
                        let coord_seed = seed
                            .wrapping_add((section_y as u64) << 32)
                            .wrapping_add((y as u64) << 16)
                            .wrapping_add((z as u64) << 8)
                            .wrapping_add(x as u64);

                        let rand_val = xorshift64(coord_seed);
                        if rand_val % 47 == 0 {
                            // Pseudo-random fake ore!
                            let ore = match (rand_val / 47) % 6 {
                                0 => if section_y < 0 { ID_DEEPSLATE_DIAMOND } else { ID_DIAMOND_ORE },
                                1 => if section_y < 0 { ID_DEEPSLATE_GOLD } else { ID_GOLD_ORE },
                                2 => if section_y < 0 { ID_DEEPSLATE_IRON } else { ID_IRON_ORE },
                                3 => if section_y < 0 { ID_DEEPSLATE_LAPIS } else { ID_LAPIS_ORE },
                                4 => if section_y < 0 { ID_DEEPSLATE_REDSTONE } else { ID_REDSTONE_ORE },
                                _ => if section_y < 0 { ID_DEEPSLATE_EMERALD } else { ID_EMERALD_ORE },
                            };
                            blocks[idx] = ore;
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// C-ABI FFI Exports
// ==========================================

#[no_mangle]
pub extern "C" fn nome_rust_antixray_process_section(
    blocks_ptr: *mut u16,
    section_y: i32,
    mode: u8,
    seed: u64,
) {
    if blocks_ptr.is_null() || mode == 0 {
        return;
    }
    unsafe {
        let slice = std::slice::from_raw_parts_mut(blocks_ptr, 4096);
        let blocks: &mut [u16; 4096] = slice.try_into().unwrap();
        process_section_antixray(blocks, section_y, mode, seed);
    }
}
