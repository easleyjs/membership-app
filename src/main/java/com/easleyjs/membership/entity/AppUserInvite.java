package com.easleyjs.membership.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "app_user_invites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUserInvite {
    @Id
    @Column(length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    public AppUserInvite(String tokenHash, AppUser user, String email, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.email = email;
        this.expiresAt = expiresAt;
    }

    public void markUsed(Instant when) {
        usedAt = when;
    }
}
