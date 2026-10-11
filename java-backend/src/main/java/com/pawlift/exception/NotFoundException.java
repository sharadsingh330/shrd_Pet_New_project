package com.pawlift.exception;

/** 404 - resource or endpoint does not exist (or is not visible to the caller). */
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(404, message);
    }
}
