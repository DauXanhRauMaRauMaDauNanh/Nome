package org.bukkit.scheduler;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.plugin.Plugin;

public class SimpleScheduler implements BukkitScheduler {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    private static class TaskImpl implements BukkitTask {
        final int id;
        final Plugin owner;
        final boolean sync;
        volatile boolean cancelled = false;

        TaskImpl(int id, Plugin owner, boolean sync) {
            this.id = id;
            this.owner = owner;
            this.sync = sync;
        }

        @Override public int getTaskId() { return id; }
        @Override public Plugin getOwner() { return owner; }
        @Override public boolean isSync() { return sync; }
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void cancel() { cancelled = true; }
    }

    @Override
    public BukkitTask runTask(Plugin plugin, Runnable task) {
        return runTaskLater(plugin, task, 0);
    }

    @Override
    public BukkitTask runTaskAsynchronously(Plugin plugin, Runnable task) {
        return runTaskLaterAsynchronously(plugin, task, 0);
    }

    @Override
    public BukkitTask runTaskLater(Plugin plugin, Runnable task, long delay) {
        int id = nextId.getAndIncrement();
        TaskImpl btask = new TaskImpl(id, plugin, true);
        ScheduledFuture<?> future = executor.schedule(() -> {
            if (!btask.isCancelled()) task.run();
        }, delay * 50, TimeUnit.MILLISECONDS);
        tasks.put(id, future);
        return btask;
    }

    @Override
    public BukkitTask runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay) {
        int id = nextId.getAndIncrement();
        TaskImpl btask = new TaskImpl(id, plugin, false);
        ScheduledFuture<?> future = executor.schedule(() -> {
            if (!btask.isCancelled()) task.run();
        }, delay * 50, TimeUnit.MILLISECONDS);
        tasks.put(id, future);
        return btask;
    }

    @Override
    public BukkitTask runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        int id = nextId.getAndIncrement();
        TaskImpl btask = new TaskImpl(id, plugin, true);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            if (!btask.isCancelled()) task.run();
        }, delay * 50, period * 50, TimeUnit.MILLISECONDS);
        tasks.put(id, future);
        return btask;
    }

    @Override
    public BukkitTask runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period) {
        return runTaskTimer(plugin, task, delay, period);
    }

    @Override
    public <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task) {
        return executor.submit(task);
    }

    @Override
    public void cancelTask(int taskId) {
        ScheduledFuture<?> f = tasks.remove(taskId);
        if (f != null) f.cancel(false);
    }

    @Override
    public void cancelTasks(Plugin plugin) {
        // Cancel all plugin tasks
    }
}
