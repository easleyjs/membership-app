package com.easleyjs.membership.repository;

import com.easleyjs.membership.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    @Query(value = "SELECT nextval('member_number_seq')", nativeQuery = true)
    Long getNextMemberNumber();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") UUID id);

    Optional<Member> findByMemberNumber(Long memberNumber);
    boolean existsByMemberNumber(Long memberNumber);
    public Member findMemberByEmailContainsIgnoreCase(String email);
    Optional<Member> findByEmail(String email);
}
