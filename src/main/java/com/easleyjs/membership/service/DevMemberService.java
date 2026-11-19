package com.easleyjs.membership.service;

import com.easleyjs.membership.dto.DevMemberCreateRequest;
import com.easleyjs.membership.dto.MemberCreateRequest;
import com.easleyjs.membership.dto.MemberResponse;
import com.easleyjs.membership.entity.Member;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TODO: Remove this service and its form/API after the demo period.
@Service
@Profile("dev")
public class DevMemberService {
    private final MemberService members;
    private final ApplicationEventPublisher events;

    public DevMemberService(MemberService members, ApplicationEventPublisher events) {
        this.members = members;
        this.events = events;
    }

    @Transactional
    public MemberResponse createMember(DevMemberCreateRequest request) {
        var member = members.saveMemberWithFreeMembership(new MemberCreateRequest(
                request.firstName(), request.lastName(), request.email()));
        // The delivery address is deliberately independent of the member's unique email.
        events.publishEvent(new DevMemberCreated(member, request.welcomeEmailTo()));
        return MemberResponse.from(member, 0);
    }

    record DevMemberCreated(Member member, String welcomeEmailTo) {}
}
