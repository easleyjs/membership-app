package com.example.adminportal.controller;

import com.example.adminportal.dto.*;
import com.example.adminportal.entity.Member;
import com.example.adminportal.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("")
    public List<MemberResponse> findAll() {
        return memberService.getAllMembers();
    }

    @GetMapping("/{id}")
    public MemberResponse findById(@PathVariable Long id) {
        return memberService.findMemberById(id);
    }

    @PostMapping("")
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody MemberCreateRequest memberRequest) {

        MemberResponse createdMember = memberService.createMember(memberRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdMember);
    }

    @PutMapping("/{memberNumber}/update")
    public ResponseEntity<MemberUpdateResponse> updateMember(
            @PathVariable Long memberNumber,
            @Valid @RequestBody MemberUpdateRequest request
    ) {
        return ResponseEntity.ok(memberService.updateMember(memberNumber, request));
    }

    // TODO: Add auth (require admin?)
    @PutMapping("/{memberNumber}/ban")
    public ResponseEntity<MemberStatusResponse> banMember(
            @PathVariable Long memberNumber,
            @Valid @RequestBody(required = false) MemberStatusRequest request
    ) {
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(memberService.banMember(memberNumber, reason));
    }

    @PutMapping("/{memberNumber}/unban")
    public ResponseEntity<MemberStatusResponse> unbanMember(
            @PathVariable Long memberNumber) {
        return ResponseEntity.ok(memberService.unbanMember(memberNumber));
    }

}
