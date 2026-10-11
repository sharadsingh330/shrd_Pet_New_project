package com.pawlift.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.pawlift.exception.BadRequestException;

public enum ApplicationStatus {
    PENDING("pending"), APPROVED("approved"), REJECTED("rejected");

    private final String value;

    ApplicationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static ApplicationStatus from(String v) {
        if (v != null) {
            for (ApplicationStatus s : values()) if (s.value.equalsIgnoreCase(v.trim())) return s;
        }
        throw new BadRequestException("Invalid application status: " + v);
    }
}
