package com.middlerim.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.middlerim.client.Config;
import com.middlerim.client.central.CentralEvents;
import com.middlerim.client.central.CentralServer;
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
    public Message onReceive(long userId, Coordinate location, String displayName, byte[] message, int numberOfDelivery) {
      Message msg = new Message();
      msg.userId = userId;
      msg.location = location;
      msg.displayName = displayName;
      msg.content = new String(message, Config.MESSAGE_ENCODING);
      msg.numberOfDelivery = numberOfDelivery;

      System.out.println("New message -------");
      System.out.println(msg.userId + ": " + msg.displayName);
      System.out.println(msg.content);
      System.out.println("( Location: " + msg.location);
      System.out.println("  number of Delivery: " + msg.numberOfDelivery + ")");
      return msg;
    }
    @Override
    public File storage() {
      return Paths.get("./", "db_test").toFile();
    }
  };

  public static class ConsoleContext extends ViewContext {
    private final File tmpDir = new File(".");
    private MessagePool<Message> messagePool = new MessagePool<>(messagePoolAdaptor).startListen();

    @Override
    public File getCacheDir() {
      return tmpDir;
    }

    @Override
    public boolean isDebug() {
      return true;
    }

    @Override
    public MessagePool<Message> getMessagePool() {
      return messagePool;
    }
  }

  private static int receivedMessageCount = 0;
  private static String displayName = "ミドルリムクライアント一号";

  public static void main(String[] args) throws Exception {
    ConsoleContext c = new ConsoleContext();
    CentralServer.run(c);
    CentralEvents.onError("ConsoleClient#main.CentralEvents.Listener<CentralEvents.ErrorEvent>", new CentralEvents.Listener<CentralEvents.ErrorEvent>() {
      @Override
      public void handle(CentralEvents.ErrorEvent event) {
        System.err.println(event.message);
        event.cause.printStackTrace(System.err);
      }
    });
    CentralEvents.onReceived("ConsoleClient#main.CentralEvents.Listener<CentralEvents.ReceivedEvent>", new CentralEvents.Listener<CentralEvents.ReceivedEvent>() {
      @Override
      public void handle(CentralEvents.ReceivedEvent event) {
        receivedMessageCount++;
      }
    });
    c.messagePool.onRemoved(new MessagePool.RemovedListener<Message>() {
      @Override
      public void onRemoved(int i, Message message) {
        // System.out.println("Removed from local message buffer: " + message.userId);
      }
    });
    ViewEvents.fireCreate();
    ViewEvents.fireResume();
    readConsole();
  }

  private static void stressTest(final int limit) throws InterruptedException, ExecutionException, TimeoutException {
    long start = System.currentTimeMillis();
    final CountDownLatch c = new CountDownLatch(limit);
    CentralEvents.onReceivedText("ConsoleClient.CentralEvents.Listener<CentralEvents.ReceivedEvent>", new CentralEvents.Listener<CentralEvents.ReceivedTextEvent>() {
      @Override
      public void handle(CentralEvents.ReceivedTextEvent event) {
        c.countDown();
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          // Ignore
        }
        if (c.getCount() > 0) {
          ViewEvents.fireSubmitMessage(0, displayName, MessageCommands.areaKM(80), new byte[]{'a', 'b', 'c', (byte) c.getCount()});
        }
      }
    });
    ViewEvents.fireSubmitMessage(0, displayName, MessageCommands.areaKM(10), new byte[]{'a', 'b', 'c', (byte) c.getCount()});
    boolean result = c.await(10, TimeUnit.SECONDS);
    System.out.println((System.currentTimeMillis() - start) + "ms" + "[" + result + "]");
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
        String[] points = point.split(", ");
        ViewEvents.fireLocationUpdate(new Coordinate(Double.parseDouble(points[0]), Double.parseDouble(points[1])));
      } else if ("B".equals(s)) {
        System.out.println("Enter Message >");
        String message = br.readLine();
        byte[] messageBytes = message.getBytes(Charset.forName("utf-8"));
        System.out.println("Sending text size: " + messageBytes.length + ", remaining: " + messageBytes.length);
        ViewEvents.fireSubmitMessage(0, displayName, MessageCommands.areaKM(10), messageBytes);
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
