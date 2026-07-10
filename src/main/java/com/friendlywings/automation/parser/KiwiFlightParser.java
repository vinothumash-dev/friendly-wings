package com.friendlywings.automation.parser;

import com.friendlywings.automation.model.FlightBooking;
import com.friendlywings.automation.model.FlightSegment;
import com.friendlywings.automation.model.Traveller;
import com.friendlywings.automation.service.GmailImapService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(9)
public class KiwiFlightParser extends ProviderParserTemplate<FlightBooking> {

    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile("Booking number\\s*(\\d{3}\\s*\\d{3}\\s*\\d{3})");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})\\s*(?:\\[image:.*\\]|\\s{2,})\\s*([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})(?=\\s+\\d{1,2}:\\d{2})");
    private static final Pattern PASSENGER_PATTERN = Pattern.compile("\\*([A-Za-z\\s]+)\\*");

    public KiwiFlightParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "KIWI";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("kiwi") ||
               subject.contains("kiwi") ||
               subject.contains("Your booking is confirmed");
    }

    @Override
    protected FlightBooking createBooking() {
        return new FlightBooking();
    }

    @Override
    protected void extractBooking(String plainText, String rawHtml, FlightBooking booking) {
        String bookingId = extractFirst(BOOKING_ID_PATTERN, plainText);
        if (bookingId != null) booking.setTripId(bookingId.replaceAll("\\s", ""));

        Matcher m = ROUTE_PATTERN.matcher(plainText);
        if (m.find()) {
            booking.setRoute(cleanRoute(m.group(1).trim()) + " to " + cleanRoute(m.group(2).trim()));
        }

        // Airport codes
        java.util.List<String> codes = extractAirportCodes(plainText, 2);
        if (codes.size() >= 2) {
            FlightSegment seg = new FlightSegment();
            seg.setDepartureAirportCode(codes.get(0));
            seg.setArrivalAirportCode(codes.get(1));
            seg.setTravelClass("Economy");
            booking.getSegments().add(seg);
        }

        // Passenger
        m = PASSENGER_PATTERN.matcher(plainText);
        if (m.find()) {
            Traveller t = new Traveller();
            t.setName(m.group(1).trim());
            booking.getTravellers().add(t);
        }
    }
}
