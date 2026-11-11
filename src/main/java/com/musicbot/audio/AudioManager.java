package com.musicbot.audio;

import com.musicbot.MusicBotPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AudioManager {

    private final MusicBotPlugin plugin;
    private final Map<UUID, BotAudioPlayer> players = new ConcurrentHashMap<>();

    public AudioManager(MusicBotPlugin plugin) {
        this.plugin = plugin;
    }

    public BotAudioPlayer getOrCreate(UUID uuid) {
        return players.computeIfAbsent(uuid, id -> new BotAudioPlayer(plugin, id));
    }

    public BotAudioPlayer get(UUID uuid) {
        return players.get(uuid);
    }

    /**
     * Find the active session playing in a
     * Voice Chat group, if any. A group has at
     * most one shared session: members queue
     * into it instead of starting a parallel
     * one, and only its owner (the DJ) controls
     * playback.
     */
    public BotAudioPlayer findSessionForGroup(
            UUID groupId) {
        if (groupId == null) return null;
        for (BotAudioPlayer p : players.values()) {
            if (!groupId.equals(
                    p.getCurrentGroupId())) {
                continue;
            }
            BotAudioPlayer.State s = p.getState();
            if (s == BotAudioPlayer.State.PLAYING
                    || s == BotAudioPlayer.State.PAUSED
                    || s == BotAudioPlayer.State.LOADING) {
                return p;
            }
        }
        return null;
    }

    public void remove(UUID uuid) {
        BotAudioPlayer p = players.remove(uuid);
        if (p != null) p.shutdown();
    }

    public void shutdownAll() {
        players.values().forEach(BotAudioPlayer::shutdown);
        players.clear();
    }
}