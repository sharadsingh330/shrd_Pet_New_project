package com.pawlift.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.pawlift.exception.BadRequestException;
import com.pawlift.exception.NotFoundException;
import com.pawlift.model.ApplicationStatus;
import com.pawlift.model.User;
import com.pawlift.service.ApplicationService;
import com.pawlift.util.ApiResponse;
import com.pawlift.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** GET/POST /api/applications, PUT/PATCH /api/applications/{id}/status */
@WebServlet(urlPatterns = {"/api/applications", "/api/applications/*"})
public class ApplicationServlet extends BaseServlet {
    private final ApplicationService applications = new ApplicationService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        User u = requireUser(req);
        if (path.isEmpty()) {
            // The list depends on the signed-in user's role, never on a browser parameter.
            switch (u.role) {
                case ADOPTER -> ApiResponse.ok(resp, applications.listMine(u));
                case SHELTER -> ApiResponse.ok(resp, applications.listForMyShelter(u));
                case ADMIN -> ApiResponse.ok(resp, applications.listAll(u));
            }
        } else if (path.size() == 1) {
            ApiResponse.ok(resp, applications.getAccessible(u, path.get(0)));
        } else {
            throw new NotFoundException("Not found");
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        if (!path.isEmpty()) throw new NotFoundException("Not found");
        User u = requireUser(req);
        JsonNode b = JsonUtil.readBody(req);
        applications.apply(u, JsonUtil.text(b, "pet_id"), JsonUtil.text(b, "housing"),
                JsonUtil.text(b, "experience"), JsonUtil.text(b, "note"));
        ApiResponse.message(resp, HttpServletResponse.SC_CREATED, "Application sent.");
    }

    @Override
    protected void handlePut(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        if (path.size() != 2 || !path.get(1).equals("status")) throw new NotFoundException("Not found");
        User u = requireUser(req);
        String status = JsonUtil.text(JsonUtil.readBody(req), "status");
        if (status == null) throw new BadRequestException("status is required");
        ApplicationStatus decided = applications.decide(u, path.get(0), ApplicationStatus.from(status)).status;
        ApiResponse.message(resp, HttpServletResponse.SC_OK, "Application " + decided.value() + ".");
    }
}
