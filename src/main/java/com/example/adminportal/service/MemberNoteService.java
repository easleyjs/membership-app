package com.example.adminportal.service;

import com.example.adminportal.dto.MemberNoteCreateRequest;
import com.example.adminportal.dto.MemberNoteResponse;
import com.example.adminportal.entity.MemberNote;
import com.example.adminportal.exception.MemberNotFoundException;
import com.example.adminportal.exception.ResourceNotFoundException;
import com.example.adminportal.repository.AppUserRepository;
import com.example.adminportal.repository.MemberNoteRepository;
import com.example.adminportal.repository.MemberRepository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class MemberNoteService {
    MemberNoteRepository memberNoteRepository;
    MemberRepository memberRepository;
    private final AppUserRepository appUserRepository;

    public MemberNoteService(
            MemberNoteRepository memberNoteRepository,
            MemberRepository memberRepository,
            AppUserRepository appUserRepository
    ) {
        this.memberNoteRepository = memberNoteRepository;
        this.memberRepository = memberRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public MemberNoteResponse createMemberNote(Long memberNumber, @Valid MemberNoteCreateRequest request) {
        var member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new MemberNotFoundException("Member: " + memberNumber + " not found."));
        var author = appUserRepository.findById(request.createdById())
                .orElseThrow(() -> new ResourceNotFoundException("User: " + request.createdById() + " not found."));
        MemberNote newMemberNote = new MemberNote();

        newMemberNote.setMember(member);
        newMemberNote.setCreatedBy(author);
        newMemberNote.setContent(request.content());

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
                .findByMember_MemberNumber(memberNumber, pageable)
                .map(MemberNoteResponse::from);
    }

}
