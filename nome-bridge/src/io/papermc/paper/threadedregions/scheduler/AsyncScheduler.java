package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.plugin.Plugin;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface AsyncScheduler {

    ScheduledTask runNow(Plugin plugin, Consumer<ScheduledTask> task);

    ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task, long delay, TimeUnit unit);

    ScheduledTask runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task, long initialDelay, long period, TimeUnit unit);

    void cancelTasks(Plugin plugin);
}
