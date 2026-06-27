package com.musicbot.voicechat;

import com.musicbot.MusicBotPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public final class VoiceChatPluginImpl
        implements VoicechatPlugin {

    private final VoiceChatHook hook;

    public VoiceChatPluginImpl(VoiceChatHook hook) {
        this.hook = hook;
    }

    @Override
    public String getPluginId() {
        return "musicbot";
    }

    @Override
    public void initialize(VoicechatApi api) {
        hook.getPlugin().getLogger().info(
                "VoiceChat plugin initialized.");
    }

    @Override
    public void registerEvents(
            EventRegistration registration) {
        registration.registerEvent(
                VoicechatServerStartedEvent.class,
                this::onServerStarted
        );
    }

    private void onServerStarted(
            VoicechatServerStartedEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        hook.setApi(api);

        // Load custom icon
        int[][] icon = loadIcon();

        // Register volume category with icon
        VolumeCategory category;
        if (icon != null) {
            category = api.volumeCategoryBuilder()
                    .setId("musicbot")
                    .setName("MusicBot")
                    .setDescription(
                            "Music Bot Audio")
                    .setIcon(icon)
                    .build();
        } else {
            category = api.volumeCategoryBuilder()
                    .setId("musicbot")
                    .setName("MusicBot")
                    .setDescription(
                            "Music Bot Audio")
                    .build();
        }

        api.registerVolumeCategory(category);

        hook.getPlugin().getLogger().info(
                "Voice Chat API ready. "
                        + "Category registered"
                        + (icon != null
                        ? " with custom icon." : "."));
    }

    /**
     * Load the 16x16 icon from resources.
     * Returns int[16][16] where each int is
     * an ARGB color value.
     * Returns null if icon not found.
     */
    private int[][] loadIcon() {
        try (InputStream is = getClass()
                .getResourceAsStream(
                        "/musicbot_icon.png")) {
            if (is == null) {
                hook.getPlugin().debugLog(
                        "Icon file not found, "
                                + "using generated icon");
                return generateMusicNoteIcon();
            }

            BufferedImage img = ImageIO.read(is);
            if (img.getWidth() != 16
                    || img.getHeight() != 16) {
                hook.getPlugin().getLogger().warning(
                        "Icon must be 16x16! "
                                + "Using generated icon.");
                return generateMusicNoteIcon();
            }

            int[][] pixels = new int[16][16];
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    pixels[y][x] = img.getRGB(x, y);
                }
            }

            hook.getPlugin().debugLog(
                    "Custom icon loaded.");
            return pixels;

        } catch (Exception e) {
            hook.getPlugin().debugLog(
                    "Icon load error: "
                            + e.getMessage());
            return generateMusicNoteIcon();
        }
    }

    /**
     * Generate a simple music note icon
     * programmatically. No external file needed!
     * 16x16 pixels, ARGB format.
     */
    private int[][] generateMusicNoteIcon() {
        int[][] icon = new int[16][16];

        // Colors (ARGB)
        int T = 0x00000000;  // Transparent
        int W = 0xFFFFFFFF;  // White
        int G = 0xFF00E5FF;  // Cyan/Aqua
        int D = 0xFF0097A7;  // Dark cyan

        // Music note pattern (♫)
        int[][] pattern = {
                {T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T},
                {T,T,T,T,T,G,G,G,G,G,G,G,T,T,T,T},
                {T,T,T,T,T,G,W,W,W,W,W,G,T,T,T,T},
                {T,T,T,T,T,G,W,W,W,W,W,G,T,T,T,T},
                {T,T,T,T,T,G,D,D,D,D,D,G,T,T,T,T},
                {T,T,T,T,T,G,T,T,T,T,T,G,T,T,T,T},
                {T,T,T,T,T,G,T,T,T,T,T,G,T,T,T,T},
                {T,T,T,T,T,G,T,T,T,T,T,G,T,T,T,T},
                {T,T,T,T,T,G,T,T,T,T,T,G,T,T,T,T},
                {T,T,T,T,T,G,T,T,T,T,T,G,T,T,T,T},
                {T,T,T,T,T,G,T,T,T,T,T,G,T,T,T,T},
                {T,T,T,G,G,G,T,T,T,G,G,G,T,T,T,T},
                {T,T,G,W,G,G,T,T,G,W,G,G,T,T,T,T},
                {T,T,G,G,G,T,T,T,G,G,G,T,T,T,T,T},
                {T,T,T,G,T,T,T,T,T,G,T,T,T,T,T,T},
                {T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T},
        };

        return pattern;
    }
}