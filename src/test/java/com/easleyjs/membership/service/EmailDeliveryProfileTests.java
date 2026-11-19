package com.easleyjs.membership.service;

import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.repository.MemberRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmailDeliveryProfileTests {
    private final ResendClient resend = mock(ResendClient.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final QrCodeService qrCodes = mock(QrCodeService.class);
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(ResendEmailDelivery.class, MailService.class)
            .withBean(ResendClient.class, () -> resend)
            .withBean(MemberRepository.class, () -> members)
            .withBean(QrCodeService.class, () -> qrCodes);

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod", "default", "local", "dev,local"})
    void allProfilesSendThroughResend(String profile) {
        runner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(EmailDelivery.class);
            assertThat(context.getBean(EmailDelivery.class)).isInstanceOf(ResendEmailDelivery.class);
            assertThat(context.getEnvironment().getProperty("spring.mail.host")).isNull();
            exerciseMemberMail(context.getBean(MailService.class));
            verify(resend).sendEmail(eq("member@example.test"), eq("WELCOME TO OUTLAND"), anyString(), eq("member-qr"), any());
            verify(resend).sendEmail(eq("demo@example.test"), eq("WELCOME TO OUTLAND"), anyString(), eq("member-qr"), any());
            verify(resend).sendEmail(eq("staff@example.test"), eq("WELCOME TO OUTLAND STAFF"),
                    contains("https://example.test/set-password"), eq("member-qr"), any());
        });
    }

    private void exerciseMemberMail(MailService mail) {
        UUID id = UUID.randomUUID();
        var member = new Member("Test", "Member", "member@example.test", 42L, "test-qr");
        ReflectionTestUtils.setField(member, "id", id);
        when(members.findById(id)).thenReturn(Optional.of(member));
        when(qrCodes.generateQr(id)).thenReturn(new byte[]{1});
        mail.sendMail(id);
        mail.sendDevWelcomeEmailAfterCommit(new DevMemberService.DevMemberCreated(member, "demo@example.test"));
        mail.sendAppUserInviteAfterCommit(new AppUserService.InviteIssued(UUID.randomUUID(), member,
                "staff@example.test", "https://example.test/set-password"));
    }
}
