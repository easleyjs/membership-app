package com.easleyjs.membership.security;

import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.Member;
import com.easleyjs.membership.entity.UserRole;
import lombok.Getter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

import java.util.Objects;
import java.util.UUID;

@Getter
public final class SessionUser extends User {
    private final UUID id;
    private final String displayName;
    private final UserRole role;

    public SessionUser(String email, AppUser account) {
        super(email, account.getPasswordHash(), account.isEnabled(), true, true, true,
                AuthorityUtils.createAuthorityList("ROLE_" + account.getRole().name()));
        id = account.getId();
        Member member = account.getMember();
        String name = (Objects.toString(member.getFirstName(), "") + " "
                + Objects.toString(member.getLastName(), "")).strip();
        displayName = name.isEmpty() ? email : name;
        role = account.getRole();
    }
}
