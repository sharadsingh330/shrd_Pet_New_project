package com.pawlift.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.pawlift.exception.ApiException;
import com.pawlift.exception.NotFoundException;
import com.pawlift.model.Shelter;
import com.pawlift.model.User;
import com.pawlift.model.UserRole;
import com.pawlift.service.AuthService;
import com.pawlift.service.ShelterService;
import com.pawlift.util.ApiResponse;
import com.pawlift.util.JsonUtil;
import com.pawlift.util.SessionUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** POST /api/auth/login | /register | /logout, GET /api/auth/me */
@WebServlet(urlPatterns = {"/api/auth/*"})
public class AuthServlet extends BaseServlet {
    private final AuthService auth = new AuthService();
    private final ShelterService shelters = new ShelterService();

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        String action = path.isEmpty() ? "" : path.get(0);
        switch (action) {
            case "login" -> {
                JsonNode b = JsonUtil.readBody(req);
                User u = auth.login(JsonUtil.text(b, "email"), JsonUtil.text(b, "password"));
                SessionUtil.signIn(req, u);
                ApiResponse.ok(resp, Map.of("user", u));
            }
            case "register" -> {
                JsonNode b = JsonUtil.readBody(req);
                User u = auth.register(JsonUtil.text(b, "full_name"), JsonUtil.text(b, "email"),
                        JsonUtil.text(b, "password"), JsonUtil.text(b, "role"),
                        JsonUtil.text(b, "shelter_name"), JsonUtil.text(b, "city"));
                SessionUtil.signIn(req, u);
                ApiResponse.json(resp, HttpServletResponse.SC_CREATED, Map.of("user", u));
            }
            case "logout" -> {
                SessionUtil.signOut(req);
                ApiResponse.message(resp, HttpServletResponse.SC_OK, "Signed out");
            }
            default -> throw new NotFoundException("Unknown auth endpoint");
        }
    }

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        if (path.size() != 1 || !path.get(0).equals("me")) throw new NotFoundException("Unknown auth endpoint");
        User u = requireUser(req);
        Shelter shelter = null;
        if (u.role == UserRole.SHELTER) {
            try {
                shelter = shelters.getMine(u);
            } catch (ApiException e) {
                shelter = null;
            }
        }
        Map<String, Object> body = new HashMap<>();
        body.put("user", u);
        body.put("shelter", shelter);
        ApiResponse.ok(resp, body);
    }
}
