package com.complianceiq.v2.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QueryResponse {
    private String summary;
    private String detailedExplanation;
    private String riskLevel;
    private String complianceCategory;
    private List<String> keyFindings;
    private List<String> actionItems;
    private List<String> referencedPolicies;
    private boolean coveredByAvailablePolicies;
    private List<String> sources;
    private double confidence;
    private String llmProvider;
    private boolean cachedResult;
}
