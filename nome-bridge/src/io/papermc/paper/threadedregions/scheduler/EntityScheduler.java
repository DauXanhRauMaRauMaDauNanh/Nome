package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.plugin.Plugin;
import java.util.function.Consumer;

public interface EntityScheduler {

    ScheduledTask run(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired);

    ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired, long delayTicks);

    ScheduledTask runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired, long initialDelayTicks, long periodTicks);

    boolean execute(Plugin plugin, Runnable run, Runnable retired, long delay);
}
