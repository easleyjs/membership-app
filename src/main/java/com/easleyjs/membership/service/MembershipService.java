package com.easleyjs.membership.service;

import com.easleyjs.membership.dto.MembershipCreateRequest;
import com.easleyjs.membership.dto.MembershipResponse;
import com.easleyjs.membership.entity.*;
import com.easleyjs.membership.exception.MemberNotFoundException;
import com.easleyjs.membership.exception.MembershipAlreadyExistsException;
import com.easleyjs.membership.repository.MemberRepository;
import com.easleyjs.membership.repository.MembershipRepository;
import jakarta.validation.Valid;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
        Member member = memberRepository.findByMemberNumberForUpdate(memberNumber)
                .orElseThrow(() -> new MemberNotFoundException(
                        "Member not found."
                ));

        Membership newMembership = new Membership();
        newMembership.setMember(member);
        newMembership.setProgram(request.membershipProgram());
        newMembership.setType(request.membershipType());
        newMembership.setStatus(MembershipStatus.ACTIVE);
        newMembership.setValidFrom(request.validFrom());
        newMembership.setValidUntil(request.validUntil());
        newMembership.setCreatedAt(Instant.now());
        newMembership.setCreatedBy(null); // TODO: once auth is in place, get authenticated user if exists

        // Check for duplicate memberships using the index constraint on Memberships
        try {
            return MembershipResponse.from(membershipRepository.saveAndFlush(newMembership));
        } catch (DataIntegrityViolationException ex) {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation) {
                    String name = violation.getConstraintName();
                    if (name != null) {
                        name = name.split(" INDEX ", 2)[0];
                    }

                    if ("UQ_MEMBERSHIPS_MEMBER_PROGRAM_TYPE".equalsIgnoreCase(name)
                            || "PUBLIC.UQ_MEMBERSHIPS_MEMBER_PROGRAM_TYPE".equalsIgnoreCase(name)) {
                        throw new MembershipAlreadyExistsException(
                                "This member already has a membership for this program and type."
                        );
                    }
                }
            }
            throw ex;
        }
    }

    @Transactional
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

    void changeMembershipType(Member member, MembershipType type) {
        Membership selected = null;
        for (Membership membership : membershipRepository.findByMember_IdAndProgram(member.getId(), MembershipProgram.OUTLAND)) {
            if (membership.getType() == type) {
                selected = membership;
            } else if (membership.getStatus() == MembershipStatus.ACTIVE) {
                membership.setStatus(MembershipStatus.CANCELLED);
            }
        }

        if (selected == null) {
            Instant now = Instant.now();
            membershipRepository.save(new Membership(member, MembershipProgram.OUTLAND, type,
                    MembershipStatus.ACTIVE, now, null, null, now));
        } else if (selected.getStatus() != MembershipStatus.ACTIVE) {
            selected.setStatus(MembershipStatus.ACTIVE);
            selected.setValidFrom(Instant.now());
            selected.setValidUntil(null);
        }
    }

    @Transactional(readOnly = true)
    public Page<MembershipResponse> getAllMembershipsByMemberId(Long memberNumber, Pageable pageable) {
        return membershipRepository.findByMember_MemberNumber(memberNumber, pageable)
                .map(MembershipResponse::from);
    }
}
