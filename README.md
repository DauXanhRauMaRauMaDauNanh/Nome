# 🍃 Nome - 100% Native Minecraft Server Engine

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-26.3%20%2F%201.21.x-brightgreen.svg)](https://minecraft.net)
[![Native Core](https://img.shields.io/badge/Native%20Engine-C%20%2B%20C%2B%2B%20%2B%20Rust-blue.svg)]()
[![Java Compatibility](https://img.shields.io/badge/Plugin%20API-Paper%20%2F%20Bukkit%20100%25-orange.svg)]()

> **Nome** is a groundbreaking, ultra-high-performance Minecraft server architecture designed to eliminate the bottlenecks of traditional JVM servers (Garbage Collection pauses, memory boxing, cache locality loss) by running the **entire game server 100% in Native C, C++, and Rust**, while preserving **100% compatibility for Java Plugins (Paper/Bukkit API)** via modern **Project Panama Foreign Function & Memory (FFM)**.

---

## 🌟 Key Features

* **⚡ 100% Native Core Engine (C + C++ + Rust)**:
  * **C Layer (`nome-c-net`)**: Ultra-low latency asynchronous TCP networking with **Windows IOCP** and **Linux `io_uring`**, SIMD VarInt/VarLong codecs, and zero-allocation packet arena buffers (Zero GC overhead).
  * **C++ Layer (`nome-cpp-physics`)**: SIMD AABB 3D voxel collision detection, fast raymarching, and a full C++ port of the **Starlight Bitmask BFS 15-level Light Engine**.
  * **Rust Layer (`nome-rust-core`)**: Safe multithreaded ECS (Entity Component System), Chunk palette management (16x16x384), Linear Region zstd storage format, and nanosecond-accurate 20 TPS tick scheduler.
* **📦 Single-File Fat JAR Deployment (`Nome.jar`)**:
  * Native libraries (`.dll`, `.so`) are packaged **directly inside `Nome.jar`**.
  * Requires **only 1 file** to run anywhere! The bootstrapper automatically detects CPU/OS and unpacks the appropriate native engine at startup.
* **🌐 Universal Hosting & Exotic Flags Compatibility**:
  * Seamlessly runs on hosting panels (Pterodactyl, Multicraft, PebbleHost, Bisect, Apex, etc.).
  * Gracefully filters and skips exotic/unknown hosting flags (`nogui`, `-o`, `--eraseCache`, `--safeMode`, `-P`, etc.).
  * Automatically generates `eula.txt` (`eula=true`) and `server.properties`.
* **🔌 100% Full Java Bukkit / Paper Plugin Support**:
  * Implements the complete `org.bukkit.*` and `io.papermc.paper.*` API surface.
  * Thin wrapper handles wrap native pointers with zero object recreation.
  * Bidirectional Event Bridge dispatches native engine events to `@EventHandler` listeners with full priority order and cancellation propagation.
* **🤖 Verified with Mineflayer & Protocol Testers**:
  * Tested and verified with automated Mineflayer bot sessions (Handshake, Status Ping, Login, and Play packet flow).

---

## 🚀 Quick Start

### Running the Server
All native components are bundled inside `Nome.jar`. You only need Java 22+ (Java 25 LTS recommended):

```bash
java -XX:+UseZGC --enable-native-access=ALL-UNNAMED -jar Nome.jar
```

Or on Windows:
```cmd
run.bat
```

### Building from Source
Run the 1-click build script to compile the C, C++, Rust FFI, and Java modules:

```powershell
.\build.ps1
```

---

## 🏗️ Architecture Overview

```
                  +---------------------------------------------------+
                  |                 Nome Bootstrap                    |
                  |                   (Nome.jar)                      |
                  +-------------------------+-------------------------+
                                            | Self-Extracts Native Libs
                                            v
+-----------------------------------------------------------------------------------------+
|                                    NOME RUNTIME                                         |
|                                                                                         |
|   +-------------------------------------+     +-------------------------------------+   |
|   |         JAVA PLUGIN ENGINE          |     |          NATIVE CORE ENGINE         |   |
|   |          (Bukkit/Paper API)         |     |           (C + C++ + Rust)          |   |
|   |                                     |     |                                     |   |
|   | - PluginLoader (Loads /plugins/*.jar|     | [Tầng C: nome-c-net]                |   |
|   | - NomeEventBus (@EventHandler)      |     | - Windows IOCP / Linux io_uring     |   |
|   | - Wrapped Entities & Worlds         |     | - SIMD VarInt/VarLong Codecs        |   |
|   | - CommandMap & Scheduler            |     | - Lockless Packet Arena Allocator   |   |
|   |                                     |     |                                     |   |
|   |   (Essentials, WorldEdit, Vault...) |     | [Tầng C++: nome-cpp-physics]        |   |
|   +------------------+------------------+     | - SIMD Voxel Collision              |   |
|                      |                        | - Starlight BFS Light Engine        |   |
|                      | Project Panama FFM     | - Fast 3D Math & AABB               |   |
|                      | (Zero-overhead FFI)    |                                     |   |
|                      |                        | [Tầng Rust: nome-rust-core]         |   |
|                      +----------------------->| - Lockless ECS (Multi-thread tick)  |   |
|                                               | - Chunk Palette 16x16x384           |   |
|                                               | - Linear Region / zstd Storage      |   |
|                                               | - Protocol 777 Packet State Machine |   |
+-----------------------------------------------+-----------------------------------------+
```

---

## 🧪 Testing with Mineflayer

To test connecting an automated bot against Nome:

```bash
node test_bot/bot_test.js
```

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).
