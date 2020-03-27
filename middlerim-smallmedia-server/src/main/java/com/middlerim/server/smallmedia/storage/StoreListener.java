package com.middlerim.server.smallmedia.storage;

import com.middlerim.message.Outbound;
import com.middlerim.session.Session;

public interface StoreListener {

  void retry(Session recipient, Outbound message);

  void unreached(long userId, Outbound message);
}
