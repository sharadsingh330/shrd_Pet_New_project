package com.pawlift.exception;

/** 400 - the request body or parameters are invalid. */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(400, message);
    }
}
