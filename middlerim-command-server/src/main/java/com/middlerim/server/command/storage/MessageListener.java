package com.middlerim.server.command.storage;

import com.middlerim.message.Outbound;
import com.middlerim.session.Session;

public interface MessageListener {

  void retry(Session recipient, Outbound message);

  void unreached(long userId, Outbound message);
}
