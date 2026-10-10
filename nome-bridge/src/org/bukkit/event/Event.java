package org.bukkit.event;

public abstract class Event {
    private final String name;
    private final boolean async;

    public Event() {
        this(false);
    }

    public Event(boolean isAsync) {
        this.async = isAsync;
        this.name = getClass().getSimpleName();
    }

    public String getEventName() {
        return name;
    }

    public boolean isAsynchronous() {
        return async;
    }

    public HandlerList getHandlers() {
        return null;
    }
}
