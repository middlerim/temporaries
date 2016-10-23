package com.middlerim.server.storage.persistent;

import java.nio.ByteBuffer;

public interface Persistent<T> {

  long id();
  void read(ByteBuffer buf);
  void write(ByteBuffer buf);
}
