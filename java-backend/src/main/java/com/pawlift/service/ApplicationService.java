package com.pawlift.service;

import com.pawlift.dao.ApplicationDAO;
import com.pawlift.dao.PetDAO;
import com.pawlift.dao.SettingDAO;
import com.pawlift.dao.ShelterDAO;
import com.pawlift.dao.impl.ApplicationDAOImpl;
import com.pawlift.dao.impl.PetDAOImpl;
import com.pawlift.dao.impl.SettingDAOImpl;
import com.pawlift.dao.impl.ShelterDAOImpl;
import com.pawlift.exception.BadRequestException;
import com.pawlift.exception.ConflictException;
import com.pawlift.exception.DuplicateKeyException;
import com.pawlift.exception.ForbiddenException;
import com.pawlift.exception.NotFoundException;
import com.pawlift.model.AdoptionApplication;
import com.pawlift.model.ApplicationStatus;
import com.pawlift.model.ListingStatus;
import com.pawlift.model.Pet;
import com.pawlift.model.PetStatus;
import com.pawlift.model.Shelter;
import com.pawlift.model.User;
import com.pawlift.util.Validator;

import java.util.List;

/** Adoption applications: adopters apply, shelters (or admins) decide. */
public class ApplicationService {

    private final ApplicationDAO applications;
    private final PetDAO pets;
    private final ShelterDAO shelters;
    private final SettingDAO settings;

    public ApplicationService() {
        this(new ApplicationDAOImpl(), new PetDAOImpl(), new ShelterDAOImpl(), new SettingDAOImpl());
    }

    public ApplicationService(ApplicationDAO applications, PetDAO pets, ShelterDAO shelters, SettingDAO settings) {
        this.applications = applications;
        this.pets = pets;
        this.shelters = shelters;
        this.settings = settings;
    }

    /** An adopter applies for a pet. The adopter id is always the signed-in user. */
    public AdoptionApplication apply(User actor, String petId, String housing, String experience, String note) {
        if (actor == null || !actor.canApplyForPets())
            throw new ForbiddenException("Only adopter accounts can apply for pets");
        if (!"true".equalsIgnoreCase(settings.findValue("applications_open").orElse("true")))
            throw new ForbiddenException("Applications are currently closed");

        if (petId == null || petId.isBlank()) throw new BadRequestException("pet_id is required");
        Pet pet = pets.findById(petId).orElseThrow(() -> new NotFoundException("Pet not found"));
        if (pet.listingStatus != ListingStatus.APPROVED) throw new NotFoundException("Pet not found");
        if (pet.adoptionStatus != PetStatus.AVAILABLE)
            throw new ConflictException("This pet is no longer available for adoption");

        if (applications.exists(petId, actor.id))
            throw new ConflictException("You have already applied for this pet");

        AdoptionApplication app = new AdoptionApplication();
        app.petId = petId;
        app.adopterId = actor.id;
        app.housing = Validator.required(housing, "housing", 120);
        app.experience = Validator.optional(experience, "experience", 200);
        app.note = Validator.optional(note, "note", 800);
        app.status = ApplicationStatus.PENDING;
        try {
            return applications.create(app);
        } catch (DuplicateKeyException e) {
            // Two clicks at the same time: the UNIQUE key caught the second one.
            throw new ConflictException("You have already applied for this pet");
        }
    }

    /** The signed-in adopter's own applications. */
    public List<AdoptionApplication> listMine(User actor) {
        if (actor == null || !actor.canApplyForPets())
            throw new ForbiddenException("Only adopter accounts have applications");
        return applications.findByAdopter(actor.id);
    }

    /** Applications for pets of the signed-in shelter user's own shelter. */
    public List<AdoptionApplication> listForMyShelter(User actor) {
        if (actor == null || !actor.canManagePets())
            throw new ForbiddenException("Only shelter accounts can view these applications");
        Shelter shelter = shelters.findByOwnerId(actor.id)
                .orElseThrow(() -> new ForbiddenException("Your account has no shelter profile"));
        return applications.findByShelter(shelter.id);
    }

    /** Admin view of every application. */
    public List<AdoptionApplication> listAll(User actor) {
        if (actor == null || !actor.isAdmin()) throw new ForbiddenException("Admins only");
        return applications.findAll();
    }

    /** Fetches one application if the caller is its adopter, its shelter, or an admin. */
    public AdoptionApplication getAccessible(User actor, String applicationId) {
        AdoptionApplication app = applications.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
        if (!canAccess(actor, app)) throw new NotFoundException("Application not found");
        return app;
    }

    /**
     * Approves or rejects a pending application. Only the pet's shelter or an admin.
     * The DAO runs this in a transaction and, on approval, also moves the pet to "pending".
     */
    public AdoptionApplication decide(User actor, String applicationId, ApplicationStatus decision) {
        if (decision != ApplicationStatus.APPROVED && decision != ApplicationStatus.REJECTED)
            throw new BadRequestException("Decision must be approved or rejected");

        AdoptionApplication app = applications.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
        if (!isShelterOwnerOrAdmin(actor, app))
            throw new ForbiddenException("You can only decide applications for your own pets");
        if (app.status != ApplicationStatus.PENDING)
            throw new ConflictException("This application has already been " + app.status.value());

        if (!applications.updateStatus(applicationId, decision))
            throw new NotFoundException("Application not found");
        return applications.findById(applicationId).orElse(app);
    }

    // ----------------------------------------------------------------- helpers

    /** Adopter of the application, owner of the pet's shelter, or admin. */
    boolean canAccess(User actor, AdoptionApplication app) {
        if (actor == null) return false;
        if (actor.id.equals(app.adopterId)) return true;
        return isShelterOwnerOrAdmin(actor, app);
    }

    private boolean isShelterOwnerOrAdmin(User actor, AdoptionApplication app) {
        if (actor == null) return false;
        if (actor.isAdmin()) return true;
        if (!actor.canManagePets() || app.pet == null) return false;
        return shelters.findByOwnerId(actor.id).map(s -> s.id.equals(app.pet.shelterId)).orElse(false);
    }
}
