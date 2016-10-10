package com.middlerim.server.storage.persistent;

import java.nio.ByteBuffer;

public interface Persistent {

  long id();
  void read(ByteBuffer buf);
}
