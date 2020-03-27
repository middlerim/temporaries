package com.middlerim.integration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.middlerim.client.session.Sessions;
import com.middlerim.location.Point;
import com.middlerim.server.MessageCommands;
import com.middlerim.server.command.Config;
import com.middlerim.server.command.storage.Locations;
import com.middlerim.session.Session;
import com.middlerim.session.SessionId;

public class DualStackIpTest extends End2EndTest {

  @BeforeClass
  public static void beforeClass() throws Exception {
    run();
  }

  @Test
  public void testIpMixedCommand() throws Exception {
    updateLocation(MARUNOUCHI_2_4_1_TOKYO);

    Session session = Sessions.getSession();
    Point point = Locations.findBySessionId(session.sessionId);
    assertThat(point, is(MARUNOUCHI_2_4_1_TOKYO.toPoint()));

    SessionId userB = createDummyUser();
    setLocation(userB, MARUNOUCHI_2_4_1_TOKYO, Config.COMMAND_SERVER_IPV4);
    {
      sendMessage(MessageCommands.areaM(32));
      assertThat(ctx.lastReceivedTextEvent.numberOfDelivery, is(1));
    }

    SessionId userC = createDummyUser();
    setLocation(userC, MARUNOUCHI_2_4_1_TOKYO, Config.COMMAND_SERVER_IPV6);
    {
      sendMessage(MessageCommands.areaM(32));
      assertThat(ctx.lastReceivedTextEvent.numberOfDelivery, is(2));
    }
  }
}
