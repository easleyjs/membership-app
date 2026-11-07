package com.example.adminportal.service;

import com.example.adminportal.repository.AppUserRepository;
import com.example.adminportal.repository.MemberNoteRepository;
import com.example.adminportal.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MemberNoteServiceTests {
    private final MemberNoteRepository notes = mock(MemberNoteRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final MemberNoteService service = new MemberNoteService(
            notes, members, mock(AppUserRepository.class));

    @Test
    void timestampSortUsesDescendingIdToBreakTies() {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        verifyRepositoryPageable(
                PageRequest.of(2, 10, sort),
                PageRequest.of(2, 10, sort.and(Sort.by(Sort.Direction.DESC, "id"))));
    }

    @Test
    void customSortIsPreservedBeforeIdTieBreaker() {
        Sort sort = Sort.by(Sort.Direction.ASC, "content");
        verifyRepositoryPageable(
                PageRequest.of(1, 5, sort),
                PageRequest.of(1, 5, sort.and(Sort.by(Sort.Direction.DESC, "id"))));
    }

    @Test
    void explicitIdSortIsAlreadyUniqueAndIsNotDuplicated() {
        Pageable pageable = PageRequest.of(0, 20,
                Sort.by("createdAt").and(Sort.by("id")));
        verifyRepositoryPageable(pageable, pageable);
    }

    @Test
    void unsortedPageUsesDescendingId() {
        verifyRepositoryPageable(
                PageRequest.of(0, 20),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id")));
    }

    @Test
    void unpagedRequestRemainsUnpagedWithStableSorting() {
        verifyRepositoryPageable(
                Pageable.unpaged(),
                Pageable.unpaged(Sort.by(Sort.Direction.DESC, "id")));
    }

    private void verifyRepositoryPageable(Pageable requested, Pageable expected) {
        when(members.existsByMemberNumber(1001L)).thenReturn(true);
        when(notes.findByMember_MemberNumber(eq(1001L), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getNotes(1001L, requested);

        verify(notes).findByMember_MemberNumber(1001L, expected);
    }
}
