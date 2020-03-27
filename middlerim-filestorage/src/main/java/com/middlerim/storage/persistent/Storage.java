package com.middlerim.storage.persistent;

import java.io.Closeable;

interface Storage<L extends Persistent> extends Closeable {
  void put(L layout);

  L get(Long id, L layout);

  void delete(Long id);
}
