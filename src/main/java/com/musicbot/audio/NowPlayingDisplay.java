package com.musicbot.audio;

import com.musicbot.MusicBotPlugin;
import com.musicbot.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows a BossBar at the top of the screen
 * for all group members while music is playing.
 *
 * Looks like:
 * ♫ MusicBot ▶ Despacito - Requested by Steve
 * [===============================-------]
 */
public final class NowPlayingDisplay {

    private final MusicBotPlugin plugin;

    // Active displays per group
    private final Map<UUID, DisplayData> displays =
            new ConcurrentHashMap<>();

    public NowPlayingDisplay(MusicBotPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Show the now playing display for a group
     */
    public void show(UUID groupId,
                     TrackInfo track,
                     List<UUID> memberUuids,
                     int totalFrames) {
        // Remove old display for this group
        hide(groupId);

        BossBar bar = Bukkit.createBossBar(
                MessageUtil.colorize(
                        "&b♫ MusicBot &f▶ &e"
                                + truncate(track.title(), 30)
                                + " &7| &f"
                                + track.requestedBy()),
                BarColor.BLUE,
                BarStyle.SOLID
        );

        bar.setProgress(0.0);

        // Add all group members
        for (UUID uuid : memberUuids) {
            Player p = plugin.getServer()
                    .getPlayer(uuid);
            if (p != null && p.isOnline()) {
                bar.addPlayer(p);
            }
        }

        bar.setVisible(true);

        // Start progress updater
        BukkitTask task = Bukkit.getScheduler()
                .runTaskTimer(plugin, () -> {
                    DisplayData data =
                            displays.get(groupId);
                    if (data == null) return;

                    double progress =
                            (double) data.currentFrame
                                    / data.totalFrames;
                    progress = Math.max(0,
                            Math.min(1, progress));

                    data.bar.setProgress(progress);

                    // Update title with elapsed time
                    int elapsedSec =
                            (data.currentFrame * 20)
                                    / 1000;
                    int totalSec =
                            (data.totalFrames * 20)
                                    / 1000;

                    data.bar.setTitle(
                            MessageUtil.colorize(
                                    "&b♫ MusicBot &f▶ &e"
                                            + truncate(
                                            track.title(), 25)
                                            + " &7["
                                            + formatTime(
                                            elapsedSec)
                                            + "/"
                                            + formatTime(
                                            totalSec)
                                            + "] &7| &f"
                                            + track
                                            .requestedBy()));

                }, 20L, 20L); // Update every second

        displays.put(groupId,
                new DisplayData(bar, task,
                        totalFrames, 0));
    }

    /**
     * Update progress (called from audio player)
     */
    public void updateProgress(UUID groupId,
                               int currentFrame) {
        DisplayData data = displays.get(groupId);
        if (data != null) {
            data.currentFrame = currentFrame;
        }
    }

    /**
     * Add a new member to existing display
     */
    public void addMember(UUID groupId, Player player) {
        DisplayData data = displays.get(groupId);
        if (data != null && player != null) {
            data.bar.addPlayer(player);
        }
    }

    /**
     * Remove a member from display
     */
    public void removeMember(UUID groupId,
                             Player player) {
        DisplayData data = displays.get(groupId);
        if (data != null && player != null) {
            data.bar.removePlayer(player);
        }
    }

    /**
     * Show paused state
     */
    public void showPaused(UUID groupId) {
        DisplayData data = displays.get(groupId);
        if (data != null) {
            String current = data.bar.getTitle();
            data.bar.setTitle(
                    MessageUtil.colorize(
                            "&b♫ MusicBot &e⏸ PAUSED "
                                    + "&7| " + current));
            data.bar.setColor(BarColor.YELLOW);
        }
    }

    /**
     * Show resumed state
     */
    public void showResumed(UUID groupId,
                            TrackInfo track) {
        DisplayData data = displays.get(groupId);
        if (data != null) {
            data.bar.setColor(BarColor.BLUE);
        }
    }

    /**
     * Hide and remove the display for a group
     */
    public void hide(UUID groupId) {
        DisplayData data = displays.remove(groupId);
        if (data != null) {
            data.task.cancel();
            data.bar.removeAll();
            data.bar.setVisible(false);
        }
    }

    /**
     * Hide all displays
     */
    public void hideAll() {
        for (UUID id : new ArrayList<>(
                displays.keySet())) {
            hide(id);
        }
    }

    private String truncate(String str, int max) {
        if (str.length() <= max) return str;
        return str.substring(0, max - 2) + "..";
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    /**
     * Holds display data for one group
     */
    private static class DisplayData {
        final BossBar bar;
        final BukkitTask task;
        final int totalFrames;
        volatile int currentFrame;

        DisplayData(BossBar bar, BukkitTask task,
                    int totalFrames,
                    int currentFrame) {
            this.bar = bar;
            this.task = task;
            this.totalFrames = totalFrames;
            this.currentFrame = currentFrame;
        }
    }
}