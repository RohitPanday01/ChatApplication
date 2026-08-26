package com.rohit.ChatApplication.service.ReadReciept;

import com.chat.protocol.ReceiptType;
import org.springframework.stereotype.Component;

@Component
public class ReceiptAggregateState {
    private long lastDeliveredSeq;
    private long lastReadSeq;

    public void merge(ReceiptType type, long seq) {
        if (type == ReceiptType.DELIVERED) {
            this.lastDeliveredSeq = Math.max(this.lastDeliveredSeq, seq);
        } else if (type == ReceiptType.READ) {
            this.lastReadSeq = Math.max(this.lastReadSeq, seq);
            // Reading a message sequence implicitly confirms delivery up to that sequence
            this.lastDeliveredSeq = Math.max(this.lastDeliveredSeq, seq);
        }
    }

    public long getLastDeliveredSeq() { return lastDeliveredSeq; }
    public long getLastReadSeq() { return lastReadSeq; }
}
