package com.friendlywings.automation.pdf.velocity;

import com.friendlywings.automation.model.Booking;
import com.friendlywings.automation.model.FlightBooking;
import com.friendlywings.automation.model.FlightSegment;
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

@Component
public class VelocityFlightPdfGenerator implements PdfGenerator<FlightBooking> {

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
        return booking instanceof FlightBooking;
    }

    @Override
    public Path generate(FlightBooking booking) throws IOException {
        return generateWithTemplate(booking, "templates/flight-voucher-v2.vm");
    }

    public Path generateV1(FlightBooking booking) throws IOException {
        return generateWithTemplate(booking, "templates/flight-voucher.vm");
    }

    private Path generateWithTemplate(FlightBooking booking, String templatePath) throws IOException {
        Path outDir = Paths.get("output");
        Files.createDirectories(outDir);
        String suffix = templatePath.contains("v2") ? "_V2" : "";
        String filename = "FW_FLIGHT" + suffix + "_" + (booking.getTripId() != null ? booking.getTripId() : "UNKNOWN") + ".pdf";
        Path outPath = outDir.resolve(filename);

        Map<String, Object> context = buildContext(booking);
        String html = velocityPdfService.mergeTemplate(templatePath, context);
        byte[] pdfBytes = htmlToPdfService.convertToPdf(html);
        Files.write(outPath, pdfBytes);

        log.info("Generated flight PDF via Velocity ({}): {}", templatePath, outPath);
        return outPath;
    }

    private Map<String, Object> buildContext(FlightBooking booking) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("tripId", booking.getTripId());
        ctx.put("route", booking.getRoute());
        ctx.put("travelDate", booking.getTravelDate() != null ? booking.getTravelDate().format(DATE_FMT) : "");
        ctx.put("pnr", booking.getPnr());
        ctx.put("segments", formatSegments(booking.getSegments()));
        ctx.put("travellers", booking.getTravellers());
        return ctx;
    }

    private List<Map<String, String>> formatSegments(List<FlightSegment> segments) {
        List<Map<String, String>> result = new ArrayList<>();
        if (segments == null) return result;
        for (FlightSegment seg : segments) {
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
            m.put("baggage", seg.getBaggage());
            m.put("layover", seg.getLayover());
            result.add(m);
        }
        return result;
    }
}
