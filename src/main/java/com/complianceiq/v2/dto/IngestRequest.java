package com.complianceiq.v2.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IngestRequest {

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "content is required")
    private String content;

    private String category;
    private String source;
}
