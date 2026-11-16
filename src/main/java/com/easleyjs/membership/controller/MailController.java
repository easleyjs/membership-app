package com.easleyjs.membership.controller;

import com.easleyjs.membership.service.MailService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
@Profile({"dev", "local"})
public class MailController {
    private MailService mailService;

    public MailController(
            MailService mailService
    ) {
        this.mailService = mailService;
    }

    @PostMapping("")
    public void sendMail() {
        mailService.sendTestEmail();
    }
}
