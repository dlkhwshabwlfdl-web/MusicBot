package com.musicbot.audio;

import java.io.File;

public record TrackInfo(
        String title,
        File sourceFile,
        String duration,
        long durationSeconds,
        String requestedBy,
        File cachedPcm
) {
    public TrackInfo withCachedPcm(File pcm) {
        return new TrackInfo(title, sourceFile, duration,
                durationSeconds, requestedBy, pcm);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title = "Unknown";
        private File sourceFile;
        private String duration = "Unknown";
        private long durationSeconds = 0;
        private String requestedBy = "Unknown";
        private File cachedPcm = null;

        public Builder title(String v) { this.title = v; return this; }
        public Builder sourceFile(File v) { this.sourceFile = v; return this; }
        public Builder duration(String v) { this.duration = v; return this; }
        public Builder durationSeconds(long v) { this.durationSeconds = v; return this; }
        public Builder requestedBy(String v) { this.requestedBy = v; return this; }
        public Builder cachedPcm(File v) { this.cachedPcm = v; return this; }

        public TrackInfo build() {
            return new TrackInfo(title, sourceFile, duration,
                    durationSeconds, requestedBy, cachedPcm);
        }
    }
}