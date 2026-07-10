package com.friendlywings.automation.model;

import java.time.LocalDateTime;

public abstract class Booking {

    private String source;
    private String bookingType;
    private String rawMessageId;
    private LocalDateTime receivedAt;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getBookingType() {
        return bookingType;
    }

    public void setBookingType(String bookingType) {
        this.bookingType = bookingType;
    }

    public String getRawMessageId() {
        return rawMessageId;
    }

    public void setRawMessageId(String rawMessageId) {
        this.rawMessageId = rawMessageId;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
