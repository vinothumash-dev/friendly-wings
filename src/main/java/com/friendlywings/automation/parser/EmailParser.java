package com.friendlywings.automation.parser;

import com.friendlywings.automation.model.Booking;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

public interface EmailParser {

    boolean canParse(Message message) throws MessagingException;

    Booking parse(Message message) throws MessagingException;
}
