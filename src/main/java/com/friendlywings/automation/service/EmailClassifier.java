package com.friendlywings.automation.service;

import com.friendlywings.automation.parser.BookingSource;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;

@Service
public class EmailClassifier {

    public BookingSource classify(Message message) throws MessagingException {
        String from = getFromAddress(message);
        String subject = message.getSubject();
        if (subject == null) subject = "";

        String lowerFrom = from != null ? from.toLowerCase() : "";
        String lowerSubject = subject.toLowerCase();

        // Check from address first (more reliable)
        if (lowerFrom.contains("makemytrip") || lowerFrom.contains("mmt")) {
            return BookingSource.MAKEMYTRIP;
        }
        if (lowerFrom.contains("booking.com")) {
            return BookingSource.BOOKINGCOM;
        }
        if (lowerFrom.contains("trip.com")) {
            return BookingSource.TRIPCOM;
        }
        if (lowerFrom.contains("cleartrip")) {
            return BookingSource.CLEARMYTRIP;
        }
        if (lowerFrom.contains("goibibo")) {
            return BookingSource.GOIBIBO;
        }
        if (lowerFrom.contains("agoda")) {
            return BookingSource.AGODA;
        }
        if (lowerFrom.contains("easemytrip")) {
            return BookingSource.EASEMYTRIP;
        }
        if (lowerFrom.contains("paytm")) {
            return BookingSource.PAYTM;
        }
        if (lowerFrom.contains("spicejet")) {
            return BookingSource.SPICEJET;
        }
        if (lowerFrom.contains("saudia")) {
            return BookingSource.SAUDIA;
        }
        if (lowerFrom.contains("kiwi")) {
            return BookingSource.KIWI;
        }
        if (lowerFrom.contains("goindigo") || lowerFrom.contains("indigo")) {
            return BookingSource.INDIGO;
        }

        // Fallback to subject checks
        if (lowerSubject.contains("makemytrip") || lowerSubject.contains("trip id") && !lowerSubject.contains("cleartrip")) {
            return BookingSource.MAKEMYTRIP;
        }
        if (lowerSubject.contains("booking.com") || lowerSubject.contains("booking confirmation")) {
            return BookingSource.BOOKINGCOM;
        }
        if (lowerSubject.contains("trip.com") || lowerSubject.matches(".*\\d{16}.*")) {
            return BookingSource.TRIPCOM;
        }
        if (lowerSubject.contains("cleartrip")) {
            return BookingSource.CLEARMYTRIP;
        }
        if (lowerSubject.contains("goibibo") || lowerSubject.contains("gofld")) {
            return BookingSource.GOIBIBO;
        }
        if (lowerSubject.contains("agoda")) {
            return BookingSource.AGODA;
        }
        if (lowerSubject.contains("easemytrip") || lowerSubject.contains("emt")) {
            return BookingSource.EASEMYTRIP;
        }
        if (lowerSubject.contains("paytm")) {
            return BookingSource.PAYTM;
        }
        if (lowerSubject.contains("spicejet") || lowerSubject.contains("spice jet")) {
            return BookingSource.SPICEJET;
        }
        if (lowerSubject.contains("saudia") || lowerSubject.contains("eticket_receipt")) {
            return BookingSource.SAUDIA;
        }
        if (lowerSubject.contains("kiwi") || lowerSubject.contains("your booking is confirmed")) {
            return BookingSource.KIWI;
        }
        if (lowerSubject.contains("indigo itinerary") || lowerSubject.contains("goindigo")) {
            return BookingSource.INDIGO;
        }

        return BookingSource.UNKNOWN;
    }

    private String getFromAddress(Message message) throws MessagingException {
        var from = message.getFrom();
        if (from != null && from.length > 0) {
            return from[0].toString();
        }
        return null;
    }
}
