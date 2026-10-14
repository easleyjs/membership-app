package com.example.adminportal.repository;

import com.example.adminportal.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    @Query(value = "SELECT NEXT VALUE FOR member_number_seq", nativeQuery = true)
    Long getNextMemberNumber();

    Optional<Member> findByMemberNumber(Long memberNumber);
    public Member findMemberById(Long id);

    public Member findMemberByMemberId(UUID uuid);
}
