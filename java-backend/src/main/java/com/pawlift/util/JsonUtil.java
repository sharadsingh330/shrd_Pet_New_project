package com.pawlift.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.pawlift.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.Reader;

/** Shared Jackson mapper (snake_case, matching src/lib/api.ts) and body helpers. */
public final class JsonUtil {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtil() {
    }

    /** Reads the request body as a JSON object; an empty body becomes {}. */
    public static JsonNode readBody(HttpServletRequest req) {
        try (Reader reader = req.getReader()) {
            JsonNode node = MAPPER.readTree(reader);
            if (node == null || node.isMissingNode() || node.isNull()) return MAPPER.createObjectNode();
            if (!node.isObject()) throw new BadRequestException("Request body must be a JSON object");
            return node;
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Malformed JSON body");
        } catch (IOException e) {
            throw new BadRequestException("Unable to read request body");
        }
    }

    /** Returns a field as text, or null when missing / JSON null. */
    public static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        return v.asText();
    }
}
