package com.friendlywings.automation.parser;

import com.friendlywings.automation.model.Booking;
import com.friendlywings.automation.service.GmailImapService;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for all booking email parsers.
 * Subclasses only need to implement:
 *  - canParse(String from, String subject)
 *  - createBooking()  (returns the concrete Booking subclass)
 *  - extractBooking(String plainText, String rawHtml, Booking booking)
 *
 * Everything else (HTML extraction, Jsoup parsing, common regex helpers) is handled here.
 */
public abstract class ProviderParserTemplate<T extends Booking> implements EmailParser {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final GmailImapService gmailService;

    protected ProviderParserTemplate(GmailImapService gmailService) {
        this.gmailService = gmailService;
    }

    /**
     * Return true if this parser handles emails from the given sender/subject.
     * Example: return from.contains("makemytrip") || subject.contains("makemytrip");
     */
    protected abstract boolean canParse(String from, String subject);

    /**
     * Create a fresh empty Booking instance (FlightBooking, HotelBooking, etc.).
     */
    protected abstract T createBooking();

    /**
     * Extract all data from the email text into the booking object.
     * @param plainText  Jsoup-stripped plain text from the email
     * @param rawHtml    Original HTML (for Jsoup CSS selectors if needed)
     * @param booking    Empty booking created by createBooking()
     */
    protected abstract void extractBooking(String plainText, String rawHtml, T booking);

    @Override
    public final boolean canParse(Message message) throws MessagingException {
        String from = message.getFrom()[0].toString().toLowerCase();
        String subject = message.getSubject();
        if (subject == null) subject = "";
        return canParse(from, subject);
    }

    @Override
    public final T parse(Message message) throws MessagingException {
        T booking = createBooking();
        booking.setSource(getSourceName());
        booking.setRawMessageId(getMessageId(message));
        booking.setReceivedAt(java.time.LocalDate.now().atStartOfDay());

        try {
            String html = gmailService.extractTextContent(message);
            return parseHtml(html, booking);
        } catch (IOException e) {
            log.warn("Could not read email content", e);
        }
        return booking;
    }

    /**
     * Parse from raw HTML string. Useful for testing without Gmail.
     */
    public final T parseHtml(String html) {
        T booking = createBooking();
        booking.setSource(getSourceName());
        return parseHtml(html, booking);
    }

