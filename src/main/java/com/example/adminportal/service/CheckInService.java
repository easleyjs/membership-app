package com.example.adminportal.service;

import com.example.adminportal.dto.CheckInCreateRequest;
import com.example.adminportal.dto.CheckInResponse;
import com.example.adminportal.entity.AppUser;
import com.example.adminportal.entity.CheckIn;
import com.example.adminportal.entity.Member;
import com.example.adminportal.exception.AppUserNotFoundException;
import com.example.adminportal.exception.MemberNotFoundException;
import com.example.adminportal.repository.AppUserRepository;
import com.example.adminportal.repository.CheckInRepository;
import com.example.adminportal.repository.MemberRepository;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CheckInService {
    CheckInRepository checkInRepository;
    MemberRepository memberRepository;
    AppUserRepository appUserRepository;
    EntityManager entityManager;

    public CheckInService(
            CheckInRepository checkInRepository,
            MemberRepository memberRepository,
            AppUserRepository appUserRepository,
            EntityManager entityManager
    ) {
        this.checkInRepository = checkInRepository;
        this.memberRepository = memberRepository;
        this.appUserRepository = appUserRepository;
    }

    //Perform a check-in
    @Transactional
    public CheckInResponse createCheckIn(
            CheckInCreateRequest request,
            UUID checkedInById
    ) {
        // Allow an offline client to retry the same check-in.
        CheckIn existing = checkInRepository.findById(request.id())
                .orElse(null);

        if (existing != null) {
            boolean sameCheckIn =
                    existing.getMember().getId().equals(request.memberId())
                            && existing.getCheckedInBy().getId().equals(checkedInById)
                            && existing.getMethod() == request.method();

            if (!sameCheckIn) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Check-in ID already exists with different details"
                );
            }

            return CheckInResponse.from(existing);
        }

        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new MemberNotFoundException(
                        "Member not found: " + request.memberId()
                ));

        AppUser staff = appUserRepository.findById(checkedInById)
                .orElseThrow(() -> new AppUserNotFoundException(
                        "App user not found: " + checkedInById
                ));

        CheckIn checkIn = new CheckIn();
        checkIn.setId(request.id());
        checkIn.setMember(member);
        checkIn.setCheckedInBy(staff);
        checkIn.setMethod(request.method());
        checkIn.setCreatedAt(Instant.now());

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

    //TODO: Later, will probably need a date range search and by-event and such.

}
