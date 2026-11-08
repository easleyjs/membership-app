package com.example.adminportal.service;

import com.example.adminportal.entity.Member;
import com.example.adminportal.repository.MemberNoteRepository;
import com.example.adminportal.repository.MemberRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemberServiceTests {
    private final MemberRepository members = mock(MemberRepository.class);
    private final MemberNoteRepository notes = mock(MemberNoteRepository.class);
    private final MembershipService membershipService = mock(MembershipService.class);
    private final MemberService service = new MemberService(members, notes, membershipService);

    @Test
    void memberListIncludesCountsAndDefaultsMembersWithoutNotesToZero() {
        Member withNotes = mock(Member.class);
        Member withoutNotes = mock(Member.class);
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        when(withNotes.getId()).thenReturn(firstId);
        when(withoutNotes.getId()).thenReturn(secondId);
        when(members.findAll()).thenReturn(List.of(withNotes, withoutNotes));

        var count = mock(MemberNoteRepository.MemberNoteCount.class);
        when(count.getMemberId()).thenReturn(firstId);
        when(count.getNoteCount()).thenReturn(3L);
        when(notes.countNotesByMemberIds(List.of(firstId, secondId))).thenReturn(List.of(count));

        var result = service.getAllMembers();

        assertEquals(2, result.size());
        assertEquals(firstId, result.get(0).id());
        assertTrue(result.get(0).hasNotes());
        assertEquals(3L, result.get(0).noteCount());
        assertFalse(result.get(1).hasNotes());
        assertEquals(0L, result.get(1).noteCount());
        verify(notes).countNotesByMemberIds(List.of(firstId, secondId));
        verifyNoMoreInteractions(notes);
    }

    @Test
    void emptyMemberListDoesNotQueryNoteCounts() {
        when(members.findAll()).thenReturn(List.of());

        assertTrue(service.getAllMembers().isEmpty());

        verifyNoInteractions(notes);
    }

    @Test
    void memberDetailRefreshesNoteCountOnEachRead() {
        Member member = mock(Member.class);
        UUID memberId = UUID.randomUUID();
        when(member.getId()).thenReturn(memberId);
        when(members.findByMemberNumber(1001L)).thenReturn(Optional.of(member));
        when(notes.countByMember_Id(memberId)).thenReturn(0L, 2L, 0L);

        var before = service.findMemberById(1001L);
        var afterCreation = service.findMemberById(1001L);
        var afterDeletion = service.findMemberById(1001L);

        assertFalse(before.hasNotes());
        assertEquals(0L, before.noteCount());
        assertTrue(afterCreation.hasNotes());
        assertEquals(2L, afterCreation.noteCount());
        assertFalse(afterDeletion.hasNotes());
        assertEquals(0L, afterDeletion.noteCount());
    }
}
