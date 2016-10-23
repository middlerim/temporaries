package com.middlerim.util;

import java.nio.ByteBuffer;

public final class Bytes {
  private Bytes() {
  }

  public static int bytesToInt(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getInt();
  }

  public static byte[] longToBytes(long value) {
    byte[] bytes = new byte[8];
    ByteBuffer.wrap(bytes).putLong(value);
    return bytes;
  }

  public static long bytesToLong(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getLong();
  }

  public static long intToLong(byte[] bytes) {
    byte[] bs = new byte[] {0, 0, 0, 0, bytes[0], bytes[1], bytes[2], bytes[3]};
    return ByteBuffer.wrap(bs).getLong();
  }

  public static byte[] doubleToBytes(double value) {
    byte[] bytes = new byte[8];
    ByteBuffer.wrap(bytes).putDouble(value);
    return bytes;
  }

  public static double bytesToDouble(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getDouble();
  }

  public static byte[] reverse(byte[] bytes) {
    for (int i = 0; i < bytes.length / 2; i++) {
      byte temp = bytes[i];
      bytes[i] = bytes[bytes.length - i - 1];
      bytes[bytes.length - i - 1] = temp;
    }
    return bytes;
  }
}
