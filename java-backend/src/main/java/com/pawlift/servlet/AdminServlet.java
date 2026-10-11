package com.pawlift.servlet;

import com.pawlift.exception.BadRequestException;
import com.pawlift.exception.NotFoundException;
import com.pawlift.model.User;
import com.pawlift.model.UserRole;
import com.pawlift.service.AdminService;
import com.pawlift.service.ShelterService;
import com.pawlift.util.ApiResponse;
import com.pawlift.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/admin/overview
 * PUT|PATCH /api/admin/users/{id}/role, DELETE /api/admin/users/{id}
 * PUT|PATCH /api/admin/settings/{key}
 * Admin-only: AdminService checks the role on every call (RoleAuthorizationFilter also guards /api/admin/*).
 */
@WebServlet(urlPatterns = {"/api/admin/*"})
public class AdminServlet extends BaseServlet {
    private final AdminService admin = new AdminService();
    private final ShelterService shelters = new ShelterService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        if (path.size() != 1 || !path.get(0).equals("overview")) throw new NotFoundException("Not found");
        User u = requireUser(req);
        Map<String, Integer> s = admin.overview(u);

        // Key names follow AdminOverview.analytics in src/lib/api.ts.
        Map<String, Integer> analytics = new LinkedHashMap<>();
        analytics.put("users", s.get("users"));
        analytics.put("shelters", s.get("shelters"));
        analytics.put("listings", s.get("pets"));
        analytics.put("pendingListings", s.get("pets_pending_review"));
        analytics.put("applications", s.get("applications"));
        analytics.put("approved", s.get("applications_approved"));
        analytics.put("adopted", s.get("pets_adopted"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("users", admin.listUsers(u));
        body.put("pets", admin.listAllPets(u));
        body.put("shelters", shelters.list());
        body.put("settings", admin.listSettings(u));
        body.put("analytics", analytics);
        ApiResponse.ok(resp, body);
    }

    /** PATCH is routed here too by BaseServlet, so both verbs work. */
    @Override
    protected void handlePut(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        User u = requireUser(req);
        if (path.size() == 3 && path.get(0).equals("users") && path.get(2).equals("role")) {
            String role = JsonUtil.text(JsonUtil.readBody(req), "role");
            if (role == null) throw new BadRequestException("role is required");
            User updated = admin.setRole(u, path.get(1), UserRole.from(role));
            ApiResponse.message(resp, HttpServletResponse.SC_OK, "Role updated to " + updated.role.value() + ".");
        } else if (path.size() == 2 && path.get(0).equals("settings")) {
            String value = JsonUtil.text(JsonUtil.readBody(req), "value");
            if (value == null) throw new BadRequestException("value is required");
            admin.updateSetting(u, path.get(1), value);
            ApiResponse.message(resp, HttpServletResponse.SC_OK, "Setting saved.");
        } else {
            throw new NotFoundException("Not found");
        }
    }

    @Override
    protected void handleDelete(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        if (path.size() != 2 || !path.get(0).equals("users")) throw new NotFoundException("Not found");
        admin.deleteUser(requireUser(req), path.get(1));
        ApiResponse.message(resp, HttpServletResponse.SC_OK, "User deleted.");
    }
}
