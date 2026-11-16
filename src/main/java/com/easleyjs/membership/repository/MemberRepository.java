package com.easleyjs.membership.repository;

import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.dto.UserMemberMatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    @Query(value = "SELECT nextval('member_number_seq')", nativeQuery = true)
    Long getNextMemberNumber();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") UUID id);

    Optional<Member> findByMemberNumber(Long memberNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.memberNumber = :memberNumber")
    Optional<Member> findByMemberNumberForUpdate(@Param("memberNumber") Long memberNumber);
    boolean existsByMemberNumber(Long memberNumber);
    public Member findMemberByEmailContainsIgnoreCase(String email);
    Optional<Member> findByEmail(String email);

    @Query("""
            select new com.easleyjs.membership.dto.UserMemberMatch(m.id, m.memberNumber, m.firstName, m.lastName, m.email)
            from Member m
            where not exists (select 1 from AppUser u where u.member = m)
              and (locate(:term, lower(trim(concat(coalesce(m.firstName, ''), ' ', coalesce(m.lastName, ''))))) > 0
                or locate(:term, lower(m.email)) > 0
                or locate(:term, cast(m.memberNumber as String)) = 1)
            order by m.lastName, m.firstName, m.memberNumber
            """)
    List<UserMemberMatch> searchMembersWithoutUser(@Param("term") String term, Pageable limit);
}
