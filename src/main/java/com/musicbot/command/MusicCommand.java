package com.musicbot.command;

import com.musicbot.MusicBotPlugin;
import com.musicbot.audio.BotAudioPlayer;
import com.musicbot.audio.MusicLibrary;
import com.musicbot.audio.TrackInfo;
import com.musicbot.util.MessageUtil;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class MusicCommand
        implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of(
            "play", "stop", "pause", "resume",
            "skip", "volume", "queue", "list",
            "reload"
    );

    // Rate limiting: prevent command spam
    private final Map<UUID, Long> cooldowns =
            new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1000;

    private final MusicBotPlugin plugin;

    public MusicCommand(MusicBotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command cmd, String label,
                             String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        // Rate limit check
        if (isOnCooldown(player.getUniqueId())) {
            MessageUtil.send(player,
                    "&cPlease wait before using "
                            + "another command!");
            return true;
        }
        setCooldown(player.getUniqueId());

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        // Sanitize subcommand
        String sub = args[0].toLowerCase().trim();
        if (sub.length() > 20) {
            MessageUtil.send(player,
                    "&cInvalid command!");
            return true;
        }

        switch (sub) {
            case "play" -> handlePlay(player, args);
            case "stop" -> handleStop(player);
            case "pause" -> handlePause(player);
            case "resume" -> handleResume(player);
            case "skip" -> handleSkip(player);
            case "volume", "vol" ->
                    handleVolume(player, args);
            case "queue", "q" ->
                    handleQueue(player);
            case "list", "songs" ->
                    handleList(player);
            case "reload" -> handleReload(player);
            default -> sendHelp(player);
        }
        return true;
    }

    // ============ RATE LIMITING ============

    private boolean isOnCooldown(UUID uuid) {
        Long last = cooldowns.get(uuid);
        if (last == null) return false;
        return System.currentTimeMillis() - last
                < COOLDOWN_MS;
    }

    private void setCooldown(UUID uuid) {
        cooldowns.put(uuid,
                System.currentTimeMillis());
    }

    // ============ PLAY ============

    private void handlePlay(Player player,
                            String[] args) {
        if (args.length < 2) {
            MessageUtil.send(player,
                    "&cUsage: /music play <song>");
            MessageUtil.send(player,
                    "&7Use &e/music list &7to see "
                            + "available songs.");
            return;
        }

        VoicechatServerApi api =
                plugin.getVoiceChatHook().getApi();
        if (api == null) {
            MessageUtil.send(player,
                    "&cVoice Chat not ready!");
            return;
        }

        ServerPlayer sp =
                api.fromServerPlayer(player);
        VoicechatConnection conn =
                api.getConnectionOf(sp);
        if (conn == null) {
            MessageUtil.send(player,
                    "&c❌ Connect to Voice Chat! "
                            + "Press &eV");
            return;
        }

        Group group = conn.getGroup();
        if (group == null) {
            MessageUtil.send(player, "");
            MessageUtil.send(player,
                    "&c❌ Join a Voice Chat group!");
            MessageUtil.send(player,
                    "&7Press &fV &7→ &fCreate Group "
                            + "&7→ &f/music play");
            return;
        }

        // Build query from args
        String query = String.join(" ",
                Arrays.copyOfRange(
                        args, 1, args.length));

        // Sanitize query
        query = query.trim();
        if (query.length() > 200) {
            query = query.substring(0, 200);
        }
        // Remove dangerous characters
        query = query.replace("..", "")
                .replace("/", "")
                .replace("\\", "");

        if (query.isEmpty()) {
            MessageUtil.send(player,
                    "&cInvalid song name!");
            return;
        }

        // Search
        MusicLibrary.SongEntry song =
                plugin.getMusicLibrary().findSong(query);

        if (song == null) {
            MessageUtil.send(player,
                    "&cSong not found: &f" + query);
            MessageUtil.send(player,
                    "&7Use &e/music list &7to see "
                            + "available songs.");
            return;
        }

        TrackInfo track = TrackInfo.builder()
                .title(song.displayName())
                .sourceFile(song.file())
                .requestedBy(player.getName())
                .build();

        BotAudioPlayer ap = plugin.getAudioManager()
                .getOrCreate(player.getUniqueId());
        ap.play(track);
    }

    // ============ LIST ============

    private void handleList(Player player) {
        plugin.getMusicLibrary().refresh();

        List<String> songs = plugin.getMusicLibrary()
                .getFormattedList();

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player,
                "&6&l━━━━━━ &b&l♫ Music Library "
                        + "&6&l━━━━━━");
        MessageUtil.sendRaw(player, "");

        if (songs.isEmpty()) {
            MessageUtil.sendRaw(player,
                    "&7No songs available.");
            MessageUtil.sendRaw(player,
                    "&7Ask server admin to add "
                            + "songs!");
        } else {
            for (String line : songs) {
                MessageUtil.sendRaw(player,
                        " " + line);
            }
            MessageUtil.sendRaw(player, "");
            MessageUtil.sendRaw(player,
                    "&7Total: &e" + songs.size()
                            + " &7songs");
            MessageUtil.sendRaw(player,
                    "&7Use: &e/music play <name>");
        }

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player,
                "&6&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ============ STOP ============

    private void handleStop(Player player) {
        BotAudioPlayer ap = plugin.getAudioManager()
                .get(player.getUniqueId());
        if (ap == null
                || ap.getState()
                == BotAudioPlayer.State.IDLE) {
            MessageUtil.send(player,
                    "&cNothing is playing!");
            return;
        }
        ap.stop();
        MessageUtil.send(player, "&c⏹ Stopped.");
    }

    // ============ PAUSE ============

    private void handlePause(Player player) {
        BotAudioPlayer ap = plugin.getAudioManager()
                .get(player.getUniqueId());
        if (ap == null
                || ap.getState()
                != BotAudioPlayer.State.PLAYING) {
            MessageUtil.send(player,
                    "&cNothing is playing!");
            return;
        }
        ap.pause();
        MessageUtil.send(player, "&e⏸ Paused.");
    }

    // ============ RESUME ============

    private void handleResume(Player player) {
        BotAudioPlayer ap = plugin.getAudioManager()
                .get(player.getUniqueId());
        if (ap == null
                || ap.getState()
                != BotAudioPlayer.State.PAUSED) {
            MessageUtil.send(player,
                    "&cNothing is paused!");
            return;
        }
        ap.resume();
        MessageUtil.send(player, "&a▶ Resumed.");
    }

    // ============ SKIP ============

    private void handleSkip(Player player) {
        BotAudioPlayer ap = plugin.getAudioManager()
                .get(player.getUniqueId());
        if (ap == null
                || (ap.getState()
                != BotAudioPlayer.State.PLAYING
                && ap.getState()
                != BotAudioPlayer.State.PAUSED)) {
            MessageUtil.send(player,
                    "&cNothing is playing!");
            return;
        }
        String t = ap.getCurrentTrack() != null
                ? ap.getCurrentTrack().title()
                : "Unknown";
        ap.skip();
        MessageUtil.send(player,
                "&e⏭ Skipped: &f" + t);
    }

    // ============ VOLUME ============

    private void handleVolume(Player player,
                              String[] args) {
        BotAudioPlayer ap = plugin.getAudioManager()
                .getOrCreate(player.getUniqueId());
        if (args.length < 2) {
            MessageUtil.send(player,
                    "&7Volume: &e"
                            + ap.getVolume() + "%");
            return;
        }
        try {
            int v = Integer.parseInt(args[1]);
            if (v < 0 || v > 100) {
                MessageUtil.send(player,
                        "&cRange: 0-100");
                return;
            }
            ap.setVolume(v);
            MessageUtil.send(player,
                    "&a🔊 Volume: &f" + v + "%");
        } catch (NumberFormatException e) {
            MessageUtil.send(player,
                    "&cInvalid number!");
        }
    }

    // ============ QUEUE ============

    private void handleQueue(Player player) {
        BotAudioPlayer ap = plugin.getAudioManager()
                .get(player.getUniqueId());
        if (ap == null) {
            MessageUtil.send(player,
                    "&7No active session.");
            return;
        }
        MessageUtil.sendQueue(player,
                ap.getCurrentTrack(),
                ap.getQueue().asList());
    }

    // ============ RELOAD ============

    private void handleReload(Player player) {
        if (!player.hasPermission("musicbot.admin")) {
            MessageUtil.send(player,
                    "&cNo permission!");
            return;
        }
        plugin.getConfigManager().reload();
        plugin.getMusicLibrary().refresh();
        MessageUtil.send(player,
                "&aReloaded! Songs: &e"
                        + plugin.getMusicLibrary()
                        .getSongCount());
    }

    // ============ HELP ============

    private void sendHelp(Player player) {
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player,
                "&6&l━━━━━ &b&l♫ MusicBot "
                        + "&6&l━━━━━");
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player,
                "&e/music play <name> "
                        + "&7- Play a song");
        MessageUtil.sendRaw(player,
                "&e/music list "
                        + "&7- Show all songs");
        MessageUtil.sendRaw(player,
                "&e/music stop "
                        + "&7- Stop & clear");
        MessageUtil.sendRaw(player,
                "&e/music pause &7- Pause");
        MessageUtil.sendRaw(player,
                "&e/music resume &7- Resume");
        MessageUtil.sendRaw(player,
                "&e/music skip &7- Skip");
        MessageUtil.sendRaw(player,
                "&e/music volume <0-100> "
                        + "&7- Volume");
        MessageUtil.sendRaw(player,
                "&e/music queue "
                        + "&7- Show queue");
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player,
                "&7⚠ Must be in a "
                        + "&eVoice Chat Group&7!");
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player,
                "&6&l━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ============ TAB COMPLETE ============

    @Override
    public List<String> onTabComplete(
            CommandSender sender, Command cmd,
            String label, String[] args) {
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(
                            args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2
                && args[0].equalsIgnoreCase("play")) {
            String typed = String.join(" ",
                            Arrays.copyOfRange(
                                    args, 1, args.length))
                    .toLowerCase();

            if (typed.isEmpty()) {
                return plugin.getMusicLibrary()
                        .getSongNames();
            }

            return plugin.getMusicLibrary()
                    .getSongNames().stream()
                    .filter(n -> n.toLowerCase()
                            .contains(typed))
                    .collect(Collectors.toList());
        }

        if (args.length == 2
                && (args[0].equalsIgnoreCase("volume")
                || args[0].equalsIgnoreCase("vol"))) {
            return List.of(
                    "10", "25", "50", "75", "100");
        }

        return new ArrayList<>();
    }
}