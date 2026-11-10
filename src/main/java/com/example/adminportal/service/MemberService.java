package com.example.adminportal.service;

import com.example.adminportal.dto.*;
import com.example.adminportal.entity.Member;
import com.example.adminportal.entity.MemberStatus;
import com.example.adminportal.exception.MemberNotFoundException;
import com.example.adminportal.repository.MemberNoteRepository;
import com.example.adminportal.repository.MemberRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberNoteRepository memberNoteRepository;
    private final MembershipService membershipService;

    public MemberService(MemberRepository memberRepository, MemberNoteRepository memberNoteRepository, MembershipService membershipService) {
        this.memberRepository = memberRepository;
        this.memberNoteRepository = memberNoteRepository;
        this.membershipService = membershipService;
    }

    public List<MemberResponse> getAllMembers() {
        List<Member> members = memberRepository.findAll();
        if (members.isEmpty()) {
            return List.of();
        }

        var memberIds = members.stream().map(Member::getId).toList();
        var noteCounts = memberNoteRepository.countNotesByMemberIds(memberIds).stream()
                .collect(Collectors.toMap(
                        MemberNoteRepository.MemberNoteCount::getMemberId,
                        MemberNoteRepository.MemberNoteCount::getNoteCount));

        return members
                .stream()
                .map(member -> MemberResponse.from(member, noteCounts.getOrDefault(member.getId(), 0L)))
                .toList();
    }

    public MemberResponse findMemberById(Long id) {
        Member member = memberRepository.findByMemberNumber(id).get();
        return MemberResponse.from(member, memberNoteRepository.countByMember_Id(member.getId()));
    }

    @Transactional
    public MemberResponse createMember(MemberCreateRequest memberRequest) {
        Long memberNumber = memberRepository.getNextMemberNumber();
        String qrToken = UUID.randomUUID().toString();

        Member newMember = new Member(
                memberRequest.firstName(),
                memberRequest.lastName(),
                memberRequest.email(),
                memberNumber,
                qrToken
        );

        Member savedMember = memberRepository.save(newMember);

        // Give the new member a free membership, if they don't already have one
        membershipService.ensureFreeMembership(newMember);

        return MemberResponse.from(
                savedMember, 0
        );
    }

    @Transactional
    public MemberUpdateResponse updateMember(
            Long memberNumber,
            MemberUpdateRequest request
    ) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new MemberNotFoundException(
                        "Member: " + memberNumber + " not found."
                ));
        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        member.setEmail(request.email());

        return MemberUpdateResponse.from(member);
    }

    // TODO: Add logging for ban + reason
    @Transactional
    public MemberStatusResponse banMember(
            Long memberNumber,
            String reason
    ) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new MemberNotFoundException(
                        "Member: " + memberNumber + " not found."
                ));
        member.setStatus(MemberStatus.BANNED);

        System.out.println(reason); // replace with logging later

        return MemberStatusResponse.from(member);
    }

    @Transactional
    public MemberStatusResponse unbanMember(Long memberNumber) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new MemberNotFoundException(
                        "Member: " + memberNumber + " not found."
                ));

        if (member.getStatus() == MemberStatus.BANNED) {
            member.setStatus(MemberStatus.ACTIVE);
        }

        return MemberStatusResponse.from(member);
    }

}
