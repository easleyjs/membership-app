package com.easleyjs.membership.service;

import com.easleyjs.membership.dto.CheckInCreateRequest;
import com.easleyjs.membership.dto.CheckInResponse;
import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.CheckIn;
import com.easleyjs.membership.entity.Event;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.entity.MemberStatus;
import com.easleyjs.membership.entity.MembershipStatus;
import com.easleyjs.membership.entity.MembershipType;
import com.easleyjs.membership.exception.*;
import com.example.adminportal.exception.*;
import com.easleyjs.membership.repository.AppUserRepository;
import com.easleyjs.membership.repository.CheckInRepository;
import com.easleyjs.membership.repository.EventRepository;
import com.easleyjs.membership.repository.MemberRepository;
import com.easleyjs.membership.repository.MembershipRepository;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CheckInService {
    CheckInRepository checkInRepository;
    MemberRepository memberRepository;
    AppUserRepository appUserRepository;
    EntityManager entityManager;
    private final EventRepository eventRepository;
    private final MembershipRepository membershipRepository;

    // How far in the future can a check-in creation timestamp be
    private static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(5);
    // How many days old can a check-in timestamp be
    private static final Duration MAX_OFFLINE_AGE = Duration.ofDays(7);

    public CheckInService(
            CheckInRepository checkInRepository,
            MemberRepository memberRepository,
            AppUserRepository appUserRepository,
            EntityManager entityManager,
            EventRepository eventRepository,
            MembershipRepository membershipRepository
    ) {
        this.checkInRepository = checkInRepository;
        this.memberRepository = memberRepository;
        this.appUserRepository = appUserRepository;
        this.entityManager = entityManager;
        this.eventRepository = eventRepository;
        this.membershipRepository = membershipRepository;
    }

    //Perform a check-in
    @Transactional
    public CheckInResponse createCheckIn(
            CheckInCreateRequest request
    ) {

        Member member = memberRepository.findByIdForUpdate(request.memberId())
                .orElseThrow(() -> new MemberNotFoundException(
                        "Member not found: " + request.memberId()
                ));

        // Normalize timestamps to milliseconds for storage and comparison.
        Instant createdAt = request.createdAt().truncatedTo(ChronoUnit.MILLIS);
        Instant receivedAt = Instant.now();

        CheckIn existingCheckIn = validateExistingCheckIn(request, createdAt);
        if (existingCheckIn != null) {
            return CheckInResponse.from(existingCheckIn);
        }

        AppUser staff = findStaff(request.staffMemberId());

        validateCheckIn(member, request.eventId(), createdAt, receivedAt);

        CheckIn checkIn = new CheckIn();
        checkIn.setId(request.id());
        checkIn.setEventId(request.eventId());
        checkIn.setMember(member);
        checkIn.setCheckedInBy(staff);
        checkIn.setMethod(request.method());
        checkIn.setCreatedAt(createdAt);
        checkIn.setReceivedAt(receivedAt);

        entityManager.persist(checkIn);
        entityManager.flush();

        return CheckInResponse.from(checkIn);
    }

    private void validateCheckIn(Member member, Long eventId, Instant createdAt, Instant receivedAt) {
        validateMemberStatus(member);
        validateCheckInTime(createdAt, receivedAt);
        if (eventId != null) {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
            validateMembershipEligibility(member.getId(), event);
        }
        validateDuplicateCheckIn(member.getId(), createdAt);
    }

    private void validateMembershipEligibility(UUID memberId, Event event) {
        if (!event.isMembershipRequired()) {
            return;
        }

        boolean paidRequired = event.isCoverRequired();
        boolean eligible = membershipRepository.findByMember_IdAndStatus(memberId, MembershipStatus.ACTIVE)
                .stream()
                .anyMatch(membership -> !paidRequired || membership.getType() == MembershipType.PAID);
        if (!eligible) {
            throw new CheckInMembershipRequiredException(paidRequired
                    ? "An active paid membership is required for this event."
                    : "An active membership is required for this event.");
        }
    }

    private CheckIn validateExistingCheckIn(CheckInCreateRequest request, Instant createdAt) {
        CheckIn existingCheckIn = checkInRepository.findById(request.id())
                .orElse(null);
        if (existingCheckIn == null) {
            return null;
        }

        boolean sameCheckIn =
                existingCheckIn.getMember().getId().equals(request.memberId())
                        && existingCheckIn.getCheckedInBy().getId().equals(request.staffMemberId())
                        && existingCheckIn.getMethod() == request.method()
                        && existingCheckIn.getCreatedAt().equals(createdAt)
                        && Objects.equals(existingCheckIn.getEventId(), request.eventId());

        if (!sameCheckIn) {
            throw new CheckInConflictException(
                    "Check-in ID already exists with different details"
            );
        }
        return existingCheckIn;
    }

    private void validateMemberStatus(Member member) {
        if (member.getStatus() == MemberStatus.BANNED) {
            throw new BannedMemberCheckInException(
                    "Banned members cannot check in."
            );
        }
    }

    private AppUser findStaff(UUID staffId) {
        // TODO: Need to check whether staff member has access to do a check-in
        return appUserRepository.findById(staffId)
                .orElseThrow(() -> new AppUserNotFoundException(
                        "App user not found: " + staffId
                ));
    }

    private void validateDuplicateCheckIn(UUID memberId, Instant createdAt) {
        boolean duplicate =
                checkInRepository
                        .existsByMember_IdAndCreatedAtGreaterThanAndCreatedAtLessThan(
                                memberId,
                                createdAt.minus(8, ChronoUnit.HOURS),
                                createdAt.plus(8, ChronoUnit.HOURS)
                        );

        if (duplicate) {
            throw new DuplicateCheckInException(
                    "Member has already checked in within 8 hours of this check-in"
            );
        }
    }

    private void validateCheckInTime(Instant createdAt, Instant receivedAt) {
        if (createdAt.isAfter(receivedAt.plus(MAX_FUTURE_SKEW))) {
            throw new InvalidCheckInTimestampException(
                    "Check-in time cannot be more than 5 minutes in the future"
            );
        }

        if (createdAt.isBefore(receivedAt.minus(MAX_OFFLINE_AGE))) {
            throw new InvalidCheckInTimestampException(
                    "Check-in time cannot be more than 7 days old"
            );
        }
    }

    public List<CheckInResponse> getCheckInsByMember(UUID memberId) {
        return checkInRepository.findByMember_Id(memberId)
                .stream()
                .map(CheckInResponse::from)
                .toList();
    }

    public List<CheckInResponse> getAllCheckIns() {
        return checkInRepository.findAll()
                .stream()
                .map(CheckInResponse::from)
                .toList();
    }

    //TODO: Later, will probably need a date range search and by-event and such.

}
