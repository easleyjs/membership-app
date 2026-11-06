package com.example.adminportal.config;

import com.example.adminportal.dto.MemberCreateRequest;
import com.example.adminportal.entity.*;
import com.example.adminportal.repository.AppUserRepository;
import com.example.adminportal.repository.EventRepository;
import com.example.adminportal.repository.MemberRepository;
import com.example.adminportal.service.MemberService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.UUID;

@Configuration
@Profile("dev")
public class DataSeeder {
    @Bean
    CommandLineRunner seedMembers(
            MemberRepository memberRepo,
            MemberService memberService,
            AppUserRepository appUserRepo
    ) {
        return args -> {
            if (memberRepo.count() == 0) {
                List<MemberCreateRequest> members = List.of(
                        new MemberCreateRequest("Peter", "Murphy", "p@bauhaus.com"),
                        new MemberCreateRequest("Bill", "Leeb", "keys@fla.com"),
                        new MemberCreateRequest("Siouxsie", "Sioux", "siouxsie@banshees.com"),
                        new MemberCreateRequest("Andrew", "Eldritch", "sister@thesistersofmercy.com"),
                        new MemberCreateRequest("Robert", "Smith", "disintegration@thecure.com"),
                        new MemberCreateRequest("Nivek", "Ogre", "ogre@skinnypuppy.com"),
                        new MemberCreateRequest("Douglas", "McCarthy", "control@nitzer-ebb.com"),
                        new MemberCreateRequest("Ronny", "Moorings", "medusa@clanofxymox.com"),
                        new MemberCreateRequest("Sascha", "Konietzko", "ultraheavybeat@kmfdm.com"),
                        new MemberCreateRequest("Dave", "Gahan", "violator@depechemode.com"),
                        new MemberCreateRequest("Josh", "Test", "josh@example.com")
                );
                members.forEach(memberService::createMember);
            }

            Member testMember = memberRepo.findMemberByEmailContainsIgnoreCase("josh@example.com");

            AppUser user = new AppUser(
                    testMember,
                    "password",
                    UserRole.ADMIN
            );

            appUserRepo.save(user);

        };
    }
    @Bean
    CommandLineRunner seedFreeEvent(EventRepository eventRepo) {
        return args -> {
            String name = "OUTLAND -- NO COVER";

            if (eventRepo.existsByName(name)) {
                return;
            }

            Event event = new Event();
            event.setName(name);
            event.setMembershipRequired(true);
            event.setCoverRequired(false);
            event.setLocation(EventLocation.OUTLAND);
            event.setStatus(EventStatus.ACTIVE);

            // No start/end dates: this is an ongoing event.
            eventRepo.save(event);
        };
    }
}
