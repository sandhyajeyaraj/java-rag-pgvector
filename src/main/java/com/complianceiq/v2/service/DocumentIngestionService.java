package com.complianceiq.v2.service;

import com.complianceiq.v2.dto.IngestRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    private final VectorStore vectorStore;

    @Async("aiThreadPool")
    public CompletableFuture<Integer> ingestDocument(IngestRequest request) {
        List<String> chunks = chunkText(request.getContent());
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", request.getTitle());
            metadata.put("category", request.getCategory() != null ? request.getCategory() : "GENERAL");
            metadata.put("source", request.getSource() != null ? request.getSource() : "manual");
            metadata.put("chunk_index", i);
            metadata.put("total_chunks", chunks.size());

            documents.add(new Document(chunks.get(i), metadata));
        }

        vectorStore.add(documents);
        log.info("Ingested {} chunks from '{}'", chunks.size(), request.getTitle());
        return CompletableFuture.completedFuture(chunks.size());
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end));
            start += CHUNK_SIZE - CHUNK_OVERLAP;
        }
        return chunks;
    }
}
