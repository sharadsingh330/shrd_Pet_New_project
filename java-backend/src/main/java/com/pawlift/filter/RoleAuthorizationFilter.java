package com.pawlift.filter;

import com.pawlift.model.User;
import com.pawlift.model.UserRole;
import com.pawlift.util.ApiResponse;
import com.pawlift.util.SessionUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Requires the role given by the "role" init-param (web.xml maps it to /api/admin/* with role=admin).
 * The role comes from the user that AuthenticationFilter reloaded from MySQL, never from the browser.
 * AdminService checks again, so this is a second layer, not the only one.
 */
public class RoleAuthorizationFilter implements Filter {
    private UserRole required;

    @Override
    public void init(FilterConfig config) throws ServletException {
        String role = config.getInitParameter("role");
        if (role == null || role.isBlank()) throw new ServletException("RoleAuthorizationFilter needs a 'role' init-param");
        required = UserRole.from(role);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if ("OPTIONS".equals(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        User u = SessionUtil.current(req);
        if (u == null) {
            ApiResponse.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please sign in");
            return;
        }
        if (u.role != required) {
            ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "You do not have permission to do this");
            return;
        }
        chain.doFilter(request, response);
    }
}
