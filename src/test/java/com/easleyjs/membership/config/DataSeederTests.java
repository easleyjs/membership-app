package com.easleyjs.membership.config;

import com.easleyjs.membership.dto.MemberCreateRequest;
import com.easleyjs.membership.dto.MemberResponse;
import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.entity.UserRole;
import com.easleyjs.membership.repository.AppUserRepository;
import com.easleyjs.membership.repository.MemberRepository;
import com.easleyjs.membership.service.MemberService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.mock.env.MockEnvironment;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSeederTests {
    @Test
    void createsDevAdminOnPopulatedDatabaseAndSkipsItOnRestart() throws Exception {
        var memberRepo = mock(MemberRepository.class);
        var memberService = mock(MemberService.class);
        var appUserRepo = mock(AppUserRepository.class);
        var passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String email = "josh@example.com";
        String password = "test-seed-password";
        UUID memberId = UUID.randomUUID();
        var member = new Member("Josh", "Test", email, 42L, "qr-token");
        var request = new MemberCreateRequest("Josh", "Test", email);
        when(memberRepo.count()).thenReturn(1L);
        when(memberRepo.findByEmail(email)).thenReturn(Optional.empty());
        when(memberService.createMember(request)).thenReturn(new MemberResponse(
                memberId, "Josh", "Test", email, 42L, member.getStatus(), false, 0));
        when(memberRepo.findById(memberId)).thenReturn(Optional.of(member));
        when(appUserRepo.findByMember_Email(email)).thenReturn(Optional.empty());

        var seeder = new DataSeeder();
        var environment = new MockEnvironment().withProperty("DEV_ADMIN_PASSWORD", password);
        seeder.seedMembers(memberRepo, memberService, appUserRepo, passwordEncoder, environment).run();

        var savedAccount = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepo).save(savedAccount.capture());
        AppUser admin = savedAccount.getValue();
        assertThat(admin.getMember()).isSameAs(member);
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(passwordEncoder.matches(password, admin.getPasswordHash())).isTrue();

        String originalHash = admin.getPasswordHash();
        admin.setEnabled(false);
        when(appUserRepo.findByMember_Email(email)).thenReturn(Optional.of(admin));
        seeder.seedMembers(memberRepo, memberService, appUserRepo, passwordEncoder, new MockEnvironment()).run();

        verify(memberService).createMember(request);
        verify(appUserRepo).save(admin);
        assertThat(admin.getPasswordHash()).isEqualTo(originalHash);
        assertThat(admin.isEnabled()).isFalse();
    }
}
