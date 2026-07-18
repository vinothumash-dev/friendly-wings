package com.friendlywings.automation.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.friendlywings.automation.client.RestTemplateOpenAiClient;
import com.friendlywings.automation.config.FriendlyWingsProperties;
import com.friendlywings.automation.model.FlightItinerary;
import com.friendlywings.automation.service.OpenAiFlightParserService;
import com.friendlywings.automation.service.PdfTextExtractor;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Standalone runner: parses e-ticket PDFs with the OpenAI-based parser and prints
 * the resulting {@link FlightItinerary} as JSON. Does not boot the Spring app.
 *
 * Usage:
 *   java com.friendlywings.automation.tools.PdfParserRunner [pdf-directory]
 *
 * Config is read from src/main/resources/application.yml (friendlywings.openai.*).
 * Env overrides: OPENAI_API_KEY, OPENAI_MODEL.
 */
public class PdfParserRunner {

    public static void main(String[] args) throws Exception {
        Path pdfDir = Path.of(args.length > 0 ? args[0] : "src/main/resources/makemytrip");

        FriendlyWingsProperties properties = loadProperties(Path.of("src/main/resources/application.yml"));
        applyEnvOverrides(properties.getOpenai());

        RestTemplateOpenAiClient openAiClient = new RestTemplateOpenAiClient(properties, new RestTemplate());
        OpenAiFlightParserService parserService = new OpenAiFlightParserService(properties, openAiClient);
        PdfTextExtractor pdfTextExtractor = new PdfTextExtractor();

        ObjectMapper out = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);

        try (Stream<Path> files = Files.list(pdfDir)) {
            for (Path pdf : files.filter(p -> p.toString().toLowerCase().endsWith(".pdf")).sorted().toList()) {
                System.out.println("=== " + pdf.getFileName() + " ===");
                try (InputStream in = Files.newInputStream(pdf)) {
                    String text = pdfTextExtractor.extractText(in);
                    FlightItinerary itinerary = parserService.parse(text);
                    System.out.println(out.writeValueAsString(itinerary));
                } catch (Exception e) {
                    System.out.println("FAILED: " + e.getMessage());
                }
                System.out.println();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static FriendlyWingsProperties loadProperties(Path applicationYml) throws Exception {
        FriendlyWingsProperties properties = new FriendlyWingsProperties();
        try (InputStream in = new FileInputStream(applicationYml.toFile())) {
            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> friendlywings = (Map<String, Object>) root.get("friendlywings");
            Map<String, Object> openai = friendlywings != null
                    ? (Map<String, Object>) friendlywings.get("openai") : null;
            if (openai != null) {
                FriendlyWingsProperties.OpenAi cfg = properties.getOpenai();
                if (openai.get("api-key") != null) cfg.setApiKey(String.valueOf(openai.get("api-key")));
                if (openai.get("model") != null) cfg.setModel(String.valueOf(openai.get("model")));
                if (openai.get("base-url") != null) cfg.setBaseUrl(String.valueOf(openai.get("base-url")));
                if (openai.get("max-tokens") != null) cfg.setMaxTokens(((Number) openai.get("max-tokens")).intValue());
                if (openai.get("temperature") != null) cfg.setTemperature(((Number) openai.get("temperature")).doubleValue());
            }
        }
        return properties;
    }

    private static void applyEnvOverrides(FriendlyWingsProperties.OpenAi openai) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) openai.setApiKey(apiKey);
        String model = System.getenv("OPENAI_MODEL");
        if (model != null && !model.isBlank()) openai.setModel(model);
    }
}
