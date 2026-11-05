package com.example.adminportal.controller;

import com.example.adminportal.dto.CheckInCreateRequest;
import com.example.adminportal.dto.CheckInResponse;
import com.example.adminportal.dto.MemberResponse;
import com.example.adminportal.service.CheckInService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/checkin/")
public class CheckInController {
    CheckInService checkInService;

    public CheckInController(CheckInService checkInService)
    {
        this.checkInService = checkInService;
    }

    @PostMapping("")
    public ResponseEntity<CheckInResponse> createCheckIn(@Valid @RequestBody CheckInCreateRequest checkInReq) {
        CheckInResponse createdCheckIn = checkInService.createCheckIn(checkInReq);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdCheckIn);
    }

    @GetMapping("/{id}")
    public List<CheckInResponse> findAll(@PathVariable UUID id) {
        return checkInService.getCheckInsByMember(id);
    }

}
