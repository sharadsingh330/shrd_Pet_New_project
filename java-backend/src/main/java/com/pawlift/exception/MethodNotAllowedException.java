package com.pawlift.exception;

/** 405 - the endpoint exists but does not support this HTTP method. */
public class MethodNotAllowedException extends ApiException {
    public MethodNotAllowedException(String method) {
        super(405, "Method " + method + " is not allowed on this endpoint");
    }
}
