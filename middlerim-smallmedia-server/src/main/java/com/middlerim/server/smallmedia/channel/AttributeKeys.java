package com.middlerim.server.smallmedia.channel;

import com.middlerim.session.Session;

import io.netty.util.AttributeKey;

public final class AttributeKeys {

  public static final AttributeKey<Session> SESSION = AttributeKey.valueOf("session");
}
