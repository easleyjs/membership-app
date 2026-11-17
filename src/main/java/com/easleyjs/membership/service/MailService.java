package com.easleyjs.membership.service;

import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.exception.MemberNotFoundException;
import com.easleyjs.membership.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    private static final String QR_CONTENT_ID = "member-qr";
    private static final ClassPathResource WELCOME_TEMPLATE =
            new ClassPathResource("templates/email/welcome-to-outland.html");

    private final JavaMailSender mailSender;
    private final MemberRepository memberRepository;
    private final QrCodeService qrCodeService;
    private final ResendClient resendClient;

    public MailService(JavaMailSender mailSender, MemberRepository memberRepository, QrCodeService qrCodeService,
                       ResendClient resendClient) {
        this.mailSender = mailSender;
        this.memberRepository = memberRepository;
        this.qrCodeService = qrCodeService;
        this.resendClient = resendClient;
    }

    public void sendMail(UUID memberId) {
        sendWelcomeEmail(findMember(memberId));
    }

    public String sendMailViaResend(UUID memberId) {
        var member = findMember(memberId);
        return resendClient.sendEmail(member.getEmail(), WELCOME_SUBJECT, renderWelcomeEmail(member),
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

    private void sendWelcomeEmail(Member member) {
        var qrPng = new ByteArrayResource(qrCodeService.generateQr(member.getId()));
        mailSender.send(mimeMessage -> {
            var message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_RELATED, UTF_8.name());
            message.setFrom("members@outland.test");
            message.setTo(member.getEmail());
            message.setSubject(WELCOME_SUBJECT);
            message.setText(renderWelcomeEmail(member), true);
            message.addInline(QR_CONTENT_ID, qrPng, "image/png");
        });
    }

    private String renderWelcomeEmail(Member member) {
        String firstName = member.getFirstName();
        String greeting = StringUtils.hasText(firstName)
                ? "Welcome, " + HtmlUtils.htmlEscape(firstName, UTF_8.name()) + "."
                : "Welcome.";

        try {
            return WELCOME_TEMPLATE.getContentAsString(UTF_8)
                    .replace("src=\"welcome-member-qr.png\"", "src=\"cid:" + QR_CONTENT_ID + "\"")
                    .replace("Member: 123456", "Member: " + member.getMemberNumber())
                    .replace("Welcome, &lt;member name&gt;.", greeting);

        } catch (IOException failure) {
            throw new MailPreparationException("Could not read the welcome email template", failure);
        }
    }
}
