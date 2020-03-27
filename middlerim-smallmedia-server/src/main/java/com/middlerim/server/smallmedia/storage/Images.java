package com.middlerim.server.smallmedia.storage;

import com.middlerim.message.Outbound;
import com.middlerim.server.smallmedia.Config;
import com.middlerim.server.smallmedia.message.Image;
import com.middlerim.session.SessionId;
import com.middlerim.storage.persistent.FixedLayoutStorageInformation;
import com.middlerim.storage.persistent.VariableLayoutPersistentStorage;

public final class Images {
  private static final int shiftSize = 16; // About 65sec
  private static final VariableLayoutPersistentStorage<Image.In> persistentStorage = new VariableLayoutPersistentStorage<>(
      new FixedLayoutStorageInformation<Image.In>("images",
          Image.In.SERIALIZED_BYTE_SIZE,
          Image.In.SERIALIZED_BYTE_SIZE * (Config.TEST ? 10_000 : SessionId.MAX_USER_SIZE), (Config.TEST ? 100_000 : 100_000_000)));

  // 63 - 32 bits for user ID
  // 31 - 16 bits for server sequence No
  // 15 - 08 bits [reserved]
  // 07 - 00 bits for created time
  private static Long makeKey(long userId, short sequenceNo) {
    byte time = (byte) (System.currentTimeMillis() >>> shiftSize);
    return ((long) userId) << 32 | sequenceNo << 16 | time;
  }

  public static Outbound putFragment(long userId, int tag, Image.In image) {
    persistentStorage.put(image);
    return null;
  }
}
