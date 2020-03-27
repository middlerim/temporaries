package com.middlerim.server.command.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.middlerim.message.Outbound;
import com.middlerim.server.command.Config;
import com.middlerim.server.storage.Sessions;
import com.middlerim.session.Session;

public final class Messages {
  private static Timer timer;
  private static final int shiftSize = 16; // About 65sec
  private static ConcurrentLinkedQueue<Key> keys = new ConcurrentLinkedQueue<>();
  private static ConcurrentHashMap<Long, Outbound> messages = new ConcurrentHashMap<>();

  private static List<MessageListener> listeners = new ArrayList<>();

  private static final class Key {
    final long key;
    int retryCount;
    Key(long key) {
      this.key = key;
    }
  }

  static {
    timer = new Timer(Config.INTERNAL_APP_NAME + "-message-cleaner", true);
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        byte currentTime = (byte) (System.currentTimeMillis() >>> shiftSize);
        while (true) {
          Key last = keys.peek();
          if (last == null) {
            return;
          }
          byte time = (byte) last.key;
          int diffMins = currentTime - time;
          if (diffMins < 1) {
            break;
          }
          Outbound message = messages.get(last.key);
          if (message == null) {
            keys.poll();
            continue;
          }
          last.retryCount++;
          if (diffMins >= Config.UNREACHED_MESSAGE_SURVIVAL_MINS || last.retryCount > Config.UNREACHED_MESSAGE_MAX_RETRY_COUNT) {
            unreached(keys.poll().key);
          } else {
            Session recipient = Sessions.getSession(keyToUserId(last.key));
            if (recipient != null) {
              retry(recipient, message);
            } else {
              // The session has been expired already.
              unreached(keys.poll().key);
            }
          }
        }
      }
    }, Config.UNREACHED_MESSAGE_RETRY_PERIOD_MILLIS * 2, Config.UNREACHED_MESSAGE_RETRY_PERIOD_MILLIS);
  }

  // 63 - 32 bits for user ID
  // 31 - 16 bits for server sequence No
  // 15 - 08 bits [reserved]
  // 07 - 00 bits for created time
  private static Long makeKey(long userId, short sequenceNo) {
    byte time = (byte) (System.currentTimeMillis() >>> shiftSize);
    return ((long) userId) << 32 | sequenceNo << 16 | time;
  }

  public static Outbound getMessage(long userId, short sequenceNo) {
    return messages.get(makeKey(userId, sequenceNo));
  }

  public static Outbound putMessage(long userId, short sequenceNo, Outbound message) {
    Long key = makeKey(userId, sequenceNo);
    Outbound outbound = messages.put(key, message);
    if (outbound == null) {
      keys.offer(new Key(key));
      if (keys.size() >= Config.MAX_MESSAGE_CACHE_SIZE) {
        unreached(keys.poll().key);
      }
    }
    return outbound;
  }

  public static void removeMessage(long userId, short sequenceNo) {
    messages.remove(makeKey(userId, sequenceNo));
  }

  public static void addListener(MessageListener l) {
    listeners.add(l);
  }

  private static void retry(Session recipient, Outbound message) {
    for (MessageListener l : listeners) {
      l.retry(recipient, message);
    }
  }

  private static void unreached(long key) {
    Outbound message = messages.remove(key);
    if (message == null) {
      return;
    }
    long userId = keyToUserId(key);
    for (MessageListener l : listeners) {
      l.unreached(userId, message);
    }
  }

  private static long keyToUserId(long key) {
    return key >>> 32;
  }
}
