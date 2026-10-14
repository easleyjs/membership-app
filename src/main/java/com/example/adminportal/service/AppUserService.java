package com.example.adminportal.service;

import com.example.adminportal.dto.AppUserCreateRequest;
import com.example.adminportal.dto.AppUserResponse;
import com.example.adminportal.entity.AppUser;
import com.example.adminportal.entity.Member;
import com.example.adminportal.exception.MemberAlreadyHasUserException;
import com.example.adminportal.repository.MemberRepository;
import com.example.adminportal.repository.AppUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AppUserService {
    private AppUserRepository appUserRepo;
    private MemberRepository memberRepo;

    public AppUserService(
            AppUserRepository appUserRepo,
            MemberRepository memberRepo
    ) {
        this.appUserRepo = appUserRepo;
        this.memberRepo = memberRepo;
    }

    public List<AppUserResponse> getAllAppUsers() {
        return appUserRepo.findAll()
                .stream()
                .map(AppUserResponse::from)
                .toList();
    }

    public AppUserResponse findAppUserById(UUID id) {
        return AppUserResponse.from(appUserRepo.findById(id));
    }

    public AppUser createUser(AppUserCreateRequest request) {

        Member member = memberRepo.findMemberByMemberId(request.memberId());
                /*
                .orElseThrow(() ->
                        new MemberNotFoundException(request.memberId()));
                */
        if (appUserRepo.existsByMember_Id((member.getId()))) {
            throw new MemberAlreadyHasUserException("App user exists for member: " + member.getId());
        }

        AppUser appUser = new AppUser(
                member,
                request.password(), //passwordEncoder.encode(request.password()),
                request.role()
        );

        return appUserRepo.save(appUser);
    }
}
