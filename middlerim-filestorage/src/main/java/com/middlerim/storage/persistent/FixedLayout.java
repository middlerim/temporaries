package com.middlerim.storage.persistent;

import java.nio.ByteBuffer;

public interface FixedLayout extends Persistent {
  long id();
  void read(ByteBuffer buf);
  void write(ByteBuffer buf);
}
