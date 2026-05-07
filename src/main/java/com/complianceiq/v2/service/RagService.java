package com.complianceiq.v2.service;

import com.complianceiq.v2.audit.AuditService;
import com.complianceiq.v2.dto.QueryRequest;
import com.complianceiq.v2.dto.QueryResponse;
import com.complianceiq.v2.llm.LlmOrchestrator;
import com.complianceiq.v2.llm.LlmResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final VectorStore vectorStore;
    private final LlmOrchestrator llmOrchestrator;
    private final AuditService auditService;

    private final Cache<String, QueryResponse> queryCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    @Async("aiThreadPool")
    public CompletableFuture<QueryResponse> query(QueryRequest request) {
        long startMs = System.currentTimeMillis();
        String cacheKey = buildCacheKey(request);

        QueryResponse cached = queryCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.info("Cache hit for: {}", request.getQuestion());
            QueryResponse cachedCopy = cloneWithCacheFlag(cached);
            auditService.record(request.getQuestion(), cachedCopy, true, System.currentTimeMillis() - startMs);
            return CompletableFuture.completedFuture(cachedCopy);
        }

        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(request.getQuestion())
                            .topK(request.getTopK())
                            .build()
            );

            log.info("Retrieved {} documents for: {}", docs.size(), request.getQuestion());

            if (docs.isEmpty()) {
                QueryResponse emptyResponse = QueryResponse.builder()
                        .summary("No relevant policy documents found for your question.")
                        .detailedExplanation("The knowledge base does not contain documents related to this query.")
                        .riskLevel("UNKNOWN")
                        .complianceCategory("UNKNOWN")
                        .keyFindings(List.of())
                        .actionItems(List.of("Consult a compliance officer or check the primary policy source."))
                        .referencedPolicies(List.of())
                        .coveredByAvailablePolicies(false)
                        .sources(List.of())
                        .confidence(0.0)
                        .llmProvider("none")
                        .cachedResult(false)
                        .build();
                auditService.record(request.getQuestion(), emptyResponse, false, System.currentTimeMillis() - startMs);
                return CompletableFuture.completedFuture(emptyResponse);
            }

            String context = docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n---\n\n"));

            LlmResult result = llmOrchestrator.call(buildSystemPrompt(), buildUserPrompt(request.getQuestion(), context));

            List<String> sources = docs.stream()
                    .map(doc -> (String) doc.getMetadata().getOrDefault("title", "Unknown"))
                    .distinct()
                    .toList();

            QueryResponse response = QueryResponse.builder()
                    .summary(result.answer().summary())
                    .detailedExplanation(result.answer().detailedExplanation())
                    .riskLevel(result.answer().riskLevel())
                    .complianceCategory(result.answer().complianceCategory())
                    .keyFindings(result.answer().keyFindings())
                    .actionItems(result.answer().actionItems())
                    .referencedPolicies(result.answer().referencedPolicies())
                    .coveredByAvailablePolicies(result.answer().coveredByAvailablePolicies())
                    .sources(sources)
                    .confidence(Math.min(1.0, docs.size() / 5.0))
                    .llmProvider(result.provider())
                    .cachedResult(false)
                    .build();

            queryCache.put(cacheKey, response);
            auditService.record(request.getQuestion(), response, false, System.currentTimeMillis() - startMs);
            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.error("RAG query failed for '{}': {}", request.getQuestion(), e.getMessage());
            auditService.recordError(request.getQuestion(), durationMs, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private String buildCacheKey(QueryRequest request) {
        String category = request.getCategory() != null ? request.getCategory().toLowerCase().trim() : "";
        return request.getQuestion().toLowerCase().trim() + "|" + request.getTopK() + "|" + category;
    }

    private QueryResponse cloneWithCacheFlag(QueryResponse original) {
        return QueryResponse.builder()
                .summary(original.getSummary())
                .detailedExplanation(original.getDetailedExplanation())
                .riskLevel(original.getRiskLevel())
                .complianceCategory(original.getComplianceCategory())
                .keyFindings(original.getKeyFindings())
                .actionItems(original.getActionItems())
                .referencedPolicies(original.getReferencedPolicies())
                .coveredByAvailablePolicies(original.isCoveredByAvailablePolicies())
                .sources(original.getSources())
                .confidence(original.getConfidence())
                .llmProvider(original.getLlmProvider())
                .cachedResult(true)
                .build();
    }

    private String buildSystemPrompt() {
        return """
                You are a fintech compliance expert. Analyze the provided policy documents and answer the user's question.
                Base your analysis strictly on the provided documents.
                Always respond with valid JSON only — no markdown, no explanation outside the JSON.
                If the question is not covered by the documents, set coveredByAvailablePolicies to false.
                """;
    }

    private String buildUserPrompt(String question, String context) {
        return """
                Policy Documents:
                %s

                Question: %s

                Return a JSON object with exactly these fields:
                {
                  "summary": "one or two sentence answer",
                  "detailedExplanation": "full explanation citing specific policy clauses",
                  "riskLevel": "one of: LOW, MEDIUM, HIGH, CRITICAL",
                  "complianceCategory": "e.g. AML, KYC, GDPR, Data Privacy, Sanctions, Fraud",
                  "keyFindings": ["finding 1", "finding 2"],
                  "actionItems": ["step 1", "step 2"],
                  "referencedPolicies": ["policy name or section"],
                  "coveredByAvailablePolicies": true or false
                }
                """.formatted(context, question);
    }
}
