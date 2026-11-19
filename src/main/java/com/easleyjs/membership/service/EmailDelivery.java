package com.easleyjs.membership.service;

public interface EmailDelivery {
    void send(String recipient, String subject, String html, String qrContentId, byte[] qrPng);
}
