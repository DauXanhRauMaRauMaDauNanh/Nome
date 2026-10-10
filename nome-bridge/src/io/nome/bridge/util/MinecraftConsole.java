package io.nome.bridge.util;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

/**
 * Authentic Minecraft / Paper / Leaf Console Logger and Formatter.
 * Formats all output as:
 *   [HH:mm:ss INFO]: <message>
 * with vibrant ANSI color highlights and full Minecraft '§' color translation.
 */
public class MinecraftConsole {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Logger MINECRAFT_LOGGER = Logger.getLogger("Minecraft");
    private static boolean initialized = false;

    // ANSI Escape Sequences
    private static final String RESET = "\u001b[0m";
    private static final String BOLD = "\u001b[1m";
    private static final String TIME_COLOR = "\u001b[90m";   // Dark gray
    private static final String INFO_COLOR = "\u001b[32m";   // Green
    private static final String WARN_COLOR = "\u001b[33m";   // Yellow
    private static final String ERROR_COLOR = "\u001b[31m";  // Bright Red

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        try {
            // Ensure UTF-8 output stream
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Throwable ignored) {}

        // Configure Root Logger and Minecraft Logger
        Logger rootLogger = Logger.getLogger("");
        for (Handler h : rootLogger.getHandlers()) {
            rootLogger.removeHandler(h);
        }

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                String time = TIME_FORMATTER.format(LocalTime.now());
                String levelTag;
                if (record.getLevel() == Level.WARNING) {
                    levelTag = WARN_COLOR + "WARN" + RESET;
                } else if (record.getLevel() == Level.SEVERE) {
                    levelTag = ERROR_COLOR + "ERROR" + RESET;
                } else {
                    levelTag = INFO_COLOR + "INFO" + RESET;
                }

                String formattedMsg = formatMessage(record);
                formattedMsg = translateColors(formattedMsg);

                StringBuilder sb = new StringBuilder();
                sb.append("[").append(time).append(" ").append(levelTag).append("]: ");
                sb.append(formattedMsg).append("\n");

                if (record.getThrown() != null) {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                    record.getThrown().printStackTrace(pw);
                    sb.append(sw);
                }

                return sb.toString();
            }
        });

        rootLogger.addHandler(handler);
        rootLogger.setLevel(Level.INFO);
    }

    public static void info(String message) {
        MINECRAFT_LOGGER.info(message);
    }

    public static void warn(String message) {
        MINECRAFT_LOGGER.warning(message);
    }

    public static void error(String message) {
        MINECRAFT_LOGGER.severe(message);
    }

    /**
     * Translates Minecraft section sign (§) color codes into terminal ANSI escape codes.
     */
    public static String translateColors(String text) {
        if (text == null || text.isEmpty()) return "";
        if (!text.contains("§")) return text;

        return text
            .replace("§0", "\u001b[30m") // Black
            .replace("§1", "\u001b[34m") // Dark Blue
            .replace("§2", "\u001b[32m") // Dark Green
            .replace("§3", "\u001b[36m") // Dark Aqua
            .replace("§4", "\u001b[31m") // Dark Red
            .replace("§5", "\u001b[35m") // Dark Purple
            .replace("§6", "\u001b[33m") // Gold
            .replace("§7", "\u001b[37m") // Gray
            .replace("§8", "\u001b[90m") // Dark Gray
            .replace("§9", "\u001b[94m") // Blue
            .replace("§a", "\u001b[92m") // Bright Green
            .replace("§b", "\u001b[96m") // Aqua
            .replace("§c", "\u001b[91m") // Light Red
            .replace("§d", "\u001b[95m") // Light Purple
            .replace("§e", "\u001b[93m") // Yellow
            .replace("§f", "\u001b[97m") // White
            .replace("§l", "\u001b[1m")  // Bold
            .replace("§o", "\u001b[3m")  // Italic
            .replace("§n", "\u001b[4m")  // Underline
            .replace("§r", RESET)        // Reset
            + RESET;
    }

    /**
     * Strips all Minecraft section sign (§) color codes.
     */
    public static String stripColors(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
