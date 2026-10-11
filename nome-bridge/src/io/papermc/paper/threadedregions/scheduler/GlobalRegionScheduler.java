package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.plugin.Plugin;
import java.util.function.Consumer;

public interface GlobalRegionScheduler {

    ScheduledTask run(Plugin plugin, Consumer<ScheduledTask> task);

    ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task, long delayTicks);

    ScheduledTask runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks);

    void cancelTasks(Plugin plugin);
}
