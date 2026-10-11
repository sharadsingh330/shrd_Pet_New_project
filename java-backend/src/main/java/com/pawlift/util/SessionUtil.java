package com.pawlift.util;

import com.pawlift.exception.ForbiddenException;
import com.pawlift.exception.UnauthorizedException;
import com.pawlift.model.User;
import com.pawlift.model.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Session keys and helpers. The session stores only the user id and role;
 * AuthenticationFilter reloads the user from MySQL on every request and
 * exposes it as a request attribute, so role changes apply immediately.
 */
public final class SessionUtil {
    public static final String SESSION_USER_ID = "userId";
    public static final String SESSION_ROLE = "role";
    public static final String REQUEST_USER = "pawlift.currentUser";

    private SessionUtil() {
    }

    /** Starts a fresh session (prevents session fixation) for the user. */
    public static void signIn(HttpServletRequest req, User user) {
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();
        HttpSession s = req.getSession(true);
        s.setAttribute(SESSION_USER_ID, user.id);
        s.setAttribute(SESSION_ROLE, user.role.value());
        req.setAttribute(REQUEST_USER, user);
    }

    public static void signOut(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
        req.removeAttribute(REQUEST_USER);
    }

    public static User current(HttpServletRequest req) {
        return (User) req.getAttribute(REQUEST_USER);
    }

    public static User require(HttpServletRequest req) {
        User u = current(req);
        if (u == null) throw new UnauthorizedException("Please sign in");
        return u;
    }

    public static User requireRole(HttpServletRequest req, UserRole... roles) {
        User u = require(req);
        for (UserRole r : roles) if (u.role == r) return u;
        throw new ForbiddenException("You do not have permission to do this");
    }
}
