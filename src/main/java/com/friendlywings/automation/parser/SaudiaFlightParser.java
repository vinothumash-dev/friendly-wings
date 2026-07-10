package com.friendlywings.automation.parser;

import com.friendlywings.automation.model.FlightBooking;
import com.friendlywings.automation.model.FlightSegment;
import com.friendlywings.automation.model.Traveller;
import com.friendlywings.automation.service.GmailImapService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(8)
public class SaudiaFlightParser extends ProviderParserTemplate<FlightBooking> {

    private static final Pattern BOOKING_REF_PATTERN = Pattern.compile("Booking ref[^:]*:\\s*([A-Z0-9]{6})");
    private static final Pattern E_TICKET_PATTERN = Pattern.compile("e-?Ticket[^0-9]*\\b(\\d{12,13})\\b");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("booking is confirmed\\s+([A-Za-z]+(?:\\s+[A-Za-z]+){0,2})\\s+to\\s+([A-Za-z]+(?:\\s+[A-Za-z]+){0,2})");
    private static final Pattern FLIGHT_PATTERN = Pattern.compile("Saudia\\s*[-–\\n\\r]*\\s*SV\\s+(\\d{1,4})");
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("(\\w{3})\\s+(\\d{1,2}):\\s*(\\d{2})\\s+(\\w{3})\\s+(\\d{1,2}):\\s*(\\d{2})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\w{3}),?\\s+(\\d{1,2})\\s+(\\w{3,10})");
    private static final Pattern TRAVELLER_PATTERN = Pattern.compile("Dear\\s+([A-Za-z\\s]+),");

    public SaudiaFlightParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "SAUDIA";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("saudia") ||
               subject.contains("saudia") ||
               subject.contains("Saudia") ||
               subject.contains("ETicket_Receipt");
    }

    @Override
    protected FlightBooking createBooking() {
        return new FlightBooking();
    }

    @Override
    protected void extractBooking(String plainText, String rawHtml, FlightBooking booking) {
        String bookingRef = extractFirst(BOOKING_REF_PATTERN, plainText);
        if (bookingRef != null) booking.setTripId(bookingRef);

        String eTicket = extractFirst(E_TICKET_PATTERN, plainText);
        if (eTicket != null && booking.getTripId() == null) booking.setTripId(eTicket);

        Matcher rm = ROUTE_PATTERN.matcher(plainText);
        if (rm.find()) {
            booking.setRoute(cleanRoute(rm.group(1).trim()) + " to " + cleanRoute(rm.group(2).trim()));
        }

        // Extract flight segments from airport codes and times
        List<String> codes = extractAirportCodes(plainText, 6);
        List<String> times = extractTimes(plainText);

        // Try to find SV flight numbers
        List<String> flightNumbers = new ArrayList<>();
        Matcher fm = Pattern.compile("SV\\s+(\\d{1,4})").matcher(plainText);
        while (fm.find()) {
            flightNumbers.add(fm.group(1));
        }

        // Build segments from airport pairs
        for (int i = 0; i < codes.size() - 1 && i < flightNumbers.size(); i += 2) {
            FlightSegment seg = new FlightSegment();
            seg.setAirline("Saudia");
            seg.setFlightNumber("SV " + flightNumbers.get(i / 2));
            seg.setDepartureAirportCode(codes.get(i));
            seg.setArrivalAirportCode(codes.get(i + 1));
            if (i * 2 < times.size()) seg.setDepartureTime(parseTime(times.get(i * 2)));
            if (i * 2 + 1 < times.size()) seg.setArrivalTime(parseTime(times.get(i * 2 + 1)));
            seg.setTravelClass("Economy");
            booking.getSegments().add(seg);
        }

        // Traveller
        String travellerName = extractFirst(TRAVELLER_PATTERN, plainText);
        if (travellerName != null) {
            Traveller t = new Traveller();
            t.setName(travellerName.trim());
            t.setPnr(booking.getPnr());
            booking.getTravellers().add(t);
        }
    }

    private List<String> extractTimes(String text) {
        List<String> times = new ArrayList<>();
        Pattern tp = Pattern.compile("(\\d{1,2}):\\s*(\\d{2})");
        Matcher m = tp.matcher(text);
        while (m.find()) {
            times.add(m.group(1) + ":" + m.group(2));
        }
        return times;
    }
}
