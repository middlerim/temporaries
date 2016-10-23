package com.middlerim.session;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class SessionIdTest {

  @Test
  public void testCompareSessionIds() {
    byte[] bs = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
    SessionId sessionId1 = new SessionId(bs);
    assertThat(sessionId1, is(new SessionId(bs)));
    // Same user ID but different sequence no.
    assertThat(sessionId1, is(new SessionId(new byte[]{1, 2, 3, 4, 5, 6, 7, 9})));
    // Difference user ID but same sequence no.
    assertThat(sessionId1, not(new SessionId(new byte[]{2, 2, 3, 4, 5, 6, 7, 8})));
  }

  @Test
  public void testDecodeEncode() {
    byte[] bs = new byte[]{1, 2, 3, 4, 0, 0, 7, 8};
    SessionId sessionId = new SessionId(bs);
    byte[] bs2 = new byte[8];
    sessionId.readBytes(bs2);
    assertThat(bs, is(bs2));
    assertThat(sessionId, is(new SessionId(bs2)));
  }

  @Test
  public void testIncrementAllClientSequenceNo() {
    byte[] bs = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
    SessionId server = new SessionId(bs);
    SessionId client = new SessionId(bs);

    for (int i = 0, max = (Byte.MAX_VALUE * 2) + 1; i < max; i++) {
      client.incrementClientSequenceNo();
      client.readBytes(bs);
      client = new SessionId(bs);

      assertTrue(Arrays.toString(bs) + server.toString() + " - " + client.toString(), server.validateClientAndRefresh(client));
      server.readBytes(bs);
      server = new SessionId(bs);
    }
  }

  @Test
  public void testIncrementAllServerSequenceNo() {
    byte[] bs = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
    SessionId server = new SessionId(bs);
    SessionId client = new SessionId(bs);

    for (int i = 0, max = (Short.MAX_VALUE * 2) + 1; i < max; i++) {
      server.incrementServerSequenceNo();
      server.readBytes(bs);
      server = new SessionId(bs);

      assertTrue(Arrays.toString(bs) + client.toString() + " - " + server.toString(), client.validateServerSequenceNoAndRefresh(server.serverSequenceNo()));
      client.readBytes(bs);
      client = new SessionId(bs);
    }
  }
  
  @Test
  public void testMaxUserId() {
    long userIdMax = 4294967295L;
    byte[] bs = new byte[]{-1, -1, -1, -1, 0, 0, 0, 0};
    SessionId sessionId = new SessionId(bs);
    assertThat(sessionId.userId(), is(userIdMax));
    byte[] bs2 = new byte[8];
    sessionId.readBytes(bs2);
    assertThat(bs, is(bs2));

    sessionId = new SessionId(userIdMax);
    assertThat(sessionId.userId(), is(userIdMax));
  }

  @Test
  public void testMinUserId() {
    long userIdMin = 0L;
    byte[] bs = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
    SessionId sessionId = new SessionId(bs);
    assertThat(sessionId.userId(), is(userIdMin));
    byte[] bs2 = new byte[8];
    sessionId.readBytes(bs2);
    assertThat(bs, is(bs2));

    sessionId = new SessionId(userIdMin);
    assertThat(sessionId.userId(), is(userIdMin));
  }

  @Test
  public void testUser1() {
    long userId = 1L;
    byte[] bs = new byte[]{0, 0, 0, 1, 0, 0, 0, 0};
    SessionId sessionId = new SessionId(bs);
    assertThat(sessionId.userId(), is(userId));
    byte[] bs2 = new byte[8];
    sessionId.readBytes(bs2);
    assertThat(bs, is(bs2));

    sessionId = new SessionId(userId);
    assertThat(sessionId.userId(), is(userId));
  }
}
