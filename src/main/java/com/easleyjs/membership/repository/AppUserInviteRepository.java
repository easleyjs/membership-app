package com.easleyjs.membership.repository;

import com.easleyjs.membership.entity.AppUserInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AppUserInviteRepository extends JpaRepository<AppUserInvite, String> {
    @Query("select i.user.id from AppUserInvite i where i.tokenHash = :hash and i.usedAt is null")
    Optional<UUID> findUnusedUserId(String hash);

    @Modifying
    @Query("update AppUserInvite i set i.usedAt = :now where i.user.id = :userId and i.usedAt is null")
    void markUnusedInvitesUsed(UUID userId, Instant now);
}
