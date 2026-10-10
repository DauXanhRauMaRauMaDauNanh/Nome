package org.bukkit.event.player;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class AsyncPlayerChatEvent extends PlayerEvent implements Cancellable {
    private boolean cancelled = false;
    private String message;
    private String format = "<%1$s> %2$s";
    private final Set<Player> recipients = new HashSet<>();

    public AsyncPlayerChatEvent(boolean async, Player who, String message, Set<Player> recipients) {
        super(who, async);
        this.message = message;
        if (recipients != null) {
            this.recipients.addAll(recipients);
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Set<Player> getRecipients() {
        return recipients;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
