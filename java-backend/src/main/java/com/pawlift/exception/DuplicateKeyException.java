package com.pawlift.exception;

/** A UNIQUE constraint was violated. Mapped to HTTP 409. */
public class DuplicateKeyException extends DataAccessException {
    public DuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
