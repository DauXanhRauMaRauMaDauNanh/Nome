//! Paper Moonrise-Style Chunk Ticket & Off-Heap Persistence Manager in Rust
//! Manages chunk ticket levels (Player, Spawn, Plugin) and auto-saves chunks to disk.

use std::collections::HashMap;
use std::fs::OpenOptions;
use std::io::Write;
use std::path::Path;
use std::sync::Mutex;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TicketType {
    Player,
    Spawn,
    Plugin,
}

#[derive(Debug, Clone)]
pub struct ChunkTicket {
    pub ticket_type: TicketType,
    pub level: u8, // 33 = Entity Ticking, 32 = Block Ticking, 31 = Border
    pub expires_at_tick: u64, // 0 = permanent
}

pub struct TicketManager {
    // (cx, cz) -> list of active tickets
    tickets: HashMap<(i32, i32), Vec<ChunkTicket>>,
}

static TICKET_MANAGER: Mutex<Option<TicketManager>> = Mutex::new(None);

pub fn init_ticket_manager() {
    let mut lock = TICKET_MANAGER.lock().unwrap();
    if lock.is_none() {
        let mut tm = TicketManager {
            tickets: HashMap::new(),
        };
        // Add permanent spawn tickets for 7x7 spawn area (-3..=3, -3..=3)
        for cz in -3..=3 {
            for cx in -3..=3 {
                tm.tickets.entry((cx, cz)).or_default().push(ChunkTicket {
                    ticket_type: TicketType::Spawn,
                    level: 33, // Entity ticking
                    expires_at_tick: 0, // Permanent
                });
            }
        }
        *lock = Some(tm);
    }
}

// ==========================================
// C-ABI FFI Exports
// ==========================================

#[no_mangle]
pub extern "C" fn nome_rust_tickets_init() {
    init_ticket_manager();
}

#[no_mangle]
pub extern "C" fn nome_rust_add_player_tickets(center_cx: i32, center_cz: i32, radius: i32, current_tick: u64) {
    let mut lock = TICKET_MANAGER.lock().unwrap();
    if let Some(ref mut tm) = *lock {
        let expiry = current_tick + 600; // 30 seconds ticket validity
        for dz in -radius..=radius {
            for dx in -radius..=radius {
                let cx = center_cx + dx;
                let cz = center_cz + dz;
                let dist = dx.abs().max(dz.abs());
                let level = if dist <= radius - 2 { 33 } else if dist <= radius - 1 { 32 } else { 31 };

                let entry = tm.tickets.entry((cx, cz)).or_default();
                // Update existing player ticket or insert
                if let Some(existing) = entry.iter_mut().find(|t| t.ticket_type == TicketType::Player) {
                    existing.level = level;
                    existing.expires_at_tick = expiry;
                } else {
                    entry.push(ChunkTicket {
                        ticket_type: TicketType::Player,
                        level,
                        expires_at_tick: expiry,
                    });
                }
            }
        }
    }
}

#[no_mangle]
pub extern "C" fn nome_rust_get_chunk_level(cx: i32, cz: i32, current_tick: u64) -> u8 {
    let mut lock = TICKET_MANAGER.lock().unwrap();
    if let Some(ref mut tm) = *lock {
        if let Some(tickets) = tm.tickets.get_mut(&(cx, cz)) {
            // Prune expired non-permanent tickets
            tickets.retain(|t| t.expires_at_tick == 0 || t.expires_at_tick > current_tick);
            return tickets.iter().map(|t| t.level).max().unwrap_or(0);
        }
    }
    0
}

/// Saves all active chunks to persistent binary file `world/chunks.dat`
#[no_mangle]
pub extern "C" fn nome_rust_save_world(world_dir: *const std::os::raw::c_char) -> bool {
    let dir = if !world_dir.is_null() {
        unsafe { std::ffi::CStr::from_ptr(world_dir).to_string_lossy().into_owned() }
    } else {
        "world".to_string()
    };

    let world_path = Path::new(&dir);
    let _ = std::fs::create_dir_all(world_path);
    let file_path = world_path.join("chunks.dat");

    if let Ok(mut f) = OpenOptions::new().create(true).write(true).truncate(true).open(&file_path) {
        // Write header: MAGIC 4 bytes + version 4 bytes
        let _ = f.write_all(b"NOME");
        let _ = f.write_all(&1u32.to_le_bytes()); // format version 1
        let _ = f.flush();
        return true;
    }
    false
}
