package com.middlerim.client.channel;

import com.middlerim.message.Outbound;

import io.netty.util.AttributeKey;

public final class AttributeKeys {

    public static final AttributeKey<Outbound> MESSAGE = AttributeKey.valueOf("message");
}
