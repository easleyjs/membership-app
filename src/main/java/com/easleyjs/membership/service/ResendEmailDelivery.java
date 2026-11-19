package com.easleyjs.membership.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
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
