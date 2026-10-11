package com.pawlift.model;

/** Administrators manage users, roles, listings and settings. */
public class AdminUser extends User {
    public AdminUser() {
        super(UserRole.ADMIN);
    }

    @Override
    public boolean isAdmin() {
        return true;
    }
}
