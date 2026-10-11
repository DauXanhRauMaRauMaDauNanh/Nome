# Nome Server - Ultra-Lean Standalone Launcher (PowerShell)
param(
    [switch]$TestBot,
    [switch]$NoGui,
    [int]$Port = 25565
)

$env:PATH = "C:\Program Files\Java\jdk-25.0.4\bin;C:\msys64\ucrt64\bin;" + $env:PATH

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [Nome] Launching Standalone Engine (16MB-32MB JVM Limit) " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$javaArgs = @(
    "-Xms16m",
    "-Xmx32m",
    "-XX:+UseSerialGC",
    "-XX:MinHeapFreeRatio=10",
    "-XX:MaxHeapFreeRatio=20",
    "--enable-native-access=ALL-UNNAMED",
    "-jar", "Nome.jar",
    "-P", $Port
)

if ($TestBot) {
    Write-Host "[*] Starting server in background for Bot Gameplay Verification..." -ForegroundColor Yellow
    $proc = Start-Process -FilePath "java.exe" -ArgumentList $javaArgs -PassThru -NoNewWindow
    Start-Sleep -Seconds 3
    node test_bot/real_gameplay_bot.js
    Stop-Process -Id $proc.Id -Force
} else {
    & "java.exe" @javaArgs
}
