package org.bukkit.scheduler;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.plugin.Plugin;

/**
 * Standard Bukkit / Paper Scheduler implementation for Nome Engine.
 * Synchronous tasks run strictly on the Server Main Thread during 20 TPS game ticks.
 * Asynchronous tasks run in a dedicated worker thread pool.
 */
public class SimpleScheduler implements BukkitScheduler {

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final AtomicLong currentTick = new AtomicLong(0);

    // Incoming synchronous tasks submitted from any thread
    private final ConcurrentLinkedQueue<TaskImpl> pendingSyncTasks = new ConcurrentLinkedQueue<>();

    // Active synchronous tasks evaluated each tick on Main Thread
    private final List<TaskImpl> activeSyncTasks = new ArrayList<>();

    // Map of all active tasks (sync & async) by ID for quick lookup and cancellation
    private final ConcurrentHashMap<Integer, TaskImpl> allTasks = new ConcurrentHashMap<>();

    // Dedicated async worker pool
    private final ScheduledExecutorService asyncExecutor = Executors.newScheduledThreadPool(4, r -> {
        Thread t = new Thread(r, "Nome-Async-Worker");
        t.setDaemon(true);
        return t;
    });

    public static class TaskImpl implements BukkitTask {
        final int id;
        final Plugin owner;
        final Runnable runnable;
        final boolean sync;
        final long period;
        long nextRunTick;
        volatile boolean cancelled = false;
        ScheduledFuture<?> asyncFuture = null;

        TaskImpl(int id, Plugin owner, Runnable runnable, boolean sync, long nextRunTick, long period) {
            this.id = id;
            this.owner = owner;
            this.runnable = runnable;
            this.sync = sync;
            this.nextRunTick = nextRunTick;
            this.period = period;
        }

        @Override public int getTaskId() { return id; }
        @Override public Plugin getOwner() { return owner; }
        @Override public boolean isSync() { return sync; }
        @Override public boolean isCancelled() { return cancelled; }

        @Override
        public void cancel() {
            cancelled = true;
            if (asyncFuture != null) {
                asyncFuture.cancel(false);
            }
        }
    }

    /**
     * Heartbeat called on the Server Main Thread by the 20 TPS native tick loop.
     */
    public void mainThreadHeartbeat(long tick) {
        currentTick.set(tick);

        // 1. Drain incoming sync tasks queued by plugins from any thread
        TaskImpl incoming;
        while ((incoming = pendingSyncTasks.poll()) != null) {
            if (!incoming.cancelled) {
                activeSyncTasks.add(incoming);
            }
        }

        // 2. Execute due sync tasks sequentially on Main Thread
        Iterator<TaskImpl> it = activeSyncTasks.iterator();
        while (it.hasNext()) {
            TaskImpl task = it.next();
            if (task.cancelled) {
                it.remove();
                allTasks.remove(task.id);
                continue;
            }

            if (task.nextRunTick <= tick) {
                try {
                    task.runnable.run();
                } catch (Throwable t) {
                    System.err.println("[Nome-Scheduler] Error in sync task #" + task.id + " (Plugin: "
                            + (task.owner != null ? task.owner.getName() : "Server") + "): " + t.getMessage());
                    t.printStackTrace();
                }

                if (task.period > 0 && !task.cancelled) {
                    task.nextRunTick = tick + task.period;
                } else {
                    it.remove();
                    allTasks.remove(task.id);
                }
            }
        }
    }

    public long getCurrentTick() {
        return currentTick.get();
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
        long targetTick = currentTick.get() + Math.max(0, delay);
        TaskImpl btask = new TaskImpl(id, plugin, task, true, targetTick, 0);
        allTasks.put(id, btask);
        pendingSyncTasks.add(btask);
        return btask;
    }

    @Override
    public BukkitTask runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay) {
        int id = nextId.getAndIncrement();
        TaskImpl btask = new TaskImpl(id, plugin, task, false, currentTick.get() + delay, 0);
        allTasks.put(id, btask);

        btask.asyncFuture = asyncExecutor.schedule(() -> {
            try {
                if (!btask.cancelled) {
                    task.run();
                }
            } catch (Throwable t) {
                System.err.println("[Nome-Scheduler] Error in async task #" + btask.id + ": " + t.getMessage());
                t.printStackTrace();
            } finally {
                allTasks.remove(id);
            }
        }, Math.max(0, delay) * 50, TimeUnit.MILLISECONDS);

        return btask;
    }

    @Override
    public BukkitTask runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        int id = nextId.getAndIncrement();
        long targetTick = currentTick.get() + Math.max(0, delay);
        TaskImpl btask = new TaskImpl(id, plugin, task, true, targetTick, Math.max(1, period));
        allTasks.put(id, btask);
        pendingSyncTasks.add(btask);
        return btask;
    }

    @Override
    public BukkitTask runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period) {
        int id = nextId.getAndIncrement();
        TaskImpl btask = new TaskImpl(id, plugin, task, false, currentTick.get() + delay, period);
        allTasks.put(id, btask);

        btask.asyncFuture = asyncExecutor.scheduleAtFixedRate(() -> {
            if (btask.cancelled) {
                throw new CancellationException();
            }
            try {
                task.run();
            } catch (Throwable t) {
                System.err.println("[Nome-Scheduler] Error in async periodic task #" + btask.id + ": " + t.getMessage());
                t.printStackTrace();
            }
        }, Math.max(0, delay) * 50, Math.max(1, period) * 50, TimeUnit.MILLISECONDS);

        return btask;
    }

    @Override
    public <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runTask(plugin, () -> {
            try {
                future.complete(task.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public void cancelTask(int taskId) {
        TaskImpl task = allTasks.remove(taskId);
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public void cancelTasks(Plugin plugin) {
        if (plugin == null) return;
        for (TaskImpl task : allTasks.values()) {
            if (task.owner == plugin) {
                task.cancel();
            }
        }
    }
}
