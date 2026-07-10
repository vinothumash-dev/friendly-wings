package com.friendlywings.automation.parser;

import com.friendlywings.automation.model.HotelBooking;
import com.friendlywings.automation.service.GmailImapService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Component
@Order(4)
public class TripComHotelParser extends ProviderParserTemplate<HotelBooking> {

    private static final Pattern BOOKING_NO_PATTERN = Pattern.compile("Booking no\\.?\\s*(\\d{16})");
    private static final Pattern HOTEL_PATTERN = Pattern.compile("Hotel\\s+([A-Z][^\n]+)");
    private static final Pattern CHECKIN_PATTERN = Pattern.compile("Check-in\\s*(\\w+),?\\s+(\\d{1,2}),?\\s+(\\w+)\\s+(\\d{4})");
    private static final Pattern CHECKOUT_PATTERN = Pattern.compile("Check-out\\s*(\\w+),?\\s+(\\d{1,2}),?\\s+(\\w+)\\s+(\\d{4})");
    private static final Pattern ROOMS_PATTERN = Pattern.compile("(\\d+)\\s+Room");
    private static final Pattern NIGHTS_PATTERN = Pattern.compile("(\\d+)\\s+Nights");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("Address\\s*([0-9\\s,\\w]+)");
    private static final Pattern GUEST_PATTERN = Pattern.compile("Guest names?\\s*([A-Z][^\n]+)");

    public TripComHotelParser(GmailImapService gmailService) {
        super(gmailService);
    }

    @Override
    protected String getSourceName() {
        return "TRIPCOM";
    }

    @Override
    protected boolean canParse(String from, String subject) {
        return from.contains("trip.com") &&
               (subject.contains("confirmation number") || subject.contains("hotel"));
    }

    @Override
    protected HotelBooking createBooking() {
        return new HotelBooking();
    }

    @Override
    protected void extractBooking(String plainText, String rawHtml, HotelBooking booking) {
        String bookingNo = extractFirst(BOOKING_NO_PATTERN, plainText);
        if (bookingNo != null) booking.setBookingRef(bookingNo);

        String hotel = extractFirst(HOTEL_PATTERN, plainText);
        if (hotel != null) {
            booking.setHotelName(hotel.trim());
            booking.setProperty(hotel.trim());
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d yyyy");
        String checkIn = extractFirst(CHECKIN_PATTERN, plainText);
        if (checkIn != null) {
            try {
                booking.setCheckIn(LocalDate.parse(checkIn, fmt));
            } catch (Exception e) {
                log.debug("Could not parse check-in date");
            }
        }

        String checkOut = extractFirst(CHECKOUT_PATTERN, plainText);
        if (checkOut != null) {
            try {
                booking.setCheckOut(LocalDate.parse(checkOut, fmt));
            } catch (Exception e) {
                log.debug("Could not parse check-out date");
            }
        }

        String rooms = extractFirst(ROOMS_PATTERN, plainText);
        if (rooms != null) booking.setRooms(Integer.parseInt(rooms));

        String nights = extractFirst(NIGHTS_PATTERN, plainText);
        if (nights != null) {
            // Store nights if needed
        }

        String address = extractFirst(ADDRESS_PATTERN, plainText);
        if (address != null) booking.setAddress(address.trim());

        String guest = extractFirst(GUEST_PATTERN, plainText);
        if (guest != null) booking.setGuestList(guest.trim());
    }
}
