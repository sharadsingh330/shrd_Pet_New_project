package com.pawlift.model;

/** Shelter staff: list their own pets and process applications for them. */
public class ShelterUser extends User {
    public ShelterUser() {
        super(UserRole.SHELTER);
    }

    @Override
    public boolean canManagePets() {
        return true;
    }
}
