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

    public void remove(UUID uuid) {
        BotAudioPlayer p = players.remove(uuid);
        if (p != null) p.shutdown();
    }

    public void shutdownAll() {
        players.values().forEach(BotAudioPlayer::shutdown);
        players.clear();
    }
}