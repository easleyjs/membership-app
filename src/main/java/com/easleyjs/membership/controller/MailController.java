package com.easleyjs.membership.controller;

import com.easleyjs.membership.dto.ApiErrorResponse;
import com.easleyjs.membership.exception.EmailDeliveryException;
import com.easleyjs.membership.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/email")
@Profile("dev")
public class MailController {
    private final MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping("/{memberId}")
    public void sendMail(@PathVariable UUID memberId) {
        mailService.sendMail(memberId);
    }

    @PostMapping("/resend/{memberId}")
    public ResponseEntity<?> sendMailViaResend(@PathVariable UUID memberId, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(Map.of("id", mailService.sendMailViaResend(memberId)));
        } catch (EmailDeliveryException failure) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiErrorResponse.of("RESEND_SEND_FAILED", failure.getMessage(), request.getRequestURI()));
        }
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse invalidMemberId(HttpServletRequest request) {
        return ApiErrorResponse.of("INVALID_MEMBER_ID", "Member ID must be a UUID.", request.getRequestURI());
    }
}
