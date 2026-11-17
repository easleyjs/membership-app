package com.easleyjs.membership.service;

import com.easleyjs.membership.dto.MemberCreateRequest;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.repository.MemberNoteRepository;
import com.easleyjs.membership.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MemberWelcomeEmailTests {
    private final MemberCreateRequest request = new MemberCreateRequest("Tess", "Example", "tess@example.com");
    private final UUID memberId = UUID.randomUUID();
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final Connection connection = mock(Connection.class);
    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    private MemberService memberService;

    @BeforeEach
    void setUp() throws Exception {
        var dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);

        var members = mock(MemberRepository.class);
        when(members.getNextMemberNumber()).thenReturn(123456L);
        when(members.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", memberId);
            return member;
        });

        var qrCodes = mock(QrCodeService.class);
        when(qrCodes.generateQr(memberId)).thenReturn(new byte[]{1});

        context.register(TransactionConfig.class);
        context.registerBean(PlatformTransactionManager.class, () -> new DataSourceTransactionManager(dataSource));
        context.registerBean(MemberService.class, () -> new MemberService(members, mock(MemberNoteRepository.class),
                mock(MembershipService.class), context));
        context.registerBean(MailService.class, () -> new MailService(mailSender, members, qrCodes, mock(ResendClient.class)));
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

        var order = inOrder(connection, mailSender);
        order.verify(connection).commit();
        order.verify(mailSender).send(any(MimeMessagePreparator.class));
        verifyNoMoreInteractions(mailSender);
    }

    @Test
    void doesNotSendWhenTheTransactionRollsBack() throws Exception {
        var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> {
            memberService.createMember(request);
            status.setRollbackOnly();
        });

        verify(connection).rollback();
        verifyNoInteractions(mailSender);
    }

    @Test
    void creatingSeedMembersDoesNotSendEmail() throws Exception {
        assertEquals(memberId, memberService.createSeedMember(request).id());

        verify(connection).commit();
        verifyNoInteractions(mailSender);
    }

    @Test
    void memberCreationStillSucceedsWhenMailFails() throws Exception {
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessagePreparator.class));

        assertEquals(memberId, memberService.createMember(request).id());

        verify(connection).commit();
        verify(mailSender).send(any(MimeMessagePreparator.class));
    }

    @EnableTransactionManagement
    static class TransactionConfig {}
}
