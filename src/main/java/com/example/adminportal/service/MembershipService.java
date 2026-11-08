package com.example.adminportal.service;

import com.example.adminportal.dto.MembershipCreateRequest;
import com.example.adminportal.dto.MembershipResponse;
import com.example.adminportal.entity.*;
import com.example.adminportal.exception.MemberNotFoundException;
import com.example.adminportal.exception.MembershipAlreadyExistsException;
import com.example.adminportal.repository.MemberRepository;
import com.example.adminportal.repository.MembershipRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class MembershipService {
    private final MembershipRepository membershipRepository;
    MemberRepository memberRepository;

    public MembershipService(
            MembershipRepository membershipRepo,
            MemberRepository memberRepository
    ) {
        this.membershipRepository = membershipRepo;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MembershipResponse createMembership(Long memberNumber, @Valid MembershipCreateRequest request) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new MemberNotFoundException(
                        "Member not found."
                ));

        boolean hasExistingMembership = membershipRepository.existsByMember_MemberNumberAndProgramAndType(
                request.memberId(),
                request.membershipProgram(),
                request.membershipType()
        );

        if (hasExistingMembership) {
            throw new MembershipAlreadyExistsException(
                    "This member already has a membership for this program."
            );
        }

        Membership newMembership = new Membership();
        newMembership.setMember(member);
        newMembership.setProgram(request.membershipProgram());
        newMembership.setType(request.membershipType());
        newMembership.setStatus(MembershipStatus.ACTIVE);
        newMembership.setValidFrom(request.validFrom());
        newMembership.setValidUntil(request.validUntil());
        newMembership.setCreatedAt(Instant.now());
        newMembership.setCreatedBy(null); // TODO: once auth is in place, get authenticated user if exists

        return MembershipResponse.from(membershipRepository.save(newMembership));
    }

    public void ensureFreeMembership(Member member) {
        boolean exists = membershipRepository.existsByMember_MemberNumberAndProgramAndType(
                member.getMemberNumber(),
                MembershipProgram.OUTLAND,
                MembershipType.FREE
        );

        if (!exists) {
            Membership membership = new Membership(
                    member,
                    MembershipProgram.OUTLAND,
                    MembershipType.FREE,
                    MembershipStatus.ACTIVE,
                    Instant.now(),
                    null,
                    null,
                    Instant.now()
            );
            membershipRepository.save(membership);
        }
    }

    @Transactional(readOnly = true)
    public Page<MembershipResponse> getAllMembershipsByMemberId(Long memberNumber, Pageable pageable) {
        return membershipRepository.findByMember_MemberNumber(memberNumber, pageable)
                .map(MembershipResponse::from);
    }
}
