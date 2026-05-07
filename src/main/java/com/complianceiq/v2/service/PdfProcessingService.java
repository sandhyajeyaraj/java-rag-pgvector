package com.complianceiq.v2.service;

import com.complianceiq.v2.kafka.PdfUploadEvent;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.pdfbox.io.RandomAccessReadBuffer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class PdfProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PdfProcessingService.class);
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public PdfProcessingService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void processAndIngest(PdfUploadEvent event) {
        String text = extractText(event.filePath());

        if (text.isBlank()) {
            log.warn("No text extracted from [{}] — skipping", event.fileName());
            return;
        }

        // Delete all existing chunks for this file so re-uploads replace old embeddings
        int deleted = jdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata->>'source' = ?",
                event.fileName()
        );
        if (deleted > 0) {
            log.info("Removed {} stale chunks for [{}]", deleted, event.fileName());
        }

        List<String> chunks = chunkText(text);
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", event.fileName());
            metadata.put("title", event.fileName().replace(".pdf", ""));
            metadata.put("uploaded_at", event.uploadedAt().toString());
            metadata.put("chunk_index", i);
            metadata.put("total_chunks", chunks.size());
            documents.add(new Document(chunks.get(i), metadata));
        }

        vectorStore.add(documents);
        log.info("Ingested {} chunks from [{}] uploaded at {}", chunks.size(), event.fileName(), event.uploadedAt());
    }

    @Async("aiThreadPool")
    public CompletableFuture<Integer> processAndIngest(MultipartFile file, String title, String category) {
        String text;
        try (InputStream in = file.getInputStream()) {
            text = extractText(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded PDF: " + file.getOriginalFilename(), e);
        }

        if (text.isBlank()) {
            log.warn("No text extracted from uploaded file [{}] — skipping", file.getOriginalFilename());
            return CompletableFuture.completedFuture(0);
        }

        int deleted = jdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata->>'title' = ?", title);
        if (deleted > 0) {
            log.info("Removed {} stale chunks for [{}]", deleted, title);
        }

        List<String> chunks = chunkText(text);
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", title);
            metadata.put("category", category != null ? category : "GENERAL");
            metadata.put("source", file.getOriginalFilename());
            metadata.put("chunk_index", i);
            metadata.put("total_chunks", chunks.size());
            documents.add(new Document(chunks.get(i), metadata));
        }

        vectorStore.add(documents);
        log.info("Ingested {} chunks from uploaded file [{}]", chunks.size(), file.getOriginalFilename());
        return CompletableFuture.completedFuture(chunks.size());
    }

    private String extractText(String filePath) {
        try (PDDocument pdf = Loader.loadPDF(new File(filePath))) {
            return new PDFTextStripper().getText(pdf);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PDF: " + filePath, e);
        }
    }

    private String extractText(InputStream in) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(new RandomAccessReadBuffer(in))) {
            return new PDFTextStripper().getText(pdf);
        }
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
