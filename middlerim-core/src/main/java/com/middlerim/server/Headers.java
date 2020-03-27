package com.middlerim.server;

import java.util.HashMap;
import java.util.Map;

public final class Headers {
  private static final byte MASK_COMMAND = 0b1000;
  private static final byte MASK_SMALL_MEDIA = 0b1000;

  private static final Map<Byte, Header> headers = new HashMap<>();

  public interface Header {

  }

  /**
   * Control headers.
   */
  public enum Control implements Header {
      /**
       * Received the message.
       */
      RECEIVED(0b0000001),

      /**
       * Exit the process.
       */
      EXIT(0b0000010),

      /**
       * Re-send the previous message again from a client.
       */
      AGAIN(0b0000011),

      /**
       * Assign Anonymous-ID.
       */
      ASSIGN_AID(0b0000100),

      /**
       * Update Anonymous-ID.
       */
      UPDATE_AID(0b0000101),

      /**
       * Unexpected error occurred.
       */
      ERROR(0b0000111);

    public final byte code;
    private Control(int b) {
      if (b > 7) {
        throw new IllegalArgumentException();
      }
      this.code = (byte) b;
      if (headers.put(this.code, this) != null) {
        throw new IllegalArgumentException();
      }
    }
  }

  public enum Command implements Header {

      /**
       * Location.
       */
      LOCATION(mask(MASK_COMMAND, (byte) 0b10)),

      /**
       * Text.
       */
      TEXT(mask(MASK_COMMAND, (byte) 0b100)),

      /**
       * Text Received.
       */
      TEXT_RECEIVED(mask(MASK_COMMAND, Control.RECEIVED.code, (byte) 0b100));

    public final byte code;
    private Command(byte b) {
      this.code = b;
      if (!isMasked(code, MASK_COMMAND)) {
        throw new IllegalArgumentException();
      }
      if (headers.put(code, this) != null) {
        throw new IllegalArgumentException();
      }
    }
  }

  public enum SmallMedia implements Header {

      /**
       * Image.
       */
      IMAGE(mask(MASK_SMALL_MEDIA, (byte) 0b10)),

      /**
       * Image Received.
       */
      IMAGE_RECEIVED(mask(MASK_SMALL_MEDIA, Control.RECEIVED.code, (byte) 0b10));

    public final byte code;
    private SmallMedia(byte b) {
      this.code = b;
      if (!isMasked(code, MASK_SMALL_MEDIA)) {
        throw new IllegalArgumentException();
      }
      if (headers.put(code, this) != null) {
        throw new IllegalArgumentException();
      }
    }
  }

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

  public boolean hasData(byte header) {
    return (header & 0b11111000) != 0;
  }

  public static Header parse(byte b) {
    return headers.get(b);
  }
}
