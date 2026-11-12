package com.easleyjs.membership.service;

import com.easleyjs.membership.dto.AppUserCreateRequest;
import com.easleyjs.membership.dto.AppUserResponse;
import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.exception.AppUserNotFoundException;
import com.easleyjs.membership.exception.MemberAlreadyHasUserException;
import com.easleyjs.membership.exception.MemberNotFoundException;
import com.easleyjs.membership.repository.MemberRepository;
import com.easleyjs.membership.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AppUserService {
    private AppUserRepository appUserRepo;
    private MemberRepository memberRepo;
    private PasswordEncoder passwordEncoder;

    public AppUserService(
            AppUserRepository appUserRepo,
            MemberRepository memberRepo,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepo = appUserRepo;
        this.memberRepo = memberRepo;
        this.passwordEncoder = passwordEncoder;
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
                passwordEncoder.encode(request.password()),
                request.role()
        );

        return appUserRepo.save(appUser);
    }
}
