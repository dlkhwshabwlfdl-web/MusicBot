package com.musicbot;

import com.musicbot.audio.AudioManager;
import com.musicbot.audio.MusicLibrary;
import com.musicbot.audio.NowPlayingDisplay;
import com.musicbot.command.MusicCommand;
import com.musicbot.config.ConfigManager;
import com.musicbot.util.TaskUtil;
import com.musicbot.voicechat.VoiceChatHook;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;

public final class MusicBotPlugin extends JavaPlugin {

    private static MusicBotPlugin instance;
    private ConfigManager configManager;
    private MusicLibrary musicLibrary;
    private AudioManager audioManager;
    private VoiceChatHook voiceChatHook;
    private NowPlayingDisplay nowPlayingDisplay;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        configManager = new ConfigManager(this);
        createDirectories();

        if (!verifyFfmpeg()) {
            getLogger().severe(
                    "==============================");
            getLogger().severe("  ffmpeg not found!");
            getLogger().severe(
                    "==============================");
            getServer().getPluginManager()
                    .disablePlugin(this);
            return;
        }

        musicLibrary = new MusicLibrary(this);
        audioManager = new AudioManager(this);
        nowPlayingDisplay =
                new NowPlayingDisplay(this);
        voiceChatHook = new VoiceChatHook(this);
        voiceChatHook.register();

        MusicCommand cmd = new MusicCommand(this);
        Objects.requireNonNull(getCommand("music"))
                .setExecutor(cmd);
        Objects.requireNonNull(getCommand("music"))
                .setTabCompleter(cmd);

        getLogger().info(
                "==============================");
        getLogger().info("  MusicBot v"
                + getDescription().getVersion()
                + " enabled!");
        getLogger().info("  Songs: "
                + musicLibrary.getSongCount());
        getLogger().info(
                "==============================");
    }

    @Override
    public void onDisable() {
        if (nowPlayingDisplay != null)
            nowPlayingDisplay.hideAll();
        if (audioManager != null)
            audioManager.shutdownAll();
        TaskUtil.shutdown();
        instance = null;
    }

    private void createDirectories() {
        new File(getDataFolder(),
                configManager.getMusicFolder())
                .mkdirs();
        new File(getDataFolder(),
                configManager.getCacheFolder())
                .mkdirs();
    }

    private boolean verifyFfmpeg() {
        try {
            Process p = new ProcessBuilder(
                    configManager.getFfmpegPath(),
                    "-version")
                    .redirectErrorStream(true)
                    .start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (Exception e) {
            getLogger().log(Level.FINE,
                    "ffmpeg failed", e);
            return false;
        }
    }

    public void debugLog(String msg) {
        if (configManager.isDebug())
            getLogger().info("[DEBUG] " + msg);
    }

    public static MusicBotPlugin getInstance() {
        return instance;
    }
    public ConfigManager getConfigManager() {
        return configManager;
    }
    public MusicLibrary getMusicLibrary() {
        return musicLibrary;
    }
    public AudioManager getAudioManager() {
        return audioManager;
    }
    public VoiceChatHook getVoiceChatHook() {
        return voiceChatHook;
    }
    public NowPlayingDisplay getNowPlayingDisplay() {
        return nowPlayingDisplay;
    }
}