package com.easleyjs.membership.repository;

import com.easleyjs.membership.entity.AppUser;
import com.easleyjs.membership.entity.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser,UUID> {
    boolean existsByMember_Id(UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AppUser> findByMember_MemberNumber(Long memberNumber);
    Optional<AppUser> findByMember_Email(String email);

    @Query("select u.role from AppUser u where u.id = :id")
    Optional<UserRole> findRoleById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.id = :id")
    Optional<AppUser> findByIdForUpdate(UUID id);
}
