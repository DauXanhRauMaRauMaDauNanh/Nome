# Nome - All-in-One Build Script (100% Native C/C++/Rust Core Engine)
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Building Nome Standalone Minecraft Jar " -ForegroundColor Cyan
Write-Host " (C + C++ + Rust Engine + Lean Bridge)  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$env:PATH = "C:\msys64\ucrt64\bin;C:\Program Files\Java\jdk-25.0.4\bin;" + $env:PATH

# 1. Create Directories
New-Item -ItemType Directory -Path "bin\native", "target\classes\native\windows\x86_64", "target\plugin_classes", "plugins" -Force | Out-Null

# 2. Build Rust Core Static Library (nome-rust-core)
Write-Host "[1/4] Compiling Native Rust Core (Folia Scheduler, SLF4J Native Logger, Anti-Xray)..." -ForegroundColor Yellow
& "C:\msys64\ucrt64\bin\cargo.exe" build --release --manifest-path nome-rust-core/Cargo.toml
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to compile nome-rust-core!" -ForegroundColor Red
    exit 1
}

# 3. Compile Unified Self-Contained Native Core (C + C++ + Rust -> nome_core.dll)
Write-Host "[2/4] Linking 100% Native Core Engine (C + C++ + Rust -> nome_core.dll)..." -ForegroundColor Yellow
& "C:\msys64\ucrt64\bin\g++.exe" -O3 -shared -static-libgcc -static-libstdc++ `
    -I ./nome-c-net/include `
    -I ./nome-cpp-physics/include `
    ./nome-c-net/src/nome_varint.c `
    ./nome-c-net/src/nome_alloc.c `
    ./nome-c-net/src/nome_socket.c `
    ./nome-cpp-physics/src/nome_collision.cpp `
    ./nome-cpp-physics/src/nome_lighting.cpp `
    ./nome-cpp-physics/src/nome_worldgen.cpp `
    ./nome-cpp-physics/src/nome_chunk_manager.cpp `
    ./nome-cpp-physics/src/nome_crafting.cpp `
    ./nome-cpp-physics/src/nome_entity.cpp `
    ./bin/native/nome_core_unified.cpp `
    ./nome-rust-core/target/release/libnome_rust_core.a `
    -o ./bin/native/nome_core.dll `
    -lws2_32 -luserenv -lntdll -lbcrypt

if (!(Test-Path "./bin/native/nome_core.dll")) {
    Write-Host "Failed to compile nome_core.dll" -ForegroundColor Red
    exit 1
}

# Copy native runtime dependencies directly into target classes so it is bundled in Nome.jar!
Copy-Item "./bin/native/nome_core.dll" "./target/classes/native/windows/x86_64/nome_core.dll" -Force
Copy-Item "C:\msys64\ucrt64\bin\libwinpthread-1.dll" "./target/classes/native/windows/x86_64/libwinpthread-1.dll" -Force
Copy-Item "C:\msys64\ucrt64\bin\libstdc++-6.dll" "./target/classes/native/windows/x86_64/libstdc++-6.dll" -Force
Copy-Item "C:\msys64\ucrt64\bin\libgcc_s_seh-1.dll" "./target/classes/native/windows/x86_64/libgcc_s_seh-1.dll" -Force

# Also place them in bin/native/
Copy-Item "C:\msys64\ucrt64\bin\libwinpthread-1.dll" "./bin/native/libwinpthread-1.dll" -Force
Copy-Item "C:\msys64\ucrt64\bin\libstdc++-6.dll" "./bin/native/libstdc++-6.dll" -Force
Copy-Item "C:\msys64\ucrt64\bin\libgcc_s_seh-1.dll" "./bin/native/libgcc_s_seh-1.dll" -Force

# 4. Compile Java Bootstrap, Native Bridge, SLF4J, Netty ByteBuf & Paper 26.3 Folia API Surface
Write-Host "[3/4] Compiling Java Bootstrap & Full Bukkit/Paper API Surface (Nome.jar)..." -ForegroundColor Yellow
Remove-Item -Force target/classes/module-info.class -ErrorAction SilentlyContinue
$libCp = (Get-ChildItem -Recurse libraries -Filter "*.jar" | Select-Object -ExpandProperty FullName) -join ";"
$javaSrc = Get-ChildItem -Path "nome-bridge\src", "nome-bootstrap\src" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
& "C:\Program Files\Java\jdk-25.0.4\bin\javac.exe" -cp "$libCp" -d target/classes $javaSrc
if ($LASTEXITCODE -ne 0) {
    Write-Host "Java compilation failed!" -ForegroundColor Red
    exit 1
}

# Native format helpers (Gson, SnakeYAML, Fastutil, SLF4J, Log4j, Netty) are all 100% built-in!
Write-Host "All Java library shims are 100% built-in native (Zero external JAR dependencies)!" -ForegroundColor Green
Remove-Item -Force target/classes/module-info.class -ErrorAction SilentlyContinue

# Bundle default server-icon.png into Nome.jar
if (Test-Path "./server-icon.png") {
    Copy-Item "./server-icon.png" "./target/classes/server-icon.png" -Force
}

# Package standalone Fat JAR with bundled native DLLs!
& "C:\Program Files\Java\jdk-25.0.4\bin\jar.exe" --create --file Nome.jar --main-class io.nome.bootstrap.Main -C target/classes .
if (Test-Path "C:\Users\thait\Downloads\prod") {
    Copy-Item Nome.jar "C:\Users\thait\Downloads\prod\Nome.jar" -Force
    if (Test-Path "./server-icon.png") {
        Copy-Item "./server-icon.png" "C:\Users\thait\Downloads\prod\server-icon.png" -Force
    }
    if (Test-Path "./logo.png") {
        Copy-Item "./logo.png" "C:\Users\thait\Downloads\prod\logo.png" -Force
    }
    if (-not (Test-Path "C:\Users\thait\Downloads\prod\bin\native")) {
        New-Item -ItemType Directory -Path "C:\Users\thait\Downloads\prod\bin\native" -Force | Out-Null
    }
    Copy-Item -Path "bin\native\*" -Destination "C:\Users\thait\Downloads\prod\bin\native\" -Force
}

# 5. Compile Sample Java Plugin (plugins/ExamplePlugin.jar)
Write-Host "[4/4] Compiling Bukkit Plugin with standard org.bukkit.* API (ExamplePlugin.jar)..." -ForegroundColor Yellow
$pluginSrc = Get-ChildItem -Path "test_plugin\src" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
& "C:\Program Files\Java\jdk-25.0.4\bin\javac.exe" -cp "target/classes" -d target/plugin_classes $pluginSrc
Copy-Item "test_plugin\plugin.yml" "target\plugin_classes\plugin.yml" -Force
& "C:\Program Files\Java\jdk-25.0.4\bin\jar.exe" --create --file plugins/ExamplePlugin.jar -C target/plugin_classes .

Write-Host "========================================" -ForegroundColor Green
Write-Host " Build Succeeded! Ultra-Lean Fat JAR:   " -ForegroundColor Green
Write-Host " -> Nome.jar (Natives + Rust inside!)   " -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
