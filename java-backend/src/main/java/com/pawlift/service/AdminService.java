package com.pawlift.service;

import com.pawlift.dao.ApplicationDAO;
import com.pawlift.dao.PetDAO;
import com.pawlift.dao.SettingDAO;
import com.pawlift.dao.ShelterDAO;
import com.pawlift.dao.UserDAO;
import com.pawlift.dao.impl.ApplicationDAOImpl;
import com.pawlift.dao.impl.PetDAOImpl;
import com.pawlift.dao.impl.SettingDAOImpl;
import com.pawlift.dao.impl.ShelterDAOImpl;
import com.pawlift.dao.impl.UserDAOImpl;
import com.pawlift.exception.BadRequestException;
import com.pawlift.exception.ForbiddenException;
import com.pawlift.exception.NotFoundException;
import com.pawlift.model.ApplicationStatus;
import com.pawlift.model.ListingStatus;
import com.pawlift.model.Pet;
import com.pawlift.model.PetStatus;
import com.pawlift.model.Setting;
import com.pawlift.model.Shelter;
import com.pawlift.model.User;
import com.pawlift.model.UserRole;
import com.pawlift.util.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Admin-only operations. Every public method first checks the caller is an admin. */
public class AdminService {

    private final UserDAO users;
    private final ShelterDAO shelters;
    private final PetDAO pets;
    private final ApplicationDAO applications;
    private final SettingDAO settings;

    public AdminService() {
        this(new UserDAOImpl(), new ShelterDAOImpl(), new PetDAOImpl(), new ApplicationDAOImpl(), new SettingDAOImpl());
    }

    public AdminService(UserDAO users, ShelterDAO shelters, PetDAO pets, ApplicationDAO applications,
                        SettingDAO settings) {
        this.users = users;
        this.shelters = shelters;
        this.pets = pets;
        this.applications = applications;
        this.settings = settings;
    }

    private static void requireAdmin(User actor) {
        if (actor == null || !actor.isAdmin()) throw new ForbiddenException("Admins only");
    }

    /** Counts for the admin dashboard. */
    public Map<String, Integer> overview(User actor) {
        requireAdmin(actor);
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("users", users.count());
        stats.put("shelters", shelters.count());
        stats.put("pets", pets.count());
        stats.put("pets_pending_review", pets.countByListingStatus(ListingStatus.PENDING));
        stats.put("pets_available", pets.countByAdoptionStatus(PetStatus.AVAILABLE));
        stats.put("pets_adopted", pets.countByAdoptionStatus(PetStatus.ADOPTED));
        stats.put("applications", applications.count());
        stats.put("applications_pending", applications.countByStatus(ApplicationStatus.PENDING));
        stats.put("applications_approved", applications.countByStatus(ApplicationStatus.APPROVED));
        return stats;
    }

    public List<User> listUsers(User actor) {
        requireAdmin(actor);
        return users.findAll();
    }

    public List<Pet> listAllPets(User actor) {
        requireAdmin(actor);
        return pets.findAll();
    }

    /**
     * Changes a user's role. Admins cannot change their own role (avoids locking
     * the last admin out). A user promoted to shelter gets a shelter profile.
     */
    public User setRole(User actor, String userId, UserRole role) {
        requireAdmin(actor);
        if (role == null) throw new BadRequestException("role is required");
        if (actor.id.equals(userId)) throw new BadRequestException("You cannot change your own role");

        User target = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!users.updateRole(userId, role)) throw new NotFoundException("User not found");

        if (role == UserRole.SHELTER && shelters.findByOwnerId(userId).isEmpty()) {
            Shelter s = new Shelter();
            s.ownerId = userId;
            s.name = target.fullName;
            s.city = target.city == null ? "" : target.city;
            s.email = target.email;
            s.verified = false;
            shelters.create(s);
        }
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }

    /** Deletes a user. Their shelter, pets, applications and messages cascade in MySQL. */
    public void deleteUser(User actor, String userId) {
        requireAdmin(actor);
        if (actor.id.equals(userId)) throw new BadRequestException("You cannot delete your own account");
        if (!users.delete(userId)) throw new NotFoundException("User not found");
    }

    /** Approve or reject a pet listing. */
    public Pet moderateListing(User actor, String petId, ListingStatus status) {
        requireAdmin(actor);
        if (status == null) throw new BadRequestException("listing_status is required");
        Pet pet = pets.findById(petId).orElseThrow(() -> new NotFoundException("Pet not found"));
        pet.listingStatus = status;
        pets.update(pet);
        return pets.findById(petId).orElse(pet);
    }

    public List<Setting> listSettings(User actor) {
        requireAdmin(actor);
        return settings.findAll();
    }

    /** Updates an existing setting. Unknown keys are rejected; booleans must be true/false. */
    public String updateSetting(User actor, String key, String value) {
        requireAdmin(actor);
        if (key == null || key.isBlank()) throw new BadRequestException("key is required");
        if (settings.findValue(key).isEmpty()) throw new NotFoundException("Unknown setting: " + key);

        String v = Validator.optional(value, "value", 200);
        if (key.equals("applications_open") || key.equals("auto_approve_listings")) {
            if (!v.equals("true") && !v.equals("false"))
                throw new BadRequestException(key + " must be true or false");
        } else if (key.equals("support_email")) {
            v = Validator.email(v);
        } else if (key.equals("platform_name")) {
            v = Validator.required(v, "platform_name", 200);
        }
        settings.update(key, v);
        return v;
    }
}
