package com.friendlywings.automation.parser;

import com.friendlywings.automation.model.FlightItinerary;
import com.friendlywings.automation.service.GmailImapService;
import com.friendlywings.automation.service.OpenAiFlightParserService;
import com.friendlywings.automation.service.PdfTextExtractor;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Single parser for all flight e-ticket emails, regardless of provider.
 * An email is treated as a ticket candidate when it carries a PDF attachment;
 * the actual data extraction is delegated to the OpenAI-based parser, so no
 * per-provider parsing logic is needed.
 */
@Component
public class FlightTicketPdfParser {

    private static final Logger log = LoggerFactory.getLogger(FlightTicketPdfParser.class);

    private final GmailImapService gmailService;
    private final PdfTextExtractor pdfTextExtractor;
    private final OpenAiFlightParserService openAiFlightParserService;

    public FlightTicketPdfParser(GmailImapService gmailService,
                                 PdfTextExtractor pdfTextExtractor,
                                 OpenAiFlightParserService openAiFlightParserService) {
        this.gmailService = gmailService;
        this.pdfTextExtractor = pdfTextExtractor;
        this.openAiFlightParserService = openAiFlightParserService;
    }

    /** Any email carrying a PDF attachment is a ticket candidate. */
    public boolean canParse(Message message) throws MessagingException, IOException {
        return gmailService.hasPdfAttachment(message);
    }

    public FlightItinerary parse(Message message) throws MessagingException, IOException {
        FlightItinerary metadata = new FlightItinerary();
        metadata.setSource(getSender(message));
        metadata.setRawMessageId(getMessageId(message));
        metadata.setReceivedAt(LocalDate.now().atStartOfDay());

        try (InputStream pdfStream = gmailService.extractPdfAttachment(message)) {
            if (pdfStream == null) {
                log.warn("No PDF attachment found in email: {}", message.getSubject());
                return metadata;
            }
            FlightItinerary parsed = parsePdf(pdfStream);
            parsed.setSource(metadata.getSource());
            parsed.setRawMessageId(metadata.getRawMessageId());
            parsed.setReceivedAt(metadata.getReceivedAt());
            return parsed;
        }
    }

    /** Parse a standalone e-ticket PDF (used by the debug endpoint and the CLI runner). */
    public FlightItinerary parsePdf(InputStream inputStream) throws IOException {
        String text = pdfTextExtractor.extractText(inputStream);
        return openAiFlightParserService.parse(text);
    }

    private String getSender(Message message) throws MessagingException {
        var from = message.getFrom();
        return from != null && from.length > 0 ? from[0].toString() : null;
    }

    private String getMessageId(Message message) throws MessagingException {
        String[] ids = message.getHeader("Message-ID");
        return ids != null && ids.length > 0 ? ids[0] : UUID.randomUUID().toString();
    }
}
