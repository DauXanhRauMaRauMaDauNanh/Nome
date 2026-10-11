//! Paper 26.3 Folia Regionized Scheduler and Multi-Threaded Task Pipeline
//! Allows plugins and native systems to schedule tasks bound to specific chunk regions,
//! entities, or asynchronously across worker threads with zero Java GC overhead.

use std::collections::{HashMap, VecDeque};
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::Mutex;
use std::time::Instant;

#[derive(Debug)]
pub struct ScheduledTask {
    pub task_id: u64,
    pub execute_at_tick: u64,
    pub interval_ticks: u64,
    pub callback_id: u64,
    pub cancelled: AtomicBool,
}

#[allow(dead_code)]
pub struct RegionSchedulerEngine {
    next_task_id: AtomicU64,
    // (region_x, region_z) -> tasks
    region_tasks: Mutex<HashMap<(i32, i32), Vec<ScheduledTask>>>,
    // entity_id -> tasks
    entity_tasks: Mutex<HashMap<u64, Vec<ScheduledTask>>>,
    // async tasks
    async_queue: Mutex<VecDeque<(u64, Instant, u64)>>, // (task_id, execute_at, callback_id)
}

static SCHEDULER: Mutex<Option<RegionSchedulerEngine>> = Mutex::new(None);

pub fn init_scheduler() {
    let mut lock = SCHEDULER.lock().unwrap();
    if lock.is_none() {
        *lock = Some(RegionSchedulerEngine {
            next_task_id: AtomicU64::new(1),
            region_tasks: Mutex::new(HashMap::new()),
            entity_tasks: Mutex::new(HashMap::new()),
            async_queue: Mutex::new(VecDeque::new()),
        });
    }
}

// ==========================================
// C-ABI FFI Exports
// ==========================================

#[no_mangle]
pub extern "C" fn nome_rust_scheduler_init() {
    init_scheduler();
}

#[no_mangle]
pub extern "C" fn nome_rust_schedule_region_task(
    rx: i32,
    rz: i32,
    delay_ticks: u64,
    interval_ticks: u64,
    callback_id: u64,
    current_tick: u64,
) -> u64 {
    let lock = SCHEDULER.lock().unwrap();
    if let Some(ref sched) = *lock {
        let task_id = sched.next_task_id.fetch_add(1, Ordering::SeqCst);
        let task = ScheduledTask {
            task_id,
            execute_at_tick: current_tick + delay_ticks,
            interval_ticks,
            callback_id,
            cancelled: AtomicBool::new(false),
        };
        let mut map = sched.region_tasks.lock().unwrap();
        map.entry((rx, rz)).or_default().push(task);
        return task_id;
    }
    0
}

#[no_mangle]
pub extern "C" fn nome_rust_schedule_entity_task(
    entity_id: u64,
    delay_ticks: u64,
    interval_ticks: u64,
    callback_id: u64,
    current_tick: u64,
) -> u64 {
    let lock = SCHEDULER.lock().unwrap();
    if let Some(ref sched) = *lock {
        let task_id = sched.next_task_id.fetch_add(1, Ordering::SeqCst);
        let task = ScheduledTask {
            task_id,
            execute_at_tick: current_tick + delay_ticks,
            interval_ticks,
            callback_id,
            cancelled: AtomicBool::new(false),
        };
        let mut map = sched.entity_tasks.lock().unwrap();
        map.entry(entity_id).or_default().push(task);
        return task_id;
    }
    0
}

#[no_mangle]
pub extern "C" fn nome_rust_cancel_task(task_id: u64) -> bool {
    let lock = SCHEDULER.lock().unwrap();
    if let Some(ref sched) = *lock {
        // Search region tasks
        {
            let mut map = sched.region_tasks.lock().unwrap();
            for tasks in map.values_mut() {
                for t in tasks.iter_mut() {
                    if t.task_id == task_id {
                        t.cancelled.store(true, Ordering::SeqCst);
                        return true;
                    }
                }
            }
        }
        // Search entity tasks
        {
            let mut map = sched.entity_tasks.lock().unwrap();
            for tasks in map.values_mut() {
                for t in tasks.iter_mut() {
                    if t.task_id == task_id {
                        t.cancelled.store(true, Ordering::SeqCst);
                        return true;
                    }
                }
            }
        }
    }
    false
}

/// Polls ready tasks for current tick and returns the count of ready callbacks in out_callbacks
#[no_mangle]
pub extern "C" fn nome_rust_poll_ready_tasks(
    current_tick: u64,
    out_callbacks: *mut u64,
    max_callbacks: usize,
) -> usize {
    if out_callbacks.is_null() || max_callbacks == 0 {
        return 0;
    }

    let lock = SCHEDULER.lock().unwrap();
    if let Some(ref sched) = *lock {
        let mut ready = Vec::new();

        // 1. Check region tasks
        {
            let mut map = sched.region_tasks.lock().unwrap();
            for tasks in map.values_mut() {
                let mut i = 0;
                while i < tasks.len() {
                    if tasks[i].cancelled.load(Ordering::SeqCst) {
                        tasks.swap_remove(i);
                        continue;
                    }
                    if tasks[i].execute_at_tick <= current_tick {
                        ready.push(tasks[i].callback_id);
                        if tasks[i].interval_ticks > 0 {
                            tasks[i].execute_at_tick = current_tick + tasks[i].interval_ticks;
                            i += 1;
                        } else {
                            tasks.swap_remove(i);
                        }
                    } else {
                        i += 1;
                    }
                    if ready.len() >= max_callbacks {
                        break;
                    }
                }
            }
        }

        let count = ready.len().min(max_callbacks);
        unsafe {
            for i in 0..count {
                *out_callbacks.add(i) = ready[i];
            }
        }
        return count;
    }
    0
}
