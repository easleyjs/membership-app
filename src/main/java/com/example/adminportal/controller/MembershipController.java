package com.example.adminportal.controller;

import com.example.adminportal.dto.MembershipResponse;
import com.example.adminportal.service.MembershipService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members/{memberNumber}/memberships")
public class MembershipController {
    MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    public Page<MembershipResponse> getMemberships(
            @PathVariable Long memberNumber,
            @PageableDefault(
                    size = 20,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return membershipService.getAllMembershipsByMemberId(memberNumber, pageable);
    }

}
