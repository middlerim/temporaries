package com.middlerim.console;

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
import com.middlerim.client.Config;
import com.middlerim.client.view.Logger;
import com.middlerim.client.view.MessagePool;
import com.middlerim.client.view.ViewContext;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.location.Coordinate;
import com.middlerim.server.MessageCommands;

public class ConsoleClient {

  private static class Message {
    long userId;
    Coordinate location;
    String displayName;
    CharSequence content;
    int numberOfDelivery;
  }

  private static MessagePool.Adapter<Message> messagePoolAdaptor = new MessagePool.Adapter<Message>() {
    public Message onReceive(long userId, Coordinate location, String displayName, ByteBuffer message, int numberOfDelivery) {
      Message msg = new Message();
      msg.userId = userId;
      msg.location = location;
      msg.displayName = displayName;
      byte[] bs = new byte[message.remaining()];
      message.get(bs);
      msg.content = new String(bs, Config.MESSAGE_ENCODING);
      msg.numberOfDelivery = numberOfDelivery;

      System.out.println("New message -------");
      System.out.println(msg.userId + ": " + msg.displayName);
      System.out.println(msg.content);
      System.out.println("( Location: " + msg.location);
      System.out.println("  number of Delivery: " + msg.numberOfDelivery + ")");
      return msg;
    }
  };

  private static MessagePool<Message> messagePool = new MessagePool<>(messagePoolAdaptor);

  private static class ConsoleLogger implements Logger {
    @Override
    public void warn(String tag, String message) {
      System.out.println("[" + tag + "] WARN " + message);
    }

    @Override
    public void debug(String tag, String message) {
      System.out.println("[" + tag + "] DEBUG " + message);
    }
  }

  public static class ConsoleContext extends ViewContext {
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
  private static String displayName = "ミドルリムクライアント一号";

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
    messagePool.onRemoved(new MessagePool.RemovedListener<Message>() {
      @Override
      public void onRemoved(int i, Message message) {
        System.out.println("Removed from local message buffer: " + message.userId + ", " + message.displayName + ", " + message.content);
      }
    });
    ViewEvents.fireCreate();
    ViewEvents.fireResume();
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
          ViewEvents.fireSubmitMessage(displayName, MessageCommands.areaKM(10), buf);
        }
      }
    });
    ViewEvents.fireSubmitMessage(displayName, MessageCommands.areaKM(10), ByteBuffer.wrap(new byte[]{'a', 'b', 'c', (byte) c.getCount()}));
    boolean result = c.await(10, TimeUnit.SECONDS);
    System.out.println((System.currentTimeMillis() - start) + "ms" + "[" + result + "]");
    System.out.println("Received message count: " + receivedMessageCount);
  }

  public static void readConsole() throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    while (interactive(br)) {
      System.out.println("Received message count: " + receivedMessageCount);
    }
    if (CentralServer.isStarted()) {
      Thread.sleep(1000);
    }
  }

  private static boolean interactive(BufferedReader br) {
    try {
      System.out.println("[Location: A | Text: B | Stress: C | Shutdown: Z] >");
      String s = br.readLine();
      if ("A".equals(s)) {
        System.out.println("Enter Point >");
        String point = br.readLine();
        String[] points = point.split(":");
        ViewEvents.fireLocationUpdate(new Coordinate(Double.parseDouble(points[0]), Double.parseDouble(points[1])));
      } else if ("B".equals(s)) {
        System.out.println("Enter Message >");
        String message = br.readLine();
        byte[] messageBytes = message.getBytes(Charset.forName("utf-8"));
        ByteBuffer buf = ByteBuffer.allocate(messageBytes.length);
        buf.put(messageBytes).rewind();
        System.out.println("Sending text size: " + messageBytes.length + ", remaining: " + buf.remaining());
        ViewEvents.fireSubmitMessage(displayName, MessageCommands.areaKM(10), buf);
      } else if ("C".equals(s)) {
        System.out.println("Enter messages count >");
        String size = br.readLine();
        stressTest(Integer.parseInt(size));
      } else if ("Z".equals(s)) {
        ViewEvents.fireDestroy();
        return false;
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return interactive(br);
    }
  }
}
