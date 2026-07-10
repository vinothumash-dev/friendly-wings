package com.friendlywings.automation.repository;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_emails")
public class ProcessedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", unique = true, nullable = false, length = 512)
    private String messageId;

    @Column(name = "subject", length = 1024)
    private String subject;

    @Column(name = "sender", length = 512)
    private String sender;

    @Column(name = "source_site", length = 100)
    private String sourceSite;

    @Column(name = "booking_type", length = 50)
    private String bookingType;

    @Column(name = "pdf_path", length = 2048)
    private String pdfPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private Status status;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getSourceSite() { return sourceSite; }
    public void setSourceSite(String sourceSite) { this.sourceSite = sourceSite; }
    public String getBookingType() { return bookingType; }
    public void setBookingType(String bookingType) { this.bookingType = bookingType; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public enum Status {
        SUCCESS,
        PARSE_ERROR,
        PDF_ERROR,
        SKIPPED
    }
}
