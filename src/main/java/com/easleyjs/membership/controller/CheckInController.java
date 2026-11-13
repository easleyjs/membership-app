package com.easleyjs.membership.controller;

import com.easleyjs.membership.dto.CheckInCreateRequest;
import com.easleyjs.membership.dto.CheckInResponse;
import com.easleyjs.membership.security.SessionUser;
import com.easleyjs.membership.service.CheckInService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/checkin")
public class CheckInController {
    CheckInService checkInService;

    public CheckInController(CheckInService checkInService)
    {
        this.checkInService = checkInService;
    }

    @PostMapping("")
    public ResponseEntity<CheckInResponse> createCheckIn(
            @Valid @RequestBody CheckInCreateRequest checkInReq,
            @AuthenticationPrincipal SessionUser user
    ) {
        CheckInResponse createdCheckIn = checkInService.createCheckIn(checkInReq, user.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdCheckIn);
    }

    @GetMapping("/{id}")
    public List<CheckInResponse> findAllByMemberId(@PathVariable UUID id) {
        return checkInService.getCheckInsByMember(id);
    }

    @GetMapping("")
    public List<CheckInResponse> findAll() {
        return checkInService.getAllCheckIns();
    }

}
