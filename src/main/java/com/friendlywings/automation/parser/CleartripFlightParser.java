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
@Order(5)
public class CleartripFlightParser extends ProviderParserTemplate<FlightBooking> {

    private static final Pattern TRIP_ID_PATTERN = Pattern.compile("Trip ID:?\\s*(\\d{12})");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})\\s*→\\s*([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\w+),?\\s+(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})");
    private static final Pattern FLIGHT_PATTERN = Pattern.compile("([A-Za-z\\s]+)\\s+([A-Z0-9]{2})\\s+([0-9]{1,4})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):\\s*(\\d{2})");
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d{1,2})h\\s*(\\d{1,2})m");
    private static final Pattern PNR_PATTERN = Pattern.compile("PNR[:\\s*]+([A-Z0-9]{6})");
    private static final Pattern AIRPORT_PATTERN = Pattern.compile("([A-Za-z\\s]+)\\s+International");
    private static final Pattern TRAVELLER_PATTERN = Pattern.compile("(M[rs]s?\\s+[A-Za-z]+(?:\\s+[A-Za-z]+){0,2})\\s+([A-Z0-9]{6})");
    private static final Pattern BAGGAGE_PATTERN = Pattern.compile("Check-in:\\s*(\\d+)kg.*Cabin:\\s*(\\d+)kg");

    public CleartripFlightParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "CLEARMYTRIP";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("cleartrip") ||
               subject.contains("cleartrip") ||
               subject.contains("Trip ID");
    }

    @Override
    protected FlightBooking createBooking() {
        return new FlightBooking();
    }

    @Override
    protected void extractBooking(String plainText, String rawHtml, FlightBooking booking) {
        String tripId = extractFirst(TRIP_ID_PATTERN, plainText);
        if (tripId != null) booking.setTripId(tripId);

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
            String airline = m.group(1).trim();
            // Skip false positives like "Trip ID"
            if (airline.equalsIgnoreCase("Trip") || airline.equalsIgnoreCase("Trip ID")) {
                // Try to find next match
                if (m.find()) {
                    airline = m.group(1).trim();
                }
            }
            FlightSegment seg = new FlightSegment();
            seg.setAirline(airline);
            seg.setFlightNumber(m.group(2) + " " + m.group(3));

            // Times
            Matcher tm = TIME_PATTERN.matcher(plainText);
            int timeCount = 0;
            while (tm.find() && timeCount < 2) {
                if (timeCount == 0) seg.setDepartureTime(java.time.LocalTime.of(Integer.parseInt(tm.group(1)), Integer.parseInt(tm.group(2))));
                else seg.setArrivalTime(java.time.LocalTime.of(Integer.parseInt(tm.group(1)), Integer.parseInt(tm.group(2))));
                timeCount++;
            }

            // Duration
            Matcher dm = DURATION_PATTERN.matcher(plainText);
            if (dm.find()) {
                seg.setDuration(dm.group(1) + "h " + dm.group(2) + "m");
            }

            // PNR
            Matcher pm = PNR_PATTERN.matcher(plainText);
            if (pm.find()) {
                seg.setFlightNumber(seg.getFlightNumber()); // already set
                booking.setPnr(pm.group(1));
            }

            // Airport codes - look for 3-letter codes
            String[] codes = extractAirportCodes(plainText, 2).toArray(new String[0]);
            if (codes.length > 0) seg.setDepartureAirportCode(codes[0]);
            if (codes.length > 1) seg.setArrivalAirportCode(codes[1]);

            // Airport names
            Matcher am = AIRPORT_PATTERN.matcher(plainText);
            int airportCount = 0;
            while (am.find() && airportCount < 2) {
                if (airportCount == 0) seg.setDepartureAirportName(am.group(1).trim());
                else seg.setArrivalAirportName(am.group(1).trim());
                airportCount++;
            }

            // Baggage
            Matcher bm = BAGGAGE_PATTERN.matcher(plainText);
            if (bm.find()) {
                seg.setBaggage("Cabin: " + bm.group(2) + "kg, Check-in: " + bm.group(1) + "kg");
            }

            seg.setTravelClass("Economy");
            booking.getSegments().add(seg);
        }

        // Travellers with PNR - deduplicate
        m = TRAVELLER_PATTERN.matcher(plainText);
        java.util.Set<String> seen = new java.util.HashSet<>();
        while (m.find()) {
            String name = m.group(1).trim();
            if (seen.add(name)) {
                Traveller t = new Traveller();
                t.setName(name);
                t.setPnr(m.group(2));
                booking.getTravellers().add(t);
            }
        }
    }
}
