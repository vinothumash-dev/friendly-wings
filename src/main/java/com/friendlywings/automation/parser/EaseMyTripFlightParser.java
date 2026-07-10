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
public class EaseMyTripFlightParser extends ProviderParserTemplate<FlightBooking> {

    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile("Booking ID\\s*[-–]\\s*(EMT\\d+)");
    private static final Pattern PNR_PATTERN = Pattern.compile("PNR:?\\s*([A-Z0-9]{6})");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})\\s*[-–]\\s*([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\w+),?\\s+(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})");
    private static final Pattern FLIGHT_PATTERN = Pattern.compile("([A-Za-z\\s&]+)\\s+([A-Z]{2})[-–]\\s*([0-9]{1,4})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):\\s*(\\d{2})");
    private static final Pattern AIRPORT_PATTERN = Pattern.compile("([A-Za-z\\s]+)\\s+Airport");
    private static final Pattern PASSENGER_PATTERN = Pattern.compile("Mr\\s+([A-Za-z]+(?:\\s+[A-Za-z]+){0,2})");

    public EaseMyTripFlightParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "EASEMYTRIP";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("easemytrip") ||
               subject.contains("easemytrip") ||
               subject.contains("EMT");
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

        // Multiple flight segments possible
        Matcher fm = FLIGHT_PATTERN.matcher(plainText);
        List<String[]> flights = new ArrayList<>();
        while (fm.find()) {
            String airline = fm.group(1).trim();
            // Skip if airline looks like a PNR (6-char uppercase)
            if (airline.matches("^[A-Z0-9]{6}\\s+.*")) {
                airline = airline.replaceFirst("^[A-Z0-9]{6}\\s+", "");
            }
            // Skip if airline is just a PNR
            if (airline.matches("^[A-Z0-9]{6}$")) continue;
            flights.add(new String[]{airline, fm.group(2) + " " + fm.group(3)});
        }

        // Times
        Matcher tm = TIME_PATTERN.matcher(plainText);
        List<String> times = new ArrayList<>();
        while (tm.find()) {
            times.add(tm.group(1) + ":" + tm.group(2));
        }

        // Airports
        Matcher am = AIRPORT_PATTERN.matcher(plainText);
        List<String> airports = new ArrayList<>();
        while (am.find()) {
            airports.add(am.group(1).trim());
        }

        // Create segments
        for (int i = 0; i < flights.size(); i++) {
            FlightSegment seg = new FlightSegment();
            seg.setAirline(flights.get(i)[0]);
            seg.setFlightNumber(flights.get(i)[1]);
            if (i * 2 < times.size()) {
                String[] t = times.get(i * 2).split(":");
                seg.setDepartureTime(java.time.LocalTime.of(Integer.parseInt(t[0]), Integer.parseInt(t[1])));
            }
            if (i * 2 + 1 < times.size()) {
                String[] t = times.get(i * 2 + 1).split(":");
                seg.setArrivalTime(java.time.LocalTime.of(Integer.parseInt(t[0]), Integer.parseInt(t[1])));
            }
            if (i < airports.size()) seg.setDepartureAirportName(airports.get(i));
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
