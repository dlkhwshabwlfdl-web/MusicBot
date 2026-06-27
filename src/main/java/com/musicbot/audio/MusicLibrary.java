package com.musicbot.audio;

import com.musicbot.MusicBotPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class MusicLibrary {

    private final MusicBotPlugin plugin;
    private final File musicFolder;
    private final List<String> supportedFormats;
    private List<SongEntry> songs = new ArrayList<>();

    public MusicLibrary(MusicBotPlugin plugin) {
        this.plugin = plugin;
        this.musicFolder = new File(
                plugin.getDataFolder(),
                plugin.getConfigManager().getMusicFolder());
        this.supportedFormats = plugin.getConfigManager()
                .getSupportedFormats();
        refresh();
    }

    public void refresh() {
        songs.clear();

        if (!musicFolder.exists()
                || !musicFolder.isDirectory()) {
            return;
        }

        File[] files = musicFolder.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (!f.isFile()) continue;
            if (!isSupported(f.getName())) continue;

            // Security: verify file is inside music folder
            if (!isInsideMusicFolder(f)) {
                plugin.getLogger().warning(
                        "Skipped unsafe file: "
                                + f.getName());
                continue;
            }

            String displayName = sanitizeDisplayName(
                    f.getName());

            songs.add(new SongEntry(displayName, f));
        }

        songs.sort(Comparator.comparing(
                s -> s.displayName().toLowerCase()));
    }

    /**
     * Find a song by name.
     * Returns null if not found.
     * SECURITY: Only returns files inside the
     * music folder.
     */
    public SongEntry findSong(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        // Security: sanitize input
        String q = sanitizeQuery(query);
        if (q.isEmpty()) return null;

        // 1. Exact match
        for (SongEntry s : songs) {
            if (s.displayName()
                    .equalsIgnoreCase(q)) {
                return s;
            }
        }

        // 2. Filename match (without extension)
        for (SongEntry s : songs) {
            String fname = getNameWithoutExt(
                    s.file().getName());
            if (fname.equalsIgnoreCase(q)) {
                return s;
            }
        }

        // 3. Contains match
        String cleanQ = q.toLowerCase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");

        SongEntry best = null;
        int bestScore = 0;

        for (SongEntry s : songs) {
            String cleanName = s.displayName()
                    .toLowerCase()
                    .replace(" ", "")
                    .replace("-", "")
                    .replace("_", "");

            if (cleanName.contains(cleanQ)) {
                int score = (cleanQ.length() * 100)
                        / cleanName.length();
                if (score > bestScore) {
                    bestScore = score;
                    best = s;
                }
            }
        }

        // 4. Word matching
        if (best == null) {
            String[] words = q.toLowerCase()
                    .split("\\s+");
            for (SongEntry s : songs) {
                String lower = s.displayName()
                        .toLowerCase();
                boolean allMatch = true;
                for (String w : words) {
                    if (!lower.contains(w)) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    best = s;
                    break;
                }
            }
        }

        return best;
    }

    /**
     * Get song names for tab complete.
     * NO system paths exposed.
     */
    public List<String> getSongNames() {
        return songs.stream()
                .map(SongEntry::displayName)
                .collect(Collectors.toList());
    }

    /**
     * Get filtered song names for tab complete
     */
    public List<String> getSongNamesFiltered(
            String prefix) {
        String lower = prefix.toLowerCase();
        return songs.stream()
                .map(SongEntry::displayName)
                .filter(n -> n.toLowerCase()
                        .startsWith(lower)
                        || n.toLowerCase()
                        .contains(lower))
                .collect(Collectors.toList());
    }

    /**
     * Get formatted list for display.
     * NO system paths. Only song names, format,
     * and size.
     */
    public List<String> getFormattedList() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            SongEntry s = songs.get(i);
            String ext = getExtension(
                    s.file().getName()).toUpperCase();
            long sizeKb = s.file().length() / 1024;
            String size = sizeKb > 1024
                    ? String.format("%.1fMB",
                    sizeKb / 1024.0)
                    : sizeKb + "KB";

            list.add("&6" + (i + 1) + ". &f"
                    + s.displayName()
                    + " &7[" + ext + " | "
                    + size + "]");
        }
        return list;
    }

    public int getSongCount() {
        return songs.size();
    }

    /**
     * Get music folder path - ONLY for admin/console.
     * Never show to players!
     */
    public File getMusicFolder() {
        return musicFolder;
    }

    // ==========================================
    //           SECURITY METHODS
    // ==========================================

    /**
     * Verify that a file is actually inside
     * the music folder. Prevents path traversal
     * attacks like ../../etc/passwd
     */
    private boolean isInsideMusicFolder(File file) {
        try {
            String musicPath = musicFolder
                    .getCanonicalPath();
            String filePath = file.getCanonicalPath();
            return filePath.startsWith(musicPath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Sanitize user query to prevent
     * path traversal and injection
     */
    private String sanitizeQuery(String input) {
        if (input == null) return "";

        // Remove path separators
        String clean = input
                .replace("/", "")
                .replace("\\", "")
                .replace("..", "")
                .replace("\0", "");

        // Remove control characters
        clean = clean.replaceAll(
                "[\\p{Cntrl}]", "");

        // Limit length
        if (clean.length() > 200) {
            clean = clean.substring(0, 200);
        }

        return clean.trim();
    }

    /**
     * Create a safe display name from filename.
     * Removes extension and cleans up.
     */
    private String sanitizeDisplayName(String filename) {
        String name = filename;

        // Remove extension
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }

        // Replace separators with spaces
        name = name.replace("-", " ")
                .replace("_", " ");

        // Remove multiple spaces
        name = name.replaceAll("\\s+", " ").trim();

        // Limit length
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }

        return name;
    }

    private boolean isSupported(String fileName) {
        String ext = getExtension(fileName)
                .toLowerCase();
        return supportedFormats.contains(ext);
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0
                ? fileName.substring(dot + 1) : "";
    }

    private String getNameWithoutExt(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0
                ? fileName.substring(0, dot)
                : fileName;
    }

    public record SongEntry(
            String displayName,
            File file
    ) {}
}