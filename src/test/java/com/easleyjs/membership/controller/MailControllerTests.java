package com.easleyjs.membership.controller;

import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.exception.GlobalExceptionHandler;
import com.easleyjs.membership.repository.MemberRepository;
import com.easleyjs.membership.service.MailService;
import com.easleyjs.membership.service.ResendEmailDelivery;
import com.easleyjs.membership.service.QrCodeService;
import com.easleyjs.membership.service.ResendClient;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MailControllerTests {
    private final MemberRepository members = mock(MemberRepository.class);
    private final ResendClient resend = mock(ResendClient.class);
    private final MailService mailService = new MailService(new ResendEmailDelivery(resend), members, new QrCodeService(), resend);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new MailController(mailService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void sendsPersonalizedWelcomeEmailWithInlineMemberQr() throws Exception {
        UUID memberId = UUID.fromString("12345678-1234-4567-8abc-123456789012");
        var member = new Member("Zoë & <Test>", "Example", "member@example.com", 424242L, "unused-token");
        ReflectionTestUtils.setField(member, "id", memberId);
        when(members.findById(memberId)).thenReturn(Optional.of(member));

        mvc.perform(post("/api/email/{memberId}", memberId)).andExpect(status().isOk());

        var html = ArgumentCaptor.forClass(String.class);
        var qrPng = ArgumentCaptor.forClass(byte[].class);
        verify(resend).sendEmail(eq("member@example.com"), eq("WELCOME TO OUTLAND"),
                html.capture(), eq("member-qr"), qrPng.capture());
        verifyNoMoreInteractions(resend);
        assertThat(html.getValue()).contains("Welcome, Zoë &amp; &lt;Test&gt;.", "Member: 424242", "src=\"cid:member-qr\"")
                .doesNotContain("welcome-member-qr.png");

        try (var input = new ByteArrayInputStream(qrPng.getValue())) {
            var image = ImageIO.read(input);
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
            var bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels)));
            assertThat(new QRCodeReader().decode(bitmap).getText()).isEqualTo(memberId.toString());
        }
    }

    @Test
    void rejectsUnknownOrInvalidMemberIdsWithoutSendingEmail() throws Exception {
        mvc.perform(post("/api/email/{memberId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
        mvc.perform(post("/api/email/{memberId}", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEMBER_ID"));

        verifyNoInteractions(resend);
    }
}
