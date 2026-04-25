package com.rohit.ChatApplication.service.MessageSequencing;

import com.rohit.ChatApplication.data.message.NodeIdentity;
import org.springframework.stereotype.Component;

@Component
public class SnowFlakeIdGenerator {

    private static final long EPOCH = 1704067200000L;

    private static final long MACHINE_ID_BITS = 5L;   // 0–31
    private static final long SEQUENCE_BITS = 12L;    // 0–4095

    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long MACHINE_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    private final long machineId;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowFlakeIdGenerator(NodeIdentity nodeIdentity) {
        this.machineId = Math.abs(nodeIdentity.getNodeId().hashCode()) % (MAX_MACHINE_ID + 1);

        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException("Invalid machineId");
        }

        System.out.println("Snowflake initialized → nodeId="
                + nodeIdentity.getNodeId() + " machineId=" + machineId);
    }

    public synchronized long generateId() {
        long timestamp = currentTime();

        if (timestamp < lastTimestamp) {
            timestamp = waitUntilNextMillis(lastTimestamp);
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;


            if (sequence == 0) {
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (machineId << MACHINE_SHIFT)
                | sequence;
    }

    private long waitUntilNextMillis(long lastTs) {
        long ts = currentTime();
        while (ts <= lastTs) {
            ts = currentTime(); // spin-wait (fast)
        }
        return ts;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }



}
