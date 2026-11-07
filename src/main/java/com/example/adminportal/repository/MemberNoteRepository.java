package com.example.adminportal.repository;

import com.example.adminportal.entity.MemberNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberNoteRepository extends JpaRepository<MemberNote,Long> {
    Page<MemberNote> findByMember_Id(
            Long memberId,
            Pageable pageable
    );
}
