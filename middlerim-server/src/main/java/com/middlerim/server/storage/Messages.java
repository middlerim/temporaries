package com.middlerim.server.storage;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.middlerim.message.Outbound;
import com.middlerim.server.Config;

public final class Messages {

  private static final int shiftSize = 16; // About 65sec
  private static LinkedList<Long> keys = new LinkedList<>();
  private static Map<Long, Outbound> messages = new ConcurrentHashMap<>();

  private static Long makeKey(long userId, short sequenceNo) {
    byte time = (byte) (System.currentTimeMillis() >>> shiftSize);
    return ((long) userId) << 24 | sequenceNo << 8 | time;
  }

  public static Outbound getMessage(long userId, short sequenceNo) {
    return messages.get(makeKey(userId, sequenceNo));
  }

  public static Outbound putMessage(long userId, short sequenceNo, Outbound message) {
    Long key = makeKey(userId, sequenceNo);
    Outbound outbound = messages.put(key, message);
    if (outbound == null) {
      keys.add(key);
      if (keys.size() >= Config.MAX_MESSAGE_CACHE_SIZE) {
        messages.remove(keys.poll());
      }
      byte currentTime = (byte) (System.currentTimeMillis() >>> shiftSize);
      while (true) {
        Long last = keys.getLast();
        byte time = last.byteValue(); // Get the most right byte.
        // Remove messages from cache around 5 minutes later.
        if ((currentTime - time) >= 5) {
          messages.remove(keys.poll());
        } else {
          break;
        }
      }
    }
    return outbound;
  }

  public static void removeMessage(long userId, short sequenceNo) {
    messages.remove(makeKey(userId, sequenceNo));
  }
}
