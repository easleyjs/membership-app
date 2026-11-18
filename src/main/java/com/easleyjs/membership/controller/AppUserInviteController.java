package com.easleyjs.membership.controller;

import com.easleyjs.membership.service.AppUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppUserInviteController {
    private final AppUserService users;

    public AppUserInviteController(AppUserService users) {
        this.users = users;
    }

    @PostMapping("/api/auth/accept-invite")
    public ResponseEntity<Void> acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        users.acceptInvite(request.token(), request.password());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    public record AcceptInviteRequest(@NotNull @Size(max = 64) String token,
                                      @NotNull @Size(max = 72) String password) {}
}
