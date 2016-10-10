package com.middlerim.android.central;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.middlerim.client.CentralEvents;
import com.middlerim.client.CentralServer;
import com.middlerim.client.view.Logger;
import com.middlerim.client.view.ViewContext;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Point;
import com.middlerim.server.MessageCommands;

public class ConsoleClient {

  private static class ConsoleLogger implements Logger {
    @Override
    public void warn(String tag, String message) {
      System.out.println("[" + tag + "]" + message);
    }

    @Override
    public void debug(String tag, String message) {
    }
  }

  private static class ConsoleContext extends ViewContext {
    private final ConsoleLogger logger = new ConsoleLogger();
    private final File tmpDir = new File(".");
    @Override
    public File getCacheDir() {
      return tmpDir;
    }

    @Override
    public Logger logger() {
      return logger;
    }

    @Override
    public boolean isDebug() {
      return true;
    }
  }

  private static int receivedMessageCount = 0;

  public static void main(String[] args) throws Exception {
    ConsoleContext c = new ConsoleContext();
    CentralServer.run(c);
    CentralEvents.onError(new CentralEvents.Listener<CentralEvents.ErrorEvent>() {
      @Override
      public void handle(CentralEvents.ErrorEvent event) {
        System.err.println(event.message);
        event.cause.printStackTrace(System.err);
      }
    });
    CentralEvents.onReceived(new CentralEvents.Listener<CentralEvents.ReceivedEvent>() {
      @Override
      public void handle(CentralEvents.ReceivedEvent event) {
        receivedMessageCount++;
      }
    });
    CentralEvents.onReceiveMessage(new CentralEvents.Listener<CentralEvents.ReceiveMessageEvent>() {
      @Override
      public void handle(CentralEvents.ReceiveMessageEvent event) {
        event.message.position(0);
        byte[] bs = new byte[event.message.remaining()];
        event.message.get(bs);
        System.out.println(new String(bs, Charset.forName("utf-8")));
      }
    });
    ViewEvents.fireCreate();
    readConsole();
  }

  private static void stressTest(final int limit) throws InterruptedException, ExecutionException, TimeoutException {
    long start = System.currentTimeMillis();
    final CountDownLatch c = new CountDownLatch(limit);
    CentralEvents.onReceived(new CentralEvents.Listener<CentralEvents.ReceivedEvent>() {
      @Override
      public void handle(CentralEvents.ReceivedEvent event) {
        c.countDown();
        if (c.getCount() > 0) {
          final ByteBuffer buf = ByteBuffer.wrap(new byte[]{'a', 'b', 'c', (byte) c.getCount()});
          ViewEvents.fireSubmitMessage(buf, MessageCommands.areaKM(10));
        }
      }
    });
    ViewEvents.fireSubmitMessage(ByteBuffer.wrap(new byte[]{'a', 'b', 'c', (byte) c.getCount()}), MessageCommands.areaKM(10));
    boolean result = c.await(10, TimeUnit.SECONDS);
    System.out.println((System.currentTimeMillis() - start) + "ms" + "[" + result + "]");
    System.out.println("Received message count: " + receivedMessageCount);
  }

  public static void readConsole() throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      interactive(br);
      System.out.println("Received message count: " + receivedMessageCount);
      if (!CentralServer.isStarted()) {
        break;
      }
    }
  }

  private static void interactive(BufferedReader br) throws Exception {
    System.out.println("[Location: A | Text: B | Stress: C | Shutdown: Z] >");
    String s = br.readLine();
    if ("A".equals(s)) {
      System.out.println("Enter Point >");
      String point = br.readLine();
      String[] points = point.split(":");
      ViewEvents.fireLocationUpdate(new Point((int)(Double.parseDouble(points[0]) * Point.GETA), (int)(Double.parseDouble(points[1]) * Point.GETA)));
    } else if ("B".equals(s)) {
      System.out.println("Enter Message >");
      String message = br.readLine();
      byte[] messageBytes = message.getBytes(Charset.forName("utf-8"));
      ByteBuffer buf = ByteBuffer.allocate(messageBytes.length);
      buf.put(messageBytes).rewind();
      System.out.println("Sending text size: " + messageBytes.length + ", remaining: " + buf.remaining());
      ViewEvents.fireSubmitMessage(buf, MessageCommands.areaKM(10));
    } else if ("C".equals(s)) {
      System.out.println("Enter messages count >");
      String size = br.readLine();
      stressTest(Integer.parseInt(size));
    } else if ("Z".equals(s)) {
      CentralServer.shutdown();
    }
  }
}
