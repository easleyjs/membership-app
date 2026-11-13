package com.easleyjs.membership.config;

import com.easleyjs.membership.controller.AuthController;
import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.entity.UserRole;
import com.easleyjs.membership.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, UserDetailsConfig.class})
class SessionSecurityTests {
    private static final String EMAIL = "staff@example.com";
    private static final String PASSWORD = "test + password";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean AppUserRepository users;

    private final UUID userId = UUID.randomUUID();
    private AppUser account;

    @BeforeEach
    void setUp() {
        var member = new Member("Door", "Staff", EMAIL, 42L, "test-qr");
        account = new AppUser(member, passwordEncoder.encode(PASSWORD), UserRole.STAFF);
        ReflectionTestUtils.setField(account, "id", userId);
        when(users.findByMember_Email(EMAIL)).thenReturn(Optional.of(account));
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
