package com.easleyjs.membership.config;

import com.easleyjs.membership.controller.AuthController;
import com.easleyjs.membership.controller.AppUserController;
import com.easleyjs.membership.controller.AppUserInviteController;
import com.easleyjs.membership.controller.CheckInController;
import com.easleyjs.membership.controller.EventController;
import com.easleyjs.membership.controller.MemberController;
import com.easleyjs.membership.controller.MemberNoteController;
import com.easleyjs.membership.dto.CheckInCreateRequest;
import com.easleyjs.membership.dto.CheckInResponse;
import com.easleyjs.membership.dto.MemberNoteCreateRequest;
import com.easleyjs.membership.dto.MemberNoteResponse;
import com.easleyjs.membership.dto.MemberStatusResponse;
import com.easleyjs.membership.entity.AdmissionType;
import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.CheckInMethod;
import com.easleyjs.membership.entity.Event;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.entity.MemberStatus;
import com.easleyjs.membership.entity.UserRole;
import com.easleyjs.membership.repository.AppUserRepository;
import com.easleyjs.membership.repository.EventRepository;
import com.easleyjs.membership.service.CheckInService;
import com.easleyjs.membership.service.AppUserService;
import com.easleyjs.membership.service.EventService;
import com.easleyjs.membership.service.MailService;
import com.easleyjs.membership.service.MemberNoteService;
import com.easleyjs.membership.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, AppUserController.class, AppUserInviteController.class,
        CheckInController.class, EventController.class,
        MemberNoteController.class, MemberController.class})
@Import({SecurityConfig.class, UserDetailsConfig.class, EventService.class})
class SessionSecurityTests {
    private static final String EMAIL = "staff@example.com";
    private static final String PASSWORD = "test + password";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean AppUserRepository users;
    @MockitoBean EventRepository events;
    @MockitoBean CheckInService checkIns;
    @MockitoBean MemberNoteService notes;
    @MockitoBean MemberService members;
    @MockitoBean MailService mailService;
    @MockitoBean AppUserService appUserService;

    private final UUID userId = UUID.randomUUID();
    private AppUser account;

    @BeforeEach
    void setUp() {
        var member = new Member("Door", "Staff", EMAIL, 42L, "test-qr");
        account = new AppUser(member, passwordEncoder.encode(PASSWORD), UserRole.STAFF);
        ReflectionTestUtils.setField(account, "id", userId);
        when(users.findByMember_Email(EMAIL)).thenReturn(Optional.of(account));
        when(users.findRoleById(userId)).thenAnswer(invocation -> Optional.of(account.getRole()));
    }

