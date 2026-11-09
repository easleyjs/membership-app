package com.example.adminportal.exception;

public class BannedMemberCheckinException extends RuntimeException {
    public BannedMemberCheckinException(String message) {
        super(message);
    }
}
