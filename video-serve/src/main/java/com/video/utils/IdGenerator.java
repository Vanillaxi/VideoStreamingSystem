package com.video.utils;

public class IdGenerator {
    private static final long EPOCH = 1704067200000L;
    private static final long WORKER_ID = 1L;
    private static final long WORKER_ID_SHIFT = 12L;
    private static final long TIMESTAMP_SHIFT = 22L;
    private static final long SEQUENCE_MASK = 4095L;
    private static long lastTimestamp = -1L;
    private static long sequence = 0L;

    private IdGenerator() {
    }

    public static synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            timestamp = lastTimestamp;
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | (WORKER_ID << WORKER_ID_SHIFT) | sequence;
    }

    private static long waitNextMillis(long current) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= current) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
