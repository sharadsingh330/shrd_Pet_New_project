package com.pawlift.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.pawlift.exception.BadRequestException;
import com.pawlift.exception.NotFoundException;
import com.pawlift.model.User;
import com.pawlift.service.MessageService;
import com.pawlift.util.ApiResponse;
import com.pawlift.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** GET /api/messages?application_id=..., POST /api/messages */
@WebServlet(urlPatterns = {"/api/messages"})
public class MessageServlet extends BaseServlet {
    private final MessageService messages = new MessageService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        User u = requireUser(req);
        String appId = req.getParameter("application_id");
        if (appId == null || appId.isBlank()) throw new BadRequestException("application_id is required");
        ApiResponse.ok(resp, messages.list(u, appId));
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        if (!path.isEmpty()) throw new NotFoundException("Not found");
        User u = requireUser(req);
        JsonNode b = JsonUtil.readBody(req);
        messages.send(u, JsonUtil.text(b, "application_id"), JsonUtil.text(b, "body"));
        ApiResponse.message(resp, HttpServletResponse.SC_CREATED, "Message sent.");
    }
}
