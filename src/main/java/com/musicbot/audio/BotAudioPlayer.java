package com.musicbot.audio;

import com.musicbot.MusicBotPlugin;
import com.musicbot.util.MessageUtil;
import com.musicbot.util.TaskUtil;
import com.musicbot.audio.NowPlayingDisplay;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class BotAudioPlayer {

    public enum State {
        IDLE, LOADING, PLAYING, PAUSED
    }

    private final MusicBotPlugin plugin;
    private final UUID ownerUuid;
    private final TrackQueue queue;

    private final AtomicReference<State> state =
            new AtomicReference<>(State.IDLE);
    private final AtomicBoolean stopFlag =
            new AtomicBoolean(false);
    private final AtomicBoolean pauseFlag =
            new AtomicBoolean(false);
    private final AtomicInteger volume;
    private final AtomicInteger frameIndex =
            new AtomicInteger(0);

    private volatile TrackInfo currentTrack;
    private volatile List<short[]> currentFrames;
    private volatile UUID currentGroupId;
    private final Map<UUID, MemberAudio> memberAudios =
            new ConcurrentHashMap<>();

    private static final long MEMBER_CHECK_MS = 3000;

    public BotAudioPlayer(MusicBotPlugin plugin,
                          UUID ownerUuid) {
        this.plugin = plugin;
        this.ownerUuid = ownerUuid;
        this.queue = new TrackQueue(
                plugin.getConfigManager()
                        .getMaxQueueSize());
        this.volume = new AtomicInteger(
                plugin.getConfigManager()
                        .getDefaultVolume());
    }

    // ============ PUBLIC API ============

    public CompletableFuture<Void> play(TrackInfo track) {
        // Security: verify file exists and is safe
        if (track.sourceFile() == null
                || !track.sourceFile().exists()) {
            notifyOwner("&cFile not found!");
            return CompletableFuture.completedFuture(null);
        }

        if (!isFileInsidePluginFolder(
                track.sourceFile())) {
            notifyOwner("&cInvalid file!");
            plugin.getLogger().warning(
                    "Security: Blocked file access "
                            + "outside plugin folder by "
                            + ownerUuid);
            return CompletableFuture.completedFuture(null);
        }

        State s = state.get();
        if (s == State.PLAYING || s == State.PAUSED
                || s == State.LOADING) {
            if (queue.isFull()) {
                return CompletableFuture.failedFuture(
                        new RuntimeException(
                                "Queue is full!"));
            }
            queue.add(track);
            notifyOwner("&eQueued: &f"
                    + track.title()
                    + " &7(#" + queue.size() + ")");
            return CompletableFuture
                    .completedFuture(null);
        }
        return loadAndPlay(track);
    }

    public void stop() {
        stopFlag.set(true);
        pauseFlag.set(false);
        queue.clear();

        // Hide BossBar
        if (currentGroupId != null) {
            plugin.getNowPlayingDisplay()
                    .hide(currentGroupId);
        }

        cleanupAll();
        currentTrack = null;
        currentFrames = null;
        currentGroupId = null;
        frameIndex.set(0);
        state.set(State.IDLE);
    }

    public void pause() {
        if (state.compareAndSet(
                State.PLAYING, State.PAUSED)) {
            pauseFlag.set(true);
            stopAllPlayers();

            if (currentGroupId != null) {
                plugin.getNowPlayingDisplay()
                        .showPaused(currentGroupId);
            }

            notifyGroup("&e⏸ Music paused.");
        }
    }

    public void resume() {
        if (state.compareAndSet(
                State.PAUSED, State.PLAYING)) {
            pauseFlag.set(false);
            TaskUtil.runSync(this::rebuildAllPlayers);

            if (currentGroupId != null
                    && currentTrack != null) {
                plugin.getNowPlayingDisplay()
                        .showResumed(
                                currentGroupId,
                                currentTrack);
            }

            notifyGroup("&a▶ Music resumed.");
        }
    }

    public void skip() {
        if (state.get() == State.PLAYING
                || state.get() == State.PAUSED) {
            pauseFlag.set(false);
            cleanupAll();
            advanceQueue();
        }
    }

    public void setVolume(int v) {
        volume.set(Math.max(0, Math.min(100, v)));
    }

    public int getVolume() { return volume.get(); }
    public State getState() { return state.get(); }
    public TrackInfo getCurrentTrack() {
        return currentTrack;
    }
    public TrackQueue getQueue() { return queue; }

    public void shutdown() {
        stopFlag.set(true);

        if (currentGroupId != null) {
            plugin.getNowPlayingDisplay()
                    .hide(currentGroupId);
        }

        cleanupAll();
        queue.clear();
        state.set(State.IDLE);
    }

    // ============ INTERNALS ============

    /**
     * Security: verify file is inside plugin folder
     */
    private boolean isFileInsidePluginFolder(File file) {
        try {
            String pluginPath = plugin.getDataFolder()
                    .getCanonicalPath();
            String filePath = file.getCanonicalPath();
            return filePath.startsWith(pluginPath);
        } catch (IOException e) {
            return false;
        }
    }

    private CompletableFuture<Void> loadAndPlay(
            TrackInfo track) {
        Group group = getOwnerGroup();
        if (group == null) {
            notifyOwner(
                    "&c❌ Join a Voice Chat group!");
            return CompletableFuture
                    .completedFuture(null);
        }

        state.set(State.LOADING);
        stopFlag.set(false);
        pauseFlag.set(false);
        frameIndex.set(0);
        currentGroupId = group.getId();

        notifyOwner("&eLoading: &f"
                + track.title() + "&e...");

        return TaskUtil.supplyAsync(() -> {
            try {
                File pcm = convertToPcm(
                        track.sourceFile());
                List<short[]> frames =
                        PcmFrameReader.readAllFrames(pcm);
                if (frames.isEmpty()) {
                    throw new RuntimeException(
                            "No audio data found");
                }
                return new LoadResult(
                        track.withCachedPcm(pcm),
                        frames);
            } catch (Exception e) {
                throw new RuntimeException(
                        sanitizeError(e.getMessage()), e);
            }
        }).thenAccept(result -> {
            if (stopFlag.get()) {
                state.set(State.IDLE);
                return;
            }
            currentTrack = result.track;
            currentFrames = result.frames;

            TaskUtil.runSync(() -> {
                Group g = getOwnerGroup();
                if (g == null) {
                    notifyOwner("&cYou left the group!");
                    state.set(State.IDLE);
                    return;
                }
                currentGroupId = g.getId();
                notifyGroup("&a▶ Now Playing: &f"
                        + currentTrack.title());
                notifyGroup("&7Requested by: &e"
                        + currentTrack.requestedBy());
                startGroupPlayback();
            });
        }).exceptionally(ex -> {
            state.set(State.IDLE);
            Throwable c = ex.getCause() != null
                    ? ex.getCause() : ex;
            notifyOwner("&cFailed to load song.");
            // Log actual error only to console
            plugin.getLogger().warning(
                    "Load error: " + c.getMessage());
            return null;
        });
    }

    /**
     * Convert audio file to PCM using ffmpeg.
     * All errors are sanitized before showing
     * to players.
     */
    private File convertToPcm(File input)
            throws Exception {
        String hash = hashString(
                input.getAbsolutePath());
        File cacheDir = new File(
                plugin.getDataFolder(),
                plugin.getConfigManager()
                        .getCacheFolder());
        File outFile = new File(cacheDir,
                hash + ".pcm");

        if (outFile.exists() && outFile.length() > 0) {
            return outFile;
        }

        ProcessBuilder pb = new ProcessBuilder(
                plugin.getConfigManager().getFfmpegPath(),
                "-y",
                "-i", input.getAbsolutePath(),
                "-f", "s16le",
                "-ar", "48000",
                "-ac", "1",
                "-acodec", "pcm_s16le",
                outFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        // Only log to console, never to player
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                plugin.debugLog("[ffmpeg] " + line);
            }
        }

        if (!proc.waitFor(120, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            throw new RuntimeException(
                    "Audio conversion timed out");
        }
        if (proc.exitValue() != 0) {
            throw new RuntimeException(
                    "Audio conversion failed");
        }
        if (!outFile.exists()
                || outFile.length() == 0) {
            throw new RuntimeException(
                    "Audio conversion produced "
                            + "no output");
        }

        return outFile;
    }

    /**
     * Remove any system paths or sensitive info
     * from error messages before showing to players
     */
    private String sanitizeError(String error) {
        if (error == null) return "Unknown error";

        // Remove file paths
        error = error.replaceAll(
                "[A-Z]:\\\\[^\\s]+", "[file]");
        error = error.replaceAll(
                "/[^\\s]+", "[file]");

        // Remove specific system info
        error = error.replaceAll(
                "(?i)(home|user|admin|root"
                        + "|server|plugins|tmp|temp"
                        + "|var|etc|usr)", "***");

        // Limit length
        if (error.length() > 100) {
            error = error.substring(0, 100) + "...";
        }

        return error;
    }

    // ============ GROUP PLAYBACK ============

    private void startGroupPlayback() {
        VoicechatServerApi api =
                plugin.getVoiceChatHook().getApi();
        if (api == null || currentGroupId == null) {
            state.set(State.IDLE);
            return;
        }

        List<GroupMember> members =
                getGroupMembers(currentGroupId);
        if (members.isEmpty()) {
            notifyOwner("&cNo members in group!");
            state.set(State.IDLE);
            return;
        }

        for (GroupMember gm : members) {
            createMemberPlayer(api, gm);
        }

        state.set(State.PLAYING);

        // Show BossBar display
        List<UUID> memberIds = members.stream()
                .map(GroupMember::uuid).toList();
        plugin.getNowPlayingDisplay().show(
                currentGroupId,
                currentTrack,
                memberIds,
                currentFrames.size()
        );

        startGroupMonitor();
    }

    private void createMemberPlayer(
            VoicechatServerApi api,
            GroupMember member) {
        removeMemberPlayer(member.uuid());

        Player bukkit = plugin.getServer()
                .getPlayer(member.uuid());
        if (bukkit == null || !bukkit.isOnline())
            return;

        StaticAudioChannel channel =
                api.createStaticAudioChannel(
                        UUID.randomUUID(),
                        api.fromServerLevel(
                                bukkit.getWorld()),
                        member.connection()
                );
        if (channel == null) return;

        channel.setCategory("musicbot");

        OpusEncoder enc = api.createEncoder();
        AudioPlayer ap = api.createAudioPlayer(
                channel, enc,
                () -> supplyFrame(member.uuid()));

        memberAudios.put(member.uuid(),
                new MemberAudio(channel, ap, enc));
        ap.startPlaying();
    }

    private short[] supplyFrame(UUID memberUuid) {
        if (stopFlag.get() || pauseFlag.get())
            return null;
        List<short[]> frames = currentFrames;
        if (frames == null) return null;

        int idx;
        if (memberUuid.equals(ownerUuid)) {
            idx = frameIndex.getAndIncrement();

            // Update BossBar progress
            if (currentGroupId != null
                    && idx % 50 == 0) { // Every ~1 sec
                plugin.getNowPlayingDisplay()
                        .updateProgress(
                                currentGroupId, idx);
            }
        } else {
            idx = frameIndex.get() - 1;
            if (idx < 0) idx = 0;
        }

        if (idx >= frames.size()) {
            if (memberUuid.equals(ownerUuid)) {
                TaskUtil.runSync(this::advanceQueue);
            }
            return null;
        }

        return PcmFrameReader.applyVolume(
                frames.get(idx), volume.get());
    }

    // ============ GROUP MONITOR ============

    private void startGroupMonitor() {
        Thread t = new Thread(() -> {
            while (state.get() == State.PLAYING
                    || state.get() == State.PAUSED) {
                try {
                    if (stopFlag.get()) break;
                    Thread.sleep(MEMBER_CHECK_MS);
                    if (stopFlag.get()) break;
                    TaskUtil.runSync(
                            this::checkGroupMembership);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "MusicBot-Mon-"
                + ownerUuid.toString()
                .substring(0, 8));
        t.setDaemon(true);
        t.start();
    }

    private void checkGroupMembership() {
        if (state.get() != State.PLAYING
                && state.get() != State.PAUSED)
            return;
        if (currentGroupId == null) return;

        VoicechatServerApi api =
                plugin.getVoiceChatHook().getApi();
        if (api == null) return;

        Group ownerGroup = getOwnerGroup();
        if (ownerGroup == null
                || !ownerGroup.getId()
                .equals(currentGroupId)) {
            notifyGroupMembers(
                    "&c⏹ Music stopped (DJ left).");
            stop();
            return;
        }

        List<GroupMember> current =
                getGroupMembers(currentGroupId);
        List<UUID> currentIds = current.stream()
                .map(GroupMember::uuid).toList();

        for (GroupMember gm : current) {
            if (!memberAudios.containsKey(
                    gm.uuid())) {
                createMemberPlayer(api, gm);
                //=========================//
                Player p = plugin.getServer()
                        .getPlayer(gm.uuid());
                if (p != null) {
                    plugin.getNowPlayingDisplay()
                            .addMember(
                                    currentGroupId, p);
                }
            }
        }
        List<UUID> toRemove = new ArrayList<>();
        for (UUID id : memberAudios.keySet()) {
            if (!currentIds.contains(id))
                toRemove.add(id);
        }
        for (UUID left : toRemove) {
            removeMemberPlayer(left);

            // Remove from BossBar
            Player p = plugin.getServer()
                    .getPlayer(left);
            if (p != null) {
                plugin.getNowPlayingDisplay()
                        .removeMember(
                                currentGroupId, p);
            }
        }

        toRemove.forEach(this::removeMemberPlayer);
    }

    // ============ HELPERS ============

    private Group getOwnerGroup() {
        VoicechatServerApi api =
                plugin.getVoiceChatHook().getApi();
        if (api == null) return null;
        Player p = plugin.getServer()
                .getPlayer(ownerUuid);
        if (p == null) return null;
        VoicechatConnection conn =
                api.getConnectionOf(
                        api.fromServerPlayer(p));
        return conn != null ? conn.getGroup() : null;
    }

    private List<GroupMember> getGroupMembers(
            UUID groupId) {
        List<GroupMember> members = new ArrayList<>();
        VoicechatServerApi api =
                plugin.getVoiceChatHook().getApi();
        if (api == null) return members;

        for (Player p : plugin.getServer()
                .getOnlinePlayers()) {
            ServerPlayer sp =
                    api.fromServerPlayer(p);
            VoicechatConnection conn =
                    api.getConnectionOf(sp);
            if (conn == null) continue;
            Group g = conn.getGroup();
            if (g != null
                    && g.getId().equals(groupId)) {
                members.add(new GroupMember(
                        p.getUniqueId(), conn));
            }
        }
        return members;
    }

    private void advanceQueue() {
        // Hide current display
        if (currentGroupId != null) {
            plugin.getNowPlayingDisplay()
                    .hide(currentGroupId);
        }

        cleanupAll();
        TrackInfo next = queue.poll();
        if (next != null) {
            currentTrack = null;
            currentFrames = null;
            state.set(State.IDLE);
            loadAndPlay(next);
        } else {
            state.set(State.IDLE);
            currentTrack = null;
            currentFrames = null;
            currentGroupId = null;
            notifyGroup("&7Queue finished.");
        }
    }

    private void removeMemberPlayer(UUID uuid) {
        MemberAudio ma = memberAudios.remove(uuid);
        if (ma != null) ma.shutdown();
    }

    private void stopAllPlayers() {
        memberAudios.values().forEach(ma -> {
            try { ma.player().stopPlaying(); }
            catch (Exception ignored) {}
        });
    }

    private void rebuildAllPlayers() {
        VoicechatServerApi api =
                plugin.getVoiceChatHook().getApi();
        if (api == null || currentGroupId == null)
            return;
        cleanupMembers();
        getGroupMembers(currentGroupId)
                .forEach(gm ->
                        createMemberPlayer(api, gm));
    }

    private void cleanupMembers() {
        memberAudios.values()
                .forEach(MemberAudio::shutdown);
        memberAudios.clear();
    }

    private void cleanupAll() { cleanupMembers(); }

    private void notifyOwner(String msg) {
        TaskUtil.runSync(() -> {
            Player p = plugin.getServer()
                    .getPlayer(ownerUuid);
            if (p != null && p.isOnline())
                MessageUtil.send(p, msg);
        });
    }

    private void notifyGroup(String msg) {
        TaskUtil.runSync(() -> {
            if (currentGroupId == null) return;
            getGroupMembers(currentGroupId)
                    .forEach(gm -> {
                        Player p = plugin.getServer()
                                .getPlayer(gm.uuid());
                        if (p != null && p.isOnline())
                            MessageUtil.send(p, msg);
                    });
        });
    }

    private void notifyGroupMembers(String msg) {
        TaskUtil.runSync(() ->
                memberAudios.keySet().forEach(uuid -> {
                    Player p = plugin.getServer()
                            .getPlayer(uuid);
                    if (p != null && p.isOnline())
                        MessageUtil.send(p, msg);
                }));
    }

    private String hashString(String input) {
        try {
            byte[] h = MessageDigest
                    .getInstance("SHA-256")
                    .digest(input.getBytes(
                            StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h)
                    .substring(0, 16);
        } catch (Exception e) {
            return Integer.toHexString(
                    input.hashCode());
        }
    }

    private record GroupMember(
            UUID uuid,
            VoicechatConnection connection) {}

    private record MemberAudio(
            StaticAudioChannel channel,
            AudioPlayer player,
            OpusEncoder encoder) {
        void shutdown() {
            try { player.stopPlaying(); }
            catch (Exception ignored) {}
            try { encoder.close(); }
            catch (Exception ignored) {}
        }
    }

    private record LoadResult(
            TrackInfo track,
            List<short[]> frames) {}
}