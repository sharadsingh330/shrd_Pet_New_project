package com.pawlift.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Allows the React app (on another origin) to call the API with the session cookie.
 * Allowed origins come from ALLOWED_ORIGINS (env var or -D system property), comma-separated.
 * Only listed origins are echoed back; wildcards are never used because cookies are sent.
 */
public class CorsFilter implements Filter {
    private static final String DEFAULT_ORIGINS = "http://localhost:5173,http://localhost:3000,http://localhost:8081";
    private Set<String> allowed;

    @Override
    public void init(FilterConfig config) {
        String raw = System.getProperty("ALLOWED_ORIGINS", System.getenv("ALLOWED_ORIGINS"));
        if (raw == null || raw.isBlank()) raw = DEFAULT_ORIGINS;
        allowed = Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> s.replaceAll("/+$", "")).collect(Collectors.toSet());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String origin = req.getHeader("Origin");
        boolean ok = origin != null && allowed.contains(origin);
        if (ok) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Access-Control-Allow-Credentials", "true");
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept");
            resp.setHeader("Access-Control-Max-Age", "3600");
        }
        resp.addHeader("Vary", "Origin");

        // Preflight: answer here, never pass to auth or servlets.
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(ok || origin == null ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
