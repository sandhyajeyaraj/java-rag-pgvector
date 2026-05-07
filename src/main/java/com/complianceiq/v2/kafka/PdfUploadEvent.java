package com.complianceiq.v2.kafka;

import java.time.Instant;

public record PdfUploadEvent(
        String fileName,
        String filePath,
        Instant uploadedAt
) {}
