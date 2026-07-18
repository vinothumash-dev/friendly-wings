package com.friendlywings.automation.client;

import com.friendlywings.automation.config.FriendlyWingsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class RestTemplateOpenAiClient implements OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateOpenAiClient.class);

    private final FriendlyWingsProperties properties;
    private final RestTemplate restTemplate;

    public RestTemplateOpenAiClient(FriendlyWingsProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public Map<String, Object> chatCompletion(Map<String, Object> requestBody) {
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured. Set the OPENAI_API_KEY environment variable.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    properties.getOpenai().getBaseUrl() + "/chat/completions",
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            return response.getBody();
        } catch (RestClientResponseException e) {
            log.error("OpenAI API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API call failed: " + e.getMessage(), e);
        }
    }
}
