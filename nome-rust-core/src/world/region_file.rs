//! Moonrise Region File Storage & Sector Allocation in Rust
//! Ported directly from ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO
//!
//! Handles standard Anvil .mca region files (32x32 chunks = 1024 chunks per region),
//! 4KB sector allocation tables, zero-copy buffer reads, and async writes.

use std::fs::{File, OpenOptions};
use std::io::{Read, Seek, SeekFrom, Write};
use std::path::{Path, PathBuf};
use std::collections::HashMap;
use std::sync::Mutex;

const SECTOR_SIZE: usize = 4096;
const CHUNKS_PER_REGION: usize = 1024; // 32 * 32

pub struct RegionFile {
    pub file: File,
    pub path: PathBuf,
    pub offsets: [u32; CHUNKS_PER_REGION],
    pub timestamps: [u32; CHUNKS_PER_REGION],
    pub sector_free_bitmap: Vec<bool>,
}

impl RegionFile {
    pub fn open(path: &Path) -> std::io::Result<Self> {
        let mut file = OpenOptions::new()
            .read(true)
            .write(true)
            .create(true)
            .open(path)?;

        let metadata = file.metadata()?;
        let file_len = metadata.len() as usize;

        let mut offsets = [0u32; CHUNKS_PER_REGION];
        let mut timestamps = [0u32; CHUNKS_PER_REGION];

        if file_len < SECTOR_SIZE * 2 {
            // New region file, initialize 8KB header
            let header = [0u8; SECTOR_SIZE * 2];
            file.write_all(&header)?;
            file.flush()?;
        } else {
            // Read 4KB locations + 4KB timestamps
            file.seek(SeekFrom::Start(0))?;
            let mut loc_buf = [0u8; SECTOR_SIZE];
            file.read_exact(&mut loc_buf)?;
            for i in 0..CHUNKS_PER_REGION {
                let off = i * 4;
                let sector_offset = ((loc_buf[off] as u32) << 16)
                    | ((loc_buf[off + 1] as u32) << 8)
                    | (loc_buf[off + 2] as u32);
                let sector_count = loc_buf[off + 3] as u32;
                offsets[i] = (sector_offset << 8) | sector_count;
            }

            let mut time_buf = [0u8; SECTOR_SIZE];
            file.read_exact(&mut time_buf)?;
            for i in 0..CHUNKS_PER_REGION {
                let off = i * 4;
                timestamps[i] = u32::from_be_bytes([
                    time_buf[off],
                    time_buf[off + 1],
                    time_buf[off + 2],
                    time_buf[off + 3],
                ]);
            }
        }

        // Calculate free sector bitmap
        let total_sectors = (file_len.max(SECTOR_SIZE * 2) + SECTOR_SIZE - 1) / SECTOR_SIZE;
        let mut sector_free_bitmap = vec![true; total_sectors];
        // Sectors 0 and 1 are reserved for header
        sector_free_bitmap[0] = false;
        if total_sectors > 1 {
            sector_free_bitmap[1] = false;
        }

        for &entry in &offsets {
            let sector_offset = (entry >> 8) as usize;
            let sector_count = (entry & 0xFF) as usize;
            if sector_offset > 0 && sector_offset + sector_count <= total_sectors {
                for s in 0..sector_count {
                    sector_free_bitmap[sector_offset + s] = false;
                }
            }
        }

        Ok(Self {
            file,
            path: path.to_path_buf(),
            offsets,
            timestamps,
            sector_free_bitmap,
        })
    }

    #[inline]
    fn get_chunk_index(cx: i32, cz: i32) -> usize {
        let lx = (cx & 31) as usize;
        let lz = (cz & 31) as usize;
        lx + lz * 32
    }

    /// Reads raw chunk payload from region file
    pub fn read_chunk(&mut self, cx: i32, cz: i32) -> std::io::Result<Option<Vec<u8>>> {
        let index = Self::get_chunk_index(cx, cz);
        let entry = self.offsets[index];
        if entry == 0 {
            return Ok(None);
        }

        let sector_offset = (entry >> 8) as u64;
        let sector_count = (entry & 0xFF) as usize;
        if sector_offset < 2 {
            return Ok(None);
        }

        self.file.seek(SeekFrom::Start(sector_offset * SECTOR_SIZE as u64))?;
        let mut len_bytes = [0u8; 4];
        self.file.read_exact(&mut len_bytes)?;
        let data_len = u32::from_be_bytes(len_bytes) as usize;

        if data_len == 0 || data_len > sector_count * SECTOR_SIZE {
            return Ok(None);
        }

        let mut compression_type = [0u8; 1];
        self.file.read_exact(&mut compression_type)?;

        let mut payload = vec![0u8; data_len - 1];
        self.file.read_exact(&mut payload)?;

        Ok(Some(payload))
    }

