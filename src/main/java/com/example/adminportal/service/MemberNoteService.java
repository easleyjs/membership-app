package com.example.adminportal.service;

import com.example.adminportal.dto.MemberNoteCreateRequest;
import com.example.adminportal.dto.MemberNoteResponse;
import com.example.adminportal.entity.MemberNote;
import com.example.adminportal.exception.MemberNotFoundException;
import com.example.adminportal.repository.MemberNoteRepository;
import com.example.adminportal.repository.MemberRepository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
public class MemberNoteService {
    MemberNoteRepository memberNoteRepository;
    MemberRepository memberRepository;

    public MemberNoteService(
            MemberNoteRepository memberNoteRepository,
            MemberRepository memberRepository
    ) {
        this.memberNoteRepository = memberNoteRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MemberNoteResponse createMemberNote(@Valid MemberNoteCreateRequest request) {
        MemberNote newMemberNote = new MemberNote();

        newMemberNote.setMember(request.member());
        newMemberNote.setCreatedBy(request.createdBy());
        newMemberNote.setContent(request.content());
        newMemberNote.setCreatedAt(request.createdAt());

        MemberNote savedMemberNote = memberNoteRepository.save(newMemberNote);

        return MemberNoteResponse.from(savedMemberNote);
    }

    @Transactional(readOnly = true)
    public Page<MemberNoteResponse> getNotes(
            Long memberNumber,
            Pageable pageable) {

        if (!memberRepository.existsByMemberNumber(memberNumber)) {
            throw new MemberNotFoundException("Member : " + memberNumber + " not found.");
        }

        return memberNoteRepository
                .findByMember_Id(memberNumber, pageable)
                .map(note -> new MemberNoteResponse(
                        note.getId(),
                        note.getContent(),
                        note.getCreatedBy().getMember().getFirstName(),
                        note.getCreatedAt()
                ));
    }

}
