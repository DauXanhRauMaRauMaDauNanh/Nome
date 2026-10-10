package org.bukkit.scheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.bukkit.plugin.Plugin;

public interface BukkitScheduler {
    BukkitTask runTask(Plugin plugin, Runnable task);
    BukkitTask runTaskAsynchronously(Plugin plugin, Runnable task);
    BukkitTask runTaskLater(Plugin plugin, Runnable task, long delay);
    BukkitTask runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay);
    BukkitTask runTaskTimer(Plugin plugin, Runnable task, long delay, long period);
    BukkitTask runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period);
    <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task);
    void cancelTask(int taskId);
    void cancelTasks(Plugin plugin);
}
