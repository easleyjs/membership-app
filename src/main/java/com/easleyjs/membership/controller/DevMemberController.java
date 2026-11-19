package com.easleyjs.membership.controller;

import com.easleyjs.membership.dto.DevMemberCreateRequest;
import com.easleyjs.membership.dto.MemberResponse;
import com.easleyjs.membership.service.DevMemberService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// TODO: Remove this controller and resources/dev/member-test.html after the demo period.
@RestController
@Profile("dev")
public class DevMemberController {
    private final DevMemberService members;

    public DevMemberController(DevMemberService members) {
        this.members = members;
    }

    @GetMapping(value = "/dev/member-test", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> page() {
        // Kept outside static resources so the page is unavailable without the dev profile.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new ClassPathResource("dev/member-test.html"));
    }

    @PostMapping("/api/dev/members")
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody DevMemberCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(members.createMember(request));
    }
}
