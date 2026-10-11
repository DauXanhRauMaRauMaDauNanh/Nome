package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.plugin.Plugin;

public interface ScheduledTask {

    Plugin getPlugin();

    boolean cancel();

    boolean isCancelled();

    ExecutionState getExecutionState();

    enum ExecutionState {
        IDLE,
        RUNNING,
        FINISHED,
        CANCELLED
    }
}
