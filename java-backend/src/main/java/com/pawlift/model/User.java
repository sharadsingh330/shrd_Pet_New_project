package com.pawlift.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A platform account. JSON field names are snake_case (full_name, created_at...)
 * to match src/lib/api.ts. The password hash is never serialized.
 */
public class User {
    public String id;
    public String fullName;
    public String email;
    @JsonIgnore
    public String passwordHash;
    public UserRole role;
    public String phone;
    public String city;
    public String bio;
    public String createdAt;

    protected User(UserRole role) {
        this.role = role;
    }

    /** Creates the concrete subclass for a role. */
    public static User forRole(UserRole role) {
        switch (role) {
            case ADMIN:
                return new AdminUser();
            case SHELTER:
                return new ShelterUser();
            default:
                return new Adopter();
        }
    }

    @JsonIgnore
    public boolean canApplyForPets() {
        return false;
    }

    @JsonIgnore
    public boolean canManagePets() {
        return false;
    }

    @JsonIgnore
    public boolean isAdmin() {
        return false;
    }
}
