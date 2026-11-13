package com.easleyjs.membership.config;

import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.repository.AppUserRepository;
import com.easleyjs.membership.security.SessionUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UserDetailsConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(AppUserRepository appUserRepo) {
        return email -> {
            AppUser account = appUserRepo.findByMember_Email(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

            return new SessionUser(email, account);
        };
    }
}
