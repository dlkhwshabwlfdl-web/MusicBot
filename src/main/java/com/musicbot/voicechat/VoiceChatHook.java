package com.musicbot.voicechat;

import com.musicbot.MusicBotPlugin;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import org.bukkit.Bukkit;

public final class VoiceChatHook {

    private final MusicBotPlugin plugin;
    private final VoiceChatPluginImpl vcPlugin;
    private volatile VoicechatServerApi api;

    public VoiceChatHook(MusicBotPlugin plugin) {
        this.plugin = plugin;
        this.vcPlugin = new VoiceChatPluginImpl(this);
    }

    public void register() {
        BukkitVoicechatService service =
                Bukkit.getServicesManager().load(BukkitVoicechatService.class);

        if (service == null) {
            plugin.getLogger().severe(
                    "Simple Voice Chat not found! Is it installed?");
            return;
        }

        service.registerPlugin(vcPlugin);
        plugin.getLogger().info("Registered with Simple Voice Chat API.");
    }

    public VoicechatServerApi getApi() {
        return api;
    }

    public void setApi(VoicechatServerApi api) {
        this.api = api;
    }

    public MusicBotPlugin getPlugin() {
        return plugin;
    }
}