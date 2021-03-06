package com.middlerim.session;

public final class SessionId {
  public static final SessionId ANONYMOUS = new SessionId(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});

  public static final byte DEFAULT = 0;
  public static final byte NEW = 1;
  byte status = 0;

  public static final long UNASSIGNED_USERID = 0;
  public static final byte UNASSIGNED_SEQUENCE_NO = 1;
  public static final long MAX_USER_SIZE = 4_294_967_295L;
  public static final int ANONYMOUS_USER_FROM = 1;
  public static final int ANONYMOUS_USER_TO = 2_000_000_000;

  private final int userId; // 0 < MAX_USER_SIZE
  private byte clientSequenceNo; // Byte.MIN < Byte.MAX
  private short serverSequenceNo; // Short.MIN < Short.MAX

  public SessionId(long id) {
    if (id > MAX_USER_SIZE) {
      throw new IllegalArgumentException();
    }
    this.userId = (int) id;

    // Assign random sequence no at first to prevent invalid access.
    while (true) {
      this.clientSequenceNo = (byte) (Math.random() * Byte.MAX_VALUE);
      if (this.clientSequenceNo != SessionId.UNASSIGNED_SEQUENCE_NO) {
        break;
      }
    }
    while (true) {
      this.serverSequenceNo = (short) (Math.random() * Short.MAX_VALUE);
      if (this.serverSequenceNo != SessionId.UNASSIGNED_SEQUENCE_NO) {
        break;
      }
    }
  }

  public SessionId(byte[] n) {
    this.userId = (int) ((((long) n[0] & 0xff) << 24) |
        (((long) n[1] & 0xff) << 16) |
        (((long) n[2] & 0xff) << 8) |
        (((long) n[3] & 0xff)));
    // n[4] is reserved.
    this.clientSequenceNo = n[5];
    this.serverSequenceNo = (short) ((n[6] << 8) | (n[7] & 0xff));
  }

  public boolean validateServerSequenceNoAndRefresh(short sequenceNo) {
    if (this == ANONYMOUS || status == NEW || (short) (this.serverSequenceNo + 1) == sequenceNo) {
      this.serverSequenceNo = sequenceNo;
      return true;
    }
    return false;
  }

  public boolean validateClientAndRefresh(SessionId sessionId) {
    if (this == ANONYMOUS || userId == sessionId.userId && (status == NEW || (byte) (this.clientSequenceNo + 1) == sessionId.clientSequenceNo)) {
      this.clientSequenceNo = sessionId.clientSequenceNo;
      return true;
    }
    return false;
  }

  public boolean validateClient(SessionId sessionId) {
    if (sessionId == ANONYMOUS
        || (this.clientSequenceNo != sessionId.clientSequenceNo && (byte) (this.clientSequenceNo + 1) != sessionId.clientSequenceNo)) {
      return false;
    }
    return true;
  }

  public long userId() {
    if (userId < 0) {
      return MAX_USER_SIZE + userId + 1;
    }
    return userId;
  }

  public short serverSequenceNo() {
    return serverSequenceNo;
  }

  public byte clientSequenceNo() {
    return clientSequenceNo;
  }

  public void synchronizeClientWithServer(byte newSequenceNo) {
    clientSequenceNo = newSequenceNo;
  }

  public void readUserIdBytes(byte[] bytes) {
    bytes[0] = (byte) (userId >>> 24);
    bytes[1] = (byte) (userId >>> 16);
    bytes[2] = (byte) (userId >>> 8);
    bytes[3] = (byte) userId;
  }

  public void readBytes(byte[] bytes) {
    bytes[0] = (byte) (userId >>> 24);
    bytes[1] = (byte) (userId >>> 16);
    bytes[2] = (byte) (userId >>> 8);
    bytes[3] = (byte) userId;
    bytes[4] = 0;
    bytes[5] = clientSequenceNo;
    bytes[6] = (byte) (serverSequenceNo >>> 8);
    bytes[7] = (byte) serverSequenceNo;
  }

  public byte incrementClientSequenceNo() {
    return ++clientSequenceNo;
  }

  public short incrementServerSequenceNo() {
    return ++serverSequenceNo;
  }

  public SessionId copyWithNewUserId(byte[] userId) {
    byte[] bytes = new byte[8];
    readBytes(bytes);
    bytes[0] = userId[0];
    bytes[1] = userId[1];
    bytes[2] = userId[2];
    bytes[3] = userId[3];
    SessionId copied = new SessionId(bytes);
    return copied;
  }

  public SessionId copyWithNewServerSequenceNo(short sequenceNo) {
    byte[] bytes = new byte[8];
    readBytes(bytes);
    SessionId copied = new SessionId(bytes);
    copied.serverSequenceNo = sequenceNo;
    return copied;
  }

  @Override
  public boolean equals(Object obj) {
    return userId == ((SessionId) obj).userId;
  }

  @Override
  public int hashCode() {
    return userId;
  }

  @Override
  public String toString() {
    return userId + "#" + serverSequenceNo + "#" + clientSequenceNo;
  }
}
