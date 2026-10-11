pub mod world;
pub mod ecs;
pub mod net;
pub mod tick;
pub mod bridge;
pub mod logging;

pub use bridge::ffi::*;
pub use logging::*;
pub use world::antixray::*;
pub use world::ticket_manager::*;
pub use tick::region_scheduler::*;

#[no_mangle]
pub extern "C" fn nome_rust_core_version() -> *const std::os::raw::c_char {
    static VERSION: &[u8] = b"Nome-Rust-Core v0.5.0 (Protocol 777 / Paper 26.3 Folia Native)\0";
    VERSION.as_ptr() as *const std::os::raw::c_char
}
