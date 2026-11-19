package com.easleyjs.membership.service;

import com.easleyjs.membership.dto.AppUserCreateRequest;
import com.easleyjs.membership.dto.AppUserResponse;
import com.easleyjs.membership.dto.AppUserStatusResponse;
import com.easleyjs.membership.dto.AppUserUpdateRequest;
import com.easleyjs.membership.dto.UserMemberMatch;
import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.AppUserInvite;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.entity.UserStatus;
import com.easleyjs.membership.exception.AppUserInviteException;
import com.easleyjs.membership.exception.AppUserNotFoundException;
import com.easleyjs.membership.exception.MemberAlreadyHasUserException;
import com.easleyjs.membership.exception.MemberNotFoundException;
import com.easleyjs.membership.exception.PasswordChangeException;
import com.easleyjs.membership.repository.AppUserInviteRepository;
import com.easleyjs.membership.repository.AppUserRepository;
import com.easleyjs.membership.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AppUserService {
    // TODO: Restore to 24 hours before production and update the invite email text in MailService.
    private static final Duration INVITE_LIFETIME = Duration.ofHours(72);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AppUserRepository appUserRepo;
    private final MemberRepository memberRepo;
    private final AppUserInviteRepository invites;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final String publicBaseUrl;

    public AppUserService(
            AppUserRepository appUserRepo,
            MemberRepository memberRepo,
            AppUserInviteRepository invites,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher events,
            @Value("${app.public-base-url}") String publicBaseUrl
    ) {
        this.appUserRepo = appUserRepo;
        this.memberRepo = memberRepo;
        this.invites = invites;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.publicBaseUrl = publicBaseUrl;
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

        AppUser user = appUserRepo.save(new AppUser(member, request.role()));
        issueInvite(user, Instant.now());
        return AppUserResponse.from(user);
    }

    @Transactional
    public void resendInvite(UUID id) {
        AppUser user = appUserRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new AppUserNotFoundException("App user not found: " + id));
        if (user.getStatus() != UserStatus.PENDING_INVITE) {
            throw new AppUserInviteException(HttpStatus.CONFLICT, "Only pending accounts can be invited.");
        }
        Instant now = Instant.now();
        invites.markUnusedInvitesUsed(id, now);
        issueInvite(user, now);
    }

    @Transactional
    public void changePassword(UUID id, String currentPassword, String newPassword) {
        AppUser user = appUserRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new AppUserNotFoundException("App user not found: " + id));
        if (!user.isEnabled() || user.getStatus() != UserStatus.ACTIVE) {
            throw new PasswordChangeException("Your account is not active. Contact an admin.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new PasswordChangeException("Your current password is incorrect.");
        }
        if (newPassword.codePointCount(0, newPassword.length()) < 12
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new PasswordChangeException(
                    "Use at least 12 characters and at most 72 UTF-8 bytes for your password.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new PasswordChangeException("Choose a password different from your current password.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void acceptInvite(String token, String password) {
        if (token.length() != 64 || !token.matches("[0-9a-f]{64}")) {
            throw invalidInvite();
        }
        if (password.codePointCount(0, password.length()) < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AppUserInviteException(HttpStatus.BAD_REQUEST,
                    "Use at least 12 characters and at most 72 UTF-8 bytes for your password.");
        }
        String hash = hashToken(token);
        UUID userId = invites.findUnusedUserId(hash).orElseThrow(AppUserService::invalidInvite);
        // Resend and acceptance both lock the account before changing an invite.
        AppUser user = appUserRepo.findByIdForUpdate(userId).orElseThrow(AppUserService::invalidInvite);
        AppUserInvite invite = invites.findById(hash).orElseThrow(AppUserService::invalidInvite);
        Instant now = Instant.now();
        if (user.getStatus() != UserStatus.PENDING_INVITE || invite.getUsedAt() != null
                || !now.isBefore(invite.getExpiresAt()) || !invite.getEmail().equals(user.getMember().getEmail())) {
            throw invalidInvite();
        }
        user.acceptInvite(passwordEncoder.encode(password));
        invite.markUsed(now);
    }

    private void issueInvite(AppUser user, Instant now) {
        URI origin = URI.create(publicBaseUrl);
        boolean localHttp = "http".equals(origin.getScheme()) && "localhost".equals(origin.getHost());
        if ((!localHttp && !"https".equals(origin.getScheme())) || origin.getHost() == null
                || origin.getUserInfo() != null || origin.getQuery() != null || origin.getFragment() != null
                || (!origin.getPath().isEmpty() && !"/".equals(origin.getPath()))) {
            throw new IllegalStateException("Set APP_PUBLIC_BASE_URL to the portal's HTTPS origin.");
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        String link = origin.resolve("/set-password.html#" + token).toASCIIString();
        Member member = user.getMember();
        String email = member.getEmail();
        invites.save(new AppUserInvite(hashToken(token), user, email, now.plus(INVITE_LIFETIME)));
        events.publishEvent(new InviteIssued(user.getId(), member, email, link));
    }

    private static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }

    private static AppUserInviteException invalidInvite() {
        return new AppUserInviteException(HttpStatus.BAD_REQUEST,
                "This invite link is invalid or expired. Ask an admin to resend your invite.");
    }

    record InviteIssued(UUID userId, Member member, String email, String link) {}
}
