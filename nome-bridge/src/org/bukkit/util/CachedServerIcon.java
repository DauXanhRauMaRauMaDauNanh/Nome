package org.bukkit.util;

/**
 * Represents a cached server-icon.
 */
public interface CachedServerIcon {
    /**
     * Gets the base64 encoded png data URI string of this server icon.
     * @return base64 encoded data URI
     */
    String getData();
}
