package com.easleyjs.membership.service;

import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.entity.UserRole;
import com.easleyjs.membership.exception.PasswordChangeException;
import com.easleyjs.membership.repository.AppUserInviteRepository;
import com.easleyjs.membership.repository.AppUserRepository;
import com.easleyjs.membership.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordChangeTests {
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final AppUserService service = new AppUserService(users, mock(MemberRepository.class),
            mock(AppUserInviteRepository.class), encoder, mock(ApplicationEventPublisher.class), "http://localhost:8080");
    private final UUID id = UUID.randomUUID();
    private final String oldPassword = "my old password";
    private AppUser account;
    private String originalHash;

    @BeforeEach
    void setUp() {
        originalHash = encoder.encode(oldPassword);
        account = new AppUser(new Member("Test", "Staff", "staff@example.com", 42L, "qr"),
                originalHash, UserRole.STAFF);
        when(users.findByIdForUpdate(id)).thenReturn(Optional.of(account));
    }

    @Test
    void changesHashAndOnlyNewPasswordMatches() {
        service.changePassword(id, oldPassword, "my new password");
        assertThat(encoder.matches("my new password", account.getPasswordHash())).isTrue();
        assertThat(encoder.matches(oldPassword, account.getPasswordHash())).isFalse();
    }

    @Test
    void rejectsIncorrectCurrentPasswordWithoutChangingHash() {
        assertThatThrownBy(() -> service.changePassword(id, "incorrect", "my new password"))
                .isInstanceOf(PasswordChangeException.class).hasMessageContaining("current password is incorrect");
        assertThat(account.getPasswordHash()).isEqualTo(originalHash);
    }

    @Test
    void rejectsShortOversizedAndUnchangedPasswords() {
        for (String password : new String[]{"short", "a".repeat(73), "é".repeat(37), "😀".repeat(11), oldPassword}) {
            assertThatThrownBy(() -> service.changePassword(id, oldPassword, password))
                    .isInstanceOf(PasswordChangeException.class);
            assertThat(account.getPasswordHash()).isEqualTo(originalHash);
        }
    }

    @Test
    void acceptsUnicodeWithinByteAndCharacterLimits() {
        String password = "😀".repeat(18);
        service.changePassword(id, oldPassword, password);
        assertThat(encoder.matches(password, account.getPasswordHash())).isTrue();
    }

    @Test
    void disabledAccountCannotChangePassword() {
        account.setEnabled(false);
        assertThatThrownBy(() -> service.changePassword(id, oldPassword, "my new password"))
                .isInstanceOf(PasswordChangeException.class);
        assertThat(account.getPasswordHash()).isEqualTo(originalHash);
    }
}
