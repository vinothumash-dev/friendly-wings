package com.friendlywings.automation.service;

import com.friendlywings.automation.client.OpenAiClient;
import com.friendlywings.automation.config.FriendlyWingsProperties;
import com.friendlywings.automation.model.FlightItinerary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OpenAiFlightParserServiceTest {

    private FriendlyWingsProperties properties;
    private OpenAiClient openAiClient;
    private OpenAiFlightParserService service;

    @BeforeEach
    void setUp() {
        properties = new FriendlyWingsProperties();
        properties.getOpenai().setApiKey("test-api-key");
        properties.getOpenai().setModel("gpt-5.5");

        openAiClient = mock(OpenAiClient.class);
        service = new OpenAiFlightParserService(properties, openAiClient);
    }

    @Test
    void parseReturnsFlightItinerary() {
        String llmJson = """
            {"bookingId":"TEST123","pnr":"ABC123","passengers":[{"name":"Mr John Doe","type":"ADULT","pnr":"ABC123","ticketNumber":null,"passportNumber":null,"cabinBaggage":"7 Kgs","checkInBaggage":"15 Kgs"}],"trips":[{"originCity":"Delhi","destinationCity":"Mumbai","tripDate":"2026-06-15","totalDuration":"02 h 05 m","routes":[{"airline":"IndiGo","flightNumber":"6E 123","pnr":"ABC123","originCity":"Delhi","destinationCity":"Mumbai","departureAirportCode":"DEL","departureAirportName":"Indira Gandhi International Airport","departureTerminal":"T1","departureDate":"2026-06-15","departureTime":"10:00","arrivalAirportCode":"BOM","arrivalAirportName":"Chhatrapati Shivaji Maharaj International Airport","arrivalTerminal":null,"arrivalDate":"2026-06-15","arrivalTime":"12:05","duration":"02 h 05 m","travelClass":"Economy","cabinBaggage":"7 Kgs","checkInBaggage":"15 Kgs","layover":null}]}]}
            """;

        when(openAiClient.chatCompletion(any())).thenReturn(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", llmJson)))
        ));

        FlightItinerary itinerary = service.parse("Some PDF text");

        assertEquals("TEST123", itinerary.getBookingId());
        assertEquals("ABC123", itinerary.getPnr());
        assertEquals(1, itinerary.getPassengers().size());
        assertEquals("Mr John Doe", itinerary.getPassengers().get(0).getName());
        assertEquals(1, itinerary.getTrips().size());
        assertEquals("Delhi", itinerary.getTrips().get(0).getOriginCity());
        assertEquals(1, itinerary.getTrips().get(0).getRoutes().size());
        assertEquals("6E 123", itinerary.getTrips().get(0).getRoutes().get(0).getFlightNumber());
    }

    @Test
    void parseThrowsWhenResponseHasNoChoices() {
        when(openAiClient.chatCompletion(any())).thenReturn(Map.of());
        assertThrows(RuntimeException.class, () -> service.parse("text"));
    }
}
