package com.easleyjs.membership.service;

import com.easleyjs.membership.dto.AppUserCreateRequest;
import com.easleyjs.membership.dto.AppUserResponse;
import com.easleyjs.membership.dto.AppUserStatusResponse;
import com.easleyjs.membership.dto.AppUserUpdateRequest;
import com.easleyjs.membership.dto.UserMemberMatch;
import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.exception.AppUserNotFoundException;
import com.easleyjs.membership.exception.MemberAlreadyHasUserException;
import com.easleyjs.membership.exception.MemberNotFoundException;
import com.easleyjs.membership.repository.MemberRepository;
import com.easleyjs.membership.repository.AppUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AppUserService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AppUserRepository appUserRepo;
    private final MemberRepository memberRepo;
    private final PasswordEncoder passwordEncoder;

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

    public List<UserMemberMatch> searchEligibleMembers(String query) {
        String term = query.strip().replaceFirst("^#", "").strip().toLowerCase(Locale.ROOT);
        return term.isEmpty() ? List.of() : memberRepo.searchMembersWithoutUser(term, PageRequest.of(0, 10));
    }

    @Transactional
    public AppUserResponse updateUser(UUID id, AppUserUpdateRequest request) {
        AppUser user = appUserRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new AppUserNotFoundException("App user not found: " + id));
        user.setRole(request.role());
        user.setEnabled(request.enabled());
        return AppUserResponse.from(user);
    }

    @Transactional
    public AppUserStatusResponse updateStatus(Long memberNumber, boolean enabled) {
        AppUser user = appUserRepo.findByMember_MemberNumber(memberNumber)
                .orElseThrow(() -> new AppUserNotFoundException("App user not found for member number: " + memberNumber));
        user.setEnabled(enabled);
        return AppUserStatusResponse.from(user);
    }

    @Transactional
    public AppUserResponse createUser(AppUserCreateRequest request) {

        Member member = memberRepo.findByIdForUpdate(request.memberId())
                .orElseThrow(() ->
                        new MemberNotFoundException("Member: " + request.memberId() + " not found."));

        if (appUserRepo.existsByMember_Id(member.getId())) {
            throw new MemberAlreadyHasUserException("App user exists for member: " + member.getId());
        }

        String password = request.password() == null ? generatePassword() : request.password();
        AppUser appUser = new AppUser(
                member,
                passwordEncoder.encode(password),
                request.role()
        );

        return AppUserResponse.from(appUserRepo.save(appUser));
    }

    private static String generatePassword() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
