package com.friendlywings.automation.parser;

import com.friendlywings.automation.model.FlightBooking;
import com.friendlywings.automation.model.FlightSegment;
import com.friendlywings.automation.model.Traveller;
import com.friendlywings.automation.service.GmailImapService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(1)
public class MakeMyTripFlightParser extends ProviderParserTemplate<FlightBooking> {

    // Patterns for MakeMyTrip confirmation emails
    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile("Booking\\s*ID[:\\s]+([A-Z0-9]+)");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})\\s*[-–—]\\s*([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})");
    private static final Pattern PNR_PATTERN = Pattern.compile("PNR[:\\s]+([A-Z0-9]{6})");
    private static final Pattern FLIGHT_PATTERN = Pattern.compile("([A-Z0-9]{2})\\s+([0-9]{1,4})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun),?\\s+(\\d{1,2})\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*hrs?");
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d{1,2})\\s*h\\s*(\\d{1,2})\\s*m");
    private static final Pattern AIRPORT_PATTERN = Pattern.compile("([A-Za-z\\s]+)\\s+Airport");
    private static final Pattern TERMINAL_PATTERN = Pattern.compile("Terminal\\s+(\\d+)");
    private static final Pattern BAGGAGE_CABIN_PATTERN = Pattern.compile("Cabin Baggage[:\\s]+(\\d+)\\s*Kgs?");
    private static final Pattern BAGGAGE_CHECKIN_PATTERN = Pattern.compile("Check-in Baggage[:\\s]+(\\d+)\\s*Kgs?");

    public MakeMyTripFlightParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "MAKEMYTRIP";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("makemytrip") ||
               from.contains("mmt") ||
               subject.contains("makemytrip");
    }

    @Override
    protected FlightBooking createBooking() {
        return new FlightBooking();
    }

    @Override
    protected void extractBooking(String plainText, String rawHtml, FlightBooking booking) {
        parseBookingDetails(plainText, booking);
        parseRouteAndDate(plainText, booking);
        parseSegment(plainText, booking);
        parseTravellers(plainText, booking);
        parseBaggage(plainText, booking);
    }

    private void parseBookingDetails(String text, FlightBooking booking) {
        String bookingId = extractFirst(BOOKING_ID_PATTERN, text);
        if (bookingId != null) booking.setTripId(bookingId);

        String pnr = extractFirst(PNR_PATTERN, text);
        if (pnr != null) booking.setPnr(pnr);
    }

    private void parseRouteAndDate(String text, FlightBooking booking) {
        Matcher m = ROUTE_PATTERN.matcher(text);
        if (m.find()) {
            booking.setRoute(cleanRoute(m.group(1).trim()) + " to " + cleanRoute(m.group(2).trim()));
        }

        String year = extractBookingYear(text);
        if (year == null) year = String.valueOf(LocalDate.now().getYear());

        m = DATE_PATTERN.matcher(text);
        if (m.find()) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM yyyy");
                booking.setTravelDate(LocalDate.parse(m.group(1) + " " + m.group(2) + " " + year, fmt));
            } catch (Exception e) {
                log.debug("Could not parse travel date");
            }
        }
    }

    private void parseSegment(String text, FlightBooking booking) {
        FlightSegment seg = new FlightSegment();

        // Airline
        String[] airlines = {"IndiGo", "Air India", "SpiceJet", "Vistara", "GoAir", "Akasa Air"};
        for (String airline : airlines) {
            if (text.contains(airline)) {
                seg.setAirline(airline);
                break;
            }
        }

        // Flight number
        Matcher m = FLIGHT_PATTERN.matcher(text);
        if (m.find()) {
            seg.setFlightNumber(m.group(1) + " - " + m.group(2));
        }

        // Times
        m = TIME_PATTERN.matcher(text);
        int timeCount = 0;
        while (m.find() && timeCount < 2) {
            LocalTime time = LocalTime.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
            if (timeCount == 0) seg.setDepartureTime(time);
            else seg.setArrivalTime(time);
            timeCount++;
        }

        // Dates
        String year = extractBookingYear(text);
        if (year == null) year = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM yyyy");
        m = DATE_PATTERN.matcher(text);
        int dateCount = 0;
        while (m.find() && dateCount < 2) {
            try {
                LocalDate date = LocalDate.parse(m.group(1) + " " + m.group(2) + " " + year, fmt);
                if (dateCount == 0) seg.setDepartureDate(date);
                else seg.setArrivalDate(date);
            } catch (Exception e) {
                log.debug("Could not parse segment date");
            }
            dateCount++;
        }

        // Duration
        m = DURATION_PATTERN.matcher(text);
        if (m.find()) {
            seg.setDuration(m.group(1) + "h " + m.group(2) + "m");
        }

        // Airport codes
        List<String> codes = extractAirportCodes(text, 2);
        if (codes.size() > 0) seg.setDepartureAirportCode(codes.get(0));
        if (codes.size() > 1) seg.setArrivalAirportCode(codes.get(1));

        // Airport names
        List<String> names = extractAll(AIRPORT_PATTERN, text);
        if (names.size() > 0) seg.setDepartureAirportName(names.get(0));
        if (names.size() > 1) seg.setArrivalAirportName(names.get(1));

        // Terminal
        String terminal = extractFirst(TERMINAL_PATTERN, text);
        if (terminal != null) seg.setDepartureTerminal(terminal);

        // Class
        if (text.contains("Economy")) seg.setTravelClass("Economy");
        else if (text.contains("Business")) seg.setTravelClass("Business");

        // Fare type
        if (text.contains("REGULAR")) seg.setFareType("REGULAR");
        else if (text.contains("MMTSPECIAL")) seg.setFareType("MMTSPECIAL");

        booking.getSegments().add(seg);
    }

    private void parseTravellers(String text, FlightBooking booking) {
        String[] lines = text.split("[\n\r]+");
        boolean inTravellerSection = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("TRAVELLER") || line.contains("E-TICKET NO") || line.contains("TICKET NO")) {
                inTravellerSection = true;
                continue;
            }
            if (inTravellerSection && (line.startsWith("Mr ") || line.startsWith("Ms ") || line.startsWith("Mrs "))) {
                Traveller t = new Traveller();
                String name = line;
                if (name.contains("(")) {
                    name = name.substring(0, name.indexOf("(")).trim();
                }
                t.setName(name);
                for (int j = i + 1; j < Math.min(i + 5, lines.length); j++) {
                    String nextLine = lines[j].trim();
                    if (nextLine.matches("[A-Z0-9]{6}")) {
                        t.setPnr(nextLine);
                        break;
                    }
                }
                if (t.getPnr() == null && booking.getPnr() != null) {
                    t.setPnr(booking.getPnr());
                }
                boolean alreadyAdded = booking.getTravellers().stream()
                    .anyMatch(existing -> existing.getName() != null && existing.getName().equals(t.getName()));
                if (!alreadyAdded) {
                    booking.getTravellers().add(t);
                }
                inTravellerSection = false;
            }
        }

        if (booking.getTravellers().isEmpty()) {
            List<String> names = extractNamesWithHonorific(text);
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String name : names) {
                if (seen.add(name)) {
                    Traveller t = new Traveller();
                    t.setName(name);
                    t.setPnr(booking.getPnr());
                    booking.getTravellers().add(t);
                }
            }
        }
    }

    private void parseBaggage(String text, FlightBooking booking) {
        if (booking.getSegments().isEmpty()) return;
        FlightSegment seg = booking.getSegments().get(0);

        StringBuilder baggage = new StringBuilder();
        String cabin = extractFirst(BAGGAGE_CABIN_PATTERN, text);
        if (cabin != null) baggage.append("Cabin: ").append(cabin).append("kg");

        String checkin = extractFirst(BAGGAGE_CHECKIN_PATTERN, text);
        if (checkin != null) {
            if (baggage.length() > 0) baggage.append(", ");
            baggage.append("Check-in: ").append(checkin).append("kg");
        }
        if (baggage.length() > 0) {
            seg.setBaggage(baggage.toString());
        }
    }
}
