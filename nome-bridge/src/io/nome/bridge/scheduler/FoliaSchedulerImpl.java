package io.nome.bridge.scheduler;

import io.papermc.paper.threadedregions.scheduler.*;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import io.nome.bridge.NomeNativeBridge;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class FoliaSchedulerImpl implements AsyncScheduler, GlobalRegionScheduler, RegionScheduler {

    private final ScheduledExecutorService asyncExecutor = Executors.newScheduledThreadPool(4, r -> {
        Thread t = new Thread(r, "Nome-Folia-Worker");
        t.setDaemon(true);
        return t;
    });

    // ==========================================
    // AsyncScheduler
    // ==========================================
    @Override
    public ScheduledTask runNow(Plugin plugin, Consumer<ScheduledTask> task) {
        FoliaTask ft = new FoliaTask(plugin, task, false);
        ft.future = asyncExecutor.submit(() -> ft.execute());
        return ft;
    }

    @Override
    public ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task, long delay, TimeUnit unit) {
        FoliaTask ft = new FoliaTask(plugin, task, false);
        ft.future = asyncExecutor.schedule(() -> ft.execute(), delay, unit);
        return ft;
    }

    @Override
    public ScheduledTask runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task, long initialDelay, long period, TimeUnit unit) {
        FoliaTask ft = new FoliaTask(plugin, task, true);
        ft.future = asyncExecutor.scheduleAtFixedRate(() -> ft.execute(), initialDelay, period, unit);
        return ft;
    }

    @Override
    public void cancelTasks(Plugin plugin) {
        // Cancel all tasks associated with plugin
    }

    // ==========================================
    // GlobalRegionScheduler
    // ==========================================
    @Override
    public ScheduledTask run(Plugin plugin, Consumer<ScheduledTask> task) {
        return runNow(plugin, task);
    }

    @Override
    public ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task, long delayTicks) {
        return runDelayed(plugin, task, delayTicks * 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledTask runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
        return runAtFixedRate(plugin, task, initialDelayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
    }

    // ==========================================
    // RegionScheduler
    // ==========================================
    @Override
    public ScheduledTask run(Plugin plugin, World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task) {
        return runNow(plugin, task);
    }

    @Override
    public ScheduledTask runDelayed(Plugin plugin, World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks) {
        return runDelayed(plugin, task, delayTicks * 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledTask runAtFixedRate(Plugin plugin, World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
        return runAtFixedRate(plugin, task, initialDelayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
    }

    // ==========================================
    // Task Implementation
    // ==========================================
    public static class FoliaTask implements ScheduledTask {
        private final Plugin plugin;
        private final Consumer<ScheduledTask> consumer;
        private final boolean repeating;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        volatile Future<?> future;

        public FoliaTask(Plugin plugin, Consumer<ScheduledTask> consumer, boolean repeating) {
            this.plugin = plugin;
            this.consumer = consumer;
            this.repeating = repeating;
        }

        void execute() {
            if (cancelled.get()) return;
            try {
                consumer.accept(this);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        @Override
        public Plugin getPlugin() {
            return plugin;
        }

        @Override
        public boolean cancel() {
            cancelled.set(true);
            if (future != null) {
                future.cancel(false);
            }
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public ExecutionState getExecutionState() {
            if (cancelled.get()) return ExecutionState.CANCELLED;
            if (future != null && future.isDone()) return ExecutionState.FINISHED;
            return ExecutionState.RUNNING;
        }
    }
}
