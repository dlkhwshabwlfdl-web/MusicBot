package com.musicbot.config;

import com.musicbot.MusicBotPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public final class ConfigManager {

    private final MusicBotPlugin plugin;

    private String ffmpegPath;
    private int defaultVolume;
    private int maxQueueSize;
    private String musicFolder;
    private List<String> supportedFormats;
    private String cacheFolder;
    private String prefix;
    private boolean debug;

    public ConfigManager(MusicBotPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        ffmpegPath = c.getString("ffmpeg-path", "ffmpeg");
        defaultVolume = clamp(c.getInt("default-volume", 50), 0, 100);
        maxQueueSize = c.getInt("max-queue-size", 50);
        musicFolder = c.getString("music-folder", "music");
        supportedFormats = c.getStringList("supported-formats");
        if (supportedFormats.isEmpty()) {
            supportedFormats = List.of("mp4", "mp3", "ogg", "wav", "m4a", "webm", "flac");
        }
        cacheFolder = c.getString("cache-folder", "cache");
        prefix = c.getString("prefix", "&6[&bMusicBot&6] &r");
        debug = c.getBoolean("debug", false);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public String getFfmpegPath() { return ffmpegPath; }
    public int getDefaultVolume() { return defaultVolume; }
    public int getMaxQueueSize() { return maxQueueSize; }
    public String getMusicFolder() { return musicFolder; }
    public List<String> getSupportedFormats() { return supportedFormats; }
    public String getCacheFolder() { return cacheFolder; }
    public String getPrefix() { return prefix; }
    public boolean isDebug() { return debug; }
}