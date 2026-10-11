package com.pawlift.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/** Writes JSON responses in the shapes expected by src/lib/api.ts. */
public final class ApiResponse {
    private ApiResponse() {
    }

    public static void json(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        JsonUtil.MAPPER.writeValue(resp.getWriter(), body);
    }

    public static void ok(HttpServletResponse resp, Object body) throws IOException {
        json(resp, HttpServletResponse.SC_OK, body);
    }

    /** { "message": "..." } */
    public static void message(HttpServletResponse resp, int status, String message) throws IOException {
        json(resp, status, Map.of("message", message));
    }

    /** { "error": "..." } */
    public static void error(HttpServletResponse resp, int status, String error) throws IOException {
        json(resp, status, Map.of("error", error == null ? "Request failed" : error));
    }
}
