package com.easleyjs.membership.exception;

public class BannedMemberCheckInException extends RuntimeException {
    public BannedMemberCheckInException(String message) {
        super(message);
    }
}
