package com.pawlift.service;

import com.pawlift.dao.PetDAO;
import com.pawlift.dao.SettingDAO;
import com.pawlift.dao.ShelterDAO;
import com.pawlift.dao.impl.PetDAOImpl;
import com.pawlift.dao.impl.SettingDAOImpl;
import com.pawlift.dao.impl.ShelterDAOImpl;
import com.pawlift.exception.BadRequestException;
import com.pawlift.exception.ForbiddenException;
import com.pawlift.exception.NotFoundException;
import com.pawlift.model.ListingStatus;
import com.pawlift.model.Pet;
import com.pawlift.model.PetStatus;
import com.pawlift.model.Shelter;
import com.pawlift.model.User;

import java.util.List;

/** Pet listings: public browsing plus shelter-owned create / update / delete. */
public class PetService {

    private final PetDAO pets;
    private final ShelterDAO shelters;
    private final SettingDAO settings;

    public PetService() {
        this(new PetDAOImpl(), new ShelterDAOImpl(), new SettingDAOImpl());
    }

    public PetService(PetDAO pets, ShelterDAO shelters, SettingDAO settings) {
        this.pets = pets;
        this.shelters = shelters;
        this.settings = settings;
    }

    // ------------------------------------------------------------- public reads

    /** Approved listings, optionally filtered by text, species and city. */
    public List<Pet> listPublic(String q, String species, String city) {
        return pets.findPublic(trimOrNull(q, 100), trimOrNull(species, 40), trimOrNull(city, 120));
    }

    /**
     * One pet. Unapproved listings are only visible to their shelter and admins;
     * everyone else gets 404 so hidden listings are not revealed. actor may be null.
     */
    public Pet get(String id, User actor) {
        Pet pet = pets.findById(id).orElseThrow(() -> new NotFoundException("Pet not found"));
        if (pet.listingStatus != ListingStatus.APPROVED && !canManage(actor, pet))
            throw new NotFoundException("Pet not found");
        return pet;
    }

    /** All pets (any status) belonging to the signed-in shelter user. */
    public List<Pet> listMine(User actor) {
        return pets.findByShelter(requireOwnShelter(actor).id);
    }

    // ------------------------------------------------------------------ writes

    /**
     * Creates a pet for the caller's own shelter. The shelter id from the browser
     * is ignored. Listing status follows the "auto_approve_listings" setting.
     */
    public Pet create(User actor, Pet input) {
        Shelter shelter = requireOwnShelter(actor);
        Pet pet = new Pet();
        applyEditableFields(pet, input);
        pet.shelterId = shelter.id;
        pet.adoptionStatus = PetStatus.AVAILABLE;
        boolean autoApprove = "true".equalsIgnoreCase(settings.findValue("auto_approve_listings").orElse("true"));
        pet.listingStatus = autoApprove ? ListingStatus.APPROVED : ListingStatus.PENDING;
        return pets.create(pet);
    }

    /**
     * Updates a pet. Only its own shelter (or an admin) may do this. shelter_id
     * and listing_status are always kept from the database row; listing
     * moderation happens in AdminService.
     */
    public Pet update(User actor, String petId, Pet input) {
        Pet existing = pets.findById(petId).orElseThrow(() -> new NotFoundException("Pet not found"));
        if (!canManage(actor, existing)) throw new ForbiddenException("You can only edit your own pets");

        applyEditableFields(existing, input);
        if (input.adoptionStatus != null) existing.adoptionStatus = input.adoptionStatus;
        // existing.shelterId and existing.listingStatus are intentionally unchanged.

        if (!pets.update(existing)) throw new NotFoundException("Pet not found");
        return pets.findById(petId).orElse(existing);
    }

    /** Changes only the adoption status (available / pending / adopted). */
    public Pet updateAdoptionStatus(User actor, String petId, PetStatus status) {
        if (status == null) throw new BadRequestException("adoption_status is required");
        Pet existing = pets.findById(petId).orElseThrow(() -> new NotFoundException("Pet not found"));
        if (!canManage(actor, existing)) throw new ForbiddenException("You can only edit your own pets");
        existing.adoptionStatus = status;
        pets.update(existing);
        return pets.findById(petId).orElse(existing);
    }

    public void delete(User actor, String petId) {
        Pet existing = pets.findById(petId).orElseThrow(() -> new NotFoundException("Pet not found"));
        if (!canManage(actor, existing)) throw new ForbiddenException("You can only delete your own pets");
        if (!pets.delete(petId)) throw new NotFoundException("Pet not found");
    }

    // ----------------------------------------------------------------- helpers

    /** True when actor is an admin or owns the shelter that lists this pet. */
    boolean canManage(User actor, Pet pet) {
        if (actor == null) return false;
        if (actor.isAdmin()) return true;
        if (!actor.canManagePets()) return false;
        return shelters.findByOwnerId(actor.id).map(s -> s.id.equals(pet.shelterId)).orElse(false);
    }

    private Shelter requireOwnShelter(User actor) {
        if (actor == null || !actor.canManagePets())
            throw new ForbiddenException("Only shelter accounts can manage pets");
        return shelters.findByOwnerId(actor.id)
                .orElseThrow(() -> new ForbiddenException("Your account has no shelter profile"));
    }

    /** Validates and copies the fields a shelter is allowed to edit. */
    private static void applyEditableFields(Pet target, Pet in) {
        if (in == null) throw new BadRequestException("Pet details are required");
        target.name = req(in.name, "name", 60);
        target.species = req(in.species, "species", 40);
        target.breed = opt(in.breed, "breed", 80);
        target.ageLabel = opt(in.ageLabel, "age_label", 30);
        target.size = opt(in.size, "size", 30);
        target.temperament = opt(in.temperament, "temperament", 60);
        target.city = opt(in.city, "city", 120);
        target.description = req(in.description, "description", 2000);
        String url = opt(in.imageUrl, "image_url", 2000);
        if (!url.isEmpty() && !url.startsWith("https://") && !url.startsWith("http://"))
            throw new BadRequestException("image_url must be an http(s) link");
        target.imageUrl = url.isEmpty() ? null : url;
        String key = opt(in.imageKey, "image_key", 30);
        target.imageKey = key.isEmpty() ? "dog" : key;
        int score = in.matchScore == 0 ? 80 : in.matchScore;
        if (score < 0 || score > 100) throw new BadRequestException("match_score must be between 0 and 100");
        target.matchScore = score;
    }

    private static String req(String v, String field, int max) {
        return com.pawlift.util.Validator.required(v, field, max);
    }

    private static String opt(String v, String field, int max) {
        return com.pawlift.util.Validator.optional(v, field, max);
    }

    private static String trimOrNull(String v, int max) {
        if (v == null || v.isBlank()) return null;
        String t = v.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
