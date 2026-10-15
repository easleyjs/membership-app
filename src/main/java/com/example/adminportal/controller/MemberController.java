package com.example.adminportal.controller;

import com.example.adminportal.dto.MemberCreateRequest;
import com.example.adminportal.dto.MemberResponse;
import com.example.adminportal.entity.Member;
import com.example.adminportal.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MemberController {
    MemberService memberService;

    @Autowired
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/members")
    public List<MemberResponse> findAll() {
        return memberService.getAllMembers();
    }

    @GetMapping("/members/{id}")
    public MemberResponse findById(@PathVariable Long id) {
        return memberService.findMemberById(id);
    }

    @PostMapping("/members")
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody MemberCreateRequest memberRequest) {

        MemberResponse createdMember = memberService.createMember(memberRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdMember);
    }

}
