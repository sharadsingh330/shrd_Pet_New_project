package com.pawlift.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.pawlift.exception.BadRequestException;

public enum UserRole {
    ADMIN("admin"), SHELTER("shelter"), ADOPTER("adopter");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static UserRole from(String v) {
        if (v != null) {
            for (UserRole r : values()) if (r.value.equalsIgnoreCase(v.trim())) return r;
        }
        throw new BadRequestException("Invalid role: " + v);
    }
}
