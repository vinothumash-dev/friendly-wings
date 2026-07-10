package com.friendlywings.automation.service;

import com.friendlywings.automation.config.FriendlyWingsProperties;
import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class GmailImapService {

    private static final Logger log = LoggerFactory.getLogger(GmailImapService.class);

    private final FriendlyWingsProperties properties;
    private Store store;

    public GmailImapService(FriendlyWingsProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        // Defer connection to first use if credentials not configured
        if (properties.getGmail().getPassword() == null || properties.getGmail().getPassword().isBlank()) {
            log.warn("Gmail password not configured. Skipping IMAP connection at startup.");
            return;
        }
        try {
            connect();
        } catch (MessagingException e) {
            log.error("Failed to connect to Gmail IMAP at startup", e);
        }
    }

    public void connect() throws MessagingException {
        var props = new Properties();
        props.put("mail.store.protocol", properties.getGmail().getProtocol());
        props.put("mail.imaps.host", properties.getGmail().getHost());
        props.put("mail.imaps.port", properties.getGmail().getPort());
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.ssl.trust", "*");

        Session session = Session.getInstance(props, null);
        store = session.getStore(properties.getGmail().getProtocol());
        store.connect(
                properties.getGmail().getHost(),
                properties.getGmail().getPort(),
                properties.getGmail().getUsername(),
                properties.getGmail().getPassword()
        );
        log.info("Connected to Gmail IMAP");
    }

    public List<Message> fetchUnprocessedEmails() throws MessagingException {
        ensureConnected();
        Folder inbox = store.getFolder(properties.getGmail().getFolder());
        inbox.open(Folder.READ_WRITE);

        Date startOfToday = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        SearchTerm term = new AndTerm(
                new FlagTerm(new Flags(Flags.Flag.SEEN), false),
                new ReceivedDateTerm(ComparisonTerm.GE, startOfToday)
        );
        Message[] messages = inbox.search(term);
        List<Message> result = new ArrayList<>(Arrays.asList(messages));
        log.info("Fetched {} unseen messages from today ({}) in {}", result.size(), LocalDate.now(), properties.getGmail().getFolder());
        return result;
    }

    public List<Message> searchBySubject(String subjectTerm) throws MessagingException {
        return searchBySubjectInFolder(subjectTerm, properties.getGmail().getFolder());
    }

    public List<Message> searchBySubjectInFolder(String subjectTerm, String folderName) throws MessagingException {
        ensureConnected();
        Folder folder = store.getFolder(folderName);
        folder.open(Folder.READ_WRITE);

        Message[] messages = folder.search(new SubjectTerm(subjectTerm));
        List<Message> result = new ArrayList<>(Arrays.asList(messages));
        log.info("Found {} messages matching subject '{}' in {}", result.size(), subjectTerm, folderName);
        return result;
    }

    public void markAsProcessed(Message message) throws MessagingException {
        if (!properties.getGmail().isMoveProcessed()) {
            message.setFlag(Flags.Flag.SEEN, true);
            return;
        }

        Folder inbox = store.getFolder(properties.getGmail().getFolder());
        Folder processed = store.getFolder(properties.getGmail().getProcessedFolder());
        if (!processed.exists()) {
            processed.create(Folder.HOLDS_MESSAGES);
        }
        inbox.open(Folder.READ_WRITE);
        processed.open(Folder.READ_WRITE);
        inbox.copyMessages(new Message[]{message}, processed);
        message.setFlag(Flags.Flag.DELETED, true);
        inbox.expunge();
    }

    public String extractTextContent(Message message) throws MessagingException, IOException {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof MimeMultipart multipart) {
            return extractTextFromMultipart(multipart);
        }
        return "";
    }

    private String extractTextFromMultipart(MimeMultipart multipart) throws MessagingException, IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                sb.append(part.getContent().toString());
            } else if (part.getContent() instanceof MimeMultipart inner) {
                sb.append(extractTextFromMultipart(inner));
            }
        }
        return sb.toString();
    }

    private void ensureConnected() throws MessagingException {
        if (store == null || !store.isConnected()) {
            connect();
        }
    }
}
