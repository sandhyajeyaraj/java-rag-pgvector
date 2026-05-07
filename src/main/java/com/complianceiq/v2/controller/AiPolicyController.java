package com.complianceiq.v2.controller;

import com.complianceiq.common.model.ApiResponse;
import com.complianceiq.v2.dto.IngestRequest;
import com.complianceiq.v2.dto.QueryRequest;
import com.complianceiq.v2.dto.QueryResponse;
import com.complianceiq.v2.service.DocumentIngestionService;
import com.complianceiq.v2.service.PdfProcessingService;
import com.complianceiq.v2.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v2/policies")
@RequiredArgsConstructor
public class AiPolicyController {

    private final DocumentIngestionService ingestionService;
    private final PdfProcessingService pdfProcessingService;
    private final RagService ragService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<ApiResponse<Map<String, Object>>>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        String resolvedTitle = (title != null && !title.isBlank())
                ? title
                : file.getOriginalFilename().replace(".pdf", "");
        return pdfProcessingService.processAndIngest(file, resolvedTitle, category)
                .thenApply(chunks -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(Map.of(
                                "document", resolvedTitle,
                                "chunks_indexed", chunks,
                                "status", "indexed"
                        ))));
    }

    @PostMapping("/ingest")
    public CompletableFuture<ResponseEntity<ApiResponse<Map<String, Object>>>> ingest(
            @Valid @RequestBody IngestRequest request) {
        return ingestionService.ingestDocument(request)
                .thenApply(chunks -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(Map.of(
                                "document", request.getTitle(),
                                "chunks_indexed", chunks,
                                "status", "indexed"
                        ))));
    }

    @PostMapping("/query")
    public CompletableFuture<ResponseEntity<ApiResponse<QueryResponse>>> query(
            @Valid @RequestBody QueryRequest request) {
        return ragService.query(request)
                .thenApply(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }
}
