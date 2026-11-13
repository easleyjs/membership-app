package com.easleyjs.membership.service;

import com.easleyjs.membership.dto.CheckInCreateRequest;
import com.easleyjs.membership.dto.CheckInResponse;
import com.easleyjs.membership.entity.AdmissionType;
import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.CheckIn;
import com.easleyjs.membership.entity.CheckInMethod;
import com.easleyjs.membership.entity.Event;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.entity.MemberStatus;
import com.easleyjs.membership.entity.Membership;
import com.easleyjs.membership.entity.MembershipStatus;
import com.easleyjs.membership.entity.MembershipType;
import com.easleyjs.membership.entity.UserRole;
import com.easleyjs.membership.exception.BannedMemberCheckInException;
import com.easleyjs.membership.exception.CheckInConflictException;
import com.easleyjs.membership.exception.CheckInMembershipRequiredException;
import com.easleyjs.membership.exception.DuplicateCheckInException;
import com.easleyjs.membership.exception.InvalidCheckInTimestampException;
import com.easleyjs.membership.exception.MemberNotFoundException;
import com.easleyjs.membership.exception.ResourceNotFoundException;
import com.easleyjs.membership.repository.AppUserRepository;
import com.easleyjs.membership.repository.CheckInRepository;
import com.easleyjs.membership.repository.EventRepository;
import com.easleyjs.membership.repository.MemberRepository;
import com.easleyjs.membership.repository.MembershipRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    private final EventRepository events = mock(EventRepository.class);
    private final MembershipRepository memberships = mock(MembershipRepository.class);
    private final CheckInService service = new CheckInService(checkIns, members, users, entityManager,
            events, memberships);

    private static final Long EVENT_ID = 42L;
    private final UUID checkInId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();
    private final Member member = mock(Member.class);
    private final AppUser staff = mock(AppUser.class);

    @Test
    void matchingRetryReturnsExistingCheckInEvenAfterOfflineWindowExpires() {
        Instant createdAt = Instant.parse("2020-01-01T12:00:00.123456Z");
        CheckIn existing = stubExistingCheckIn(createdAt.truncatedTo(ChronoUnit.MILLIS));

        assertEquals(CheckInResponse.from(existing), service.createCheckIn(requestAt(createdAt), staffId));
        verifyNoInteractions(entityManager);
    }

    @Test
    void existingIdWithDifferentDetailsIsRejected() {
        Instant createdAt = Instant.parse("2020-01-01T12:00:00Z");
        stubExistingCheckIn(createdAt);
        CheckInCreateRequest request = new CheckInCreateRequest(
                checkInId, memberId, staffId, CheckInMethod.QR_SCAN, createdAt, null, null);

        assertThrows(CheckInConflictException.class, () -> service.createCheckIn(request, staffId));
        verifyNoInteractions(entityManager);
    }

    @Test
    void bannedMemberIsRejected() {
        stubNewCheckIn(MemberStatus.BANNED);
        CheckInCreateRequest request = requestAt(Instant.now());

        assertThrows(BannedMemberCheckInException.class, () -> service.createCheckIn(request, staffId));
        verifyNoInteractions(entityManager);
    }

    @Test
    void timestampMoreThanFiveMinutesInTheFutureIsRejected() {
        stubNewCheckIn(MemberStatus.ACTIVE);
        CheckInCreateRequest request = requestAt(Instant.now().plus(6, ChronoUnit.MINUTES));

        InvalidCheckInTimestampException exception = assertThrows(
                InvalidCheckInTimestampException.class, () -> service.createCheckIn(request, staffId));
        assertEquals("Check-in time cannot be more than 5 minutes in the future", exception.getMessage());
        verifyNoInteractions(entityManager);
    }

    @Test
    void timestampMoreThanSevenDaysOldIsRejected() {
        stubNewCheckIn(MemberStatus.ACTIVE);
        CheckInCreateRequest request = requestAt(Instant.now().minus(8, ChronoUnit.DAYS));

        InvalidCheckInTimestampException exception = assertThrows(
                InvalidCheckInTimestampException.class, () -> service.createCheckIn(request, staffId));
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

        assertThrows(DuplicateCheckInException.class, () -> service.createCheckIn(request, staffId));
        verifyNoInteractions(entityManager);
    }

    @Test
    void unknownMemberIsRejected() {
        stubStaff();
        CheckInCreateRequest request = requestAt(Instant.now());

        assertThrows(MemberNotFoundException.class, () -> service.createCheckIn(request, staffId));
        verifyNoInteractions(entityManager);
    }

    @Test
    void unknownStaffIsRejected() {
        when(members.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        CheckInCreateRequest request = requestAt(Instant.now());

        assertThrows(AccessDeniedException.class, () -> service.createCheckIn(request, staffId));
        verifyNoInteractions(entityManager);
    }

    @Test
    void validOfflineCheckInIsPersistedAndReturned() {
        stubNewCheckIn(MemberStatus.ACTIVE);
        Instant createdAt = Instant.now().minus(6, ChronoUnit.DAYS);
        Instant normalizedCreatedAt = createdAt.truncatedTo(ChronoUnit.MILLIS);

        CheckInResponse response = service.createCheckIn(requestAt(createdAt), staffId);

        ArgumentCaptor<CheckIn> persisted = ArgumentCaptor.forClass(CheckIn.class);
        verify(entityManager).persist(persisted.capture());
        verify(entityManager).flush();
        assertSame(member, persisted.getValue().getMember());
        assertSame(staff, persisted.getValue().getCheckedInBy());
        assertEquals(new CheckInResponse(checkInId, memberId, staffId, normalizedCreatedAt,
                CheckInMethod.MANUAL, null, AdmissionType.STANDARD), response);
        verifyNoInteractions(events, memberships);
    }

    @ParameterizedTest
    @CsvSource({"true, PAID", "false, FREE", "false, PAID"})
    void qualifyingActiveMembershipAllowsCheckIn(boolean coverRequired, MembershipType type) {
        stubEvent(true, coverRequired);
        Membership active = new Membership();
        active.setType(type);
        active.setStatus(MembershipStatus.ACTIVE);
        when(memberships.findByMember_IdAndStatus(memberId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(active));

        assertEquals(EVENT_ID, service.createCheckIn(eventRequest(), staffId).eventId());
        verify(entityManager).flush();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void membershipRequiredEventRejectsMemberWithoutActiveMembership(boolean coverRequired) {
        stubEvent(true, coverRequired);
        CheckInCreateRequest request = eventRequest();

        assertThrows(CheckInMembershipRequiredException.class, () -> service.createCheckIn(request, staffId));
        verify(memberships).findByMember_IdAndStatus(memberId, MembershipStatus.ACTIVE);
        verifyNoInteractions(entityManager);
    }

    @Test
    void activeFreeMembershipIsInsufficientWhenMembershipAndCoverAreRequired() {
        stubEvent(true, true);
        Membership free = new Membership();
        free.setType(MembershipType.FREE);
        free.setStatus(MembershipStatus.ACTIVE);
        when(memberships.findByMember_IdAndStatus(memberId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(free));
        CheckInCreateRequest request = eventRequest();

        assertThrows(CheckInMembershipRequiredException.class, () -> service.createCheckIn(request, staffId));
        verifyNoInteractions(entityManager);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void explicitOverrideAdmitsMemberWithoutQualifyingMembership(boolean coverRequired) {
        stubEvent(true, coverRequired);
        Membership free = new Membership();
        free.setType(MembershipType.FREE);
        when(memberships.findByMember_IdAndStatus(memberId, MembershipStatus.ACTIVE))
                .thenReturn(coverRequired ? List.of(free) : List.of());
        CheckInCreateRequest request = overrideRequest(Instant.now(), null);

        CheckInResponse response = service.createCheckIn(request, staffId);

        var persisted = ArgumentCaptor.forClass(CheckIn.class);
        verify(entityManager).persist(persisted.capture());
        assertEquals(AdmissionType.MEMBERSHIP_OVERRIDE, persisted.getValue().getAdmissionType());
        assertEquals(AdmissionType.MEMBERSHIP_OVERRIDE, response.admissionType());
        assertEquals(staffId, response.checkedInById());
        assertEquals(CheckInMethod.QR_SCAN, response.method());
    }

    @Test
    void overrideRequestIsRecordedAsStandardWhenMembershipQualifies() {
        stubEvent(true, true);
        Membership paid = new Membership();
        paid.setType(MembershipType.PAID);
        when(memberships.findByMember_IdAndStatus(memberId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(paid));

        assertEquals(AdmissionType.STANDARD,
                service.createCheckIn(overrideRequest(Instant.now(), null), staffId).admissionType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"STANDARD", "MEMBERSHIP_OVERRIDE"})
    void overrideRetryReturnsOriginalDecisionAndRejectsChangedIntent(String originalAdmissionType) {
        Instant createdAt = Instant.parse("2020-01-01T12:00:00Z");
        CheckIn existing = stubExistingCheckIn(createdAt);
        existing.setEventId(EVENT_ID);
        existing.setMethod(CheckInMethod.QR_SCAN);
        existing.setMembershipOverrideRequested(true);
        existing.setAdmissionType(AdmissionType.valueOf(originalAdmissionType));
        var changed = new CheckInCreateRequest(checkInId, memberId, null, CheckInMethod.QR_SCAN,
                createdAt, EVENT_ID, false);

        assertEquals(CheckInResponse.from(existing),
                service.createCheckIn(overrideRequest(createdAt, null), staffId));
        assertThrows(CheckInConflictException.class, () -> service.createCheckIn(changed, staffId));
        verifyNoInteractions(events, memberships, entityManager);
    }

    @Test
    void overrideCannotUseAnotherStaffAccount() {
        var request = overrideRequest(Instant.now(), UUID.randomUUID());

        assertThrows(AccessDeniedException.class, () -> service.createCheckIn(request, staffId));
        verifyNoInteractions(users, members, entityManager);
    }

    @Test
    void disabledStaffCannotOverride() {
        stubStaff();
        when(staff.isEnabled()).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.createCheckIn(overrideRequest(Instant.now(), null), staffId));
        verifyNoInteractions(members, entityManager);
    }

    @Test
    void overrideCannotAdmitBannedMember() {
        stubNewCheckIn(MemberStatus.BANNED);

        assertThrows(BannedMemberCheckInException.class,
                () -> service.createCheckIn(overrideRequest(Instant.now(), null), staffId));
        verifyNoInteractions(events, memberships, entityManager);
    }

    @Test
    void overrideCannotBypassDuplicateCheck() {
        stubEvent(true, false);
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        when(checkIns.existsByMember_IdAndCreatedAtGreaterThanAndCreatedAtLessThan(
                memberId, createdAt.minus(8, ChronoUnit.HOURS), createdAt.plus(8, ChronoUnit.HOURS)))
                .thenReturn(true);

        assertThrows(DuplicateCheckInException.class,
                () -> service.createCheckIn(overrideRequest(createdAt, null), staffId));
        verifyNoInteractions(entityManager);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void eventWithoutMembershipRequirementSkipsMembershipLookup(boolean coverRequired) {
        stubEvent(false, coverRequired);

        assertEquals(EVENT_ID, service.createCheckIn(eventRequest(), staffId).eventId());
        verifyNoInteractions(memberships);
    }

    @Test
    void unknownEventIsRejected() {
        stubNewCheckIn(MemberStatus.ACTIVE);
        CheckInCreateRequest request = eventRequest();

        assertThrows(ResourceNotFoundException.class, () -> service.createCheckIn(request, staffId));
        verifyNoInteractions(memberships, entityManager);
    }

    @Test
    void retryWithDifferentEventIsRejected() {
        CheckInCreateRequest request = eventRequest();
        stubExistingCheckIn(request.createdAt().truncatedTo(ChronoUnit.MILLIS)).setEventId(43L);

        assertThrows(CheckInConflictException.class, () -> service.createCheckIn(request, staffId));
        verifyNoInteractions(events, memberships, entityManager);
    }

    private void stubEvent(boolean membershipRequired, boolean coverRequired) {
        stubNewCheckIn(MemberStatus.ACTIVE);
        Event event = new Event();
        event.setMembershipRequired(membershipRequired);
        event.setCoverRequired(coverRequired);
        when(events.findById(EVENT_ID)).thenReturn(Optional.of(event));
    }

    private CheckInCreateRequest eventRequest() {
        return new CheckInCreateRequest(checkInId, memberId, staffId, CheckInMethod.MANUAL,
                Instant.now(), EVENT_ID, null);
    }

    private CheckInCreateRequest requestAt(Instant createdAt) {
        return new CheckInCreateRequest(checkInId, memberId, staffId, CheckInMethod.MANUAL, createdAt, null, null);
    }

    private CheckInCreateRequest overrideRequest(Instant createdAt, UUID requestedStaffId) {
        return new CheckInCreateRequest(checkInId, memberId, requestedStaffId, CheckInMethod.QR_SCAN,
                createdAt, EVENT_ID, true);
    }

    private void stubNewCheckIn(MemberStatus status) {
        when(members.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        when(member.getId()).thenReturn(memberId);
        when(member.getStatus()).thenReturn(status);
        stubStaff();
    }

    private void stubStaff() {
        when(users.findById(staffId)).thenReturn(Optional.of(staff));
        when(staff.isEnabled()).thenReturn(true);
        when(staff.getRole()).thenReturn(UserRole.STAFF);
        when(staff.getId()).thenReturn(staffId);
    }

    private CheckIn stubExistingCheckIn(Instant createdAt) {
        when(members.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        when(member.getId()).thenReturn(memberId);
        stubStaff();
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
