package com.friendlywings.automation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "friendlywings")
public class FriendlyWingsProperties {

    private Gmail gmail = new Gmail();
    private Output output = new Output();
    private Smtp smtp = new Smtp();
    private Notification notification = new Notification();
    private OpenAi openai = new OpenAi();

    public Gmail getGmail() {
        return gmail;
    }

    public void setGmail(Gmail gmail) {
        this.gmail = gmail;
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public Smtp getSmtp() {
        return smtp;
    }

    public void setSmtp(Smtp smtp) {
        this.smtp = smtp;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }

    public static class OpenAi {
        private String apiKey;
        private String model = "gpt-5.5";
        private String baseUrl = "https://api.openai.com/v1";
        private int maxTokens = 4000;
        private double temperature = 0.1;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
    }

    public static class Gmail {
        private String username;
        private String password;
        private String host = "imap.gmail.com";
        private int port = 993;
        private String protocol = "imaps";
        private int pollIntervalMinutes = 30;
        private String folder = "INBOX";
        private boolean moveProcessed = true;
        private String processedFolder = "Processed";

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        public int getPollIntervalMinutes() { return pollIntervalMinutes; }
        public void setPollIntervalMinutes(int pollIntervalMinutes) { this.pollIntervalMinutes = pollIntervalMinutes; }
        public String getFolder() { return folder; }
        public void setFolder(String folder) { this.folder = folder; }
        public boolean isMoveProcessed() { return moveProcessed; }
        public void setMoveProcessed(boolean moveProcessed) { this.moveProcessed = moveProcessed; }
        public String getProcessedFolder() { return processedFolder; }
        public void setProcessedFolder(String processedFolder) { this.processedFolder = processedFolder; }
    }

    public static class Output {
        private String directory = "./output";

        public String getDirectory() { return directory; }
        public void setDirectory(String directory) { this.directory = directory; }
    }

    public static class Smtp {
        private String host = "smtp.gmail.com";
        private int port = 587;
        private String username;
        private String password;
        private boolean auth = true;
        private boolean starttls = true;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public boolean isAuth() { return auth; }
        public void setAuth(boolean auth) { this.auth = auth; }
        public boolean isStarttls() { return starttls; }
        public void setStarttls(boolean starttls) { this.starttls = starttls; }
    }

    public static class Notification {
        private List<String> recipients = List.of();
        private String subjectPrefix = "[Friendly Wings] New Booking Voucher";
        private String bodyText = "Please find the attached booking voucher PDF.";

        public List<String> getRecipients() { return recipients; }
        public void setRecipients(List<String> recipients) { this.recipients = recipients; }
        public String getSubjectPrefix() { return subjectPrefix; }
        public void setSubjectPrefix(String subjectPrefix) { this.subjectPrefix = subjectPrefix; }
        public String getBodyText() { return bodyText; }
        public void setBodyText(String bodyText) { this.bodyText = bodyText; }
    }
}
