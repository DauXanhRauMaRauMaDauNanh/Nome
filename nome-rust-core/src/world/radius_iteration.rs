//! Moonrise ParallelSearchRadiusIteration in Rust
//! Ported directly from ca.spottedleaf.moonrise.patches.chunk_system.util.ParallelSearchRadiusIteration
//!
//! Provides concentric Euclidean distance chunk iteration tables for smooth,
//! prioritized chunk streaming to players from center outward.

use std::sync::RwLock;

static CACHED_ITERATIONS: RwLock<Option<Vec<Vec<(i32, i32)>>>> = RwLock::new(None);

pub struct ParallelSearchRadiusIteration;

impl ParallelSearchRadiusIteration {
    /// Pre-calculates concentric Euclidean sorted chunk offsets for radii 0..=32
    pub fn init() {
        let mut write = CACHED_ITERATIONS.write().unwrap();
        if write.is_none() {
            let mut list = Vec::with_capacity(33);
            for r in 0..=32 {
                list.push(Self::compute_iteration(r));
            }
            *write = Some(list);
        }
    }

    /// Computes search iteration for radius `r` sorted by Euclidean distance `dx*dx + dz*dz`
    pub fn compute_iteration(r: i32) -> Vec<(i32, i32)> {
        let mut coords = Vec::with_capacity(((2 * r + 1) * (2 * r + 1)) as usize);
        for dz in -r..=r {
            for dx in -r..=r {
                coords.push((dx, dz));
            }
        }
        // Sort by Euclidean distance squared, then Chebyshev
        coords.sort_by(|a, b| {
            let dist_a = a.0 * a.0 + a.1 * a.1;
            let dist_b = b.0 * b.0 + b.1 * b.1;
            dist_a.cmp(&dist_b).then_with(|| {
                let cheb_a = a.0.abs().max(a.1.abs());
                let cheb_b = b.0.abs().max(b.1.abs());
                cheb_a.cmp(&cheb_b)
            })
        });
        coords
    }

    /// Retrieves pre-sorted chunk offsets for radius `radius`.
    pub fn get_search_iteration(radius: i32) -> Vec<(i32, i32)> {
        let r = radius.clamp(0, 32) as usize;
        let read = CACHED_ITERATIONS.read().unwrap();
        if let Some(ref tables) = *read {
            if r < tables.len() {
                return tables[r].clone();
            }
        }
        Self::compute_iteration(radius)
    }
}

#[no_mangle]
pub extern "C" fn nome_rust_radius_iteration_init() {
    ParallelSearchRadiusIteration::init();
}

#[no_mangle]
pub extern "C" fn nome_rust_get_radius_count(radius: i32) -> i32 {
    let r = radius.clamp(0, 32);
    (2 * r + 1) * (2 * r + 1)
}

/// Fills `out_dx` and `out_dz` buffers with sorted chunk offsets for view distance `radius`.
#[no_mangle]
pub extern "C" fn nome_rust_get_radius_iteration(radius: i32, out_dx: *mut i32, out_dz: *mut i32, max_len: i32) -> i32 {
    if out_dx.is_null() || out_dz.is_null() || max_len <= 0 {
        return 0;
    }
    let iteration = ParallelSearchRadiusIteration::get_search_iteration(radius);
    let count = (iteration.len() as i32).min(max_len);
    for i in 0..count as usize {
        unsafe {
            *out_dx.add(i) = iteration[i].0;
            *out_dz.add(i) = iteration[i].1;
        }
    }
    count
}
