package com.example.adminportal.repository;

import com.example.adminportal.entity.MemberNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MemberNoteRepository extends JpaRepository<MemberNote,Long> {
    interface MemberNoteCount {
        UUID getMemberId();
        long getNoteCount();
    }

    long countByMember_Id(UUID memberId);

    @Query("""
            select n.member.id as memberId, count(n) as noteCount
            from MemberNote n
            where n.member.id in :memberIds
            group by n.member.id
            """)
    List<MemberNoteCount> countNotesByMemberIds(@Param("memberIds") Collection<UUID> memberIds);

    Page<MemberNote> findByMember_MemberNumber(
            Long memberNumber,
            Pageable pageable
    );
}
