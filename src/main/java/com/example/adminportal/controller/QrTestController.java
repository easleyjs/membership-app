package com.example.adminportal.controller;

import com.example.adminportal.exception.MemberNotFoundException;
import com.example.adminportal.repository.MemberRepository;
import com.example.adminportal.service.QrCodeService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
public class QrTestController {
    MemberRepository memberRepository;
    QrCodeService qrCodeService;

    @GetMapping(
            value = "/api/dev/members/{memberNumber}/qr.png",
            produces = MediaType.IMAGE_PNG_VALUE
    )
    public ResponseEntity<byte[]> previewQr(@PathVariable Long memberNumber) {
        var member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new MemberNotFoundException(
                        "Member not found: " + memberNumber));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(qrCodeService.generateQr(member.getId()));
    }
}
