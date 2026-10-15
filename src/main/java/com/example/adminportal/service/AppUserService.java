package com.example.adminportal.service;

import com.example.adminportal.dto.AppUserCreateRequest;
import com.example.adminportal.dto.AppUserResponse;
import com.example.adminportal.entity.AppUser;
import com.example.adminportal.entity.Member;
import com.example.adminportal.exception.AppUserNotFoundException;
import com.example.adminportal.exception.MemberAlreadyHasUserException;
import com.example.adminportal.exception.MemberNotFoundException;
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
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() ->
                        new AppUserNotFoundException("App user not found: " + id));

        return AppUserResponse.from(user);
    }

    public AppUser createUser(AppUserCreateRequest request) {

        Member member = memberRepo.findById(request.memberId())
                .orElseThrow(() ->
                        new MemberNotFoundException("Member: " + request.memberId() + " not found."));

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
