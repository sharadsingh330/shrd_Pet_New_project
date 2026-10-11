package com.pawlift.exception;

/** Wraps SQLExceptions thrown by the DAO layer. Mapped to HTTP 500. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
