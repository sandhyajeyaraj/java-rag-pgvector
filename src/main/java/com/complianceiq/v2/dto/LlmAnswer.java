package com.complianceiq.v2.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

public record LlmAnswer(
        String summary,
        String detailedExplanation,
        String riskLevel,
        String complianceCategory,
        @JsonDeserialize(using = FlexibleStringListDeserializer.class) List<String> keyFindings,
        @JsonDeserialize(using = FlexibleStringListDeserializer.class) List<String> actionItems,
        @JsonDeserialize(using = FlexibleStringListDeserializer.class) List<String> referencedPolicies,
        boolean coveredByAvailablePolicies
) {}
