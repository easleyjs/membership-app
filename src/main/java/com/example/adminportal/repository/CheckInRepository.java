package com.example.adminportal.repository;

import com.example.adminportal.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {
    List<CheckIn> findByMember_Id(UUID memberId);

    // TODO: Add "..event" later
    boolean existsByMember_IdAndCreatedAtAfter(
            UUID memberId,
            Instant cutoff
    );
}
