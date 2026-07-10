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
@Order(3)
public class TripComFlightParser extends ProviderParserTemplate<FlightBooking> {

    // Trip.com uses 16-digit numeric booking numbers
    private static final Pattern BOOKING_NO_PATTERN = Pattern.compile("Booking No\\.?\\s*[:]?\\s*(\\d{16})");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("Flight Booking Confirmed: ([A-Za-z ,-]+)");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\w+),?\\s+(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})");
    private static final Pattern FLIGHT_PATTERN = Pattern.compile("([A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+){0,3})(?:\\s*\\([^)]*\\))?\\s+([A-Z]{1,2}\\d{1,4}|[0-9]{1,2}[A-Z]\\d{1,3})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):\\s*(\\d{2})\\s*([AP]M)?");
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d{1,2})h\\s*(\\d{1,2})m");
    private static final Pattern PNR_PATTERN = Pattern.compile("PNR[^:]*:\\s*([A-Z0-9]{6})");
    private static final Pattern AIRPORT_CODE_PATTERN = Pattern.compile("\\(([A-Z]{3})\\)");
    private static final Pattern PASSENGER_PATTERN = Pattern.compile("(\\w+)\\s*\\(First name\\)\\s*(\\w+)\\s*\\(Last name\\)");
    private static final Pattern PASSENGER_CAPS_PATTERN = Pattern.compile("\\b([A-Z]{2,}(?:\\s+[A-Z]{2,}){0,2})\\b\\s*(?:Adult|Child|Infant)");

    public TripComFlightParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "TRIPCOM";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("trip.com") &&
               (subject.contains("flight booking confirmed") || subject.contains("flight"));
    }

    @Override
    protected FlightBooking createBooking() {
        return new FlightBooking();
    }

    @Override
    protected void extractBooking(String plainText, String rawHtml, FlightBooking booking) {
        String bookingNo = extractFirst(BOOKING_NO_PATTERN, plainText);
        if (bookingNo != null) booking.setTripId(bookingNo);

        String route = extractFirst(ROUTE_PATTERN, plainText);
        if (route != null) booking.setRoute(cleanRoute(route.trim()));

        // Extract all dates
        List<String> dates = extractAll(DATE_PATTERN, plainText);
        if (!dates.isEmpty()) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");
                booking.setTravelDate(LocalDate.parse(dates.get(0), fmt));
            } catch (Exception e) {
                log.debug("Could not parse travel date");
            }
        }

        // Extract PNRs
        List<String> pnrs = extractAll(PNR_PATTERN, plainText);
        if (!pnrs.isEmpty()) booking.setPnr(pnrs.get(0));

        // Extract airport codes
        List<String> codes = extractAll(AIRPORT_CODE_PATTERN, plainText);

        // Extract flights
        List<String[]> flights = extractFlights(plainText);

        // Create segments from flights
        for (int i = 0; i < flights.size(); i++) {
            String[] flight = flights.get(i);
            FlightSegment seg = new FlightSegment();
            seg.setAirline(flight[0]);
            seg.setFlightNumber(flight[1]);
            if (!codes.isEmpty() && i * 2 < codes.size()) {
                seg.setDepartureAirportCode(codes.get(i * 2));
            }
            if (!codes.isEmpty() && i * 2 + 1 < codes.size()) {
                seg.setArrivalAirportCode(codes.get(i * 2 + 1));
            }
            seg.setTravelClass("Economy");
            booking.getSegments().add(seg);
        }

        // Passengers - deduplicate by name
        java.util.Set<String> seenTravellers = new java.util.HashSet<>();
        Matcher m = PASSENGER_PATTERN.matcher(plainText);
        while (m.find()) {
            String name = m.group(1) + " " + m.group(2);
            if (seenTravellers.add(name)) {
                Traveller t = new Traveller();
                t.setName(name);
                t.setPnr(booking.getPnr());
                booking.getTravellers().add(t);
            }
        }
        // Fallback for all-caps names like "RONAL KURNIANSYAH"
        m = PASSENGER_CAPS_PATTERN.matcher(plainText);
        while (m.find()) {
            String name = m.group(1).trim();
            if (seenTravellers.add(name)) {
                Traveller t = new Traveller();
                t.setName(name);
                t.setPnr(booking.getPnr());
                booking.getTravellers().add(t);
            }
        }
    }

    private List<String[]> extractFlights(String text) {
        List<String[]> result = new ArrayList<>();
        Matcher m = FLIGHT_PATTERN.matcher(text);
        while (m.find()) {
            String airline = m.group(1).trim();
            String number = m.group(2).trim();
            // Filter out duplicates and false positives
            if (airline.length() > 2 && number.length() >= 2 && number.length() <= 6
                    && !airline.contains("Airport")
                    && !airline.matches("^[A-Z]{3}\\s+.*")
                    && !airline.contains("Airbus")
                    && !airline.contains("Boeing")
                    && !number.matches("^[AB]\\d{3}$")
                    && !isDuplicateFlight(result, number)) {
                result.add(new String[]{airline, number});
            }
        }
        return result;
    }

    private boolean isDuplicateFlight(List<String[]> flights, String number) {
        for (String[] f : flights) {
            if (f[1].equals(number)) return true;
        }
        return false;
    }
}
