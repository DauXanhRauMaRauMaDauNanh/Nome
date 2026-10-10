//! High Performance Chunk and SubChunk Representation in Rust
//! Minecraft World height: -64 to 319 (384 blocks total = 24 sections of 16x16x16)

pub const SECTION_SIZE: usize = 16 * 16 * 16; // 4096 blocks
pub const SECTIONS_PER_CHUNK: usize = 24;

#[derive(Clone)]
pub struct SubChunk {
    pub blocks: Box<[u16; SECTION_SIZE]>,
    pub block_light: Box<[u8; SECTION_SIZE / 2]>,
    pub sky_light: Box<[u8; SECTION_SIZE / 2]>,
}

impl SubChunk {
    pub fn new() -> Self {
        Self {
            blocks: Box::new([0u16; SECTION_SIZE]),
            block_light: Box::new([0u8; SECTION_SIZE / 2]),
            sky_light: Box::new([0u8; SECTION_SIZE / 2]),
        }
    }

    #[inline(always)]
    pub fn get_block(&self, x: usize, y: usize, z: usize) -> u16 {
        self.blocks[(y & 15) * 256 + (z & 15) * 16 + (x & 15)]
    }

    #[inline(always)]
    pub fn set_block(&mut self, x: usize, y: usize, z: usize, block_id: u16) {
        self.blocks[(y & 15) * 256 + (z & 15) * 16 + (x & 15)] = block_id;
    }
}

pub struct Chunk {
    pub chunk_x: i32,
    pub chunk_z: i32,
    pub sections: Vec<SubChunk>,
    pub is_dirty: bool,
}

impl Chunk {
    pub fn new(chunk_x: i32, chunk_z: i32) -> Self {
        let mut sections = Vec::with_capacity(SECTIONS_PER_CHUNK);
        for _ in 0..SECTIONS_PER_CHUNK {
            sections.push(SubChunk::new());
        }
        Self {
            chunk_x,
            chunk_z,
            sections,
            is_dirty: false,
        }
    }

    pub fn set_block(&mut self, x: usize, y: i32, z: usize, block_id: u16) {
        let section_idx = ((y + 64) / 16) as usize;
        if section_idx < SECTIONS_PER_CHUNK {
            self.sections[section_idx].set_block(x, (y + 64) as usize % 16, z, block_id);
            self.is_dirty = true;
        }
    }

    pub fn get_block(&self, x: usize, y: i32, z: usize) -> u16 {
        let section_idx = ((y + 64) / 16) as usize;
        if section_idx < SECTIONS_PER_CHUNK {
            self.sections[section_idx].get_block(x, (y + 64) as usize % 16, z)
        } else {
            0
        }
    }
}
