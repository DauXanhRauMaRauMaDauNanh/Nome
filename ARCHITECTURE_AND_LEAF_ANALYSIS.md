# NOME - KIẾN TRÚC MINECRAFT SERVER 100% NATIVE ENGINE
## (Lõi hoàn toàn trong C, C++, Rust + Bootstrap & Plugin Loader bằng Java)

---

## 1. Phân Tích & Tích Hợp `mojang_26.3.jar`, `leaf.jar`, và `paper.jar`

Trong thư mục dự án hiện có đầy đủ 3 bản tham chiếu:
- `mojang_26.3.jar`: Server chính thức từ Mojang (chứa toàn bộ `data/minecraft/`, registry, dimension, biomes, recipes).
- `paper.jar`: Server Paperclip (vá Vanilla bằng bsdiff, hỗ trợ Paper API).
- `leaf.jar`: Server fork hiệu năng cao nhất (SparklyPaper multithreading, Async Mob Pathfinding, Velocity-Native libdeflate, Pufferfish SIMD).

### Cơ chế hoạt động của Nome đối với `mojang_26.3.jar`:
Thay vì chạy máy ảo Java để thực thi mã nguồn bytecode nặng nề của Mojang, **Nome thiết kế lại 100% server core trong C, C++, và Rust**:
- Toàn bộ registry data, block definitions, và packet state machine của phiên bản 26.3 (Protocol 777) được xử lý trực tiếp bởi Native Engine.
- Java **KHÔNG tham gia vào vòng lặp tick server, KHÔNG quản lý bộ nhớ thế giới, và KHÔNG xử lý gói tin mạng**.

---

## 2. Phân Chia Ranh Giới Kiến Trúc Rõ Ràng

```
+-----------------------------------------------------------------------------------------+
|                                    NOME ARCHITECTURE                                    |
+-----------------------------------------------------------------------------------------+
|                                                                                         |
|   [ PHẦN 1: JAVA RUNTIME - CHỈ DÙNG CHO BOOTSTRAP & LOAD PLUGIN ]                       |
|                                                                                         |
|   1. Nome Bootstrap (Nome.jar):                                                         |
|      - Kiểm tra kiến trúc CPU & Hệ điều hành (Windows, Linux, macOS, x86_64, aarch64).  |
|      - Tự động trích xuất / cài đặt và nạp thư viện Native (bin/native/nome_core.dll).  |
|      - Cấu hình quyền Project Panama FFM (--enable-native-access=ALL-UNNAMED).          |
|                                                                                         |
|   2. Bukkit / Paper Plugin Container (org.bukkit.*):                                    |
|      - Cung cấp đầy đủ bề mặt API: Bukkit, Server, World, Player, Block, Material.      |
|      - SimplePluginManager: Quét /plugins/*.jar, nạp plugin.yml, khởi chạy JavaPlugin.  |
|      - SimpleScheduler: Điều phối task hẹn giờ của plugin.                              |
|      - Event Bus: Chuyển tiếp sự kiện từ Native sang @EventHandler của Plugin.           |
|                                                                                         |
+---------------------------------------------+-------------------------------------------+
                                              |
                                              | Project Panama FFM (Zero Overhead)
                                              | Con trỏ Native Memory Handles
                                              v
+-----------------------------------------------------------------------------------------+
|   [ PHẦN 2: NATIVE CORE - CHẠY 100% TRONG C, C++ VÀ RUST ]                              |
|                                                                                         |
|   1. Tầng C (nome-c-net):                                                               |
|      - Chạy TCP Server trực tiếp trên cổng 25565 bằng Winsock IOCP / Linux io_uring.    |
|      - Giải mã gói tin Minecraft Protocol 777 (Handshake, Status Ping/Pong, Login, Play)|
|      - Bộ giải mã SIMD VarInt / VarLong siêu tốc.                                       |
|      - Memory Arena Buffer: Quản lý gói tin không cần cấp phát qua Java GC.             |
|                                                                                         |
|   2. Tầng C++ (nome-cpp-physics):                                                       |
|      - Tính toán va chạm Voxel 3D (AABB Collision Detection) vector hóa CPU.            |
|      - Starlight-Style Bitmask BFS Lighting Engine (Block Light & Sky Light).           |
|      - Voxel Raycasting DDA Engine cho tương tác khối.                                  |
|                                                                                         |
|   3. Tầng Rust (nome-rust-core):                                                        |
|      - Safe Multithreading: Quản lý đa luồng an toàn tuyệt đối.                         |
|      - Lockless ECS (Entity Component System) chia luồng tick Entity song song.         |
|      - Chunk Storage 16x16x384 (-64 -> 319) với Palette nén và định dạng Linear zstd.  |
|      - Precision 20 TPS Tick Loop độ chính xác nano-giây.                               |
|                                                                                         |
+-----------------------------------------------------------------------------------------+
```

