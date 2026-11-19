package com.easleyjs.membership.service;

import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.exception.EmailDeliveryException;
import com.easleyjs.membership.exception.MemberNotFoundException;
import com.easleyjs.membership.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final String WELCOME_SUBJECT = "WELCOME TO OUTLAND";
    private static final String STAFF_WELCOME_SUBJECT = "WELCOME TO OUTLAND STAFF";
    private static final String QR_CONTENT_ID = "member-qr";
    private static final String INVITE_PLACEHOLDER = "<!-- App user invitation -->";
    private static final ClassPathResource WELCOME_TEMPLATE =
            new ClassPathResource("templates/email/welcome-to-outland.html");

    private final EmailDelivery delivery;
    private final MemberRepository memberRepository;
    private final QrCodeService qrCodeService;
    private final ResendClient resendClient;

    public MailService(EmailDelivery delivery, MemberRepository memberRepository, QrCodeService qrCodeService,
                       ResendClient resendClient) {
        this.delivery = delivery;
        this.memberRepository = memberRepository;
        this.qrCodeService = qrCodeService;
        this.resendClient = resendClient;
    }

    public void sendMail(UUID memberId) {
        sendWelcomeEmail(findMember(memberId));
    }

    public String sendMailViaResend(UUID memberId) {
        var member = findMember(memberId);
        return sendWelcomeEmailViaResend(member, member.getEmail());
    }

    private String sendWelcomeEmailViaResend(Member member, String recipient) {
        return resendClient.sendEmail(recipient, WELCOME_SUBJECT, renderWelcomeEmail(member, WELCOME_SUBJECT, ""),
                QR_CONTENT_ID, qrCodeService.generateQr(member.getId()));
    }

    private Member findMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Member not found: " + memberId));
    }

    @TransactionalEventListener
    void sendWelcomeEmailAfterCommit(MemberService.MemberCreated event) {
        Member member = event.member();
        try {
            sendWelcomeEmail(member);
        } catch (RuntimeException failure) {
            log.warn("Could not send welcome email for member {}", member.getId(), failure);
        }
    }

    @TransactionalEventListener
    void sendDevWelcomeEmailAfterCommit(DevMemberService.DevMemberCreated event) {
        try {
            sendWelcomeEmail(event.member(), event.welcomeEmailTo());
        } catch (RuntimeException failure) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(failure);
            log.warn("Could not send demo welcome email for member {} ({}; cause: {})",
                    event.member().getId(), failure.getClass().getSimpleName(), cause.getClass().getSimpleName());
        }
    }

    @TransactionalEventListener
    void sendAppUserInviteAfterCommit(AppUserService.InviteIssued event) {
        String invite = """
                <tr>
                    <td align="center" style="padding:0 24px 28px; color:#ffffff;">
                        <p style="margin:0 0 12px;">You've been invited to the Outland admin portal.</p>
                        <a href="%s" style="color:#ff69b4; font-weight:bold;">Set your password</a>
                        <p style="margin:12px 0 0;">This link expires in 24 hours and can be used once.</p>
                    </td>
                </tr>
                """.formatted(HtmlUtils.htmlEscape(event.link(), UTF_8.name()));
        try {
            Member member = event.member();
            delivery.send(event.email(), STAFF_WELCOME_SUBJECT,
                    renderWelcomeEmail(member, STAFF_WELCOME_SUBJECT, invite),
                    QR_CONTENT_ID, qrCodeService.generateQr(member.getId()));
        } catch (RuntimeException failure) {
            // Mail exceptions can include message contents.
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(failure);
            log.warn("Could not send invitation for app user {} ({}; cause: {})",
                    event.userId(), failure.getClass().getSimpleName(), cause.getClass().getSimpleName());
        }
    }

    private void sendWelcomeEmail(Member member) {
        sendWelcomeEmail(member, member.getEmail());
    }

    private void sendWelcomeEmail(Member member, String recipient) {
        delivery.send(recipient, WELCOME_SUBJECT, renderWelcomeEmail(member, WELCOME_SUBJECT, ""),
                QR_CONTENT_ID, qrCodeService.generateQr(member.getId()));
    }

    private String renderWelcomeEmail(Member member, String subject, String invite) {
        String firstName = member.getFirstName();
        String greeting = StringUtils.hasText(firstName)
                ? "Welcome, " + HtmlUtils.htmlEscape(firstName, UTF_8.name()) + "."
                : "Welcome.";

        try {
            return WELCOME_TEMPLATE.getContentAsString(UTF_8)
                    .replace(WELCOME_SUBJECT, subject)
                    .replace("src=\"welcome-member-qr.png\"", "src=\"cid:" + QR_CONTENT_ID + "\"")
                    .replace("Member: 123456", "Member: " + member.getMemberNumber())
                    .replace(INVITE_PLACEHOLDER, invite)
                    .replace("Welcome, &lt;member name&gt;.", greeting);

        } catch (IOException failure) {
            throw new EmailDeliveryException("Could not read the welcome email template", failure);
        }
    }
}
