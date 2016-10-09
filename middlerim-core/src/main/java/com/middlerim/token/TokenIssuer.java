package com.middlerim.token;

import java.nio.charset.Charset;

import com.middlerim.session.SessionId;
import com.middlerim.util.Bytes;

public final class TokenIssuer {

  private static final Charset CHARSET = Charset.forName("utf-8");

  public static String issueToken(long sessionId) {
    return String.valueOf(sessionId);
  }

  public static byte[] encode(String decoded) {
    return decoded.getBytes(CHARSET);
  }
  public static SessionId decodeTosessionId(byte[] encoded) {
    return new SessionId(encoded);
  }

  public static byte[] getBytes(String token) {
    return Bytes.longToBytes(Long.parseLong(token));
  }
}
