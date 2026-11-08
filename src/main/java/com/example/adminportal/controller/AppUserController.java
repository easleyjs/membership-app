package com.example.adminportal.controller;

import com.example.adminportal.dto.AppUserCreateRequest;
import com.example.adminportal.dto.AppUserResponse;
import com.example.adminportal.dto.AppUserStatusRequest;
import com.example.adminportal.dto.AppUserStatusResponse;
import com.example.adminportal.entity.AppUser;
import com.example.adminportal.exception.AppUserNotFoundException;
import com.example.adminportal.repository.AppUserRepository;
import com.example.adminportal.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class AppUserController {
    private final AppUserRepository appUserRepository;
    private AppUserService appUserService;

    public AppUserController(AppUserService appUserService, AppUserRepository appUserRepository) {
        this.appUserService = appUserService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping()
    public List<AppUserResponse> findAll() {
        return appUserService.getAllAppUsers();
    }

    @GetMapping("/{id}")
    public AppUserResponse findByMember_Id(@PathVariable UUID id) {
        return appUserService.findAppUserById(id);
    }

    @PostMapping
    public ResponseEntity<AppUserResponse> createUser(
            @RequestBody AppUserCreateRequest request
    ) {
        AppUser appUser = appUserService.createUser(request);

        AppUserResponse response = AppUserResponse.from(appUser);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{memberNumber}/status")
    @Transactional
    public AppUserStatusResponse appUserStatusChange(
            @PathVariable Long memberNumber,
            @Valid @RequestBody AppUserStatusRequest request
            ) {
        AppUser appUser = appUserRepository.findByMember_MemberNumber(memberNumber)
                .orElseThrow(() -> new AppUserNotFoundException(
                        "App user not found for member number: " + memberNumber));
        appUser.setEnabled(request.enabled());

        return AppUserStatusResponse.from(appUser);

    }
}
