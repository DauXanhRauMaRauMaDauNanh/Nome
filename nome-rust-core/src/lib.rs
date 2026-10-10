pub mod world;
pub mod ecs;
pub mod net;
pub mod tick;
pub mod bridge;

pub use bridge::ffi::*;

#[no_mangle]
pub extern "C" fn nome_rust_version() -> *const std::os::raw::c_char {
    static VERSION: &[u8] = b"Nome-Rust-Engine v0.1.0-alpha (Protocol 777 / MC 26.3)\0";
    VERSION.as_ptr() as *const std::os::raw::c_char
}
