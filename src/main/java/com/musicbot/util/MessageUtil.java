package com.musicbot.util;

import com.musicbot.MusicBotPlugin;
import com.musicbot.audio.TrackInfo;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageUtil {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private MessageUtil() {}

    public static String colorize(String msg) {
        Matcher m = HEX.matcher(msg);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, ChatColor.of("#" + m.group(1)).toString());
        }
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static void send(CommandSender s, String msg) {
        String pfx = MusicBotPlugin.getInstance().getConfigManager().getPrefix();
        s.sendMessage(colorize(pfx + msg));
    }

    public static void sendRaw(CommandSender s, String msg) {
        s.sendMessage(colorize(msg));
    }

    public static void sendNowPlaying(CommandSender s, TrackInfo t) {
        send(s, "&a▶ Now Playing: &f" + t.title());
    }

    public static void sendQueue(CommandSender s, TrackInfo current, List<TrackInfo> queue) {
        sendRaw(s, "");
        sendRaw(s, "&6&l━━━━━━ &b&l♫ Queue &6&l━━━━━━");
        sendRaw(s, "");

        if (current != null) {
            sendRaw(s, "&a▶ &fNow: &b" + current.title());
        } else {
            sendRaw(s, "&7Nothing playing.");
        }

        if (queue.isEmpty()) {
            sendRaw(s, "&7Queue is empty.");
        } else {
            sendRaw(s, "");
            sendRaw(s, "&e&lUp Next:");
            for (int i = 0; i < queue.size(); i++) {
                sendRaw(s, " &6" + (i + 1) + ". &f" + queue.get(i).title());
            }
        }

        sendRaw(s, "");
        sendRaw(s, "&6&l━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}