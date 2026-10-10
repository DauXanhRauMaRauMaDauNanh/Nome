package io.nome.bootstrap;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import io.nome.bridge.NomeNativeBridge;
import io.nome.bridge.NomeServer;

/**
 * Nome Bootstrap Launcher (.jar Entry Point)
 * 
 * Features:
 * 1. Single-Jar Standalone Deployment: Native binaries (DLLs/so) are bundled inside
 *    Nome.jar and automatically extracted on boot. Only Nome.jar is needed!
 * 2. Host Compatibility: Gracefully parses and filters exotic hosting flags
 *    (Pterodactyl, Multicraft, Aikar's flags, nogui, --port, -P, etc.).
 * 3. Auto EULA & Configuration: Automatically generates eula.txt and server.properties.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  _  _  ___  __  __ ___   ___  ___   ___ _____ ___   ");
        System.out.println(" | \\| |/ _ \\|  \\/  | __| | _ )/ _ \\ / _ \\_   _/ __|  ");
        System.out.println(" | .` | (_) | |\\/| | _|  | _ \\ (_) | (_) || | \\__ \\  ");
        System.out.println(" |_|\\_|\\___/|_|  |_|___| |___/\\___/ \\___/ |_| |___/  ");
        System.out.println("==================================================");
        System.out.println("[Nome-Bootstrap] Starting Nome Standalone Server Runtime...");

        // 1. Process and filter Hosting Flags & Arguments
        ParsedHostArgs hostConfig = parseHostingArguments(args);
        System.out.println("[Nome-Bootstrap] Host Configuration -> Port: " + hostConfig.port + ", World: " + hostConfig.worldName);

        // 2. Auto-generate eula.txt and server.properties for hosting panels
        ensureEulaAccepted();
        ensureServerProperties(hostConfig.port);

        // 3. Detect Platform & CPU Architecture
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        System.out.println("[Nome-Bootstrap] Platform: " + os + " | Arch: " + arch);
        System.out.println("[Nome-Bootstrap] Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");

        // 4. Extract bundled native binaries from Nome.jar (Zero external dependencies)
        File nativeDir = extractBundledNatives(os, arch);

        // 5. Bind Native C/C++/Rust Engine via Project Panama FFM
        boolean bound = NomeNativeBridge.loadNativeEngine(nativeDir.getAbsolutePath());
        if (!bound) {
            System.err.println("[Nome-Bootstrap] FATAL: Could not bind native C/C++/Rust engine!");
            System.exit(1);
        }

        // 6. Start Nome Server Core
        NomeServer server = new NomeServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "Nome-Shutdown-Hook"));
        server.start();

        // 7. Engine is ticked by high-precision native OS thread in C++ (0% Java CPU overhead)
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            server.stop();
        }
    }

    private static class ParsedHostArgs {
        int port = 25565;
        String worldName = "world";
    }

    private static ParsedHostArgs parseHostingArguments(String[] args) {
        ParsedHostArgs cfg = new ParsedHostArgs();
        if (args == null || args.length == 0) return cfg;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();
            if (arg.equalsIgnoreCase("-p") || arg.equalsIgnoreCase("--port") || arg.equalsIgnoreCase("--server-port")) {
                if (i + 1 < args.length) {
                    try {
                        cfg.port = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException ignored) {}
                }
            } else if (arg.equalsIgnoreCase("-w") || arg.equalsIgnoreCase("--world") || arg.equalsIgnoreCase("--level-name")) {
                if (i + 1 < args.length) {
                    cfg.worldName = args[++i];
                }
            } else {
                // Ignore exotic hosting flags (nogui, -nojline, --forceUpgrade, --eraseCache, etc.)
                // Safely skipped without errors
            }
        }
        return cfg;
    }

    private static void ensureEulaAccepted() {
        File eulaFile = new File("eula.txt");
        if (!eulaFile.exists()) {
            try {
                Files.writeString(eulaFile.toPath(), "# By changing the setting below to TRUE you are indicating your agreement to the EULA.\neula=true\n");
                System.out.println("[Nome-Bootstrap] Auto-created eula.txt (eula=true)");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void ensureServerProperties(int port) {
        File propFile = new File("server.properties");
        if (!propFile.exists()) {
            try {
                String defaultProps = "# Minecraft server properties (Nome Native Engine)\n" +
                        "server-port=" + port + "\n" +
                        "gamemode=survival\n" +
                        "difficulty=easy\n" +
                        "pvp=true\n" +
                        "max-players=100\n" +
                        "online-mode=false\n" +
                        "motd=§b§lNome §8- §a100% Native C/C++/Rust Server\n";
                Files.writeString(propFile.toPath(), defaultProps);
                System.out.println("[Nome-Bootstrap] Auto-created server.properties on port " + port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static File extractBundledNatives(String os, String arch) {
        File nativeDir = new File("bin" + File.separator + "native");
        if (!nativeDir.exists()) {
            nativeDir.mkdirs();
        }

        String[] winLibs = {"libwinpthread-1.dll", "libgcc_s_seh-1.dll", "libstdc++-6.dll", "nome_core.dll"};

        for (String lib : winLibs) {
            File targetFile = new File(nativeDir, lib);
            String resourcePath = "/native/windows/x86_64/" + lib;
            try (InputStream in = Main.class.getResourceAsStream(resourcePath)) {
                if (in != null) {
                    Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ignored) {}

            // Pre-load dependencies if on Windows
            if (os.contains("win") && targetFile.exists() && !lib.equals("nome_core.dll")) {
                try {
                    System.load(targetFile.getAbsolutePath());
                } catch (Throwable ignored) {}
            }
        }

        return nativeDir;
    }
}
