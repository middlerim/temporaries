package com.middlerim.util;

import java.nio.ByteBuffer;

public final class Bytes {
  private Bytes() {
  }

  public static byte[] longToBytes(long value) {
    byte[] bytes = new byte[8];
    ByteBuffer.wrap(bytes).putLong(value);
    return bytes;
  }

  public static long bytesToLong(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getLong();
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
