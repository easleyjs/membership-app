package com.easleyjs.membership.controller;

import com.easleyjs.membership.entity.UserRole;
import com.easleyjs.membership.security.SessionUser;
import com.easleyjs.membership.service.AppUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserService users;

    public AuthController(AppUserService users) {
        this.users = users;
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal SessionUser user,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        users.changePassword(user.getId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    public record ChangePasswordRequest(@NotNull @Size(min = 1, max = 1024) String currentPassword,
                                        @NotNull @Size(min = 1, max = 144) String newPassword) {}

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
