package com.easleyjs.membership.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTestEmail() {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("members@outland.test");
        message.setTo("josh@example.com");
        message.setSubject("Outland Membership Test");
        message.setText("Your membership email system works");

        mailSender.send(message);
    }

}
