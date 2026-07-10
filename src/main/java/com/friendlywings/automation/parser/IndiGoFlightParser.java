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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(10)
public class IndiGoFlightParser extends ProviderParserTemplate<FlightBooking> {

    private static final Pattern PNR_PATTERN = Pattern.compile("PNR/Booking Ref\\.?:\\s*([A-Z0-9]{6})");
    // IndiGo route is extracted from airport codes after segments are built
    private static final Pattern FLIGHT_PATTERN = Pattern.compile("(6E)\\s*(\\d{1,4})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2})\\s+(\\w{3})\\s+(\\d{2,4})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):\\s*(\\d{2})");
    private static final Pattern PASSENGER_PATTERN = Pattern.compile("(Mr\\.\\s+[A-Za-z]+(?:\\s+[A-Za-z]+){0,2})(?:\\s*$|\\s+Date|\\s+Flight)");
    private static final Pattern AIRPORT_CODE_PATTERN = Pattern.compile("\\b([A-Z]{3})\\b");

    public IndiGoFlightParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "INDIGO";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("goindigo") ||
               from.contains("indigo") ||
               subject.contains("IndiGo Itinerary") ||
               subject.contains("indigo");
    }

    @Override
    protected FlightBooking createBooking() {
        return new FlightBooking();
    }

    @Override
    protected void extractBooking(String plainText, String rawHtml, FlightBooking booking) {
        String pnr = extractFirst(PNR_PATTERN, plainText);
        if (pnr != null) {
            booking.setPnr(pnr);
            booking.setTripId(pnr);
        }

        // Extract all flight segments
        List<String> codes = extractAirportCodes(plainText, 10);
        List<LocalTime> times = new ArrayList<>();
        Matcher tm = TIME_PATTERN.matcher(plainText);
        while (tm.find()) {
            times.add(LocalTime.of(Integer.parseInt(tm.group(1)), Integer.parseInt(tm.group(2))));
        }

        // Find flight numbers and build segments - deduplicate by flight number
        Matcher fm = FLIGHT_PATTERN.matcher(plainText);
        int segIndex = 0;
        int timeIdx = 0;
        java.util.Set<String> seenFlights = new java.util.HashSet<>();
        while (fm.find()) {
            String flightNum = fm.group(1) + " " + fm.group(2);
            if (!seenFlights.add(flightNum)) continue;

            FlightSegment seg = new FlightSegment();
            seg.setAirline("IndiGo");
            seg.setFlightNumber(flightNum);

            // Assign airport codes - each segment uses 2 codes
            int codeIdx = segIndex * 2;
            if (codeIdx < codes.size()) {
                seg.setDepartureAirportCode(codes.get(codeIdx));
            }
            if (codeIdx + 1 < codes.size()) {
                seg.setArrivalAirportCode(codes.get(codeIdx + 1));
            }

            // Assign times - each segment uses 2 times
            if (timeIdx < times.size()) {
                seg.setDepartureTime(times.get(timeIdx));
            }
            if (timeIdx + 1 < times.size()) {
                seg.setArrivalTime(times.get(timeIdx + 1));
            }
            timeIdx += 2;

            seg.setTravelClass("Economy");
            booking.getSegments().add(seg);
            segIndex++;
        }

        // Route from segments
        if (!booking.getSegments().isEmpty()) {
            String from = booking.getSegments().get(0).getDepartureAirportCode();
            String to = booking.getSegments().get(booking.getSegments().size() - 1).getArrivalAirportCode();
            if (from != null && to != null) {
                booking.setRoute(from + " to " + to);
            }
        }

        // Extract travel date
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM yy");
        Matcher dm = DATE_PATTERN.matcher(plainText);
        if (dm.find()) {
            try {
                String yearStr = dm.group(3);
                if (yearStr.length() == 2) yearStr = "20" + yearStr;
                booking.setTravelDate(LocalDate.parse(dm.group(1) + " " + dm.group(2) + " " + yearStr, fmt));
            } catch (Exception e) {
                log.debug("Could not parse travel date");
            }
        }

        // Passengers
        Matcher pm = PASSENGER_PATTERN.matcher(plainText);
        java.util.Set<String> seen = new java.util.HashSet<>();
        while (pm.find()) {
            String name = pm.group(1).trim();
            if (seen.add(name)) {
                Traveller t = new Traveller();
                t.setName(name);
                t.setPnr(booking.getPnr());
                booking.getTravellers().add(t);
            }
        }
    }
}
