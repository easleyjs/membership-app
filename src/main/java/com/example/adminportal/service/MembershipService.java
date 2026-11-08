package com.example.adminportal.service;

import com.example.adminportal.dto.MembershipCreateRequest;
import com.example.adminportal.dto.MembershipResponse;
import com.example.adminportal.entity.*;
import com.example.adminportal.exception.MemberNotFoundException;
import com.example.adminportal.exception.MembershipAlreadyExistsException;
import com.example.adminportal.repository.MemberRepository;
import com.example.adminportal.repository.MembershipRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.util.List;

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
    public MembershipResponse createMembership(@Valid MembershipCreateRequest request) {
        Member member = memberRepository.findByMemberNumber(request.memberId())
                .orElseThrow(() -> new MemberNotFoundException(
                        "Member not found."
                ));

        boolean hasExistingMembership = membershipRepository.existsByMember_IdAndProgramAndType(
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

        return MembershipResponse.from(membershipRepository.save(newMembership));
    }

    public void ensureFreeMembership(Member member) {
        boolean exists = membershipRepository.existsByMember_IdAndProgramAndType(
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
                    null
            );
            membershipRepository.save(membership);
        }
    }

    public List<MembershipResponse> getAllMembershipsByMemberId(Long memberId) {
                return membershipRepository.findByMember_Id(memberId)
                        .stream()
                        .map(MembershipResponse::from)
                        .toList();
    }
}