    @Test
    void pendingAccountCannotSignIn() throws Exception {
        when(users.findByMember_Email(EMAIL))
                .thenReturn(Optional.of(new AppUser(account.getMember(), UserRole.STAFF)));
        var session = new MockHttpSession();
        Csrf csrf = csrf(session);

        mvc.perform(login(session, PASSWORD).header(csrf.headerName(), csrf.token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousRecipientCanAcceptInviteWithCsrf() throws Exception {
        var session = new MockHttpSession();
        var body = mapper.writeValueAsString(new AppUserInviteController.AcceptInviteRequest(
                "a".repeat(64), "an example password"));
        String path = "/api/auth/accept-invite";
        mvc.perform(post(path).session(session).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        verifyNoInteractions(appUserService);

        Csrf csrf = csrf(session);
        mvc.perform(post(path).session(session).header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        verify(appUserService).acceptInvite("a".repeat(64), "an example password");
    }

    @Test
    void onlyAdminsCanResendInvites() throws Exception {
        var session = new MockHttpSession();
        Csrf loginCsrf = csrf(session);
        mvc.perform(login(session, PASSWORD).header(loginCsrf.headerName(), loginCsrf.token()))
                .andExpect(status().isNoContent());
        Csrf csrf = csrf(session);
        String path = "/api/users/" + userId + "/invite/resend";
        mvc.perform(post(path).session(session).header(csrf.headerName(), csrf.token()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(appUserService);

        account.setRole(UserRole.ADMIN);
        mvc.perform(post(path).session(session).header(csrf.headerName(), csrf.token()))
                .andExpect(status().isNoContent());
        verify(appUserService).resendInvite(userId);
    }

    @Test
    void formLoginRotatesSessionAndLogoutInvalidatesIt() throws Exception {
        var session = new MockHttpSession();
        String originalId = session.getId();
        Csrf csrf = csrf(session);

        mvc.perform(login(session, PASSWORD).header(csrf.headerName(), csrf.token()))
                .andExpect(status().isNoContent());
        assertThat(session.getId()).isNotEqualTo(originalId);

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.displayName").value("Door Staff"))
                .andExpect(jsonPath("$.role").value("STAFF"));
        mvc.perform(get("/api/users").session(session))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/auth/logout").session(session))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/logout").session(session).header(csrf.headerName(), csrf.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
        assertThat(session.isInvalid()).isFalse();

        Csrf fresh = csrf(session);
        mvc.perform(post("/api/auth/logout").session(session).header(fresh.headerName(), fresh.token()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("JSESSIONID", 0));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMissingCsrfBadCredentialsAndDisabledAccount() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(login(session, PASSWORD)).andExpect(status().isForbidden());

        Csrf csrf = csrf(session);
        mvc.perform(login(session, "incorrect").header(csrf.headerName(), csrf.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        account.setEnabled(false);
        mvc.perform(login(session, PASSWORD).header(csrf.headerName(), csrf.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mvc.perform(get("/members.html"))
                .andExpect(redirectedUrl("/login.html"));
    }

    @Test
    void qrPreviewPromptsForSignInAndLoadsAfterward() throws Exception {
        mvc.perform(get("/qr-preview.html"))
                .andExpect(redirectedUrl("/login.html?next=qr-preview"));
        mvc.perform(get("/api/dev/members/42/qr.png"))
                .andExpect(status().isUnauthorized());

        var session = new MockHttpSession();
        Csrf loginCsrf = csrf(session);
        mvc.perform(login(session, PASSWORD).header(loginCsrf.headerName(), loginCsrf.token()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/qr-preview.html").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void eventCreationUsesAuthenticatedAccountAsCreator() throws Exception {
        var session = new MockHttpSession();
        Csrf loginCsrf = csrf(session);
        mvc.perform(login(session, PASSWORD).header(loginCsrf.headerName(), loginCsrf.token()))
                .andExpect(status().isNoContent());

        when(users.findById(userId)).thenReturn(Optional.of(account));
        when(events.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Csrf eventCsrf = csrf(session);
        mvc.perform(post("/api/events").session(session)
                        .header(eventCsrf.headerName(), eventCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Door night",
                                  "location": "OUTLAND",
                                  "membershipRequired": true,
                                  "coverRequired": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value(userId.toString()));

        var savedEvent = ArgumentCaptor.forClass(Event.class);
        verify(events).save(savedEvent.capture());
        assertThat(savedEvent.getValue().getCreatedBy()).isSameAs(account);
    }

    @Test
    void membershipOverrideUsesSignedInAccountAndReturnsAdmissionType() throws Exception {
        var session = new MockHttpSession();
        Csrf loginCsrf = csrf(session);
        mvc.perform(login(session, PASSWORD).header(loginCsrf.headerName(), loginCsrf.token()))
                .andExpect(status().isNoContent());

        UUID checkInId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        var request = new CheckInCreateRequest(checkInId, memberId, null, CheckInMethod.QR_SCAN,
                createdAt, 42L, true);
        when(checkIns.createCheckIn(request, userId)).thenReturn(new CheckInResponse(checkInId,
                memberId, userId, createdAt, CheckInMethod.QR_SCAN, 42L, AdmissionType.MEMBERSHIP_OVERRIDE,
                "Door Staff"));

        Csrf checkInCsrf = csrf(session);
        mvc.perform(post("/api/checkin").session(session)
                        .header(checkInCsrf.headerName(), checkInCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "%s",
                                  "memberId": "%s",
                                  "createdAt": "%s",
                                  "eventId": 42,
                                  "method": "QR_SCAN",
                                  "overrideMembership": true
                                }
                                """.formatted(checkInId, memberId, createdAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkedInById").value(userId.toString()))
                .andExpect(jsonPath("$.checkedInByName").value("Door Staff"))
                .andExpect(jsonPath("$.admissionType").value("MEMBERSHIP_OVERRIDE"));

        when(checkIns.createCheckIn(request, userId)).thenThrow(new AccessDeniedException("Access denied."));
        mvc.perform(post("/api/checkin").session(session)
                        .header(checkInCsrf.headerName(), checkInCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void staffCanAddNotesUnderTheirOwnAccount() throws Exception {
        var session = new MockHttpSession();
        Csrf loginCsrf = csrf(session);
        mvc.perform(login(session, PASSWORD).header(loginCsrf.headerName(), loginCsrf.token()))
                .andExpect(status().isNoContent());

        var note = new MemberNoteCreateRequest(userId, "Member requested a follow-up.");
        when(notes.createMemberNote(42L, note)).thenReturn(
                new MemberNoteResponse(1L, "Door", note.content(), Instant.now(), "Door Staff"));
        Csrf noteCsrf = csrf(session);

        mvc.perform(post("/api/members/42/notes").session(session)
                        .header(noteCsrf.headerName(), noteCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(note)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdByName").value("Door Staff"));

        mvc.perform(post("/api/members/42/notes").session(session)
                        .header(noteCsrf.headerName(), noteCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new MemberNoteCreateRequest(UUID.randomUUID(), note.content()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void noteDeletionFollowsRoleChangesWithoutEndingTheSession() throws Exception {
        var session = new MockHttpSession();
        Csrf loginCsrf = csrf(session);
        mvc.perform(login(session, PASSWORD).header(loginCsrf.headerName(), loginCsrf.token()))
                .andExpect(status().isNoContent());
        String sessionId = session.getId();
        Csrf noteCsrf = csrf(session);
        String path = "/api/members/42/notes/1";

        mvc.perform(delete(path).session(session).header(noteCsrf.headerName(), noteCsrf.token()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notes);

        account.setRole(UserRole.ADMIN);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(delete(path).session(session))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
        mvc.perform(delete(path).session(session).header(noteCsrf.headerName(), noteCsrf.token()))
                .andExpect(status().isNoContent());
        verify(notes).deleteMemberNote(42L, 1L);

        account.setRole(UserRole.STAFF);
        account.setEnabled(false);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STAFF"));
        mvc.perform(delete(path).session(session).header(noteCsrf.headerName(), noteCsrf.token()))
                .andExpect(status().isForbidden());
        assertThat(session.isInvalid()).isFalse();
        assertThat(session.getId()).isEqualTo(sessionId);
    }

    @Test
    void onlyAdminsCanBanOrUnbanMembers() throws Exception {
        var staffSession = new MockHttpSession();
        Csrf loginCsrf = csrf(staffSession);
        mvc.perform(login(staffSession, PASSWORD).header(loginCsrf.headerName(), loginCsrf.token()))
                .andExpect(status().isNoContent());
        Csrf staffCsrf = csrf(staffSession);
        for (String action : new String[]{"ban", "unban"}) {
            mvc.perform(put("/api/members/42/" + action).session(staffSession)
                            .header(staffCsrf.headerName(), staffCsrf.token()))
                    .andExpect(status().isForbidden());
        }

        account.setRole(UserRole.ADMIN);
        var adminSession = new MockHttpSession();
        Csrf adminLoginCsrf = csrf(adminSession);
        mvc.perform(login(adminSession, PASSWORD).header(adminLoginCsrf.headerName(), adminLoginCsrf.token()))
                .andExpect(status().isNoContent());
        Csrf adminCsrf = csrf(adminSession);
        when(members.banMember(42L, null)).thenReturn(new MemberStatusResponse(MemberStatus.BANNED));
        when(members.unbanMember(42L)).thenReturn(new MemberStatusResponse(MemberStatus.ACTIVE));
        mvc.perform(put("/api/members/42/ban").session(adminSession)
                        .header(adminCsrf.headerName(), adminCsrf.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BANNED"));
        mvc.perform(put("/api/members/42/unban").session(adminSession)
                        .header(adminCsrf.headerName(), adminCsrf.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void staffCanResendMemberQrWithCsrf() throws Exception {
        var session = new MockHttpSession();
        UUID memberId = UUID.randomUUID();
        String route = "/api/members/" + memberId + "/qr/resend";
        Csrf loginCsrf = csrf(session);
        mvc.perform(login(session, PASSWORD).header(loginCsrf.headerName(), loginCsrf.token()))
                .andExpect(status().isNoContent());

        mvc.perform(post(route).session(session)).andExpect(status().isForbidden());
        verifyNoInteractions(mailService);

        Csrf resendCsrf = csrf(session);
        mvc.perform(post(route).session(session).header(resendCsrf.headerName(), resendCsrf.token()))
                .andExpect(status().isNoContent());
        verify(mailService).sendMail(memberId);
    }

    private MockHttpServletRequestBuilder login(MockHttpSession session, String password) {
        return post("/api/auth/login").session(session)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formField("username", EMAIL)
                .formField("password", password);
    }

    private Csrf csrf(MockHttpSession session) throws Exception {
        var response = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        return mapper.readValue(response.getContentAsString(), Csrf.class);
    }

    private record Csrf(String headerName, String parameterName, String token) {}
}
