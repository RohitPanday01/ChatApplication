package com.rohit.ChatApplication.service.MessageSequencing;


import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SnowFlakeDecoder {

    private static final long MACHINE_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;


    private static final long MACHINE_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    private static final long MACHINE_MASK = ~(-1L << MACHINE_ID_BITS);

    private static final long EPOCH = 1704067200000L;

    public void Decode(long id) {
        long seq = id & SEQUENCE_MASK;

        long machineId = (id >> MACHINE_SHIFT) & MACHINE_MASK;

        long timeStamp = (id >> TIMESTAMP_SHIFT) + EPOCH;

        System.out.println("ID: " + id);
        System.out.println("Timestamp: " + timeStamp);
        System.out.println("MachineId: " + machineId);
        System.out.println("Sequence: " + seq);

    }





}
