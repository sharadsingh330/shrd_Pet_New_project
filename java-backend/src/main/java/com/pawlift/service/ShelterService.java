package com.pawlift.service;

import com.pawlift.dao.ShelterDAO;
import com.pawlift.dao.impl.ShelterDAOImpl;
import com.pawlift.exception.ConflictException;
import com.pawlift.exception.DuplicateKeyException;
import com.pawlift.exception.ForbiddenException;
import com.pawlift.exception.NotFoundException;
import com.pawlift.model.Shelter;
import com.pawlift.model.User;
import com.pawlift.model.UserRole;
import com.pawlift.util.Validator;

import java.util.List;

/** Shelter profiles. Each shelter user owns at most one shelter. */
public class ShelterService {

    private final ShelterDAO shelters;

    public ShelterService() {
        this(new ShelterDAOImpl());
    }

    public ShelterService(ShelterDAO shelters) {
        this.shelters = shelters;
    }

    public Shelter get(String id) {
        return shelters.findById(id).orElseThrow(() -> new NotFoundException("Shelter not found"));
    }

    /** The shelter owned by the signed-in shelter user. */
    public Shelter getMine(User actor) {
        if (actor.role != UserRole.SHELTER) throw new ForbiddenException("Only shelter accounts have a shelter");
        return shelters.findByOwnerId(actor.id)
                .orElseThrow(() -> new NotFoundException("Your shelter profile was not found"));
    }

    /** Admins may look up any owner; other users only themselves. */
    public Shelter getByOwner(User actor, String ownerId) {
        if (!actor.isAdmin() && !actor.id.equals(ownerId))
            throw new ForbiddenException("You can only view your own shelter");
        return shelters.findByOwnerId(ownerId).orElseThrow(() -> new NotFoundException("Shelter not found"));
    }

    public List<Shelter> list() {
        return shelters.findAll();
    }

    /**
     * Creates the shelter for the signed-in shelter user. The owner is always the
     * caller (never an id sent by the browser), and new shelters start unverified.
     */
    public Shelter create(User actor, String name, String city, String email) {
        if (actor.role != UserRole.SHELTER) throw new ForbiddenException("Only shelter accounts can create a shelter");
        if (shelters.findByOwnerId(actor.id).isPresent())
            throw new ConflictException("You already have a shelter");

        Shelter s = new Shelter();
        s.ownerId = actor.id;
        s.name = Validator.required(name, "name", 160);
        s.city = Validator.optional(city, "city", 120);
        String e = Validator.optional(email, "email", 255);
        s.email = e.isEmpty() ? actor.email : Validator.email(e);
        s.verified = false;
        try {
            return shelters.create(s);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("You already have a shelter");
        }
    }
}
