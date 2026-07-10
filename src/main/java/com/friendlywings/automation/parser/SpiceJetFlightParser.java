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
public class SpiceJetFlightParser extends ProviderParserTemplate<FlightBooking> {

    private static final Pattern PNR_PATTERN = Pattern.compile("PNR[:\\s]+([A-Z0-9]{6})");
    private static final Pattern BOOKING_REF_PATTERN = Pattern.compile("Booking Ref\\. No[:\\s]+(\\d+)");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})\\s*[-–]\\s*([A-Za-z]{2,}(?:\\s+[A-Za-z]{2,}){0,2})");
    private static final Pattern FLIGHT_PATTERN = Pattern.compile("([A-Z]{2})\\s+(\\d{1,4})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\w{3})\\.\\s+(\\d{1,2}),?\\s+(\\d{4})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):\\s*(\\d{2})\\s*Hrs");
    private static final Pattern AIRPORT_CODE_PATTERN = Pattern.compile("\\(([A-Z]{3})\\)");
    private static final Pattern TRAVELLER_PATTERN = Pattern.compile("(Mr\\.\\s+[A-Za-z]+(?:\\s+[A-Za-z]+)?)(?=\\s|$)");

    public SpiceJetFlightParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "SPICEJET";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("spicejet") ||
               subject.contains("spicejet") ||
               subject.contains("SpiceJet Booking PNR");
    }

    @Override
    protected FlightBooking createBooking() {
        return new FlightBooking();
    }

    @Override
    protected void extractBooking(String plainText, String rawHtml, FlightBooking booking) {
        String pnr = extractFirst(PNR_PATTERN, plainText);
        if (pnr != null) booking.setPnr(pnr);

        String bookingRef = extractFirst(BOOKING_REF_PATTERN, plainText);
        if (bookingRef != null) booking.setTripId(bookingRef);

        Matcher m = ROUTE_PATTERN.matcher(plainText);
        if (m.find()) {
            booking.setRoute(cleanRoute(m.group(1).trim()) + " to " + cleanRoute(m.group(2).trim()));
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM. d yyyy");
        m = DATE_PATTERN.matcher(plainText);
        if (m.find()) {
            try {
                booking.setTravelDate(LocalDate.parse(m.group(1) + " " + m.group(2) + " " + m.group(3), fmt));
            } catch (Exception e) {
                log.debug("Could not parse travel date");
            }
        }

        // Flight segment
        m = FLIGHT_PATTERN.matcher(plainText);
        if (m.find()) {
            FlightSegment seg = new FlightSegment();
            seg.setAirline("SpiceJet");
            seg.setFlightNumber(m.group(1) + " " + m.group(2));

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

            seg.setTravelClass("Economy");
            booking.getSegments().add(seg);
        }

        // Travellers - deduplicate by name
        m = TRAVELLER_PATTERN.matcher(plainText);
        java.util.Set<String> seen = new java.util.HashSet<>();
        while (m.find()) {
            String name = m.group(1).trim();
            if (seen.add(name)) {
                Traveller t = new Traveller();
                t.setName(name);
                t.setPnr(booking.getPnr());
                booking.getTravellers().add(t);
            }
        }
    }
}
