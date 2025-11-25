package com.opensearchloadtester.metricsreporter.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom deserializer for raw JSON strings.
 * Handles both String and Object representations of JSON responses.
 */
public class RawJsonDeserializer extends JsonDeserializer<String> {
    
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        // If it's already a string, return as-is
        if (node.isTextual()) {
            return node.asText();
        }
        
        // If it's an object or array, convert to JSON string
        return node.toString();
    }
}

