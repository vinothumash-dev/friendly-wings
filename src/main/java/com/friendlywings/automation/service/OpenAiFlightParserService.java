package com.friendlywings.automation.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.friendlywings.automation.client.OpenAiClient;
import com.friendlywings.automation.config.FriendlyWingsProperties;
import com.friendlywings.automation.model.FlightItinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OpenAiFlightParserService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiFlightParserService.class);

    private static final String SYSTEM_PROMPT = """
        You are a flight booking data extraction assistant. Your job is to read the raw text extracted from an airline e-ticket PDF and return a structured JSON object matching the exact schema below.

        Rules:
        1. Extract only factual information present in the text. Do not invent or guess values.
        2. Return a single valid JSON object. Do not include markdown formatting, explanations, or extra text.
        3. Use ISO-8601 date format for all dates (yyyy-MM-dd).
        4. Use 24-hour HH:MM format for all times.
        5. Airport codes are 3-letter IATA codes (e.g., DEL, BOM, DXB).
        6. PNR is typically a 6-character alphanumeric code.
        7. For baggage, preserve the original text like "7 Kgs (1 piece)" or "25 Kgs".
        8. For duration, preserve the original text like "01 h 15 m" or "5h 20m".
        9. If a field is not present in the text, use null or an empty string, not "N/A".
        10. One-way booking = 1 trip. Round-trip = 2 trips. Multi-city = n trips.
        11. Direct flight = 1 route per trip. Connecting flight = multiple routes per trip.
        12. Store layover information on the route that PRECEDES the layover.

        Expected JSON schema:
        {
          "bookingId": "string or null",
          "pnr": "string or null",
          "passengers": [
            {
              "name": "string with title, e.g. Mr Stanislav Kulikov",
              "type": "ADULT or CHILD or INFANT",
              "pnr": "string or null",
              "ticketNumber": "string or null",
              "passportNumber": "string or null",
              "cabinBaggage": "string or null",
              "checkInBaggage": "string or null"
            }
          ],
          "trips": [
            {
              "originCity": "string",
              "destinationCity": "string",
              "tripDate": "yyyy-MM-dd",
              "totalDuration": "string or null",
              "routes": [
                {
                  "airline": "string",
                  "flightNumber": "string e.g. EY 842",
                  "pnr": "string or null",
                  "originCity": "string or null",
                  "destinationCity": "string or null",
                  "departureAirportCode": "3-letter code",
                  "departureAirportName": "string or null",
                  "departureTerminal": "string or null",
                  "departureDate": "yyyy-MM-dd",
                  "departureTime": "HH:MM",
                  "arrivalAirportCode": "3-letter code",
                  "arrivalAirportName": "string or null",
                  "arrivalTerminal": "string or null",
                  "arrivalDate": "yyyy-MM-dd",
                  "arrivalTime": "HH:MM",
                  "duration": "string or null",
                  "travelClass": "Economy/Business/First/Premium Economy or null",
                  "cabinBaggage": "string or null",
                  "checkInBaggage": "string or null",
                  "layover": {
                    "duration": "string or null",
                    "airportCode": "3-letter code or null",
                    "airportName": "string or null"
                  } or null
                }
              ]
            }
          ]
        }

        Example 1 - One-way direct:
        Input text contains: Booking ID:NF7ALRND77183465468, PNR: IBBL7D, passenger Ms Karri Laxmi Prasanna, IndiGo flight 6E 783, VTZ 16:25 to HYD 17:40 on 2026-06-13.
        Output:
        {
          "bookingId": "NF7ALRND77183465468",
          "pnr": "IBBL7D",
          "passengers": [{"name":"Ms Karri Laxmi Prasanna","type":"ADULT","pnr":"IBBL7D","ticketNumber":null,"passportNumber":null,"cabinBaggage":"7 Kgs (1 piece)","checkInBaggage":"15 Kgs (1 piece)"}],
          "trips": [{
            "originCity": "Visakhapatnam",
            "destinationCity": "Hyderabad",
            "tripDate": "2026-06-13",
            "totalDuration": "01 h 15 m",
            "routes": [{
              "airline": "IndiGo",
              "flightNumber": "6E 783",
              "pnr": "IBBL7D",
              "originCity": "Visakhapatnam",
              "destinationCity": "Hyderabad",
              "departureAirportCode": "VTZ",
              "departureAirportName": "Visakhapatnam International Airport",
              "departureTerminal": null,
              "departureDate": "2026-06-13",
              "departureTime": "16:25",
              "arrivalAirportCode": "HYD",
              "arrivalAirportName": "Rajiv Gandhi International Airport",
              "arrivalTerminal": null,
              "arrivalDate": "2026-06-13",
              "arrivalTime": "17:40",
              "duration": "01 h 15 m",
              "travelClass": "Economy",
              "cabinBaggage": "7 Kgs (1 piece)",
              "checkInBaggage": "15 Kgs (1 piece)",
              "layover": null
            }]
          }]
        }

        Example 2 - Round trip with connection:
        Input text contains: Booking ID:NN7AJXJJ27432955064, PNR: 7BNHD5, passenger Mr Stanislav Kulikov, Etihad Airways flights EY 842 SVO-AUH and EY 488 AUH-KUL on 2026-03-11, return EY 487 KUL-AUH and EY 841 AUH-SVO on 2026-03-18-19.
        Output should contain 2 trips, each with 2 routes, and layovers on the first route of each trip.
        """;

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");

    private final FriendlyWingsProperties properties;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public OpenAiFlightParserService(FriendlyWingsProperties properties, OpenAiClient openAiClient) {
        this.properties = properties;
        this.openAiClient = openAiClient;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public FlightItinerary parse(String pdfText) {
        Map<String, Object> requestBody = new HashMap<>(Map.of(
                "model", properties.getOpenai().getModel(),
                "max_completion_tokens", properties.getOpenai().getMaxTokens(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", "Extract the flight booking from this PDF text:\n\n" + pdfText)
                )
        ));
        // Reasoning models (gpt-5*, o1, o3) reject a non-default temperature; only the default (1) is allowed
        if (!isReasoningModel(properties.getOpenai().getModel())) {
            requestBody.put("temperature", properties.getOpenai().getTemperature());
        }

        Map<String, Object> responseBody = openAiClient.chatCompletion(requestBody);
        return extractItinerary(responseBody);
    }

    private boolean isReasoningModel(String model) {
        return model != null && (model.startsWith("gpt-5") || model.startsWith("o1") || model.startsWith("o3"));
    }

    @SuppressWarnings("unchecked")
    private FlightItinerary extractItinerary(Map<String, Object> responseBody) {
        if (responseBody == null) {
            throw new RuntimeException("OpenAI returned empty response");
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("OpenAI returned no choices");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            throw new RuntimeException("OpenAI returned no message");
        }
        String content = (String) message.get("content");
        if (content == null || content.isBlank()) {
            throw new RuntimeException("OpenAI returned empty content");
        }

        String json = extractJson(content);
        try {
            return objectMapper.readValue(json, FlightItinerary.class);
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response as FlightItinerary. JSON: {}", json, e);
            throw new RuntimeException("Failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }

    private String extractJson(String content) {
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return content.trim();
    }
}
