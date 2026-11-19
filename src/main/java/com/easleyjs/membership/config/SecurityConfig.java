package com.easleyjs.membership.config;

import com.easleyjs.membership.dto.ApiErrorResponse;
import com.easleyjs.membership.repository.AppUserRepository;
import com.easleyjs.membership.security.SessionRoleFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
                                            AppUserRepository users) throws Exception {
        return http
                .addFilterBefore(new SessionRoleFilter(users), AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/login.html", "/set-password.html",
                                "/css/**", "/js/**", "/api/auth/csrf", "/images/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/accept-invite").permitAll()
                        // TODO: Remove the demo member routes after the demo period.
                        .requestMatchers("/dev/**", "/api/dev/**").hasRole("ADMIN")
                        .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/members/{memberNumber}/ban",
                                "/api/members/{memberNumber}/unban").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/members/{memberNumber}/notes/{noteId}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/checkin/comp").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/events/{id}/disable").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login.html?next=member-test"),
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/dev/member-test"))
                        .defaultAuthenticationEntryPointFor(
                                (request, response, failure) -> writeError(objectMapper, request, response,
                                        HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Sign in to continue."),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/**"))
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login.html?next=qr-preview"),
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/qr-preview.html"))
                        .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login.html"),
                                AnyRequestMatcher.INSTANCE)
                        .accessDeniedHandler((request, response, failure) -> {
                            if (failure instanceof CsrfException) {
                                writeError(objectMapper, request, response, HttpStatus.FORBIDDEN,
                                        "INVALID_CSRF_TOKEN", "Your security token expired. Try again.");
                            } else {
                                writeError(objectMapper, request, response, HttpStatus.FORBIDDEN,
                                        "ACCESS_DENIED", "Access denied.");
                            }
                        }))
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                        .failureHandler((request, response, failure) -> {
                            if (failure instanceof AuthenticationServiceException) {
                                writeError(objectMapper, request, response, HttpStatus.INTERNAL_SERVER_ERROR,
                                        "LOGIN_UNAVAILABLE", "Sign-in is temporarily unavailable.");
                            } else {
                                writeError(objectMapper, request, response, HttpStatus.UNAUTHORIZED,
                                        "INVALID_CREDENTIALS", "Incorrect email or password.");
                            }
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .build();
    }

    private static void writeError(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                                   HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(code, message, request.getRequestURI()));
    }
}
