package com.complianceiq.v2.llm;

import com.complianceiq.v2.dto.LlmAnswer;

public interface LlmStrategy {
    String name();
    boolean isConfigured();
    LlmAnswer call(String systemPrompt, String userPrompt);
}
