package com.middlerim.server;

public final class MessageCommands {

  public static final int MIN_METER = 32;

  private MessageCommands() {
  }

  public static final byte AREA_MASK = 0b11111;
  public static final byte AREA_UNIT_MASK = 0b1;
  public static final byte AREA_M = 0b0;
  public static final byte AREA_KM = 0b1;

  public static byte areaM(int radius) {
    if (radius < MIN_METER) {
      throw new IllegalStateException("Radius for meter must be over or equal to 32");
    }
    return area(radius, AREA_M);
  }

  public static byte areaKM(int radius) {
    return area(radius, AREA_KM);
  }

  public static boolean isArea(byte command) {
    return (command & AREA_MASK) != 0;
  }

  public static int toMeter(byte command) {
    int unit = ((byte) (command & AREA_UNIT_MASK) == AREA_KM) ? 1000 : 1;
    int meter = (((command >>> 1) << 3) + 1) * unit;
    if (meter <= MIN_METER) {
      throw new IllegalStateException("unit:" + unit + ", meter: " + meter);
    }
    return meter;
  }

  private static byte area(int radius, byte unitMask) {
    if (radius > 1000) {
      throw new IllegalArgumentException("Radius must be under 1000: " + radius);
    }
    return (byte) (((radius >>> 3) << 1) | unitMask);
  }
}
