package com.complianceiq.v2.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles LLMs (e.g. llama3.2) that return list fields as comma-separated strings
 * instead of proper JSON arrays. Accepts both formats transparently.
 */
public class FlexibleStringListDeserializer extends StdDeserializer<List<String>> {

    public FlexibleStringListDeserializer() {
        super(List.class);
    }

    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            List<String> result = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                result.add(p.getValueAsString());
            }
            return result;
        }

        // LLM returned a plain string — split on comma
        String value = p.getValueAsString();
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
