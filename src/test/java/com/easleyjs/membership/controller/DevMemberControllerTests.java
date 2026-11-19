package com.easleyjs.membership.controller;

import com.easleyjs.membership.config.SecurityConfig;
import com.easleyjs.membership.dto.DevMemberCreateRequest;
import com.easleyjs.membership.dto.MemberResponse;
import com.easleyjs.membership.entity.MemberStatus;
import com.easleyjs.membership.repository.AppUserRepository;
import com.easleyjs.membership.service.DevMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DevMemberController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class DevMemberControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean AppUserRepository users;
    @MockitoBean DevMemberService members;

    private static final String BODY = """
            {"firstName":"Demo","lastName":"Member","email":"demo@example.test",
             "welcomeEmailTo":"existing@example.com"}
            """;

    @Test
    void pageAndApiRequireAdminLogin() throws Exception {
        mvc.perform(get("/dev/member-test"))
                .andExpect(redirectedUrl("/login.html?next=member-test"));
        mvc.perform(post("/api/dev/members").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/dev/member-test").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/dev/members").with(user("staff").roles("STAFF")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(members);
        mvc.perform(get("/dev/member-test").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Send welcome emails to:")))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void adminCreationRequiresCsrfAndValidMemberAndRecipientFields() throws Exception {
        mvc.perform(post("/api/dev/members").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        for (String body : new String[]{"{}", BODY.replace("existing@example.com", "invalid"),
                BODY.replace("demo@example.test", "invalid"), BODY.replace("Demo", " "),
                BODY.replace("Member", ""), BODY.replace("existing@example.com", "")}) {
            mvc.perform(post("/api/dev/members").with(user("admin").roles("ADMIN")).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(members);
        var request = new DevMemberCreateRequest("Demo", "Member", "demo@example.test", "existing@example.com");
        when(members.createMember(request)).thenReturn(new MemberResponse(UUID.randomUUID(), "Demo", "Member",
                "demo@example.test", 123L, MemberStatus.ACTIVE, false, 0));
        mvc.perform(post("/api/dev/members").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("demo@example.test"));
        verify(members).createMember(request);
    }
}
