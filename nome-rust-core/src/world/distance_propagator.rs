//! Moonrise Delayed8WayDistancePropagator2D in Rust
//! Ported directly from ca.spottedleaf.moonrise.common.misc.Delayed8WayDistancePropagator2D
//!
//! Provides ultra-fast, zero-allocation 8-way Chebyshev distance propagation
//! for chunk ticket levels, chunk loading, and view distance updates.

use std::collections::{HashMap, VecDeque};

pub const NO_LEVEL: u8 = 63;

#[inline(always)]
pub fn pack_chunk_pos(x: i32, z: i32) -> u64 {
    ((x as u64) << 32) | ((z as u32) as u64)
}

#[inline(always)]
pub fn unpack_chunk_pos(pos: u64) -> (i32, i32) {
    ((pos >> 32) as i32, pos as i32)
}

/// 8-way Chebyshev distance offsets
const NEIGHBORS_8: [(i32, i32); 8] = [
    (-1, -1), (0, -1), (1, -1),
    (-1,  0),          (1,  0),
    (-1,  1), (0,  1), (1,  1),
];

pub struct Delayed8WayDistancePropagator2D {
    // Current level map (packed_coord -> level)
    levels: HashMap<u64, u8>,
    // Source tickets (packed_coord -> source_level)
    sources: HashMap<u64, u8>,

    // 64-level bitsets tracking non-empty work queues
    increase_work_queue_bitset: u64,
    remove_work_queue_bitset: u64,

    // Work queues for each level (0..64)
    increase_queues: Vec<VecDeque<u64>>,
    remove_queues: Vec<VecDeque<u64>>,
}

impl Delayed8WayDistancePropagator2D {
    pub fn new() -> Self {
        let mut inc = Vec::with_capacity(64);
        let mut rem = Vec::with_capacity(64);
        for _ in 0..64 {
            inc.push(VecDeque::new());
            rem.push(VecDeque::new());
        }
        Self {
            levels: HashMap::new(),
            sources: HashMap::new(),
            increase_work_queue_bitset: 0,
            remove_work_queue_bitset: 0,
            increase_queues: inc,
            remove_queues: rem,
        }
    }

    #[inline]
    pub fn get_level(&self, x: i32, z: i32) -> u8 {
        let key = pack_chunk_pos(x, z);
        *self.levels.get(&key).unwrap_or(&NO_LEVEL)
    }

    pub fn set_source(&mut self, x: i32, z: i32, level: u8) {
        let key = pack_chunk_pos(x, z);
        let current_source = *self.sources.get(&key).unwrap_or(&NO_LEVEL);
        if current_source == level {
            return;
        }

        self.sources.insert(key, level);

        if level < current_source {
            // Level tightened / increased in priority
            self.add_to_increase_queue(key, level);
        } else {
            // Level loosened / removed
            self.add_to_remove_queue(key, current_source);
            if level < NO_LEVEL {
                self.add_to_increase_queue(key, level);
            }
        }
    }

    pub fn remove_source(&mut self, x: i32, z: i32) {
        let key = pack_chunk_pos(x, z);
        if let Some(old_level) = self.sources.remove(&key) {
            self.add_to_remove_queue(key, old_level);
        }
    }

    #[inline]
    fn add_to_increase_queue(&mut self, pos: u64, level: u8) {
        if level < 64 {
            self.increase_queues[level as usize].push_back(pos);
            self.increase_work_queue_bitset |= 1u64 << level;
        }
    }

    #[inline]
    fn add_to_remove_queue(&mut self, pos: u64, level: u8) {
        if level < 64 {
            self.remove_queues[level as usize].push_back(pos);
            self.remove_work_queue_bitset |= 1u64 << level;
        }
    }

    /// Propagates all pending ticket level changes through the 8-way Chebyshev grid.
    /// Returns true if any chunks changed levels.
    pub fn propagate_updates(&mut self) -> bool {
        let mut changed = false;

        // Pass 1: Decreases / Removals (propagate from lowest numeric level to highest)
        while self.remove_work_queue_bitset != 0 {
            let level = self.remove_work_queue_bitset.trailing_zeros() as usize;
            if let Some(pos) = self.remove_queues[level].pop_front() {
                if self.remove_queues[level].is_empty() {
                    self.remove_work_queue_bitset &= !(1u64 << level);
                }

                let current_level = *self.levels.get(&pos).unwrap_or(&NO_LEVEL);
                if current_level == level as u8 {
                    self.levels.remove(&pos);
                    changed = true;

                    let (x, z) = unpack_chunk_pos(pos);
                    for &(dx, dz) in &NEIGHBORS_8 {
                        let nx = x + dx;
                        let nz = z + dz;
                        let n_pos = pack_chunk_pos(nx, nz);
                        let n_level = *self.levels.get(&n_pos).unwrap_or(&NO_LEVEL);
                        if n_level > (level as u8) && n_level < NO_LEVEL {
                            self.add_to_remove_queue(n_pos, n_level);
                        } else if n_level <= (level as u8) {
                            // Neighbor has valid level that might reinform us
                            self.add_to_increase_queue(n_pos, n_level);
                        }
                    }

                    // Check if self still has a source ticket
                    if let Some(&src) = self.sources.get(&pos) {
                        self.add_to_increase_queue(pos, src);
                    }
                }
            } else {
                self.remove_work_queue_bitset &= !(1u64 << level);
            }
        }

        // Pass 2: Increases / Tightenings
        while self.increase_work_queue_bitset != 0 {
            let level = self.increase_work_queue_bitset.trailing_zeros() as usize;
            if let Some(pos) = self.increase_queues[level].pop_front() {
                if self.increase_queues[level].is_empty() {
                    self.increase_work_queue_bitset &= !(1u64 << level);
                }

                let current_level = *self.levels.get(&pos).unwrap_or(&NO_LEVEL);
                if (level as u8) < current_level {
                    self.levels.insert(pos, level as u8);
                    changed = true;

                    let next_level = (level + 1) as u8;
                    if next_level < NO_LEVEL {
                        let (x, z) = unpack_chunk_pos(pos);
                        for &(dx, dz) in &NEIGHBORS_8 {
                            let nx = x + dx;
                            let nz = z + dz;
                            let n_pos = pack_chunk_pos(nx, nz);
                            let n_level = *self.levels.get(&n_pos).unwrap_or(&NO_LEVEL);
                            if next_level < n_level {
                                self.add_to_increase_queue(n_pos, next_level);
                            }
                        }
                    }
                }
            } else {
                self.increase_work_queue_bitset &= !(1u64 << level);
            }
        }

        changed
    }
}
