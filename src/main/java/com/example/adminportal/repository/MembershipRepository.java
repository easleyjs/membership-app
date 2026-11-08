package com.example.adminportal.repository;

import com.example.adminportal.entity.Membership;
import com.example.adminportal.entity.MembershipProgram;
import com.example.adminportal.entity.MembershipStatus;
import com.example.adminportal.entity.MembershipType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Page<Membership> findByMember_MemberNumber(Long memberNumber, Pageable pageable);

    List<Membership> findByMember_IdAndStatus(
            UUID memberId,
            MembershipStatus status
    );

    boolean existsByMember_MemberNumberAndProgramAndType(
            Long memberNumber,
            MembershipProgram program,
            MembershipType type
    );
}
