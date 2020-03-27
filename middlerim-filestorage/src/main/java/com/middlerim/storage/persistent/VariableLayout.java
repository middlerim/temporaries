package com.middlerim.storage.persistent;

import java.nio.ByteBuffer;

public interface VariableLayout extends Persistent {

  long id();
  int length();
  ByteBuffer read();
  void write(ByteBuffer buf);
}
