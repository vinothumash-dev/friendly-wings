package com.friendlywings.automation.controller;

import com.friendlywings.automation.model.FlightItinerary;
import com.friendlywings.automation.model.HotelBooking;
import com.friendlywings.automation.model.Layover;
import com.friendlywings.automation.model.Passenger;
import com.friendlywings.automation.model.Route;
import com.friendlywings.automation.model.Trip;
import com.friendlywings.automation.parser.FlightTicketPdfParser;
import com.friendlywings.automation.pdf.velocity.VelocityFlightPdfGenerator;
import com.friendlywings.automation.pdf.velocity.VelocityHotelPdfGenerator;
import com.friendlywings.automation.repository.ProcessedEmail;
import com.friendlywings.automation.repository.ProcessedEmailRepository;
import com.friendlywings.automation.service.BookingProcessingService;
import com.friendlywings.automation.service.EmailNotificationService;
import com.friendlywings.automation.service.GmailImapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingProcessingService processingService;
    private final ProcessedEmailRepository repository;
    private final VelocityFlightPdfGenerator flightPdfGenerator;
    private final VelocityHotelPdfGenerator hotelPdfGenerator;
    private final GmailImapService gmailService;
    private final EmailNotificationService emailNotificationService;
    private final FlightTicketPdfParser ticketParser;

    public BookingController(BookingProcessingService processingService,
                             ProcessedEmailRepository repository,
                             VelocityFlightPdfGenerator flightPdfGenerator,
                             VelocityHotelPdfGenerator hotelPdfGenerator,
                             GmailImapService gmailService,
                             EmailNotificationService emailNotificationService,
                             FlightTicketPdfParser ticketParser) {
        this.processingService = processingService;
        this.repository = repository;
        this.flightPdfGenerator = flightPdfGenerator;
        this.hotelPdfGenerator = hotelPdfGenerator;
        this.gmailService = gmailService;
        this.emailNotificationService = emailNotificationService;
        this.ticketParser = ticketParser;
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> processNow() {
        processingService.processEmails();
        Map<String, String> resp = new HashMap<>();
        resp.put("status", "Processing triggered");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/search")
    public ResponseEntity<Map<String, String>> searchAndProcess(@RequestParam String subject) {
        processingService.searchAndProcessBySubject(subject);
        Map<String, String> resp = new HashMap<>();
        resp.put("status", "Search triggered for subject: " + subject);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/search-folder")
    public ResponseEntity<Map<String, String>> searchAndProcessFromFolder(@RequestParam String subject, @RequestParam String folder) {
        processingService.searchAndProcessBySubjectFromFolder(subject, folder);
        Map<String, String> resp = new HashMap<>();
        resp.put("status", "Search triggered in folder " + folder + " for subject: " + subject);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/debug/email-html")
    public ResponseEntity<String> getEmailHtml(@RequestParam String subject) throws Exception {
        var messages = gmailService.searchBySubject(subject);
        if (messages.isEmpty()) {
            return ResponseEntity.ok("No email found with subject: " + subject);
        }
        String html = gmailService.extractTextContent(messages.get(0));
        return ResponseEntity.ok(html);
    }

    @PostMapping("/debug/test-pdf-parser")
    public ResponseEntity<FlightItinerary> testPdfParser(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ticketParser.parsePdf(file.getInputStream()));
    }

    @GetMapping("/status")
    public ResponseEntity<List<ProcessedEmail>> getStatus() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping("/demo/flight")
    public ResponseEntity<Map<String, String>> generateDemoFlight() throws IOException {
        FlightItinerary booking = createSampleFlightItinerary();
        Path path = flightPdfGenerator.generate(booking);
        emailNotificationService.sendVoucher(path, booking.getBookingType(), booking.getBookingId());
        Map<String, String> resp = new HashMap<>();
        resp.put("status", "Generated and emailed");
        resp.put("path", path.toString());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/demo/flight-v1")
    public ResponseEntity<Map<String, String>> generateDemoFlightV1() throws IOException {
        FlightItinerary booking = createSampleFlightItinerary();
        Path path = flightPdfGenerator.generateV1(booking);
        emailNotificationService.sendVoucher(path, booking.getBookingType(), booking.getBookingId());
        Map<String, String> resp = new HashMap<>();
        resp.put("status", "Generated V1 and emailed");
        resp.put("path", path.toString());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/demo/hotel")
    public ResponseEntity<Map<String, String>> generateDemoHotel() throws IOException {
        HotelBooking booking = createSampleHotelBooking();
        Path path = hotelPdfGenerator.generate(booking);
        emailNotificationService.sendVoucher(path, booking.getBookingType(), booking.getBookingRef());
        Map<String, String> resp = new HashMap<>();
        resp.put("status", "Generated and emailed");
        resp.put("path", path.toString());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/demo/hotel-v1")
    public ResponseEntity<Map<String, String>> generateDemoHotelV1() throws IOException {
        HotelBooking booking = createSampleHotelBooking();
        Path path = hotelPdfGenerator.generateV1(booking);
        emailNotificationService.sendVoucher(path, booking.getBookingType(), booking.getBookingRef());
        Map<String, String> resp = new HashMap<>();
        resp.put("status", "Generated V1 and emailed");
        resp.put("path", path.toString());
        return ResponseEntity.ok(resp);
    }

    private FlightItinerary createSampleFlightItinerary() {
        FlightItinerary b = new FlightItinerary();
        b.setBookingId("26012732210");
        b.setPnr("78C7UV");

        Trip trip = new Trip();
        trip.setOriginCity("Rio de Janeiro");
        trip.setDestinationCity("Doha");
        trip.setTripDate(LocalDate.of(2026, 1, 29));
        trip.setTotalDuration("19h");

        Route seg1 = new Route();
        seg1.setAirline("Qatar Airways");
        seg1.setOperatedBy("TAM Linhas Aereas");
        seg1.setFlightNumber("JJ - 7296");
        seg1.setFareType("ECONOMY CONVENIENCE");
        seg1.setDepartureAirportCode("GIG");
        seg1.setDepartureTime(LocalTime.of(13, 55));
        seg1.setDepartureDate(LocalDate.of(2026, 1, 29));
        seg1.setDepartureAirportName("Rio de Janeiro - Rio Internacional");
        seg1.setDepartureTerminal("Terminal 2");
        seg1.setDuration("1h 10min");
        seg1.setTravelClass("Economy");
        seg1.setArrivalAirportCode("GRU");
        seg1.setArrivalTime(LocalTime.of(15, 5));
        seg1.setArrivalDate(LocalDate.of(2026, 1, 29));
        seg1.setArrivalAirportName("Sao Paulo - Guarulhos");
        seg1.setArrivalTerminal("Terminal 2");
        seg1.setCabinBaggage("12kg");
        seg1.setCheckInBaggage("46kg");
        Layover layover1 = new Layover();
        layover1.setDuration("5h 20min");
        layover1.setAirportCode("GRU");
        seg1.setLayover(layover1);
        trip.getRoutes().add(seg1);

        Route seg2 = new Route();
        seg2.setAirline("Qatar Airways");
        seg2.setFlightNumber("QR - 780");
        seg2.setFareType("ECONOMY CONVENIENCE");
        seg2.setDepartureAirportCode("GRU");
        seg2.setDepartureTime(LocalTime.of(20, 25));
        seg2.setDepartureDate(LocalDate.of(2026, 1, 29));
        seg2.setDepartureAirportName("Sao Paulo - Guarulhos");
        seg2.setDepartureTerminal("Terminal 3");
        seg2.setDuration("13h 30min");
        seg2.setTravelClass("Economy");
        seg2.setArrivalAirportCode("DOH");
        seg2.setArrivalTime(LocalTime.of(15, 55));
        seg2.setArrivalDate(LocalDate.of(2026, 1, 30));
        seg2.setArrivalAirportName("Doha - Hamad International Airport");
        seg2.setCabinBaggage("12kg");
        seg2.setCheckInBaggage("46kg");
        trip.getRoutes().add(seg2);

        b.getTrips().add(trip);

        Passenger t = new Passenger();
        t.setName("Mr Claudio Eustaquio");
        t.setPnr("78C7UV");
        t.setTicketNumber("1575012847089");
        t.setPassportNumber("GB715187");
        t.setType("ADULT");
        b.getPassengers().add(t);

        return b;
    }

    private HotelBooking createSampleHotelBooking() {
        HotelBooking b = new HotelBooking();
        b.setBookingId("12345678");
        b.setBookingRef("ABC12345");
        b.setClient("Corporate Client A");
        b.setMemberId("MEM001");
        b.setCountryOfResidence("India");
        b.setProperty("Grand Hotel");
        b.setHotelName("Grand Hotel Downtown");
        b.setAddress("123 Main Street, Downtown, City, Country");
        b.setPropertyContactNumber("+91 9876543210");
        b.setRooms(1);
        b.setExtraBeds(0);
        b.setAdults(1);
        b.setChildren(0);
        b.setRoomType("Deluxe King Room");
        b.setPromotion("Corporate Rate");
        b.setMealsIncluded("Breakfast Included");
        b.setCancellationPolicy("This booking is Non-Refundable and cannot be amended or modified.");
        b.setBenefitsIncluded("Benefits Included Free WiFi");
        b.setCheckIn(LocalDate.of(2026, 3, 26));
        b.setCheckOut(LocalDate.of(2026, 3, 28));
        b.setCheckInTime("After 12:00 am");
        b.setCheckOutTime("After 12:00 am");
        b.setPaymentMethod("Visa");
        b.setCardNo("XXXX-XXXX-XXXX-1111");
        b.setGuestRef("12345678901");
        b.setRemarks("XXXXXX");
        b.setGuestList("Mr. John Doe");
        return b;
    }
}
