package com.middlerim.server;

public final class Headers {

  private Headers() {
  }

  // --- Control command.

  /**
   * Exit the process.
   */
  public static final byte EXIT = 0b0000001;

  /**
   * Received the message.
   */
  public static final byte RECEIVED = 0b0000010;

  /**
   * Re-send previous message again.
   */
  public static final byte AGAIN = 0b0000011;

  /**
   * Assign Anonymous-ID.
   */
  public static final byte ASSIGN_AID = 0b0000100;

  /**
   * Update Anonymous-ID.
   */
  public static final byte UPDATE_AID = 0b0000101;

  /**
   * Unexpected error occurred.
   */
  public static final byte ERROR = 0b0000111;

  /**
   * Location mask.
   */
  public static final byte LOCATION = 0b0001000;

  /**
   * Text mask.
   */
  public static final byte TEXT = 0b0010000;

  /**
   * Fragment mask.
   */
  public static final byte FRAGMENT = 0b0100000;

  /**
   * Completed sending all payload mask.
   */
  public static final byte COMPLETE = 0b1000000;

  public static byte mask(byte... headers) {
    byte b = headers[0];
    for (int i = 1; i < headers.length; i++) {
      b |= headers[i];
    }
    return b;
  }

  public static boolean isMasked(byte header, byte mask) {
    return (header & mask) == mask;
  }

  public static boolean hasData(byte header) {
    return (header & 0b11111000) != 0;
  }
}
