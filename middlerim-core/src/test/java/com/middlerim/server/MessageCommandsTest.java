package com.middlerim.server;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class MessageCommandsTest {

  @Test
  public void testMeter() {

    for (int i = 32; i < 100; i += 8) {
      byte area = MessageCommands.areaM(i);
      assertTrue(i + ":" + Integer.toBinaryString(area), MessageCommands.isArea(area));
      int meter = MessageCommands.toMeter(area);
      System.out.println(i + "m become " + meter);
      assertThat(meter + ":" + i, meter, is(i + 1));
    }
    for (int i = 100; i < 999; i += 80) {
      byte area = MessageCommands.areaM(i);
      assertTrue(i + ":" + Integer.toBinaryString(area), MessageCommands.isArea(area));
      int meter = MessageCommands.toMeter(area);
      System.out.println(i + "m become " + meter);
      assertThat(meter + ":" + i, meter, is(i));
    }
  }

  @Test
  public void testKMeter() {

    for (int i = 1; i < 100; i += 8) {
      byte area = MessageCommands.areaKM(i);
      assertTrue(MessageCommands.isArea(area));
      int meter = MessageCommands.toMeter(area);
      System.out.println(i + "km become " + meter);
      assertThat(meter + ":" + i, meter, is(i * 1000));
    }
  }
}
