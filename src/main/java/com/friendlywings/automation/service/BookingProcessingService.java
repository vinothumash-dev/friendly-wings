package com.friendlywings.automation.service;

import com.friendlywings.automation.config.FriendlyWingsProperties;
import com.friendlywings.automation.model.Booking;
import com.friendlywings.automation.model.FlightBooking;
import com.friendlywings.automation.parser.BookingSource;
import com.friendlywings.automation.parser.EmailParser;
import com.friendlywings.automation.pdf.PdfGenerator;
import com.friendlywings.automation.repository.ProcessedEmail;
import com.friendlywings.automation.repository.ProcessedEmailRepository;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class BookingProcessingService {

    private static final Logger log = LoggerFactory.getLogger(BookingProcessingService.class);

    private final GmailImapService gmailService;
    private final EmailClassifier classifier;
    private final List<EmailParser> parsers;
    private final List<PdfGenerator<? extends Booking>> generators;
    private final ProcessedEmailRepository repository;
    private final FriendlyWingsProperties properties;
    private final EmailNotificationService emailNotificationService;

    public BookingProcessingService(GmailImapService gmailService,
                                    EmailClassifier classifier,
                                    List<EmailParser> parsers,
                                    List<PdfGenerator<? extends Booking>> generators,
                                    ProcessedEmailRepository repository,
                                    FriendlyWingsProperties properties,
                                    EmailNotificationService emailNotificationService) {
        this.gmailService = gmailService;
        this.classifier = classifier;
        this.parsers = parsers;
        this.generators = generators;
        this.repository = repository;
        this.properties = properties;
        this.emailNotificationService = emailNotificationService;
    }

    @Scheduled(fixedDelayString = "${friendlywings.gmail.poll-interval-minutes:30}00000")
    public void scheduledProcess() {
        log.info("Starting scheduled email processing...");
        processEmails();
    }

    public void processEmails() {
        try {
            List<Message> messages = gmailService.fetchUnprocessedEmails();
            for (Message message : messages) {
                processSingleEmail(message);
            }
        } catch (Exception e) {
            log.error("Error during email processing", e);
        }
    }

    public void searchAndProcessBySubject(String subjectTerm) {
        searchAndProcessBySubjectFromFolder(subjectTerm, properties.getGmail().getFolder());
    }

    public void searchAndProcessBySubjectFromFolder(String subjectTerm, String folderName) {
        try {
            List<Message> messages = gmailService.searchBySubjectInFolder(subjectTerm, folderName);
            if (messages.isEmpty()) {
                log.warn("No emails found with subject containing: {}", subjectTerm);
                return;
            }
            for (Message message : messages) {
                processSingleEmail(message);
            }
        } catch (Exception e) {
            log.error("Error searching emails by subject: {}", subjectTerm, e);
        }
    }

    public void processSingleEmail(Message message) {
        String messageId = null;
        Path pdfPath = null;
        Booking booking = null;
        try {
            messageId = getMessageId(message);
            if (repository.existsByMessageId(messageId)) {
                log.info("Email already processed: {}", messageId);
                gmailService.markAsProcessed(message);
                return;
            }

            BookingSource source = classifier.classify(message);
            log.info("Classified email {} as source: {}", messageId, source);

            if (source == BookingSource.UNKNOWN) {
                log.warn("Unknown source for email: {}", message.getSubject());
                saveStatus(messageId, message, null, null, ProcessedEmail.Status.SKIPPED, "Unknown source");
                return;
            }

            EmailParser parser = findParser(message);
            if (parser == null) {
                log.warn("No parser found for email: {}", message.getSubject());
                saveStatus(messageId, message, source.name(), null, ProcessedEmail.Status.SKIPPED, "No parser available");
                return;
            }

            booking = parser.parse(message);
            if (booking == null) {
                saveStatus(messageId, message, source.name(), null, ProcessedEmail.Status.PARSE_ERROR, "Parser returned null");
                return;
            }

            PdfGenerator generator = findGenerator(booking);
            if (generator == null) {
                saveStatus(messageId, message, source.name(), booking.getBookingType(), ProcessedEmail.Status.PDF_ERROR, "No PDF generator");
                return;
            }

            pdfPath = generator.generate(booking);
            log.info("Generated PDF: {}", pdfPath);

            saveStatus(messageId, message, source.name(), booking.getBookingType(), pdfPath.toString(), ProcessedEmail.Status.SUCCESS, null);

            // Send email notification with PDF attachment
            String tripId = (booking instanceof FlightBooking fb) ? fb.getTripId() : null;
            emailNotificationService.sendVoucher(pdfPath, booking.getBookingType(), tripId);

            gmailService.markAsProcessed(message);

        } catch (Exception e) {
            log.error("Failed to process email: {}", messageId, e);
            try {
                saveStatus(messageId, message, booking != null ? booking.getSource() : null,
                           booking != null ? booking.getBookingType() : null,
                           pdfPath != null ? pdfPath.toString() : null,
                           ProcessedEmail.Status.PARSE_ERROR, e.getMessage());
            } catch (MessagingException ex) {
                log.error("Failed to save error status", ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private PdfGenerator<Booking> findGenerator(Booking booking) {
        for (PdfGenerator<? extends Booking> gen : generators) {
            if (gen.supports(booking)) {
                return (PdfGenerator<Booking>) gen;
            }
        }
        return null;
    }

    private EmailParser findParser(Message message) throws MessagingException {
        for (EmailParser parser : parsers) {
            if (parser.canParse(message)) {
                return parser;
            }
        }
        return null;
    }

    private String getMessageId(Message message) throws MessagingException {
        String[] ids = message.getHeader("Message-ID");
        return ids != null && ids.length > 0 ? ids[0] : UUID.randomUUID().toString();
    }

    private void saveStatus(String messageId, Message message, String source, String bookingType,
                            ProcessedEmail.Status status, String error) throws MessagingException {
        saveStatus(messageId, message, source, bookingType, null, status, error);
    }

    private void saveStatus(String messageId, Message message, String source, String bookingType,
                            String pdfPath, ProcessedEmail.Status status, String error) throws MessagingException {
        ProcessedEmail pe = new ProcessedEmail();
        pe.setMessageId(messageId);
        pe.setSubject(message.getSubject());
        pe.setSender(message.getFrom()[0].toString());
        pe.setSourceSite(source);
        pe.setBookingType(bookingType);
        pe.setPdfPath(pdfPath);
        pe.setStatus(status);
        pe.setErrorMessage(error);
        repository.save(pe);
    }
}