    private T parseHtml(String html, T booking) {
        String plainText = Jsoup.parse(html).text();
        // Strip zero-width and invisible Unicode characters that some providers embed
        plainText = plainText.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "");
        extractBooking(plainText, html, booking);
        return booking;
    }

    /**
     * Source name string stored in Booking.source (e.g. "MAKEMYTRIP", "BOOKINGCOM").
     * Default is the simple class name without "Parser" suffix.
     */
    protected String getSourceName() {
        String name = getClass().getSimpleName();
        if (name.endsWith("Parser")) {
            name = name.substring(0, name.length() - 6);
        }
        return name.toUpperCase();
    }

    // ─────────────────────────────────────────────────────────────
    // Regex helpers
    // ─────────────────────────────────────────────────────────────

    /** Extract first capture group (group 1) from the first match, or null. */
    protected String extractFirst(Pattern pattern, String text) {
        return extractFirst(pattern, text, 1);
    }

    /** Extract a specific capture group from the first match, or null. */
    protected String extractFirst(Pattern pattern, String text, int group) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(group).trim() : null;
    }

    /** Extract all matches for a specific capture group. */
    protected List<String> extractAll(Pattern pattern, String text, int group) {
        List<String> results = new ArrayList<>();
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            results.add(m.group(group).trim());
        }
        return results;
    }

    /** Extract all matches for group 1. */
    protected List<String> extractAll(Pattern pattern, String text) {
        return extractAll(pattern, text, 1);
    }

    /** Extract exactly N matches for group 1 (stops after N). */
    protected List<String> extractN(Pattern pattern, String text, int n) {
        List<String> results = new ArrayList<>();
        Matcher m = pattern.matcher(text);
        while (m.find() && results.size() < n) {
            results.add(m.group(1).trim());
        }
        return results;
    }

    // ─────────────────────────────────────────────────────────────
    // Date / Time helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Try to parse a date string using multiple format patterns.
     * Returns null if none match.
     */
    protected LocalDate parseDate(String dateStr, String... patterns) {
        if (dateStr == null || dateStr.isBlank()) return null;
        for (String p : patterns) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern(p));
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    /**
     * Try to parse a time string using multiple format patterns.
     * Returns null if none match.
     */
    protected LocalTime parseTime(String timeStr, String... patterns) {
        if (timeStr == null || timeStr.isBlank()) return null;
        for (String p : patterns) {
            try {
                return LocalTime.parse(timeStr.trim(), DateTimeFormatter.ofPattern(p));
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    /**
     * Extract year from text using "Booked on ... YEAR" pattern.
     */
    protected String extractBookingYear(String text) {
        Pattern yearPattern = Pattern.compile("Booked on\\s+\\d{1,2}\\s+\\w+\\s+(\\d{4})");
        return extractFirst(yearPattern, text);
    }

    // ─────────────────────────────────────────────────────────────
    // Common booking field extractors
    // ─────────────────────────────────────────────────────────────

    /**
     * Extract 3-letter airport codes from text.
     * Skips common non-airport codes.
     */
    protected List<String> extractAirportCodes(String text, int max) {
        Pattern codePattern = Pattern.compile("\\b([A-Z]{3})\\b");
        List<String> results = new ArrayList<>();
        Matcher m = codePattern.matcher(text);
        while (m.find() && results.size() < max) {
            String code = m.group(1);
            if (code.equals("APP") || code.equals("DIG") || code.equals("PNR")
                    || code.equals("REG") || code.equals("INR") || code.equals("GST")
                    || code.equals("GBP") || code.equals("USD") || code.equals("EUR")
                    || code.equals("API") || code.equals("AIR") || code.equals("TAX")
                    || code.equals("UTC") || code.equals("GMT") || code.equals("ILP")
                    || code.equals("ORR") || code.equals("GRT")) {
                continue;
            }
            results.add(code);
        }
        return results;
    }

    /**
     * Extract traveller names prefixed with Mr/Ms/Mrs.
     * Removes (ADULT), (CHILD) suffixes.
     */
    protected List<String> extractNamesWithHonorific(String text) {
        List<String> names = new ArrayList<>();
        Pattern pattern = Pattern.compile("(Mr|Ms|Mrs)\\s+([A-Za-z\\s]+?)(?:\\s*\\(|$)");
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            String name = (m.group(1) + " " + m.group(2).trim()).trim();
            names.add(name);
        }
        return names;
    }

    /**
     * Extract PNR codes (6-character alphanumeric) from text.
     */
    protected List<String> extractPnrs(String text, int max) {
        Pattern pnrPattern = Pattern.compile("\\b([A-Z0-9]{6})\\b");
        return extractN(pnrPattern, text, max);
    }

    /**
     * Clean up a route string by stripping common trailing non-city words
     * (day names, direction words, action words, etc.).
     */
    protected String cleanRoute(String route) {
        if (route == null) return null;
        String cleaned = route.trim();
        String[] noiseWords = {
            "To", "From", "For", "On", "At", "Please", "Thank", "Dear", "Booking",
            "Flight", "Ticket", "Manage", "View", "Confirm", "Confirmation",
            "One Way", "Round Trip", "Return", "Non-stop",
            "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
            "Your", "Trip", "E-Ticket", "ETicket", "Itinerary", "Passenger", "Passengers",
            "UTC", "GMT", "Hrs", "AM", "PM", "MR", "Saver", "Check", "Sign", "Booking",
            "Is", "Confirmed", "Confirm", "Tickets"
        };
        boolean changed;
        do {
            changed = false;
            for (String word : noiseWords) {
                // Strip suffix
                String suffix = " " + word;
                if (cleaned.toLowerCase().endsWith(suffix.toLowerCase())) {
                    cleaned = cleaned.substring(0, cleaned.length() - suffix.length()).trim();
                    changed = true;
                }
                // Strip prefix
                String prefix = word + " ";
                if (cleaned.toLowerCase().startsWith(prefix.toLowerCase())) {
                    cleaned = cleaned.substring(prefix.length()).trim();
                    changed = true;
                }
            }
        } while (changed);
        return cleaned.isEmpty() ? route.trim() : cleaned;
    }

    private String getMessageId(Message message) throws MessagingException {
        String[] ids = message.getHeader("Message-ID");
        return ids != null && ids.length > 0 ? ids[0] : java.util.UUID.randomUUID().toString();
    }
}
