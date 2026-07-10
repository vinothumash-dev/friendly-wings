package com.friendlywings.automation.service;

import com.friendlywings.automation.config.FriendlyWingsProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final FriendlyWingsProperties properties;
    private JavaMailSender mailSender;

    public EmailNotificationService(FriendlyWingsProperties properties) {
        this.properties = properties;
        this.mailSender = createMailSender();
    }

    public void sendVoucher(Path pdfPath, String bookingType, String tripId) {
        List<String> recipients = properties.getNotification().getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            log.info("No notification recipients configured. Skipping email send.");
            return;
        }

        String subject = properties.getNotification().getSubjectPrefix()
                + " - " + bookingType
                + (tripId != null ? " (" + tripId + ")" : "");
        String body = properties.getNotification().getBodyText();

        for (String to : recipients) {
            if (to == null || to.isBlank()) continue;
            try {
                sendEmailWithAttachment(to.trim(), subject, body, pdfPath.toFile());
                log.info("Voucher email sent to {}", to.trim());
            } catch (MessagingException e) {
                log.error("Failed to send voucher email to {}", to.trim(), e);
            }
        }
    }

    private void sendEmailWithAttachment(String to, String subject, String body, File attachment)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(properties.getSmtp().getUsername());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body);
        helper.addAttachment(attachment.getName(), attachment);
        mailSender.send(message);
    }

    private JavaMailSender createMailSender() {
        FriendlyWingsProperties.Smtp smtp = properties.getSmtp();
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtp.getHost());
        sender.setPort(smtp.getPort());
        sender.setUsername(smtp.getUsername());
        sender.setPassword(smtp.getPassword());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(smtp.isAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtp.isStarttls()));

        return sender;
    }
}
