package com.easleyjs.membership.exception;

public class MemberAlreadyHasUserException extends RuntimeException {
    public MemberAlreadyHasUserException(String message) {
        super(message);
    }
}
