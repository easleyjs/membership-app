package com.easleyjs.membership.controller;

import com.easleyjs.membership.dto.AppUserCreateRequest;
import com.easleyjs.membership.dto.AppUserResponse;
import com.easleyjs.membership.dto.AppUserStatusRequest;
import com.easleyjs.membership.dto.AppUserStatusResponse;
import com.easleyjs.membership.dto.AppUserUpdateRequest;
import com.easleyjs.membership.dto.UserMemberMatch;
import com.easleyjs.membership.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class AppUserController {
    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping()
    public List<AppUserResponse> findAll() {
        return appUserService.getAllAppUsers();
    }

    @GetMapping("/{id}")
    public AppUserResponse findByMember_Id(@PathVariable UUID id) {
        return appUserService.findAppUserById(id);
    }

    @GetMapping("/eligible-members")
    public List<UserMemberMatch> searchEligibleMembers(@RequestParam String query) {
        return appUserService.searchEligibleMembers(query);
    }

    @PatchMapping("/{id}")
    public AppUserResponse updateUser(@PathVariable UUID id, @Valid @RequestBody AppUserUpdateRequest request) {
        return appUserService.updateUser(id, request);
    }

    @PostMapping
    public ResponseEntity<AppUserResponse> createUser(
            @Valid @RequestBody AppUserCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(appUserService.createUser(request));
    }

    @PatchMapping("/{memberNumber}/status")
    public AppUserStatusResponse appUserStatusChange(
            @PathVariable Long memberNumber,
            @Valid @RequestBody AppUserStatusRequest request
            ) {
        return appUserService.updateStatus(memberNumber, request.enabled());
    }
}
