package io.papermc.paper.threadedregions.scheduler;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import java.util.function.Consumer;

public interface RegionScheduler {

    ScheduledTask run(Plugin plugin, World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task);

    ScheduledTask runDelayed(Plugin plugin, World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks);

    ScheduledTask runAtFixedRate(Plugin plugin, World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks);

    void cancelTasks(Plugin plugin);
}
