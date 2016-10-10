package com.middlerim.session;

public final class SessionId {
  public static final SessionId ANONYMOUS = new SessionId(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});

  static final byte DEFAULT = 0;
  static final byte NEW = 1;
  byte status = 0;

  public static final long MAX_USER_SIZE = 42_949_672_95l;
  public static final long ANONYMOUS_USER_FROM = MAX_USER_SIZE - 2_000_000_000;
  public static final long ANONYMOUS_USER_TO = MAX_USER_SIZE;

  private final long userId; // 0 < MAX_USER_SIZE
  private final int hashCode;
  private short sequenceNo; // Short.MIN < Short.MAX
  private short retryCount;

  public SessionId(long id) {
    this.userId = id;
    this.hashCode = (int) (userId ^ (userId >>> 32));
    this.retryCount = 0;
    this.sequenceNo = (short) (Math.random() * Short.MAX_VALUE); // Assign random sequence no at first to prevent invalid access.
  }

  public SessionId(byte[] n) {
    this.userId = (((long) n[0] & 0xff) << 24) |
        (((long) n[1] & 0xff) << 16) |
        (((long) n[2] & 0xff) << 8) |
        (((long) n[3] & 0xff));
    this.hashCode = (int) (userId ^ (userId >>> 32));
    // n[4] is reserved.
    this.retryCount = (short) n[5];
    this.sequenceNo = (short) ((n[6] << 8) | (n[7] & 0xff));
  }

  public boolean validateSequenceNoAndRefresh(short sequenceNo) {
    if (this == ANONYMOUS || status == NEW || this.sequenceNo + 1 == sequenceNo) {
      this.sequenceNo = sequenceNo;
      status = DEFAULT;
      return true;
    }
    return false;
  }

  public boolean validateAndRefresh(SessionId sessionId) {
    if (this == ANONYMOUS || userId == sessionId.userId && (status == NEW || (short) (this.sequenceNo + 1) == sessionId.sequenceNo)) {
      this.sequenceNo = sessionId.sequenceNo;
      status = DEFAULT;
      return true;
    }
    return false;
  }

  public long userId() {
    return userId;
  }

  public short sequenceNo() {
    return sequenceNo;
  }

  public void readBytes(byte[] bytes) {
    bytes[0] = (byte) (userId >>> 24);
    bytes[1] = (byte) (userId >>> 16);
    bytes[2] = (byte) (userId >>> 8);
    bytes[3] = (byte) userId;
    bytes[4] = 0;
    bytes[5] = 0;
    bytes[6] = (byte) (sequenceNo >>> 8);
    bytes[7] = (byte) sequenceNo;
  }

  public void incrementSequenceNo() {
    retryCount = 0;
    sequenceNo++;
    status = DEFAULT;
  }

  public short retry() {
    sequenceNo--;
    return ++retryCount;
  }

  @Override
  public boolean equals(Object obj) {
    return userId == ((SessionId) obj).userId;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public String toString() {
    return userId + "#" + sequenceNo;
  }
}