---

## 3. Hệ Thống Bukkit API Hoàn Chỉnh Trong `Nome`

Dự án cung cấp cây thư mục `org.bukkit.*` chuẩn mực để bất kỳ plugin Java thông thường nào cũng có thể chạy ngay lập tức:

* `org.bukkit.Bukkit`: Singleton tĩnh điều khiển server (`getServer()`, `broadcastMessage()`, `getPlayer()`, v.v.).
* `org.bukkit.Server`: Interface server hoàn chỉnh (`getOnlinePlayers()`, `getPluginManager()`, `getScheduler()`, `getWorlds()`, `getTPS()`, `getMSPT()`).
* `org.bukkit.Material`: Toàn bộ danh mục vật phẩm và khối (AIR, STONE, DIRT, DIAMOND_BLOCK, NETHERITE_BLOCK, ...).
* `org.bukkit.Location` & `org.bukkit.World`: Không gian 3D, tính toán khoảng cách, khối, người chơi.
* `org.bukkit.block.Block`: Khối trong thế giới, ánh xạ trực tiếp đến địa chỉ bộ nhớ Native C/C++.
* `org.bukkit.entity.Player`: Thực thể người chơi (`sendMessage`, `teleport`, `getHealth`, `kickPlayer`, `getAddress`).
* `org.bukkit.plugin.Plugin` & `org.bukkit.plugin.java.JavaPlugin`: Lớp nền tảng cho plugin.
* `org.bukkit.plugin.SimplePluginManager`: Tự động nạp các `.jar` từ `plugins/`, phân giải `plugin.yml`, gọi `onLoad()` và `onEnable()`.
* `org.bukkit.scheduler.SimpleScheduler`: Hỗ trợ đầy đủ `runTask()`, `runTaskLater()`, `runTaskTimer()` đồng bộ và bất đồng bộ.
* `org.bukkit.event.*`: Cây sự kiện đầy đủ:
  - `PlayerJoinEvent`, `PlayerQuitEvent`, `PlayerMoveEvent`, `AsyncPlayerChatEvent`.
  - `BlockBreakEvent`, `BlockPlaceEvent`.
  - Hỗ trợ đầy đủ `@EventHandler(priority = ..., ignoreCancelled = ...)`.

---

## 4. Hướng Dẫn Biên Dịch & Chạy Thử Nghiệm

### Bước 1: Biên dịch toàn bộ
Chạy script PowerShell tự động trong thư mục:
```powershell
.\build.ps1
```
Script sẽ tự động:
1. Biên dịch toàn bộ mã C, C++, và Rust FFI thành `bin/native/nome_core.dll` hoàn toàn khép kín (self-contained, static-linked, không phụ thuộc DLL ngoài).
2. Biên dịch mã nguồn Java Bootstrap và `org.bukkit.*` API surface thành `Nome.jar`.
3. Biên dịch plugin mẫu [`plugins/ExamplePlugin.jar`](file:///c:/Users/thait/Downloads/Nome/plugins/ExamplePlugin.jar) kế thừa chuẩn `org.bukkit.plugin.java.JavaPlugin`.

### Bước 2: Chạy Server
Chạy qua batch file:
```cmd
run.bat
```
Hoặc lệnh trực tiếp:
```cmd
java -XX:+UseZGC -XX:+ZGenerational --enable-native-access=ALL-UNNAMED -jar Nome.jar
```

### Kết quả chạy thực tế:
- Server Native C mở cổng TCP 25565 trong chế độ Non-blocking IOCP.
- Lõi Native C/C++/Rust khởi động vòng lặp 20 TPS với Zero-GC.
- Plugin Java được nạp đầy đủ qua chuẩn `org.bukkit.*`, bắt sự kiện `PlayerJoinEvent` và `BlockBreakEvent` mượt mà!
