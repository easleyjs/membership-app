package com.easleyjs.membership.service;

import com.easleyjs.membership.dto.MemberCreateRequest;
import com.easleyjs.membership.dto.DevMemberCreateRequest;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.exception.EmailDeliveryException;
import com.easleyjs.membership.repository.MemberNoteRepository;
import com.easleyjs.membership.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;

class MemberWelcomeEmailTests {
    private final MemberCreateRequest request = new MemberCreateRequest("Tess", "Example", "tess@example.com");
    private final UUID memberId = UUID.randomUUID();
    private final ResendClient resend = mock(ResendClient.class);
    private final Connection connection = mock(Connection.class);
    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    private MemberService memberService;
    private MemberRepository members;
    private MembershipService memberships;

    @BeforeEach
    void setUp() throws Exception {
        var dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);

        members = mock(MemberRepository.class);
        memberships = mock(MembershipService.class);
        when(members.getNextMemberNumber()).thenReturn(123456L);
        when(members.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", memberId);
            return member;
        });

        var qrCodes = mock(QrCodeService.class);
        when(qrCodes.generateQr(memberId)).thenReturn(new byte[]{1});

        context.getEnvironment().setActiveProfiles("dev");
        context.register(TransactionConfig.class);
        context.registerBean(PlatformTransactionManager.class, () -> new DataSourceTransactionManager(dataSource));
        context.registerBean(MemberService.class, () -> new MemberService(members, mock(MemberNoteRepository.class),
                memberships, context));
        context.registerBean(DevMemberService.class, () -> new DevMemberService(
                context.getBean(MemberService.class), context));
        context.registerBean(MailService.class, () -> new MailService(new ResendEmailDelivery(resend), members, qrCodes, resend));
        context.refresh();
        memberService = context.getBean(MemberService.class);
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void sendsWelcomeEmailAfterMemberCreationCommits() throws Exception {
        assertEquals(memberId, memberService.createMember(request).id());

        var order = inOrder(connection, resend);
        order.verify(connection).commit();
        order.verify(resend).sendEmail(eq(request.email()), eq("WELCOME TO OUTLAND"),
                contains("Member: 123456"), eq("member-qr"), aryEq(new byte[]{1}));
        verifyNoMoreInteractions(resend);
    }

    @Test
    void doesNotSendWhenTheTransactionRollsBack() throws Exception {
        var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> {
            memberService.createMember(request);
            status.setRollbackOnly();
        });

        verify(connection).rollback();
        verifyNoInteractions(resend);
    }

    @Test
    void creatingSeedMembersDoesNotSendEmail() throws Exception {
        assertEquals(memberId, memberService.createSeedMember(request).id());

        verify(connection).commit();
        verifyNoInteractions(resend);
    }

    @Test
    void memberCreationStillSucceedsWhenMailFails() throws Exception {
        doThrow(new EmailDeliveryException("Resend unavailable")).when(resend)
                .sendEmail(anyString(), anyString(), anyString(), anyString(), any(byte[].class));

        assertEquals(memberId, memberService.createMember(request).id());

        verify(connection).commit();
        verify(resend).sendEmail(eq(request.email()), anyString(), anyString(), anyString(), any(byte[].class));
    }

    @Test
    void demoWelcomeUsesExistingRecipientWithoutChangingSavedEmail() throws Exception {
        String recipient = "existing@example.com";
        when(members.findByEmail(recipient)).thenReturn(Optional.of(
                new Member("Existing", "Member", recipient, 42L, "existing-qr")));
        var demo = context.getBean(DevMemberService.class);
        demo.createMember(new DevMemberCreateRequest(request.firstName(), request.lastName(), request.email(), recipient));

        var order = inOrder(connection, resend);
        order.verify(connection).commit();
        order.verify(resend).sendEmail(eq(recipient), eq("WELCOME TO OUTLAND"),
                contains("Member: 123456"), eq("member-qr"), aryEq(new byte[]{1}));
        var saved = ArgumentCaptor.forClass(Member.class);
        verify(members).save(saved.capture());
        assertEquals(request.email(), saved.getValue().getEmail());
        verify(memberships).ensureFreeMembership(saved.getValue());
        verify(members, never()).findByEmail(recipient);
        verifyNoMoreInteractions(resend);
    }

    @Test
    void demoRollbackDoesNotSendWelcome() throws Exception {
        var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> {
            context.getBean(DevMemberService.class).createMember(new DevMemberCreateRequest(
                    request.firstName(), request.lastName(), request.email(), "existing@example.com"));
            status.setRollbackOnly();
        });
        verify(connection).rollback();
        verifyNoInteractions(resend);
    }

    @Test
    void demoCreationStillSucceedsWhenResendFails() throws Exception {
        doThrow(new EmailDeliveryException("Resend unavailable")).when(resend)
                .sendEmail(anyString(), anyString(), anyString(), anyString(), any(byte[].class));
        var created = context.getBean(DevMemberService.class).createMember(new DevMemberCreateRequest(
                request.firstName(), request.lastName(), request.email(), "existing@example.com"));
        assertEquals(memberId, created.id());
        verify(connection).commit();
        verify(resend).sendEmail(eq("existing@example.com"), eq("WELCOME TO OUTLAND"),
                anyString(), eq("member-qr"), any(byte[].class));
    }

    @EnableTransactionManagement
    static class TransactionConfig {}
}
