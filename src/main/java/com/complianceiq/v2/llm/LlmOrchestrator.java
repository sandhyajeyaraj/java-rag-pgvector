package com.complianceiq.v2.llm;

import com.complianceiq.v2.dto.LlmAnswer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tries each LlmStrategy in @Order sequence.
 *
 * Skip conditions (move to next provider without counting a failure):
 *   - isConfigured() is false  → API key is blank
 *   - circuit state is OPEN    → provider is cooling down after repeated failures
 *   - LlmAuthException thrown  → 401/403, permanent config error, not a transient fault
 *
 * Circuit breaker states per provider:
 *   CLOSED    → calls flow through normally
 *   OPEN      → provider skipped for waitDuration (30s), then transitions to HALF_OPEN
 *   HALF_OPEN → 3 trial calls allowed; success → CLOSED, failure → OPEN
 */
@Component
public class LlmOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(LlmOrchestrator.class);

    private final List<LlmStrategy> strategies;
    private final Map<String, CircuitBreaker> circuitBreakers;

    public LlmOrchestrator(List<LlmStrategy> strategies) {
        this.strategies = strategies;

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)                        // open after ≥50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // stay OPEN for 30s, then go HALF_OPEN
                .permittedNumberOfCallsInHalfOpenState(3)        // 3 trial calls in HALF_OPEN
                .slidingWindowSize(10)                           // measure failure rate over last 10 calls
                .ignoreExceptions(LlmAuthException.class)        // 401/403 are config errors, not circuit faults
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        this.circuitBreakers = strategies.stream()
                .collect(Collectors.toMap(
                        LlmStrategy::name,
                        s -> registry.circuitBreaker(s.name(), config)
                ));
    }

    public LlmResult call(String systemPrompt, String userPrompt) {
        for (LlmStrategy strategy : strategies) {

            if (!strategy.isConfigured()) {
                log.warn("[{}] not configured (blank API key) — skipping", strategy.name());
                continue;
            }

            CircuitBreaker cb = circuitBreakers.get(strategy.name());

            if (cb.getState() == CircuitBreaker.State.OPEN) {
                log.warn("[{}] circuit OPEN — skipping", strategy.name());
                continue;
            }

            try {
                LlmAnswer answer = CircuitBreaker
                        .decorateSupplier(cb, () -> strategy.call(systemPrompt, userPrompt))
                        .get();

                log.info("[{}] call succeeded (circuit: {})", strategy.name(), cb.getState());
                return new LlmResult(answer, strategy.name());

            } catch (LlmAuthException e) {
                log.error("{} — skipping this provider", e.getMessage());

            } catch (Exception e) {
                log.warn("[{}] call failed (circuit: {}) — {}", strategy.name(), cb.getState(), e.getMessage());
            }
        }

        throw new RuntimeException("All LLM providers are unavailable. Check API keys and provider health.");
    }
}
