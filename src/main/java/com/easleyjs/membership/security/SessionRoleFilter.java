package com.easleyjs.membership.security;

import com.easleyjs.membership.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public final class SessionRoleFilter extends OncePerRequestFilter {
    private static final PathPatternRequestMatcher API = PathPatternRequestMatcher.withDefaults().matcher("/api/**");
    private final AppUserRepository users;

    public SessionRoleFilter(AppUserRepository users) {
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !API.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SessionUser sessionUser) {
            var role = users.findRoleById(sessionUser.getId()).orElse(null);
            if (role == null) {
                SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
            } else if (role != sessionUser.getRole()) {
                var user = sessionUser.withRole(role);
                var refreshed = UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
                // Use a request-local context so concurrent requests can keep using the shared session.
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(refreshed);
                SecurityContextHolder.setContext(context);
            }
        }
        chain.doFilter(request, response);
    }
}
