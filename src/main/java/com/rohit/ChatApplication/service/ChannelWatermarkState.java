package com.rohit.ChatApplication.service;

import org.springframework.stereotype.Component;

@Component
public class ChannelWatermarkState {

    // User A's Cursors
    private volatile long userA_MostSig = 0, userA_LeastSig = 0;
    private volatile long userA_Delivered = 0;
    private volatile long userA_Read = 0;

    // User B's Cursors
    private volatile long userB_MostSig = 0, userB_LeastSig = 0;
    private volatile long userB_Delivered = 0;
    private volatile long userB_Read = 0;

    public synchronized void syncWatermarks(long userMost, long userLeast, long delivered, long read) {
        if (userA_MostSig == 0 || (userA_MostSig == userMost && userA_LeastSig == userLeast)) {
            userA_MostSig = userMost;
            userA_LeastSig = userLeast;
            // Strict Math.max() prevents out-of-order network frames from moving the cursor backward
            userA_Delivered = Math.max(userA_Delivered, delivered);
            userA_Read = Math.max(userA_Read, read);
        } else {
            userB_MostSig = userMost;
            userB_LeastSig = userLeast;
            userB_Delivered = Math.max(userB_Delivered, delivered);
            userB_Read = Math.max(userB_Read, read);
        }
    }
}
