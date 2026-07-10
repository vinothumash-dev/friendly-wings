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
@Order(7)
public class AgodaFlightParser extends ProviderParserTemplate<FlightBooking> {

    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile("Booking ID:?\\s*(\\d{10})");
    private static final Pattern FLIGHT_PATTERN = Pattern.compile("([A-Za-z\\s]+)\\s+([A-Z]{2})\\s+([0-9]{1,4})");
    private static final Pattern AIRLINE_REF_PATTERN = Pattern.compile("Airline Reference:?\\s*([A-Z0-9]{6})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\w+)\\s+(\\w+)\\s+(\\d{1,2}),?\\s+(\\d{4})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):\\s*(\\d{2})\\s*(AM|PM)");
    private static final Pattern AIRPORT_PATTERN = Pattern.compile("([A-Za-z\\s]+)\\s+\\(([A-Z]{3})\\)");
    private static final Pattern PASSENGER_PATTERN = Pattern.compile("([A-Z][a-z]+\\s+[A-Z][a-z]+)\\s*·\\s*Adult");
    private static final Pattern TICKET_PATTERN = Pattern.compile("Ticket number:?\\s*(\\d+)");

    public AgodaFlightParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "AGODA";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("agoda") ||
               subject.contains("agoda");
    }

    @Override
    protected FlightBooking createBooking() {
        return new FlightBooking();
    }

    @Override
    protected void extractBooking(String plainText, String rawHtml, FlightBooking booking) {
        String bookingId = extractFirst(BOOKING_ID_PATTERN, plainText);
        if (bookingId != null) booking.setTripId(bookingId);

        String airlineRef = extractFirst(AIRLINE_REF_PATTERN, plainText);
        if (airlineRef != null) booking.setPnr(airlineRef);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE MMMM d yyyy");
        Matcher m = DATE_PATTERN.matcher(plainText);
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
                int hour = Integer.parseInt(tm.group(1));
                int minute = Integer.parseInt(tm.group(2));
                if ("PM".equals(tm.group(3)) && hour != 12) hour += 12;
                if ("AM".equals(tm.group(3)) && hour == 12) hour = 0;
                if (timeCount == 0) seg.setDepartureTime(java.time.LocalTime.of(hour, minute));
                else seg.setArrivalTime(java.time.LocalTime.of(hour, minute));
                timeCount++;
            }

            // Airports
            Matcher am = AIRPORT_PATTERN.matcher(plainText);
            int airportCount = 0;
            while (am.find() && airportCount < 2) {
                if (airportCount == 0) {
                    seg.setDepartureAirportName(am.group(1).trim());
                    seg.setDepartureAirportCode(am.group(2));
                } else {
                    seg.setArrivalAirportName(am.group(1).trim());
                    seg.setArrivalAirportCode(am.group(2));
                }
                airportCount++;
            }

            seg.setTravelClass("Economy");
            booking.getSegments().add(seg);
        }

        // Passengers
        m = PASSENGER_PATTERN.matcher(plainText);
        while (m.find()) {
            Traveller t = new Traveller();
            t.setName(m.group(1));
            t.setPnr(booking.getPnr());
            booking.getTravellers().add(t);
        }
    }
}
