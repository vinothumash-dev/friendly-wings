package com.friendlywings.automation.client;

import java.util.Map;

public interface OpenAiClient {

    Map<String, Object> chatCompletion(Map<String, Object> requestBody);
}
