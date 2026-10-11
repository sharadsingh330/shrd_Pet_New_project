package com.pawlift.servlet;

import com.pawlift.exception.NotFoundException;
import com.pawlift.service.ShelterService;
import com.pawlift.util.ApiResponse;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** GET /api/shelters (public list), GET /api/shelters/{id} */
@WebServlet(urlPatterns = {"/api/shelters", "/api/shelters/*"})
public class ShelterServlet extends BaseServlet {
    private final ShelterService shelters = new ShelterService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        if (path.isEmpty()) ApiResponse.ok(resp, shelters.list());
        else if (path.size() == 1) ApiResponse.ok(resp, shelters.get(path.get(0)));
        else throw new NotFoundException("Not found");
    }
}
