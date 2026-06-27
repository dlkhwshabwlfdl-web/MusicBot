package com.musicbot.audio;

import com.musicbot.MusicBotPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public final class PcmFrameReader {

    public static final int FRAME_SAMPLES = 960;
    public static final int FRAME_BYTES = FRAME_SAMPLES * 2;

    private PcmFrameReader() {}

    public static List<short[]> readAllFrames(File pcmFile) throws IOException {
        List<short[]> frames = new ArrayList<>();
        byte[] buf = new byte[FRAME_BYTES];

        try (FileInputStream fis = new FileInputStream(pcmFile)) {
            int bytesRead;
            while ((bytesRead = readFully(fis, buf)) > 0) {
                short[] frame = new short[FRAME_SAMPLES];
                ByteBuffer bb = ByteBuffer.wrap(buf, 0, bytesRead)
                        .order(ByteOrder.LITTLE_ENDIAN);
                int samples = bytesRead / 2;
                for (int i = 0; i < samples; i++) {
                    frame[i] = bb.getShort();
                }
                frames.add(frame);
            }
        }

        MusicBotPlugin.getInstance().debugLog(
                "Read " + frames.size() + " frames from " + pcmFile.getName());
        return frames;
    }

    public static short[] applyVolume(short[] frame, int volumePercent) {
        if (volumePercent == 100) return frame;
        if (volumePercent <= 0) return new short[frame.length];
        float mul = volumePercent / 100.0f;
        short[] out = new short[frame.length];
        for (int i = 0; i < frame.length; i++) {
            int v = (int)(frame[i] * mul);
            out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v));
        }
        return out;
    }

    private static int readFully(FileInputStream fis, byte[] buf) throws IOException {
        int total = 0;
        while (total < buf.length) {
            int r = fis.read(buf, total, buf.length - total);
            if (r == -1) break;
            total += r;
        }
        return total;
    }
}