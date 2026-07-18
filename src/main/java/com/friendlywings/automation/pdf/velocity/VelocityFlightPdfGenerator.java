package com.friendlywings.automation.pdf.velocity;

import com.friendlywings.automation.model.Booking;
import com.friendlywings.automation.model.FlightItinerary;
import com.friendlywings.automation.model.Passenger;
import com.friendlywings.automation.model.Route;
import com.friendlywings.automation.model.Trip;
import com.friendlywings.automation.pdf.PdfGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class VelocityFlightPdfGenerator implements PdfGenerator<FlightItinerary> {

    private static final Logger log = LoggerFactory.getLogger(VelocityFlightPdfGenerator.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final VelocityPdfService velocityPdfService;
    private final HtmlToPdfService htmlToPdfService;

    public VelocityFlightPdfGenerator(VelocityPdfService velocityPdfService, HtmlToPdfService htmlToPdfService) {
        this.velocityPdfService = velocityPdfService;
        this.htmlToPdfService = htmlToPdfService;
    }

    @Override
    public boolean supports(Booking booking) {
        return booking instanceof FlightItinerary;
    }

    @Override
    public Path generate(FlightItinerary booking) throws IOException {
        return generateWithTemplate(booking, "templates/flight-voucher-v2.vm");
    }

    public Path generateV1(FlightItinerary booking) throws IOException {
        return generateWithTemplate(booking, "templates/flight-voucher.vm");
    }

    private Path generateWithTemplate(FlightItinerary booking, String templatePath) throws IOException {
        Path outDir = Paths.get("output");
        Files.createDirectories(outDir);
        String suffix = templatePath.contains("v2") ? "_V2" : "";
        String filename = "FW_FLIGHT" + suffix + "_" + (booking.getBookingId() != null ? booking.getBookingId() : "UNKNOWN") + ".pdf";
        Path outPath = outDir.resolve(filename);

        Map<String, Object> context = buildContext(booking);
        String html = velocityPdfService.mergeTemplate(templatePath, context);
        byte[] pdfBytes = htmlToPdfService.convertToPdf(html);
        Files.write(outPath, pdfBytes);

        log.info("Generated flight PDF via Velocity ({}): {}", templatePath, outPath);
        return outPath;
    }

    private Map<String, Object> buildContext(FlightItinerary booking) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("tripId", booking.getBookingId());
        ctx.put("pnr", booking.getPnr());
        ctx.put("travellers", booking.getPassengers());

        List<Trip> trips = booking.getTrips();
        if (trips == null || trips.isEmpty()) {
            ctx.put("route", "");
            ctx.put("travelDate", "");
            ctx.put("segments", List.of());
            return ctx;
        }

        Trip firstTrip = trips.get(0);
        ctx.put("route", buildRouteLabel(trips));
        ctx.put("travelDate", firstTrip.getTripDate() != null ? firstTrip.getTripDate().format(DATE_FMT) : "");
        ctx.put("segments", formatSegments(flattenRoutes(trips)));
        return ctx;
    }

    private String buildRouteLabel(List<Trip> trips) {
        if (trips.size() == 1) {
            Trip t = trips.get(0);
            return joinCity(t.getOriginCity()) + " - " + joinCity(t.getDestinationCity());
        }
        return trips.stream()
                .map(t -> joinCity(t.getOriginCity()) + " - " + joinCity(t.getDestinationCity()))
                .collect(Collectors.joining(" / "));
    }

    private String joinCity(String city) {
        return city == null ? "" : city;
    }

    private List<Route> flattenRoutes(List<Trip> trips) {
        List<Route> all = new ArrayList<>();
        for (Trip trip : trips) {
            if (trip.getRoutes() != null) {
                all.addAll(trip.getRoutes());
            }
        }
        return all;
    }

    private List<Map<String, String>> formatSegments(List<Route> routes) {
        List<Map<String, String>> result = new ArrayList<>();
        if (routes == null) return result;
        for (Route seg : routes) {
            Map<String, String> m = new HashMap<>();
            m.put("airline", seg.getAirline());
            m.put("operatedBy", seg.getOperatedBy());
            m.put("flightNumber", seg.getFlightNumber());
            m.put("fareType", seg.getFareType());
            m.put("departureAirportCode", seg.getDepartureAirportCode());
            m.put("departureTime", seg.getDepartureTime() != null ? seg.getDepartureTime().format(TIME_FMT) : "");
            m.put("departureDate", seg.getDepartureDate() != null ? seg.getDepartureDate().format(DATE_FMT) : "");
            m.put("departureAirportName", seg.getDepartureAirportName());
            m.put("departureTerminal", seg.getDepartureTerminal());
            m.put("duration", seg.getDuration());
            m.put("travelClass", seg.getTravelClass());
            m.put("arrivalAirportCode", seg.getArrivalAirportCode());
            m.put("arrivalTime", seg.getArrivalTime() != null ? seg.getArrivalTime().format(TIME_FMT) : "");
            m.put("arrivalDate", seg.getArrivalDate() != null ? seg.getArrivalDate().format(DATE_FMT) : "");
            m.put("arrivalAirportName", seg.getArrivalAirportName());
            m.put("arrivalTerminal", seg.getArrivalTerminal());
            m.put("baggage", formatBaggage(seg.getCabinBaggage(), seg.getCheckInBaggage()));
            m.put("layover", seg.getLayover() != null ? seg.getLayover().getDuration() + " layover at " + seg.getLayover().getAirportCode() : "");
            result.add(m);
        }
        return result;
    }

    private String formatBaggage(String cabin, String checkIn) {
        StringBuilder sb = new StringBuilder();
        if (cabin != null && !cabin.isBlank()) {
            sb.append("Cabin: ").append(cabin);
        }
        if (checkIn != null && !checkIn.isBlank()) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append("Check-in: ").append(checkIn);
        }
        return sb.toString();
    }
}
