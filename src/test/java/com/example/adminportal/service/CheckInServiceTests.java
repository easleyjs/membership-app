package com.example.adminportal.service;

import com.example.adminportal.dto.CheckInCreateRequest;
import com.example.adminportal.dto.CheckInResponse;
import com.example.adminportal.entity.AppUser;
import com.example.adminportal.entity.CheckIn;
import com.example.adminportal.entity.CheckInMethod;
import com.example.adminportal.entity.Member;
import com.example.adminportal.entity.MemberStatus;
import com.example.adminportal.exception.AppUserNotFoundException;
import com.example.adminportal.exception.BannedMemberCheckInException;
import com.example.adminportal.exception.CheckInConflictException;
import com.example.adminportal.exception.DuplicateCheckInException;
import com.example.adminportal.exception.InvalidCheckInTimestampException;
import com.example.adminportal.exception.MemberNotFoundException;
import com.example.adminportal.repository.AppUserRepository;
import com.example.adminportal.repository.CheckInRepository;
import com.example.adminportal.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CheckInServiceTests {
    private final CheckInRepository checkIns = mock(CheckInRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final CheckInService service = new CheckInService(checkIns, members, users, entityManager);

    private final UUID checkInId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();
    private final Member member = mock(Member.class);
    private final AppUser staff = mock(AppUser.class);

    @Test
    void matchingRetryReturnsExistingCheckInEvenAfterOfflineWindowExpires() {
        Instant createdAt = Instant.parse("2020-01-01T12:00:00.123456Z");
        CheckIn existing = stubExistingCheckIn(createdAt.truncatedTo(ChronoUnit.MILLIS));

        assertEquals(CheckInResponse.from(existing), service.createCheckIn(requestAt(createdAt)));
        verifyNoInteractions(users, entityManager);
    }

    @Test
    void existingIdWithDifferentDetailsIsRejected() {
        Instant createdAt = Instant.parse("2020-01-01T12:00:00Z");
        stubExistingCheckIn(createdAt);
        CheckInCreateRequest request = new CheckInCreateRequest(
                checkInId, memberId, staffId, CheckInMethod.QR_SCAN, createdAt);

        assertThrows(CheckInConflictException.class, () -> service.createCheckIn(request));
        verifyNoInteractions(users, entityManager);
    }

    @Test
    void bannedMemberIsRejected() {
        stubNewCheckIn(MemberStatus.BANNED);
        CheckInCreateRequest request = requestAt(Instant.now());

        assertThrows(BannedMemberCheckInException.class, () -> service.createCheckIn(request));
        verifyNoInteractions(entityManager);
    }

    @Test
    void timestampMoreThanFiveMinutesInTheFutureIsRejected() {
        stubNewCheckIn(MemberStatus.ACTIVE);
        CheckInCreateRequest request = requestAt(Instant.now().plus(6, ChronoUnit.MINUTES));

        InvalidCheckInTimestampException exception = assertThrows(
                InvalidCheckInTimestampException.class, () -> service.createCheckIn(request));
        assertEquals("Check-in time cannot be more than 5 minutes in the future", exception.getMessage());
        verifyNoInteractions(entityManager);
    }

    @Test
    void timestampMoreThanSevenDaysOldIsRejected() {
        stubNewCheckIn(MemberStatus.ACTIVE);
        CheckInCreateRequest request = requestAt(Instant.now().minus(8, ChronoUnit.DAYS));

        InvalidCheckInTimestampException exception = assertThrows(
                InvalidCheckInTimestampException.class, () -> service.createCheckIn(request));
        assertEquals("Check-in time cannot be more than 7 days old", exception.getMessage());
        verifyNoInteractions(entityManager);
    }

    @Test
    void checkInForSameMemberWithinEightHoursIsRejected() {
        stubNewCheckIn(MemberStatus.ACTIVE);
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        CheckInCreateRequest request = requestAt(createdAt);
        when(checkIns.existsByMember_IdAndCreatedAtGreaterThanAndCreatedAtLessThan(
                memberId, createdAt.minus(8, ChronoUnit.HOURS), createdAt.plus(8, ChronoUnit.HOURS)))
                .thenReturn(true);

        assertThrows(DuplicateCheckInException.class, () -> service.createCheckIn(request));
        verifyNoInteractions(entityManager);
    }

    @Test
    void unknownMemberIsRejected() {
        CheckInCreateRequest request = requestAt(Instant.now());

        assertThrows(MemberNotFoundException.class, () -> service.createCheckIn(request));
        verifyNoInteractions(entityManager);
    }

    @Test
    void unknownStaffIsRejected() {
        when(members.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        CheckInCreateRequest request = requestAt(Instant.now());

        assertThrows(AppUserNotFoundException.class, () -> service.createCheckIn(request));
        verifyNoInteractions(entityManager);
    }

    @Test
    void validOfflineCheckInIsPersistedAndReturned() {
        stubNewCheckIn(MemberStatus.ACTIVE);
        Instant createdAt = Instant.now().minus(6, ChronoUnit.DAYS);
        Instant normalizedCreatedAt = createdAt.truncatedTo(ChronoUnit.MILLIS);

        CheckInResponse response = service.createCheckIn(requestAt(createdAt));

        ArgumentCaptor<CheckIn> persisted = ArgumentCaptor.forClass(CheckIn.class);
        verify(entityManager).persist(persisted.capture());
        verify(entityManager).flush();
        assertSame(member, persisted.getValue().getMember());
        assertSame(staff, persisted.getValue().getCheckedInBy());
        assertEquals(new CheckInResponse(checkInId, memberId, staffId, normalizedCreatedAt,
                CheckInMethod.MANUAL), response);
    }

    private CheckInCreateRequest requestAt(Instant createdAt) {
        return new CheckInCreateRequest(checkInId, memberId, staffId, CheckInMethod.MANUAL, createdAt);
    }

    private void stubNewCheckIn(MemberStatus status) {
        when(members.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        when(member.getId()).thenReturn(memberId);
        when(member.getStatus()).thenReturn(status);
        when(users.findById(staffId)).thenReturn(Optional.of(staff));
        when(staff.getId()).thenReturn(staffId);
    }

    private CheckIn stubExistingCheckIn(Instant createdAt) {
        when(members.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        when(member.getId()).thenReturn(memberId);
        when(staff.getId()).thenReturn(staffId);
        CheckIn existing = new CheckIn();
        existing.setId(checkInId);
        existing.setMember(member);
        existing.setCheckedInBy(staff);
        existing.setCreatedAt(createdAt);
        existing.setMethod(CheckInMethod.MANUAL);
        when(checkIns.findById(checkInId)).thenReturn(Optional.of(existing));
        return existing;
    }
}
