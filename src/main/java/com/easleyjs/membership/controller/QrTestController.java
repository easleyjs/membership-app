package com.easleyjs.membership.controller;

import com.easleyjs.membership.exception.MemberNotFoundException;
import com.easleyjs.membership.repository.MemberRepository;
import com.easleyjs.membership.service.QrCodeService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
public class QrTestController {
    private final MemberRepository memberRepository;
    private final QrCodeService qrCodeService;

    public QrTestController(MemberRepository memberRepository, QrCodeService qrCodeService) {
        this.memberRepository = memberRepository;
        this.qrCodeService = qrCodeService;
    }

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
