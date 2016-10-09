package com.middlerim.client.storage;

import com.middlerim.message.Outbound;

public final class MessageStorage {

    private static Outbound lastSentMessage;

    public static Outbound getSentMessage(short sequenceNo) {
        return lastSentMessage;
    }

    public static void putMessage(short sequenceNo, Outbound message) {
        lastSentMessage = message;
    }
}
