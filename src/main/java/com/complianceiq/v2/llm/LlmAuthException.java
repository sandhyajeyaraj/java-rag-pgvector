package com.complianceiq.v2.llm;

/**
 * Thrown when an LLM provider rejects the request due to an invalid or missing API key.
 * The circuit breaker ignores this exception — auth failures are permanent config errors,
 * not transient faults, so opening the circuit would not help.
 */
public class LlmAuthException extends RuntimeException {
    public LlmAuthException(String provider, int statusCode) {
        super("[" + provider + "] authentication failed (HTTP " + statusCode + ") — check your API key");
    }
}
