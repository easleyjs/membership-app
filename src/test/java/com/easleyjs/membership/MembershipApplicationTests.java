package com.easleyjs.membership;

import com.easleyjs.membership.dto.AppUserCreateRequest;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.entity.UserRole;
import com.easleyjs.membership.entity.UserStatus;
import com.easleyjs.membership.exception.AppUserInviteException;
import com.easleyjs.membership.repository.AppUserRepository;
import com.easleyjs.membership.repository.MemberRepository;
import com.easleyjs.membership.service.AppUserService;
import com.easleyjs.membership.service.ResendClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = "app.public-base-url=https://portal.example.com")
class MembershipApplicationTests {
    @Autowired AppUserService users;
    @Autowired AppUserRepository accounts;
    @Autowired MemberRepository members;
    @Autowired UserDetailsService userDetails;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean ResendClient resendClient;

    @Test
    void appUserInviteExpiresCanBeResentAndActivatesExactlyOnce() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String password = "12 char pass";
        var tokens = new ArrayList<String>();
        doAnswer(invocation -> {
            String html = invocation.getArgument(2);
            assertThat(html).contains("<title>WELCOME TO OUTLAND STAFF</title>", ">WELCOME TO OUTLAND STAFF</h1>");
            var matcher = Pattern.compile("https://portal\\.example\\.com/set-password\\.html#([0-9a-f]{64})")
                    .matcher(html);
            assertThat(matcher.find()).isTrue();
            tokens.add(matcher.group(1));
            return "test-resend-id";
        }).when(resendClient).sendEmail(eq(email), eq("WELCOME TO OUTLAND STAFF"), anyString(),
                eq("member-qr"), any(byte[].class));

        Member member = members.save(new Member("Invite", "Recipient", email, members.getNextMemberNumber(),
                UUID.randomUUID().toString()));
        try {
            var account = users.createUser(new AppUserCreateRequest(member.getId(), UserRole.STAFF));
            assertThat(account.status()).isEqualTo(UserStatus.PENDING_INVITE);
            assertThatThrownBy(() -> userDetails.loadUserByUsername(email)).isInstanceOf(UsernameNotFoundException.class);
            assertThat(tokens).hasSize(1);

            String firstToken = tokens.get(0);
            String firstHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(firstToken.getBytes(US_ASCII)));
            assertThat(jdbc.update("UPDATE app_user_invites SET expires_at = CURRENT_TIMESTAMP WHERE token_hash = ?",
                    firstHash)).isEqualTo(1);
            assertThatThrownBy(() -> users.acceptInvite(firstToken, password)).isInstanceOf(AppUserInviteException.class);

            users.resendInvite(account.id());
            assertThat(tokens).hasSize(2).doesNotHaveDuplicates();
            assertThat(jdbc.queryForObject("SELECT used_at IS NOT NULL FROM app_user_invites WHERE token_hash = ?",
                    Boolean.class, firstHash)).isTrue();
            assertThatThrownBy(() -> users.acceptInvite(firstToken, password)).isInstanceOf(AppUserInviteException.class);

            String replacement = tokens.get(1);
            assertThatThrownBy(() -> users.acceptInvite(replacement, password.substring(1)))
                    .isInstanceOf(AppUserInviteException.class);
            users.acceptInvite(replacement, password);
            assertThat(users.findAppUserById(account.id()).status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(passwordEncoder.matches(password, userDetails.loadUserByUsername(email).getPassword())).isTrue();
            assertThatThrownBy(() -> users.acceptInvite(replacement, password)).isInstanceOf(AppUserInviteException.class);
            assertThatThrownBy(() -> users.resendInvite(account.id())).isInstanceOf(AppUserInviteException.class);
        } finally {
            accounts.findByMember_Email(email).ifPresent(accounts::delete);
            members.deleteById(member.getId());
        }
    }
}
