package com.pawlift.filter;

import com.pawlift.exception.ApiException;
import com.pawlift.model.User;
import com.pawlift.service.AuthService;
import com.pawlift.util.ApiResponse;
import com.pawlift.util.SessionUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Loads the signed-in user from the server-side session (user id only) and
 * reloads them from MySQL, so role changes and deletions apply immediately.
 * Public endpoints pass through; everything else needs a valid session (401).
 * Fine-grained role and ownership checks remain in the Services.
 */
public class AuthenticationFilter implements Filter {
    private final AuthService auth = new AuthService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        User user = loadUser(req);
        if (user != null) req.setAttribute(SessionUtil.REQUEST_USER, user);

        if (user == null && !isPublic(req)) {
            ApiResponse.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please sign in");
            return;
        }
        chain.doFilter(request, response);
    }

    private User loadUser(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return null;
        Object id = s.getAttribute(SessionUtil.SESSION_USER_ID);
        if (!(id instanceof String userId)) return null;
        try {
            User u = auth.findUser(userId);
            s.setAttribute(SessionUtil.SESSION_ROLE, u.role.value());
            return u;
        } catch (ApiException e) {
            s.invalidate(); // the account was deleted
            return null;
        }
    }

    /** Endpoints anyone may call without signing in. */
    static boolean isPublic(HttpServletRequest req) {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        String method = req.getMethod();
        if ("OPTIONS".equals(method)) return true;
        if ("POST".equals(method)
                && (path.equals("/api/auth/login") || path.equals("/api/auth/register") || path.equals("/api/auth/logout")))
            return true;
        if ("GET".equals(method)) {
            // Public pet list/details (PetService hides unapproved pets) and shelters.
            if (path.equals("/api/pets") || path.startsWith("/api/pets/")) return true;
            if (path.equals("/api/shelters") || path.startsWith("/api/shelters/")) return true;
        }
        return false;
    }
}
