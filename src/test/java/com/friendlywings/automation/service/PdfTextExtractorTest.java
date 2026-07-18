package com.friendlywings.automation.service;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void extractOneWayDirectText() throws Exception {
        InputStream is = getClass().getResourceAsStream("/makemytrip/E-Ticket_NF7ALRND77183465468.pdf");
        assertNotNull(is);
        String text = extractor.extractText(is);
        assertNotNull(text);
        assertTrue(text.contains("Booking ID"));
        assertTrue(text.contains("PNR"));
        assertTrue(text.contains("IndiGo") || text.contains("6E"));
    }

    @Test
    void extractRoundTripText() throws Exception {
        InputStream is = getClass().getResourceAsStream("/makemytrip/E-Ticket_NN7AJXJJ27432955064.pdf");
        assertNotNull(is);
        String text = extractor.extractText(is);
        assertNotNull(text);
        assertTrue(text.contains("Booking ID"));
        assertTrue(text.contains("Etihad Airways") || text.contains("EY"));
    }
}
