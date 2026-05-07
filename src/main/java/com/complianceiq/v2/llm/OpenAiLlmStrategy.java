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
@Order(1)
public class OpenAiLlmStrategy implements LlmStrategy {

    private static final String MODEL = "gpt-4o";
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_TOKENS = 1024;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    private RestClient client;
    private final ObjectMapper objectMapper;

    public OpenAiLlmStrategy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void init() {
        this.client = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String name() {
        return "OpenAI";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public LlmAnswer call(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", MODEL,
                "temperature", TEMPERATURE,
                "max_tokens", MAX_TOKENS,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            JsonNode response = client.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String content = response.path("choices").get(0).path("message").path("content").asText();
            return objectMapper.readValue(content, LlmAnswer.class);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new LlmAuthException(name(), e.getStatusCode().value());
            }
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OpenAI call failed: " + e.getMessage(), e);
        }
    }
}
