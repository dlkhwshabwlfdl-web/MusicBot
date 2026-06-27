package com.musicbot.util;

import com.musicbot.MusicBotPlugin;
import org.bukkit.Bukkit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class TaskUtil {

    private static final ExecutorService DOWNLOAD_POOL =
            Executors.newFixedThreadPool(3, r -> {
                Thread t = new Thread(r, "MusicBot-Download");
                t.setDaemon(true);
                return t;
            });

    private TaskUtil() {}

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, DOWNLOAD_POOL);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, DOWNLOAD_POOL);
    }

    public static void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(MusicBotPlugin.getInstance(), runnable);
    }

    public static void runSyncLater(Runnable runnable, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(MusicBotPlugin.getInstance(), runnable, delayTicks);
    }

    public static void shutdown() {
        DOWNLOAD_POOL.shutdownNow();
        try {
            DOWNLOAD_POOL.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}