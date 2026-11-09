package com.example.adminportal.exception;

public class BannedMemberCheckInException extends RuntimeException {
    public BannedMemberCheckInException(String message) {
        super(message);
    }
}
