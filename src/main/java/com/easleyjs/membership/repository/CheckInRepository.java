package com.easleyjs.membership.repository;

import com.easleyjs.membership.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {
    List<CheckIn> findByMember_Id(UUID memberId);

    // TODO: Add "..event" later
    boolean existsByMember_IdAndCreatedAtGreaterThanAndCreatedAtLessThan(
            UUID memberId,
            Instant start,
            Instant end
    );
}
