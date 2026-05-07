package com.complianceiq;

import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = OpenAiEmbeddingAutoConfiguration.class)
public class ComplianceIqApplication {
    public static void main(String[] args) {
        SpringApplication.run(ComplianceIqApplication.class, args);
    }
}
