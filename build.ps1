# Nome - All-in-One Build Script
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Building Nome Standalone Minecraft Jar " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$env:PATH = "C:\Program Files\Java\jdk-25.0.4\bin;C:\msys64\ucrt64\bin;" + $env:PATH

# 1. Create Directories
New-Item -ItemType Directory -Path "bin\native", "target\classes\native\windows\x86_64", "target\plugin_classes", "plugins" -Force | Out-Null

# 2. Compile Unified Self-Contained Native Core (nome_core.dll)
Write-Host "[1/4] Compiling 100% Native Core Engine (C + C++ + Rust FFI -> nome_core.dll)..." -ForegroundColor Yellow
g++ -O3 -shared -static-libgcc -static-libstdc++ `
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
    -o ./bin/native/nome_core.dll `
    -lws2_32

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

# 3. Compile Java Bootstrap & Full Bukkit/Paper API Surface
Write-Host "[2/4] Compiling Java Bootstrap & Full Bukkit/Paper API Surface (Nome.jar)..." -ForegroundColor Yellow
Remove-Item -Force target/classes/module-info.class -ErrorAction SilentlyContinue
$libCp = (Get-ChildItem -Recurse libraries -Filter "*.jar" | Select-Object -ExpandProperty FullName) -join ";"
$javaSrc = Get-ChildItem -Path "nome-bridge\src", "nome-bootstrap\src" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
javac -cp "$libCp" -d target/classes $javaSrc
if ($LASTEXITCODE -ne 0) {
    Write-Host "Java compilation failed!" -ForegroundColor Red
    exit 1
}

# Unpack standard server dependencies (Guava, Gson, Netty, JOML, Slf4j) for Bukkit plugin compatibility
Write-Host "Unpacking Guava, Gson, Netty & common runtime libs into target/classes..." -ForegroundColor Cyan
$unpackJars = @(
    "libraries/com/google/guava/guava/33.6.0-jre/guava-33.6.0-jre.jar",
    "libraries/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar",
    "libraries/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar",
    "libraries/io/netty/netty-common/4.2.16.Final/netty-common-4.2.16.Final.jar",
    "libraries/io/netty/netty-buffer/4.2.16.Final/netty-buffer-4.2.16.Final.jar",
    "libraries/io/netty/netty-transport/4.2.16.Final/netty-transport-4.2.16.Final.jar",
    "libraries/io/netty/netty-handler/4.2.16.Final/netty-handler-4.2.16.Final.jar",
    "libraries/io/netty/netty-codec-base/4.2.16.Final/netty-codec-base-4.2.16.Final.jar",
    "libraries/io/netty/netty-resolver/4.2.16.Final/netty-resolver-4.2.16.Final.jar",
    "libraries/org/apache/commons/commons-lang3/3.20.0/commons-lang3-3.20.0.jar",
    "libraries/commons-io/commons-io/2.20.0/commons-io-2.20.0.jar",
    "libraries/org/joml/joml/1.10.9/joml-1.10.9.jar",
    "libraries/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar"
)
foreach ($uj in $unpackJars) {
    if (Test-Path $uj) {
        & "C:\Program Files\Java\jdk-25.0.4\bin\jar.exe" -x -f $uj --dir target/classes
    }
}
Remove-Item -Force target/classes/module-info.class -ErrorAction SilentlyContinue

# Package standalone Fat JAR with bundled native DLLs!
& "C:\Program Files\Java\jdk-25.0.4\bin\jar.exe" --create --file Nome.jar --main-class io.nome.bootstrap.Main -C target/classes .
if (Test-Path "C:\Users\thait\Downloads\prod") {
    Copy-Item Nome.jar "C:\Users\thait\Downloads\prod\Nome.jar" -Force
    if (-not (Test-Path "C:\Users\thait\Downloads\prod\bin\native")) {
        New-Item -ItemType Directory -Path "C:\Users\thait\Downloads\prod\bin\native" -Force | Out-Null
    }
    Copy-Item -Path "bin\native\*" -Destination "C:\Users\thait\Downloads\prod\bin\native\" -Force
}

# 4. Compile Sample Java Plugin (plugins/ExamplePlugin.jar)
Write-Host "[3/4] Compiling Bukkit Plugin with standard org.bukkit.* API (ExamplePlugin.jar)..." -ForegroundColor Yellow
$pluginSrc = Get-ChildItem -Path "test_plugin\src" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
javac -cp "target/classes" -d target/plugin_classes $pluginSrc
Copy-Item "test_plugin\plugin.yml" "target\plugin_classes\plugin.yml" -Force
jar --create --file plugins/ExamplePlugin.jar -C target/plugin_classes .

Write-Host "========================================" -ForegroundColor Green
Write-Host " Build Succeeded! Standalone Fat JAR:   " -ForegroundColor Green
Write-Host " -> Nome.jar (Natives bundled inside!)  " -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
