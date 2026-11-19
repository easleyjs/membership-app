package com.easleyjs.membership.service;

import org.springframework.stereotype.Component;

@Component
public class ResendEmailDelivery implements EmailDelivery {
    private final ResendClient resend;

    public ResendEmailDelivery(ResendClient resend) {
        this.resend = resend;
    }

    @Override
    public void send(String recipient, String subject, String html, String qrContentId, byte[] qrPng) {
        resend.sendEmail(recipient, subject, html, qrContentId, qrPng);
    }
}
