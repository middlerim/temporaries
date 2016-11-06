package com.middlerim.server.storage;

import com.middlerim.message.Outbound;
import com.middlerim.session.Session;

public interface MessageListener {

  void retry(Session recipient, Outbound message);

  void unreached(long userId, Outbound message);
}
