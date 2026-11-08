package com.example.adminportal.repository;

import com.example.adminportal.entity.Membership;
import com.example.adminportal.entity.MembershipProgram;
import com.example.adminportal.entity.MembershipStatus;
import com.example.adminportal.entity.MembershipType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByMember_Id(Long memberId);

    List<Membership> findByMember_IdAndStatus(
            Long memberId,
            MembershipStatus status
    );

    boolean existsByMember_IdAndProgramAndType(
            Long memberNumber,
            MembershipProgram program,
            MembershipType type
    );
}
