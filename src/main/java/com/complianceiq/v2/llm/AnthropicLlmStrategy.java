package com.complianceiq.v2.llm;

import com.complianceiq.v2.dto.LlmAnswer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@Order(2)
public class AnthropicLlmStrategy implements LlmStrategy {

    private static final String MODEL = "claude-opus-4-7";
    private static final int MAX_TOKENS = 1024;

    @Value("${anthropic.api-key:}")
    private String apiKey;

    private RestClient client;
    private final ObjectMapper objectMapper;

    public AnthropicLlmStrategy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void init() {
        this.client = RestClient.builder()
                .baseUrl("https://api.anthropic.com/v1")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String name() {
        return "Anthropic";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public LlmAnswer call(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", MODEL,
                "max_tokens", MAX_TOKENS,
                "system", systemPrompt,
                "messages", List.of(
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            JsonNode response = client.post()
                    .uri("/messages")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String content = response.path("content").get(0).path("text").asText();
            return objectMapper.readValue(content, LlmAnswer.class);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new LlmAuthException(name(), e.getStatusCode().value());
            }
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Anthropic call failed: " + e.getMessage(), e);
        }
    }
}
