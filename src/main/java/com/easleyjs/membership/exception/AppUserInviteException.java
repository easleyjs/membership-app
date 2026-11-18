package com.easleyjs.membership.exception;

import org.springframework.http.HttpStatus;

public class AppUserInviteException extends RuntimeException {
    private final HttpStatus status;

    public AppUserInviteException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
