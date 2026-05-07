package com.complianceiq.v2.llm;

import com.complianceiq.v2.dto.LlmAnswer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@Order(3)
public class OllamaLlmStrategy implements LlmStrategy {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:llama3.2}")
    private String model;

    private RestClient client;
    private final ObjectMapper objectMapper;

    public OllamaLlmStrategy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void init() {
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String name() {
        return "Ollama";
    }

    @Override
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    @Override
    public LlmAnswer call(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "format", "json",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            JsonNode response = client.post()
                    .uri("/api/chat")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String content = response.path("message").path("content").asText();
            return objectMapper.readValue(content, LlmAnswer.class);

        } catch (ResourceAccessException e) {
            throw new RuntimeException("Ollama is not reachable at " + baseUrl + " — is it running?", e);
        } catch (Exception e) {
            throw new RuntimeException("Ollama call failed: " + e.getMessage(), e);
        }
    }
}
