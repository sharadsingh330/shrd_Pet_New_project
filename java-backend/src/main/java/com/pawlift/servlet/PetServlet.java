package com.pawlift.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pawlift.exception.ApiException;
import com.pawlift.exception.BadRequestException;
import com.pawlift.exception.NotFoundException;
import com.pawlift.model.ListingStatus;
import com.pawlift.model.Pet;
import com.pawlift.model.PetStatus;
import com.pawlift.model.Shelter;
import com.pawlift.model.User;
import com.pawlift.service.AdminService;
import com.pawlift.service.PetService;
import com.pawlift.service.ShelterService;
import com.pawlift.util.ApiResponse;
import com.pawlift.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** GET/POST /api/pets, GET/PUT/PATCH/DELETE /api/pets/{id} */
@WebServlet(urlPatterns = {"/api/pets", "/api/pets/*"})
public class PetServlet extends BaseServlet {
    private static final String[] EDITABLE = {
            "name", "species", "breed", "age_label", "size", "temperament", "city", "description", "image_url", "image_key"};

    private final PetService pets = new PetService();
    private final ShelterService shelters = new ShelterService();
    private final AdminService admin = new AdminService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        if (path.isEmpty()) {
            ApiResponse.ok(resp, pets.listPublic(req.getParameter("q"), req.getParameter("species"), req.getParameter("city")));
            return;
        }
        if (path.size() != 1) throw new NotFoundException("Not found");
        Pet pet = pets.get(path.get(0), currentUser(req));
        Shelter shelter;
        try {
            shelter = shelters.get(pet.shelterId);
        } catch (NotFoundException e) {
            shelter = null;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("pet", pet);
        body.put("shelter", shelter);
        ApiResponse.ok(resp, body);
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        if (!path.isEmpty()) throw new NotFoundException("Not found");
        User u = requireUser(req);
        Pet input = toPet(JsonUtil.readBody(req));
        Pet created = pets.create(u, input);
        String msg = created.listingStatus == ListingStatus.APPROVED
                ? created.name + " is now live."
                : created.name + " was submitted for admin review.";
        ApiResponse.message(resp, HttpServletResponse.SC_CREATED, msg);
    }

    @Override
    protected void handlePut(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        if (path.size() != 1) throw new NotFoundException("Not found");
        User u = requireUser(req);
        String id = path.get(0);
        JsonNode body = JsonUtil.readBody(req);

        boolean hasFields = false;
        for (String f : EDITABLE) if (body.has(f)) hasFields = true;

        if (hasFields) {
            // Partial update: start from the stored pet, overlay only the fields that were sent.
            Pet existing = pets.get(id, u);
            ObjectNode merged = (ObjectNode) JsonUtil.MAPPER.valueToTree(existing);
            for (String f : EDITABLE) if (body.has(f)) merged.set(f, body.get(f));
            Pet input = toPet(merged);
            input.adoptionStatus = null;
            pets.update(u, id, input);
        }
        String adoption = JsonUtil.text(body, "adoption_status");
        if (adoption != null) pets.updateAdoptionStatus(u, id, PetStatus.from(adoption));
        String listing = JsonUtil.text(body, "listing_status");
        if (listing != null) admin.moderateListing(u, id, ListingStatus.from(listing));

        if (!hasFields && adoption == null && listing == null)
            throw new BadRequestException("Nothing to update");
        ApiResponse.message(resp, HttpServletResponse.SC_OK, "Pet updated.");
    }

    @Override
    protected void handleDelete(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
        if (path.size() != 1) throw new NotFoundException("Not found");
        pets.delete(requireUser(req), path.get(0));
        ApiResponse.message(resp, HttpServletResponse.SC_OK, "Pet deleted.");
    }

    private static Pet toPet(JsonNode node) {
        try {
            return JsonUtil.MAPPER.treeToValue(node, Pet.class);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Invalid pet data");
        }
    }
}
