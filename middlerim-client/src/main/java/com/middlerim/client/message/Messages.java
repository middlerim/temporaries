package com.middlerim.client.message;

import java.util.HashMap;
import java.util.Map;

import com.middlerim.message.Outbound;

public final class Messages {

  private static Map<Short, OutboundMessage<? extends Outbound>> lastSentMessage = new HashMap<>();

  static OutboundMessage<? extends Outbound> removeSentMessage(short sequenceNo) {
    return lastSentMessage.remove(sequenceNo);
  }

  public static OutboundMessage<? extends Outbound> getSentMessage(short sequenceNo) {
    return lastSentMessage.get(sequenceNo);
  }

  static void putMessage(short sequenceNo, OutboundMessage<? extends Outbound> message) {
    lastSentMessage.put(sequenceNo, message);
  }
}
