package com.rohit.ChatApplication.data;

import java.util.UUID;

public record ReadReceiptChannelKey(UUID channelId, UUID userId) {
}
