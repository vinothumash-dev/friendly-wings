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
@Order(6)
public class GoibiboFlightParser extends ProviderParserTemplate<FlightBooking> {

    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile("Booking ID:?\\s*(GOFLD[A-Z0-9]+)");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})\\s*[-–]\\s*([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\w+),?\\s+(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})");
    private static final Pattern FLIGHT_PATTERN = Pattern.compile("([A-Za-z\\s]+)\\s+([A-Z0-9]{2})\\s+([0-9]{1,4})");
    private static final Pattern PNR_PATTERN = Pattern.compile("PNR:?\\s*([A-Z0-9]{6})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):\\s*(\\d{2})\\s*hrs");
    private static final Pattern AIRPORT_CODE_PATTERN = Pattern.compile("\\b([A-Z]{3})\\b");
    private static final Pattern BAGGAGE_CABIN_PATTERN = Pattern.compile("Cabin Baggage:?\\s*\\*?(\\d+)\\s*Kgs?");
    private static final Pattern BAGGAGE_CHECKIN_PATTERN = Pattern.compile("Check-in Baggage:?\\s*\\*?(\\d+)\\s*Kgs?");

    public GoibiboFlightParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "GOIBIBO";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("goibibo") ||
               subject.contains("goibibo") ||
               subject.contains("GOFLD");
    }

    @Override
    protected FlightBooking createBooking() {
        return new FlightBooking();
    }

    @Override
    protected void extractBooking(String plainText, String rawHtml, FlightBooking booking) {
        String bookingId = extractFirst(BOOKING_ID_PATTERN, plainText);
        if (bookingId != null) booking.setTripId(bookingId);

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

        String pnr = extractFirst(PNR_PATTERN, plainText);
        if (pnr != null) booking.setPnr(pnr);

        // Flight segment
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

            // Airport codes
            java.util.List<String> codes = extractAirportCodes(plainText, 2);
            if (codes.size() > 0) seg.setDepartureAirportCode(codes.get(0));
            if (codes.size() > 1) seg.setArrivalAirportCode(codes.get(1));

            // Baggage
            StringBuilder baggage = new StringBuilder();
            String cabin = extractFirst(BAGGAGE_CABIN_PATTERN, plainText);
            if (cabin != null) baggage.append("Cabin: ").append(cabin).append("kg");
            String checkin = extractFirst(BAGGAGE_CHECKIN_PATTERN, plainText);
            if (checkin != null) {
                if (baggage.length() > 0) baggage.append(", ");
                baggage.append("Check-in: ").append(checkin).append("kg");
            }
            if (baggage.length() > 0) seg.setBaggage(baggage.toString());

            seg.setTravelClass("Economy");
            booking.getSegments().add(seg);
        }

        // Travellers - deduplicate
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String name : extractNamesWithHonorific(plainText)) {
            if (seen.add(name)) {
                Traveller t = new Traveller();
                t.setName(name);
                t.setPnr(booking.getPnr());
                booking.getTravellers().add(t);
            }
        }
    }
}
