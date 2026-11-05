package com.example.adminportal.service;

import com.example.adminportal.dto.CheckInCreateRequest;
import com.example.adminportal.dto.CheckInResponse;
import com.example.adminportal.entity.AppUser;
import com.example.adminportal.entity.CheckIn;
import com.example.adminportal.entity.Member;
import com.example.adminportal.exception.*;
import com.example.adminportal.repository.AppUserRepository;
import com.example.adminportal.repository.CheckInRepository;
import com.example.adminportal.repository.MemberRepository;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class CheckInService {
    CheckInRepository checkInRepository;
    MemberRepository memberRepository;
    AppUserRepository appUserRepository;
    EntityManager entityManager;

    // How far in the future can a check-in creation timestamp be
    private static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(5);
    // How many days old can a check-in timestamp be
    private static final Duration MAX_OFFLINE_AGE = Duration.ofDays(7);

    public CheckInService(
            CheckInRepository checkInRepository,
            MemberRepository memberRepository,
            AppUserRepository appUserRepository,
            EntityManager entityManager
    ) {
        this.checkInRepository = checkInRepository;
        this.memberRepository = memberRepository;
        this.appUserRepository = appUserRepository;
        this.entityManager = entityManager;
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

        UUID staffId = request.staffMemberId();

        // Normalize timestamps to milliseconds for storage and comparison.
        Instant createdAt = request.createdAt().truncatedTo(ChronoUnit.MILLIS);

        CheckIn existingCheckIn = checkInRepository.findById(request.id())
                .orElse(null);

        if (existingCheckIn != null) {
            boolean sameCheckIn =
                    existingCheckIn.getMember().getId().equals(request.memberId())
                            && existingCheckIn.getCheckedInBy().getId()
                            .equals(request.staffMemberId())
                            && existingCheckIn.getMethod() == request.method()
                            && existingCheckIn.getCreatedAt().equals(createdAt);

            if (!sameCheckIn) {
                throw new CheckInConflictException(
                        "Check-in ID already exists with different details"
                );
            }

            return CheckInResponse.from(existingCheckIn);
        }

        // Allow for offline sync check-ins
        Instant receivedAt = Instant.now();

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

        // Check for duplicates - same member, same timeframe <and same event, once added>
        boolean duplicate =
                checkInRepository
                        .existsByMember_IdAndCreatedAtGreaterThanAndCreatedAtLessThan(
                                member.getId(),
                                createdAt.minus(8, ChronoUnit.HOURS),
                                createdAt.plus(8, ChronoUnit.HOURS)
                        );

        if (duplicate) {
            throw new DuplicateCheckInException(
                    "Member has already checked in within 8 hours of this check-in"
            );
        }

        AppUser staff = appUserRepository.findById(staffId)
                .orElseThrow(() -> new AppUserNotFoundException(
                        "App user not found: " + staffId
                ));

        CheckIn checkIn = new CheckIn();
        checkIn.setId(request.id());
        checkIn.setMember(member);
        checkIn.setCheckedInBy(staff);
        checkIn.setMethod(request.method());
        checkIn.setCreatedAt(createdAt);
        checkIn.setReceivedAt(receivedAt);

        entityManager.persist(checkIn);
        entityManager.flush();

        return CheckInResponse.from(checkIn);
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
