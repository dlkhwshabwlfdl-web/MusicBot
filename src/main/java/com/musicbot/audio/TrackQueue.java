package com.musicbot.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class TrackQueue {

    private final ConcurrentLinkedDeque<TrackInfo> queue = new ConcurrentLinkedDeque<>();
    private final int maxSize;

    public TrackQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    public boolean add(TrackInfo track) {
        if (queue.size() >= maxSize) return false;
        queue.addLast(track);
        return true;
    }

    public TrackInfo poll() { return queue.pollFirst(); }
    public boolean isEmpty() { return queue.isEmpty(); }
    public int size() { return queue.size(); }
    public void clear() { queue.clear(); }
    public boolean isFull() { return queue.size() >= maxSize; }

    public List<TrackInfo> asList() {
        return Collections.unmodifiableList(new ArrayList<>(queue));
    }
}