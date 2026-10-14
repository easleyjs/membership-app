package com.example.adminportal.repository;

import com.example.adminportal.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser,UUID> {
    boolean existsByMember_Id(UUID id);
}
