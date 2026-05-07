package com.complianceiq.v2.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QueryRequest {

    @NotBlank(message = "question is required")
    private String question;

    private int topK = 5;
    private String category;
}
