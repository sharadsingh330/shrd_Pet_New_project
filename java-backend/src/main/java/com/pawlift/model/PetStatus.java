package com.pawlift.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.pawlift.exception.BadRequestException;

/** Adoption status of a pet (pets.adoption_status). */
public enum PetStatus {
    AVAILABLE("available"), PENDING("pending"), ADOPTED("adopted");

    private final String value;

    PetStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static PetStatus from(String v) {
        if (v != null) {
            for (PetStatus s : values()) if (s.value.equalsIgnoreCase(v.trim())) return s;
        }
        throw new BadRequestException("Invalid adoption status: " + v);
    }
}
