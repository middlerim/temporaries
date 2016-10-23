package com.middlerim.server;

public final class MessageCommands {

  public static final int MIN_METER = 32;

  private MessageCommands() {
  }

  private static final byte AREA_UNIT_MASK = 0b11;
  private static final byte AREA_M = 0b00;
  private static final byte AREA_100M = 0b01;
  private static final byte AREA_KM = 0b10;

  public static byte areaM(int radius) {
    if (radius < MIN_METER) {
      throw new IllegalStateException("Radius for meter must be over or equal to 32");
    }
    if (radius < 100) {
      return area(radius, AREA_M);
    } else {
      return area(radius, AREA_100M);
    }
  }

  public static byte areaKM(int radius) {
    return area(radius, AREA_KM);
  }

  public static boolean isArea(byte command) {
    return (command & AREA_UNIT_MASK) <= 2;
  }

  public static int toMeter(byte command) {
    byte areaUnit = (byte) (command & AREA_UNIT_MASK);
    int meter = 0;
    switch(areaUnit) {
      case AREA_M:
        meter = ((command >>> 2) << 3) + 1;
        break;
      case AREA_100M:
        meter = (((command >>> 2) << 3) + 1) * 10 + 10;
        break;
      case AREA_KM:
        meter = (((command >>> 2) << 3) + 1) * 1000;
        break;
    }
    if (meter <= MIN_METER) {
      throw new IllegalStateException("unit:" + areaUnit + ", meter: " + meter);
    }
    return meter;
  }

  private static byte area(int radius, byte unitMask) {
    if (radius > 1000) {
      throw new IllegalArgumentException("Radius must be under 1000: " + radius);
    }
    if (unitMask == AREA_100M) {
      radius = radius / 10;
    }
    return (byte) (((radius >>> 3) << 2) | unitMask);
  }
}