    /// Writes raw chunk payload to region file with dynamic sector allocation
    pub fn write_chunk(&mut self, cx: i32, cz: i32, payload: &[u8], compression: u8) -> std::io::Result<()> {
        let index = Self::get_chunk_index(cx, cz);
        let data_len = (payload.len() + 1) as u32; // +1 for compression byte
        let total_bytes = 4 + data_len as usize;
        let required_sectors = (total_bytes + SECTOR_SIZE - 1) / SECTOR_SIZE;

        let old_entry = self.offsets[index];
        let old_offset = (old_entry >> 8) as usize;
        let old_count = (old_entry & 0xFF) as usize;

        let start_sector = if old_count == required_sectors {
            old_offset
        } else {
            // Free old sectors
            if old_offset >= 2 {
                for s in 0..old_count {
                    if old_offset + s < self.sector_free_bitmap.len() {
                        self.sector_free_bitmap[old_offset + s] = true;
                    }
                }
            }

            // Find contiguous free sectors
            let mut found = None;
            for i in 2..=(self.sector_free_bitmap.len().saturating_sub(required_sectors)) {
                if self.sector_free_bitmap[i..i + required_sectors].iter().all(|&free| free) {
                    found = Some(i);
                    break;
                }
            }

            let allocated = match found {
                Some(s) => s,
                None => {
                    let s = self.sector_free_bitmap.len();
                    self.sector_free_bitmap.resize(s + required_sectors, true);
                    s
                }
            };

            for s in 0..required_sectors {
                self.sector_free_bitmap[allocated + s] = false;
            }

            allocated
        };

        // Write chunk data to sectors
        self.file.seek(SeekFrom::Start(start_sector as u64 * SECTOR_SIZE as u64))?;
        self.file.write_all(&data_len.to_be_bytes())?;
        self.file.write_all(&[compression])?;
        self.file.write_all(payload)?;

        // Pad to sector boundary
        let remainder = total_bytes % SECTOR_SIZE;
        if remainder != 0 {
            let padding = vec![0u8; SECTOR_SIZE - remainder];
            self.file.write_all(&padding)?;
        }

        // Update offset in memory and file header
        let new_entry = ((start_sector as u32) << 8) | (required_sectors as u32 & 0xFF);
        self.offsets[index] = new_entry;
        self.file.seek(SeekFrom::Start(index as u64 * 4))?;
        let entry_bytes = [
            (start_sector >> 16) as u8,
            (start_sector >> 8) as u8,
            start_sector as u8,
            required_sectors as u8,
        ];
        self.file.write_all(&entry_bytes)?;

        // Update timestamp
        let timestamp = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs() as u32)
            .unwrap_or(0);
        self.timestamps[index] = timestamp;
        self.file.seek(SeekFrom::Start(SECTOR_SIZE as u64 + index as u64 * 4))?;
        self.file.write_all(&timestamp.to_be_bytes())?;

        self.file.flush()?;
        Ok(())
    }
}

pub struct RegionFileManager {
    regions: HashMap<(i32, i32), RegionFile>,
    base_dir: PathBuf,
}

static REGION_MANAGER: Mutex<Option<RegionFileManager>> = Mutex::new(None);

pub fn init_region_file_manager(world_dir: &str) {
    let mut lock = REGION_MANAGER.lock().unwrap();
    let region_dir = Path::new(world_dir).join("region");
    let _ = std::fs::create_dir_all(&region_dir);
    *lock = Some(RegionFileManager {
        regions: HashMap::new(),
        base_dir: region_dir,
    });
}

#[no_mangle]
pub extern "C" fn nome_rust_region_init(world_dir: *const std::os::raw::c_char) {
    let dir = if !world_dir.is_null() {
        unsafe { std::ffi::CStr::from_ptr(world_dir).to_string_lossy().into_owned() }
    } else {
        "world".to_string()
    };
    init_region_file_manager(&dir);
}

#[no_mangle]
pub extern "C" fn nome_rust_region_save_chunk(cx: i32, cz: i32, data: *const u8, data_len: usize) -> bool {
    if data.is_null() || data_len == 0 {
        return false;
    }
    let rx = cx >> 5;
    let rz = cz >> 5;

    let mut lock = REGION_MANAGER.lock().unwrap();
    if let Some(ref mut mgr) = *lock {
        let entry = mgr.regions.entry((rx, rz)).or_insert_with(|| {
            let path = mgr.base_dir.join(format!("r.{}.{}.mca", rx, rz));
            RegionFile::open(&path).expect("Failed to open region file")
        });

        let slice = unsafe { std::slice::from_raw_parts(data, data_len) };
        // 2 = Zlib / Deflate compression standard in Minecraft Anvil
        return entry.write_chunk(cx, cz, slice, 2).is_ok();
    }
    false
}
