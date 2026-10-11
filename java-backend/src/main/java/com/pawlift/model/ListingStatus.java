package com.pawlift.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.pawlift.exception.BadRequestException;

/** Moderation status of a pet listing (pets.listing_status). */
public enum ListingStatus {
    PENDING("pending"), APPROVED("approved"), REJECTED("rejected");

    private final String value;

    ListingStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static ListingStatus from(String v) {
        if (v != null) {
            for (ListingStatus s : values()) if (s.value.equalsIgnoreCase(v.trim())) return s;
        }
        throw new BadRequestException("Invalid listing status: " + v);
    }
}
