package com.easleyjs.membership.controller;

import com.easleyjs.membership.config.SecurityConfig;
import com.easleyjs.membership.repository.AppUserRepository;
import com.easleyjs.membership.service.DevMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DevMemberController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("prod")
class DevMemberProfileTests {
    @Autowired MockMvc mvc;
    @MockitoBean AppUserRepository users;
    @MockitoBean DevMemberService members;

    @Test
    void pageAndApiAreAbsentWithoutDevEvenForAdmins() throws Exception {
        mvc.perform(get("/dev/member-test").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/dev/members").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/dev/member-test.html").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
        verifyNoInteractions(members);
    }
}
