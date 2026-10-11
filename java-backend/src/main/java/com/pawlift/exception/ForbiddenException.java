package com.pawlift.exception;

/** 403 - signed in, but not allowed to perform this action. */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(403, message);
    }
}
