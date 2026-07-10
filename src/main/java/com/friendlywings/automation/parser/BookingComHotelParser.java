package com.friendlywings.automation.parser;

import com.friendlywings.automation.model.HotelBooking;
import com.friendlywings.automation.service.GmailImapService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(2)
public class BookingComHotelParser extends ProviderParserTemplate<HotelBooking> {

    private static final Pattern BOOKING_REF_PATTERN = Pattern.compile("booking\\s*reference\\s*[:#]?\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("property\\s*[:#]?\\s*([^\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOTEL_PATTERN = Pattern.compile("hotel\\s*[:#]?\\s*([^\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHECK_IN_PATTERN = Pattern.compile("check[- ]?in\\s*[:#]?\\s*([A-Za-z0-9,\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHECK_OUT_PATTERN = Pattern.compile("check[- ]?out\\s*[:#]?\\s*([A-Za-z0-9,\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROOMS_PATTERN = Pattern.compile("([0-9]+)\\s*room", Pattern.CASE_INSENSITIVE);
    private static final Pattern GUESTS_PATTERN = Pattern.compile("([0-9]+)\\s*(adult|guest)", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, d MMMM yyyy");

    public BookingComHotelParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "BOOKINGCOM";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("booking.com") ||
               subject.contains("booking.com");
    }

    @Override
    protected HotelBooking createBooking() {
        return new HotelBooking();
    }

    @Override
    protected void extractBooking(String plainText, String rawHtml, HotelBooking booking) {
        String ref = extractFirst(BOOKING_REF_PATTERN, plainText);
        if (ref != null) booking.setBookingRef(ref);

        String property = extractFirst(PROPERTY_PATTERN, plainText);
        if (property != null) {
            booking.setProperty(property);
            booking.setHotelName(property);
        } else {
            String hotel = extractFirst(HOTEL_PATTERN, plainText);
            if (hotel != null) booking.setHotelName(hotel);
        }

        String checkInStr = extractFirst(CHECK_IN_PATTERN, plainText);
        if (checkInStr != null) {
            try {
                booking.setCheckIn(LocalDate.parse(checkInStr, DATE_FMT));
            } catch (Exception e) {
                log.debug("Could not parse check-in date: {}", checkInStr);
            }
        }

        String checkOutStr = extractFirst(CHECK_OUT_PATTERN, plainText);
        if (checkOutStr != null) {
            try {
                booking.setCheckOut(LocalDate.parse(checkOutStr, DATE_FMT));
            } catch (Exception e) {
                log.debug("Could not parse check-out date: {}", checkOutStr);
            }
        }

        String rooms = extractFirst(ROOMS_PATTERN, plainText);
        if (rooms != null) booking.setRooms(Integer.parseInt(rooms));

        String guests = extractFirst(GUESTS_PATTERN, plainText);
        if (guests != null) booking.setAdults(Integer.parseInt(guests));

        // Line-based heuristic extraction
        String[] lines = plainText.split("[\n\r]+");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.toLowerCase().contains("address")) {
                if (i + 1 < lines.length) {
                    booking.setAddress(lines[i + 1].trim());
                }
            }
            if (line.toLowerCase().contains("phone") || line.toLowerCase().contains("contact")) {
                if (i + 1 < lines.length && lines[i + 1].trim().matches(".*\\d.*")) {
                    booking.setPropertyContactNumber(lines[i + 1].trim());
                }
            }
        }
    }
}
