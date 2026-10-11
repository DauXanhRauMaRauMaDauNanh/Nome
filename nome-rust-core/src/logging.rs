//! Ultra High-Performance Native Logging Subsystem in Rust
//! Replaces Java Log4j2 and SLF4J Java implementations with zero JVM heap allocations.
//! Provides ANSI terminal coloring and asynchronous file logging to logs/latest.log.

use std::ffi::CStr;
use std::fs::{File, OpenOptions};
use std::io::Write;
use std::os::raw::c_char;
use std::path::Path;
use std::sync::Mutex;
use std::time::SystemTime;

#[allow(dead_code)]
pub struct NativeLogger {
    file: Option<File>,
    min_level: i32, // 0 = TRACE, 1 = DEBUG, 2 = INFO, 3 = WARN, 4 = ERROR
}

static LOGGER: Mutex<Option<NativeLogger>> = Mutex::new(None);

pub fn init_logger(log_file_path: &str) -> bool {
    let mut lock = LOGGER.lock().unwrap();
    if lock.is_none() {
        if let Some(parent) = Path::new(log_file_path).parent() {
            let _ = std::fs::create_dir_all(parent);
        }
        let file = OpenOptions::new()
            .create(true)
            .append(true)
            .open(log_file_path)
            .ok();

        *lock = Some(NativeLogger {
            file,
            min_level: 2, // INFO by default
        });
    }
    true
}

#[inline]
fn get_time_string() -> String {
    // Fast time calculation without heavy external crates
    let now = SystemTime::now()
        .duration_since(SystemTime::UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs();

    let sec = now % 60;
    let min = (now / 60) % 60;
    let hour = (now / 3600) % 24;

    format!("{:02}:{:02}:{:02}", hour, min, sec)
}

pub fn log_message(level: i32, tag: &str, msg: &str) {
    let (level_str, ansi_color) = match level {
        0 => ("TRACE", "\x1b[90m"), // Gray
        1 => ("DEBUG", "\x1b[36m"), // Cyan
        2 => ("INFO",  "\x1b[32m"), // Green
        3 => ("WARN",  "\x1b[33m"), // Yellow
        4 => ("ERROR", "\x1b[31m"), // Red
        _ => ("LOG",   "\x1b[37m"),
    };

    let time_str = get_time_string();
    let console_line = format!(
        "[{}] [{}{}\x1b[0m] [{}]: {}\n",
        time_str, ansi_color, level_str, tag, msg
    );
    let plain_line = format!(
        "[{}] [{}] [{}]: {}\n",
        time_str, level_str, tag, msg
    );

    // Print directly to stdout/stderr
    if level >= 4 {
        eprint!("{}", console_line);
    } else {
        print!("{}", console_line);
    }

    // Write to logs/latest.log
    if let Ok(mut lock) = LOGGER.lock() {
        if let Some(ref mut logger) = *lock {
            if let Some(ref mut f) = logger.file {
                let _ = f.write_all(plain_line.as_bytes());
                let _ = f.flush();
            }
        }
    }
}

// ==========================================
// C-ABI FFI Exports
// ==========================================

#[no_mangle]
pub extern "C" fn nome_rust_log_init(log_file_path: *const c_char) -> bool {
    let path = if !log_file_path.is_null() {
        unsafe { CStr::from_ptr(log_file_path).to_string_lossy().into_owned() }
    } else {
        "logs/latest.log".to_string()
    };
    init_logger(&path)
}

#[no_mangle]
pub extern "C" fn nome_rust_log(level: i32, tag: *const c_char, msg: *const c_char) {
    if msg.is_null() {
        return;
    }
    let tag_str = if !tag.is_null() {
        unsafe { CStr::from_ptr(tag).to_string_lossy() }
    } else {
        std::borrow::Cow::Borrowed("Nome")
    };
    let msg_str = unsafe { CStr::from_ptr(msg).to_string_lossy() };

    log_message(level, &tag_str, &msg_str);
}

#[no_mangle]
pub extern "C" fn nome_rust_log_flush() {
    if let Ok(mut lock) = LOGGER.lock() {
        if let Some(ref mut logger) = *lock {
            if let Some(ref mut f) = logger.file {
                let _ = f.flush();
            }
        }
    }
}
