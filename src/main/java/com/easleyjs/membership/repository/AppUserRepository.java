package com.easleyjs.membership.repository;

import com.easleyjs.membership.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser,UUID> {
    boolean existsByMember_Id(UUID id);
    Optional<AppUser> findByMember_MemberNumber(Long memberNumber);
}