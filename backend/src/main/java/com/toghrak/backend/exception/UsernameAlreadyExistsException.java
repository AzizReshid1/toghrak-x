package com.toghrak.backend.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
    // getRootCause() removed — it duplicated getCause() from Throwable and was never called.
}