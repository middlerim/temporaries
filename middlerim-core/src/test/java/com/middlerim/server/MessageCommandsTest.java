package com.middlerim.server;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class MessageCommandsTest {

  @Test
  public void testMeter() {

    for (int i = 32; i < 100; i += 8) {
      byte area = MessageCommands.areaM(i);
      assertTrue(Byte.toString(area), MessageCommands.isArea(area));
      int meter = MessageCommands.toMeter(area);
      System.out.println(i + "m become " + meter);
      assertThat(meter + ":" + i, meter, is(i + 1));
    }
  }

  @Test
  public void testKMeter() {

    for (int i = 0; i < 100; i += 8) {
      byte area = MessageCommands.areaKM(i);
      assertTrue(MessageCommands.isArea(area));
      int meter = MessageCommands.toMeter(area);
      System.out.println(i + "km become " + meter);
      assertThat(meter + ":" + i, meter, is((i + 1) * 1000));
    }
  }
}
