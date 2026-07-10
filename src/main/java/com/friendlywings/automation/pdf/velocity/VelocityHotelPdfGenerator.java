package com.friendlywings.automation.pdf.velocity;

import com.friendlywings.automation.model.Booking;
import com.friendlywings.automation.model.HotelBooking;
import com.friendlywings.automation.pdf.PdfGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class VelocityHotelPdfGenerator implements PdfGenerator<HotelBooking> {

    private static final Logger log = LoggerFactory.getLogger(VelocityHotelPdfGenerator.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, d MMMM yyyy");

    private final VelocityPdfService velocityPdfService;
    private final HtmlToPdfService htmlToPdfService;

    public VelocityHotelPdfGenerator(VelocityPdfService velocityPdfService, HtmlToPdfService htmlToPdfService) {
        this.velocityPdfService = velocityPdfService;
        this.htmlToPdfService = htmlToPdfService;
    }

    @Override
    public boolean supports(Booking booking) {
        return booking instanceof HotelBooking;
    }

    @Override
    public Path generate(HotelBooking booking) throws IOException {
        return generateWithTemplate(booking, "templates/hotel-voucher-v2.vm");
    }

    public Path generateV1(HotelBooking booking) throws IOException {
        return generateWithTemplate(booking, "templates/hotel-voucher.vm");
    }

    private Path generateWithTemplate(HotelBooking booking, String templatePath) throws IOException {
        Path outDir = Paths.get("output");
        Files.createDirectories(outDir);
        String suffix = templatePath.contains("v2") ? "_V2" : "";
        String filename = "FW_HOTEL" + suffix + "_" + (booking.getBookingRef() != null ? booking.getBookingRef() : "UNKNOWN") + ".pdf";
        Path outPath = outDir.resolve(filename);

        Map<String, Object> context = buildContext(booking);
        String html = velocityPdfService.mergeTemplate(templatePath, context);
        byte[] pdfBytes = htmlToPdfService.convertToPdf(html);
        Files.write(outPath, pdfBytes);

        log.info("Generated hotel PDF via Velocity ({}): {}", templatePath, outPath);
        return outPath;
    }

    private Map<String, Object> buildContext(HotelBooking b) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("bookingId", b.getBookingId());
        ctx.put("bookingRef", b.getBookingRef());
        ctx.put("client", b.getClient());
        ctx.put("memberId", b.getMemberId());
        ctx.put("countryOfResidence", b.getCountryOfResidence());
        ctx.put("property", b.getProperty());
        ctx.put("hotelName", b.getHotelName());
        ctx.put("address", b.getAddress());
        ctx.put("propertyContactNumber", b.getPropertyContactNumber());
        ctx.put("rooms", b.getRooms());
        ctx.put("extraBeds", b.getExtraBeds());
        ctx.put("adults", b.getAdults());
        ctx.put("children", b.getChildren());
        ctx.put("roomType", b.getRoomType());
        ctx.put("promotion", b.getPromotion());
        ctx.put("mealsIncluded", b.getMealsIncluded());
        ctx.put("cancellationPolicy", b.getCancellationPolicy());
        ctx.put("benefitsIncluded", b.getBenefitsIncluded());
        ctx.put("checkIn", b.getCheckIn() != null ? b.getCheckIn().format(DATE_FMT) : "");
        ctx.put("checkOut", b.getCheckOut() != null ? b.getCheckOut().format(DATE_FMT) : "");
        ctx.put("checkInTime", b.getCheckInTime());
        ctx.put("checkOutTime", b.getCheckOutTime());
        ctx.put("paymentMethod", b.getPaymentMethod());
        ctx.put("cardNo", b.getCardNo());
        ctx.put("guestRef", b.getGuestRef());
        ctx.put("remarks", b.getRemarks());
        ctx.put("guestList", b.getGuestList());
        if (b.getCheckIn() != null && b.getCheckOut() != null) {
            long nights = java.time.temporal.ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut());
            ctx.put("nights", nights > 0 ? String.valueOf(nights) : "");
        } else {
            ctx.put("nights", "");
        }
        return ctx;
    }
}
