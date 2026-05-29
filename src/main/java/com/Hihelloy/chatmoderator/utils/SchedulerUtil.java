package com.Hihelloy.chatmoderator.utils;

import com.cjcrafter.foliascheduler.TaskImplementation;
import com.cjcrafter.foliascheduler.folia.FoliaTask;
import com.Hihelloy.chatmoderator.ChatModeratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

import static com.cjcrafter.foliascheduler.util.ServerVersions.isFolia;
import static com.Hihelloy.chatmoderator.ChatModeratorPlugin.plugin;
import static com.Hihelloy.chatmoderator.ChatModeratorPlugin.scheduler;

public class SchedulerUtil {

    private static AtomicBoolean SHUTTING_DOWN = new AtomicBoolean(false);

    public SchedulerUtil(Plugin plugin) {

    }

    public static void ensureEntity(@NotNull Entity entity, @NotNull Runnable runnable) {
        if (entity instanceof Player && !((Player)entity).isOnline()) return;

        if (isFolia()) {
            if (scheduler.isOwnedByCurrentRegion(entity) || SHUTTING_DOWN.get()) {
                runCatch(runnable, "Error in ensureEntity task on shutdown");
                return;
            }
            scheduler.entity(entity).execute(runnable, null, 1L);
        } else {
            if (Bukkit.isPrimaryThread()) {
                runCatch(runnable, "Error in ensureEntity task");
                return;
            }
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static Object ensureEntityLater(@NotNull Entity entity, @NotNull Runnable runnable, long delay) {
        if (entity instanceof Player && !((Player)entity).isOnline()) return null;
        delay = Math.max(1, delay);
        if (isFolia()) {
            return scheduler.entity(entity).execute(runnable, null, delay);
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    public static Object ensureEntityTimer(@NotNull Entity entity, @NotNull Runnable runnable, long delay, long repeat) {
        if (entity instanceof Player && !((Player)entity).isOnline()) return null;
        delay = Math.max(1, delay);
        repeat = Math.max(1, repeat);
        if (isFolia()) {
            return scheduler.entity(entity).runAtFixedRate((task) -> {
                if (!runCatch(runnable, "Error in ensureEntityTimer task")) {
                    task.cancel();
                }
            }, null, delay, repeat);
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, repeat);
        }
    }

    public static void ensureLocation(@NotNull Location location, @NotNull Runnable runnable) {
        if (isFolia()) {
            if (scheduler.isOwnedByCurrentRegion(location) || SHUTTING_DOWN.get()) {
                runCatch(runnable, "Error in ensureLocation task on shutdown");
                return;
            }

            scheduler.region(location).execute(runnable);
        } else {
            if (Bukkit.isPrimaryThread()) {
                runCatch(runnable, "Error in ensureLocation task on shutdown");
                return;
            }
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static @NotNull Object ensureLocationLater(@NotNull Location location, @NotNull Runnable runnable, long delay) {
        delay = Math.max(1, delay);
        if (isFolia()) {
            return scheduler.region(location).runDelayed((task) -> {
                if (!runCatch(runnable, "Error in ensureLocationLater")) {
                    task.cancel();
                }
            }, delay);
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    public static @NotNull Object ensureLocationTimer(@NotNull Location location, @NotNull Runnable runnable, long delay, long repeat) {
        delay = Math.max(1, delay);
        repeat = Math.max(1, repeat);
        if (isFolia()) {

            return scheduler.region(location).runAtFixedRate((task) -> {
                if (!runCatch(runnable, "Error in ensureLocationTimer task"))
                    task.cancel();
            }, delay, repeat);
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, repeat);
        }
    }

    public static void runAsync(@NotNull Runnable runnable) {
        if (isFolia()) {
            if (SHUTTING_DOWN.get()) {
                runCatch(runnable, "Error in runAsync task on shutdown");
                return;
            }

            scheduler.async().runNow((task) -> {
                if (!runCatch(runnable, "Error in runAsync task")) {
                    task.cancel();
                }
            });
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public static @NotNull Object runAsyncLater(@NotNull Runnable runnable, long delay) {
        delay = Math.max(1, delay);
        if (isFolia()) {
            return scheduler.async().runDelayed((task) -> {
                if (!runCatch(runnable, "Error in runAsyncLater task")) {
                    task.cancel();
                }
            }, delay * 50, TimeUnit.MILLISECONDS);
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    public static @NotNull Object runAsyncTimer(@NotNull Runnable runnable, long delay, long repeat) {
        delay = Math.max(1, delay);
        if (isFolia()) {
            return scheduler.async().runAtFixedRate((Consumer<TaskImplementation<Void>>) (task) -> runnable.run(), delay * 50, repeat * 50, TimeUnit.MILLISECONDS);
        } else {
            return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, repeat);
        }
    }

    public static void runGlobal(@NotNull Runnable runnable) {
        if (isFolia()) {
            if (SHUTTING_DOWN.get()) {
                runCatch(runnable, "Error in runGlobal task on shutdown");
                return;
            }
            scheduler.global().run((task) -> {
                if (!runCatch(runnable, "Error in runGlobal task")) {
                    task.cancel();
                }
            });
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static @NotNull Object runGlobalLater(@NotNull Runnable runnable, long delay) {
        delay = Math.max(1, delay);
        if (isFolia()) {
            return scheduler.global().runDelayed((task) -> {
                if (!runCatch(runnable, "Error in runGlobalLater task")) {
                    task.cancel();
                }
            }, delay);
        } else {
            return  Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    public static @NotNull Object runGlobalTimer(@NotNull Runnable runnable, long delay, long repeat) {
        delay = Math.max(1, delay);
        if (isFolia()) {
            return scheduler.global().runAtFixedRate((task) -> {
                if (!runCatch(runnable, "Error in runGlobalTimer task")) {
                    task.cancel();
                }
            }, delay, repeat);
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, repeat);
        }
    }

    public static boolean cancelTask(Object task) {
        if (task == null) return false;
        if (isFolia()) {
            if (task instanceof FoliaTask<?>) {
                ((FoliaTask) task).cancel();
                return true;
            }
        } else {
            if (task instanceof org.bukkit.scheduler.BukkitTask) {
                ((org.bukkit.scheduler.BukkitTask) task).cancel();
                return true;
            }
        }
        return false;
    }

    public static boolean isTaskCancelled(Object task) {
        if (task == null) return true;
        if (isFolia()) {
            if (task instanceof FoliaTask<?>) {
                return ((FoliaTask) task).isCancelled();
            }
        } else {
            if (task instanceof org.bukkit.scheduler.BukkitTask) {
                return ((org.bukkit.scheduler.BukkitTask) task).isCancelled();
            }
        }
        return false;
    }

    private static boolean runCatch(Runnable runnable, String error) {
        try {
            runnable.run();
            return true;
        } catch (Exception e) {
            ChatModeratorPlugin.log.log(Level.WARNING, error, e);
            return false;
        }
    }

    public static void shutdown() {
        SHUTTING_DOWN.set(true);
    }
}
