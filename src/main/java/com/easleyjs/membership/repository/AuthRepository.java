package com.easleyjs.membership.repository;

import com.easleyjs.membership.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuthRepository extends JpaRepository<AppUser, UUID> {}
