package com.complianceiq.v2.llm;

import com.complianceiq.v2.dto.LlmAnswer;

public record LlmResult(LlmAnswer answer, String provider) {}
