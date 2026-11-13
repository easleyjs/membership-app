package com.easleyjs.membership.controller;

import com.easleyjs.membership.entity.UserRole;
import com.easleyjs.membership.security.SessionUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public CurrentUser me(@AuthenticationPrincipal SessionUser user) {
        return new CurrentUser(user.getId(), user.getDisplayName(), user.getRole());
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    public record CurrentUser(UUID id, String displayName, UserRole role) {}
}
