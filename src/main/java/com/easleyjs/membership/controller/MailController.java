package com.easleyjs.membership.controller;

import com.easleyjs.membership.dto.ApiErrorResponse;
import com.easleyjs.membership.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

@RestController
@RequestMapping("/api/email")
@Profile({"dev", "local"})
public class MailController {
    private final MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping("/{memberId}")
    public void sendMail(@PathVariable UUID memberId) {
        mailService.sendMail(memberId);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse invalidMemberId(HttpServletRequest request) {
        return ApiErrorResponse.of("INVALID_MEMBER_ID", "Member ID must be a UUID.", request.getRequestURI());
    }
}
