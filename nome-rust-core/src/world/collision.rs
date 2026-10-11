//! Moonrise Collision & Voxel Shape Optimization
//! Ported directly from Paper 26.3 ca/spottedleaf/moonrise/patches/collisions/CollisionUtil.class
//! Completely avoids VoxelShape heap allocations by checking fast bit flags (Full Cube, Air, Slab).

#[derive(Debug, Clone, Copy)]
pub struct AABB {
    pub min_x: f64,
    pub min_y: f64,
    pub min_z: f64,
    pub max_x: f64,
    pub max_y: f64,
    pub max_z: f64,
}

impl AABB {
    pub fn new(min_x: f64, min_y: f64, min_z: f64, max_x: f64, max_y: f64, max_z: f64) -> Self {
        Self { min_x, min_y, min_z, max_x, max_y, max_z }
    }

    pub fn intersects(&self, other: &AABB) -> bool {
        self.min_x < other.max_x && self.max_x > other.min_x &&
        self.min_y < other.max_y && self.max_y > other.min_y &&
        self.min_z < other.max_z && self.max_z > other.min_z
    }
}

// Block shape flags:
pub const SHAPE_EMPTY: u8 = 0;
pub const SHAPE_FULL_CUBE: u8 = 1;
pub const SHAPE_SLAB_BOTTOM: u8 = 2;
pub const SHAPE_SLAB_TOP: u8 = 3;

#[inline(always)]
pub fn get_block_shape_type(block_id: u16) -> u8 {
    if block_id == 0 {
        SHAPE_EMPTY // Air
    } else if block_id == 44 || block_id == 126 { // Slabs
        SHAPE_SLAB_BOTTOM
    } else {
        SHAPE_FULL_CUBE // Solid full block (Stone, Dirt, Planks, etc.)
    }
}

/// Moonrise fast collision pass: Checks if entity AABB intersects any solid block
pub fn collides_with_blocks(
    entity_box: &AABB,
    block_accessor: impl Fn(i32, i32, i32) -> u16
) -> bool {
    let min_bx = entity_box.min_x.floor() as i32;
    let max_bx = entity_box.max_x.ceil() as i32;
    let min_by = entity_box.min_y.floor() as i32;
    let max_by = entity_box.max_y.ceil() as i32;
    let min_bz = entity_box.min_z.floor() as i32;
    let max_bz = entity_box.max_z.ceil() as i32;

    for x in min_bx..max_bx {
        for y in min_by..max_by {
            for z in min_bz..max_bz {
                let block = block_accessor(x, y, z);
                let shape = get_block_shape_type(block);
                match shape {
                    SHAPE_EMPTY => continue,
                    SHAPE_FULL_CUBE => {
                        let block_box = AABB::new(
                            x as f64, y as f64, z as f64,
                            (x + 1) as f64, (y + 1) as f64, (z + 1) as f64
                        );
                        if entity_box.intersects(&block_box) {
                            return true;
                        }
                    }
                    SHAPE_SLAB_BOTTOM => {
                        let block_box = AABB::new(
                            x as f64, y as f64, z as f64,
                            (x + 1) as f64, y as f64 + 0.5, (z + 1) as f64
                        );
                        if entity_box.intersects(&block_box) {
                            return true;
                        }
                    }
                    SHAPE_SLAB_TOP => {
                        let block_box = AABB::new(
                            x as f64, y as f64 + 0.5, z as f64,
                            (x + 1) as f64, (y + 1) as f64, (z + 1) as f64
                        );
                        if entity_box.intersects(&block_box) {
                            return true;
                        }
                    }
                    _ => continue,
                }
            }
        }
    }
    false
}

// C FFI
#[no_mangle]
pub extern "C" fn nome_rust_check_aabb_collision(
    min_x: f64, min_y: f64, min_z: f64,
    max_x: f64, max_y: f64, max_z: f64,
    block_grid: *const u16,
    grid_w: i32, grid_h: i32, grid_d: i32
) -> bool {
    if block_grid.is_null() {
        return false;
    }
    let aabb = AABB::new(min_x, min_y, min_z, max_x, max_y, max_z);
    collides_with_blocks(&aabb, |x, y, z| {
        if x >= 0 && x < grid_w && y >= 0 && y < grid_h && z >= 0 && z < grid_d {
            let idx = (y * grid_w * grid_d + z * grid_w + x) as usize;
            unsafe { *block_grid.add(idx) }
        } else {
            0
        }
    })
}
