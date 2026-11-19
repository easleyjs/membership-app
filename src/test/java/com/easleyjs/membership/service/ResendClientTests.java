package com.easleyjs.membership.service;

import com.easleyjs.membership.controller.MailController;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.exception.GlobalExceptionHandler;
import com.easleyjs.membership.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResendClientTests {
    private static final String EMAILS_URL = "https://api.resend.com/emails";
    private final UUID memberId = UUID.fromString("12345678-1234-4567-8abc-123456789012");
    private final String route = "/api/email/resend/" + memberId;
    private final MockEnvironment environment = new MockEnvironment();
    private MockRestServiceServer resendServer;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        resendServer = MockRestServiceServer.bindTo(builder).build();
        var members = mock(MemberRepository.class);
        var member = new Member("Tess & Co", "Example", "tess@example.test", 42L, "unused-token");
        ReflectionTestUtils.setField(member, "id", memberId);
        when(members.findById(memberId)).thenReturn(Optional.of(member));
        var resend = new ResendClient(environment, builder.build());
        var mail = new MailService(new ResendEmailDelivery(resend), members, new QrCodeService(), resend);
        mvc = MockMvcBuilders.standaloneSetup(new MailController(mail))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void sendsPersonalizedWelcomeEmailWithInlineQrThroughResend() throws Exception {
        environment.setProperty("RESEND_API_KEY", "test-key");
        environment.setProperty("RESEND_FROM", "Outland <members@example.test>");
        resendServer.expect(requestTo(EMAILS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.from").value("Outland <members@example.test>"))
                .andExpect(jsonPath("$.to[0]").value("tess@example.test"))
                .andExpect(jsonPath("$.subject").value("WELCOME TO OUTLAND"))
                .andExpect(jsonPath("$.html", containsString("Welcome, Tess &amp; Co.")))
                .andExpect(jsonPath("$.html", containsString("Member: 42")))
                .andExpect(jsonPath("$.html", containsString("src=\"cid:member-qr\"")))
                .andExpect(jsonPath("$.attachments[0].content_id").value("member-qr"))
                .andExpect(jsonPath("$.attachments[0].content", startsWith("iVBORw0KGgo")))
                .andRespond(withSuccess("{\"id\":\"test-delivery-id\"}", MediaType.APPLICATION_JSON));

        mvc.perform(post(route)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value("test-delivery-id"));

        resendServer.verify();
    }

    @Test
    void reportsResendRejectionWithoutReturningTheUpstreamBody() throws Exception {
        environment.setProperty("RESEND_API_KEY", "test-key");
        resendServer.expect(requestTo(EMAILS_URL))
                .andExpect(jsonPath("$.from").value("Outland <onboarding@resend.dev>"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("upstream-only details"));

        mvc.perform(post(route)).andExpect(status().isBadGateway())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("Resend rejected the email (HTTP 403)."));
        resendServer.verify();
    }

    @Test
    void explainsTheTestSenderRestrictionWithoutExposingTheResendAccountAddress() throws Exception {
        environment.setProperty("RESEND_API_KEY", "test-key");
        resendServer.expect(requestTo(EMAILS_URL))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body("""
                        {"statusCode":403,"name":"validation_error","message":"You can only send testing emails to your own email address (owner@example.test)."}
                        """));

        mvc.perform(post(route)).andExpect(status().isBadGateway())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value(
                        "Resend rejected the email (HTTP 403). Use a test member whose email matches your Resend account, or set RESEND_FROM to an address on a verified domain."));
        resendServer.verify();
    }
}
