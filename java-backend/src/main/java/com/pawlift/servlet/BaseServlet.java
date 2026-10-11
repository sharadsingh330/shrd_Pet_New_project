package com.pawlift.servlet;

import com.pawlift.exception.ApiException;
import com.pawlift.exception.MethodNotAllowedException;
import com.pawlift.exception.UnauthorizedException;
import com.pawlift.model.User;
import com.pawlift.service.AuthService;
import com.pawlift.util.ApiResponse;
import com.pawlift.util.SessionUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Shared plumbing for every Pawlift servlet:
 *  - dispatches GET/POST/PUT/PATCH/DELETE (HttpServlet has no doPatch),
 *  - turns exceptions into JSON { "error": ... } with the right HTTP status,
 *  - resolves the signed-in user from the server-side session only.
 * Business rules live in the Service layer, never here.
 */
public abstract class BaseServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<String> path = segments(req);
            switch (req.getMethod()) {
                case "GET" -> handleGet(req, resp, path);
                case "POST" -> handlePost(req, resp, path);
                case "PUT" -> handlePut(req, resp, path);
                case "PATCH" -> handlePatch(req, resp, path);
                case "DELETE" -> handleDelete(req, resp, path);
                case "OPTIONS" -> resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                default -> throw new MethodNotAllowedException(req.getMethod());
            }
        } catch (ApiException e) {
            ApiResponse.error(resp, e.getStatus(), e.getMessage());
        } catch (Exception e) {
            log("Unhandled error on " + req.getMethod() + " " + req.getRequestURI(), e);
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    protected void handleGet(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        throw new MethodNotAllowedException(req.getMethod());
    }

    protected void handlePost(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        throw new MethodNotAllowedException(req.getMethod());
    }

    protected void handlePut(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        throw new MethodNotAllowedException(req.getMethod());
    }

    /** By default PATCH behaves like PUT. */
    protected void handlePatch(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        handlePut(req, resp, path);
    }

    protected void handleDelete(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        throw new MethodNotAllowedException(req.getMethod());
    }

    /** Path after the servlet mapping, split on "/": "/abc/status" -> [abc, status]. */
    protected static List<String> segments(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.equals("/")) return List.of();
        return Arrays.stream(info.split("/")).filter(s -> !s.isBlank()).toList();
    }

    /** Signed-in user or null. Prefers the user loaded by AuthenticationFilter; falls back to the session id. */
    protected User currentUser(HttpServletRequest req) {
        User u = SessionUtil.current(req);
        if (u != null) return u;
        HttpSession s = req.getSession(false);
        if (s == null) return null;
        Object id = s.getAttribute(SessionUtil.SESSION_USER_ID);
        if (!(id instanceof String userId)) return null;
        try {
            u = authService.findUser(userId);
        } catch (ApiException e) {
            s.invalidate(); // account no longer exists
            return null;
        }
        req.setAttribute(SessionUtil.REQUEST_USER, u);
        return u;
    }

    protected User requireUser(HttpServletRequest req) {
        User u = currentUser(req);
        if (u == null) throw new UnauthorizedException("Please sign in");
        return u;
    }
}
