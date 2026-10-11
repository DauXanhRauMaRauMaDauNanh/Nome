package io.nome.bridge.scheduler;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EntitySchedulerImpl implements EntityScheduler {

    private final Entity entity;
    private static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "Nome-Entity-Scheduler");
        t.setDaemon(true);
        return t;
    });

    public EntitySchedulerImpl(Entity entity) {
        this.entity = entity;
    }

    @Override
    public ScheduledTask run(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired) {
        return runDelayed(plugin, task, retired, 0);
    }

    @Override
    public ScheduledTask runDelayed(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired, long delayTicks) {
        FoliaSchedulerImpl.FoliaTask ft = new FoliaSchedulerImpl.FoliaTask(plugin, task, false);
        ft.future = pool.schedule(() -> {
            if (entity.isDead() && retired != null) {
                retired.run();
            } else {
                ft.execute();
            }
        }, delayTicks * 50, TimeUnit.MILLISECONDS);
        return ft;
    }

    @Override
    public ScheduledTask runAtFixedRate(Plugin plugin, Consumer<ScheduledTask> task, Runnable retired, long initialDelayTicks, long periodTicks) {
        FoliaSchedulerImpl.FoliaTask ft = new FoliaSchedulerImpl.FoliaTask(plugin, task, true);
        ft.future = pool.scheduleAtFixedRate(() -> {
            if (entity.isDead() && retired != null) {
                retired.run();
                ft.cancel();
            } else {
                ft.execute();
            }
        }, initialDelayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
        return ft;
    }

    @Override
    public boolean execute(Plugin plugin, Runnable run, Runnable retired, long delay) {
        if (entity.isDead()) {
            if (retired != null) retired.run();
            return false;
        }
        pool.schedule(run, delay * 50, TimeUnit.MILLISECONDS);
        return true;
    }
}
