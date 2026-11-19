package com.easleyjs.membership.service;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@Profile("local")
public class LocalEmailDelivery implements EmailDelivery {
    private final JavaMailSender mailSender;

    public LocalEmailDelivery(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(String recipient, String subject, String html, String qrContentId, byte[] qrPng) {
        mailSender.send(mimeMessage -> {
            var message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_RELATED, UTF_8.name());
            message.setFrom("members@outland.test");
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(html, true);
            message.addInline(qrContentId, new ByteArrayResource(qrPng), "image/png");
        });
    }
}
