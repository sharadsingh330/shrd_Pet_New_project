package com.pawlift.model;

/** Adopters browse pets and submit adoption applications. */
public class Adopter extends User {
    public Adopter() {
        super(UserRole.ADOPTER);
    }

    @Override
    public boolean canApplyForPets() {
        return true;
    }
}
