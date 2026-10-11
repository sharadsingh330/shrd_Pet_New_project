package com.pawlift.exception;

/** 401 - no valid session. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(401, message);
    }
}
