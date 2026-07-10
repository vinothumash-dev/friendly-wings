package com.friendlywings.automation.parser;

import com.friendlywings.automation.model.FlightBooking;
import com.friendlywings.automation.model.FlightSegment;
import com.friendlywings.automation.model.Traveller;
import com.friendlywings.automation.service.GmailImapService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(9)
public class PaytmFlightParser extends ProviderParserTemplate<FlightBooking> {

    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile("Booking ID\\s*(\\d+)");
    private static final Pattern PNR_PATTERN = Pattern.compile("PNR\\s*([A-Z0-9]{6})");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})\\s*[-–]\\s*([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\w+),?\\s+(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})");
    private static final Pattern FLIGHT_PATTERN = Pattern.compile("([A-Za-z\\s]+)\\s+([A-Z]{2})[-–]\\s*([0-9]{1,4})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):\\s*(\\d{2})");
    private static final Pattern AIRPORT_PATTERN = Pattern.compile("([A-Za-z\\s]+),\\s*([A-Za-z\\s]+)\\s+International\\s+Airport");
    private static final Pattern PASSENGER_PATTERN = Pattern.compile("Traveller\\s*Mr\\s+([A-Za-z]+(?:\\s+[A-Za-z]+){0,2})");

    public PaytmFlightParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "PAYTM";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("paytm") ||
               subject.contains("paytm");
    }

    @Override
    protected FlightBooking createBooking() {
        return new FlightBooking();
    }

    @Override
    protected void extractBooking(String plainText, String rawHtml, FlightBooking booking) {
        String bookingId = extractFirst(BOOKING_ID_PATTERN, plainText);
        if (bookingId != null) booking.setTripId(bookingId);

        String pnr = extractFirst(PNR_PATTERN, plainText);
        if (pnr != null) booking.setPnr(pnr);

        Matcher m = ROUTE_PATTERN.matcher(plainText);
        if (m.find()) {
            booking.setRoute(cleanRoute(m.group(1).trim()) + " to " + cleanRoute(m.group(2).trim()));
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");
        m = DATE_PATTERN.matcher(plainText);
        if (m.find()) {
            try {
                booking.setTravelDate(LocalDate.parse(m.group(1) + " " + m.group(2) + " " + m.group(3) + " " + m.group(4), fmt));
            } catch (Exception e) {
                log.debug("Could not parse travel date");
            }
        }

        // Flight
        m = FLIGHT_PATTERN.matcher(plainText);
        if (m.find()) {
            FlightSegment seg = new FlightSegment();
            seg.setAirline(m.group(1).trim());
            seg.setFlightNumber(m.group(2) + " " + m.group(3));

            // Times
            Matcher tm = TIME_PATTERN.matcher(plainText);
            int timeCount = 0;
            while (tm.find() && timeCount < 2) {
                if (timeCount == 0) seg.setDepartureTime(java.time.LocalTime.of(Integer.parseInt(tm.group(1)), Integer.parseInt(tm.group(2))));
                else seg.setArrivalTime(java.time.LocalTime.of(Integer.parseInt(tm.group(1)), Integer.parseInt(tm.group(2))));
                timeCount++;
            }

            // Airports
            Matcher am = AIRPORT_PATTERN.matcher(plainText);
            int airportCount = 0;
            while (am.find() && airportCount < 2) {
                if (airportCount == 0) seg.setDepartureAirportName(am.group(1).trim() + ", " + am.group(2).trim());
                else seg.setArrivalAirportName(am.group(1).trim() + ", " + am.group(2).trim());
                airportCount++;
            }

            seg.setTravelClass("Economy");
            booking.getSegments().add(seg);
        }

        // Passenger - deduplicate
        m = PASSENGER_PATTERN.matcher(plainText);
        java.util.Set<String> seen = new java.util.HashSet<>();
        while (m.find()) {
            String name = "Mr " + m.group(1).trim();
            if (seen.add(name)) {
                Traveller t = new Traveller();
                t.setName(name);
                t.setPnr(booking.getPnr());
                booking.getTravellers().add(t);
            }
        }
    }
}
