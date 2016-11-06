package com.middlerim.android.ui;

import com.middlerim.location.Coordinate;

import java.text.NumberFormat;

public class Message {
    private static NumberFormat numberFormat = NumberFormat.getIntegerInstance();

    public final long userId;
    public final Coordinate location;
    public final String displayName;
    public final CharSequence message;
    public final String numberOfDelivery;

    public Message(long userId, Coordinate location, String displayName, CharSequence message, int numberOfDelivery) {
        this.userId = userId;
        this.location = location;
        this.displayName = displayName;
        this.message = message;
        if (numberOfDelivery >= 0) {
            this.numberOfDelivery = numberFormat.format(numberOfDelivery);
        } else {
            this.numberOfDelivery = null;
        }
    }

    @Override
    public String toString() {
        return "user ID: " + userId + ", location: " + location + ", displayName: " + displayName + ", message: " + message + ", number of delivery: " + numberOfDelivery;
    }
}
