package com.pawlift.exception;

/** 409 - the request conflicts with existing data (duplicate email, duplicate application...). */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(409, message);
    }
}
