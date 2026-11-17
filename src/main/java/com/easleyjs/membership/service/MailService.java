package com.easleyjs.membership.service;

import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.exception.MemberNotFoundException;
import com.easleyjs.membership.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final String QR_CONTENT_ID = "member-qr";
    private static final ClassPathResource WELCOME_TEMPLATE =
            new ClassPathResource("templates/email/welcome-to-outland.html");

    private final JavaMailSender mailSender;
    private final MemberRepository memberRepository;
    private final QrCodeService qrCodeService;

    public MailService(JavaMailSender mailSender, MemberRepository memberRepository, QrCodeService qrCodeService) {
        this.mailSender = mailSender;
        this.memberRepository = memberRepository;
        this.qrCodeService = qrCodeService;
    }

    public void sendMail(UUID memberId) {
        var member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Member not found: " + memberId));
        sendWelcomeEmail(member);
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
        String firstName = member.getFirstName();
        String greeting = StringUtils.hasText(firstName)
                ? "Welcome, " + HtmlUtils.htmlEscape(firstName, UTF_8.name()) + "."
                : "Welcome.";

        mailSender.send(mimeMessage -> {
            String html = WELCOME_TEMPLATE.getContentAsString(UTF_8)
                    .replace("src=\"welcome-member-qr.png\"", "src=\"cid:" + QR_CONTENT_ID + "\"")
                    .replace("Member: 123456", "Member: " + member.getMemberNumber())
                    .replace("Welcome, &lt;member name&gt;.", greeting);

            var message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_RELATED, UTF_8.name());
            message.setFrom("members@outland.test");
            message.setTo(member.getEmail());
            message.setSubject("WELCOME TO OUTLAND");
            message.setText(html, true);
            message.addInline(QR_CONTENT_ID, qrPng, "image/png");
        });
    }
}
