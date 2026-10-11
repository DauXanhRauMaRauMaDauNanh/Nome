//! Foreign Function Interface (FFI) bindings for Java (JNI & Project Panama) and C/C++
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Mutex;
use crate::world::Chunk;
use crate::ecs::EntityManager;
use crate::tick::TickScheduler;

pub struct NativeServerContext {
    pub is_running: AtomicBool,
    pub scheduler: Mutex<TickScheduler>,
    pub entity_manager: Mutex<EntityManager>,
    pub spawn_chunk: Mutex<Chunk>,
}

static GLOBAL_SERVER: Mutex<Option<NativeServerContext>> = Mutex::new(None);

#[no_mangle]
pub extern "C" fn nome_rust_init() -> bool {
    let mut lock = GLOBAL_SERVER.lock().unwrap();
    if lock.is_none() {
        let ctx = NativeServerContext {
            is_running: AtomicBool::new(false),
            scheduler: Mutex::new(TickScheduler::new(20.0)),
            entity_manager: Mutex::new(EntityManager::new()),
            spawn_chunk: Mutex::new(Chunk::new(0, 0)),
        };
        *lock = Some(ctx);
    }
    true
}

#[no_mangle]
pub extern "C" fn nome_rust_start() -> bool {
    let lock = GLOBAL_SERVER.lock().unwrap();
    if let Some(ref server) = *lock {
        server.is_running.store(true, Ordering::SeqCst);
        return true;
    }
    false
}

#[no_mangle]
pub extern "C" fn nome_rust_stop() {
    let lock = GLOBAL_SERVER.lock().unwrap();
    if let Some(ref server) = *lock {
        server.is_running.store(false, Ordering::SeqCst);
    }
}

#[no_mangle]
pub extern "C" fn nome_rust_tick_once() -> f64 {
    let lock = GLOBAL_SERVER.lock().unwrap();
    if let Some(ref server) = *lock {
        let mut scheduler = server.scheduler.lock().unwrap();
        let start = scheduler.begin_tick();

        // Run ECS physics tick
        {
            let mut em = server.entity_manager.lock().unwrap();
            em.tick_physics();
        }

        scheduler.end_tick(start);
        return scheduler.rolling_tps;
    }
    20.0
}

#[no_mangle]
pub extern "C" fn nome_rust_get_mspt() -> f64 {
    let lock = GLOBAL_SERVER.lock().unwrap();
    if let Some(ref server) = *lock {
        let scheduler = server.scheduler.lock().unwrap();
        return scheduler.current_mspt;
    }
    0.0
}

#[no_mangle]
pub extern "C" fn nome_rust_set_block(x: i32, y: i32, z: i32, block_id: u16) {
    let lock = GLOBAL_SERVER.lock().unwrap();
    if let Some(ref server) = *lock {
        let mut chunk = server.spawn_chunk.lock().unwrap();
        chunk.set_block((x & 15) as usize, y, (z & 15) as usize, block_id);
    }
}

#[no_mangle]
pub extern "C" fn nome_rust_get_block(x: i32, y: i32, z: i32) -> u16 {
    let lock = GLOBAL_SERVER.lock().unwrap();
    if let Some(ref server) = *lock {
        let chunk = server.spawn_chunk.lock().unwrap();
        return chunk.get_block((x & 15) as usize, y, (z & 15) as usize);
    }
    0
}
