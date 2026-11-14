package com.easleyjs.membership.repository;

import com.easleyjs.membership.entity.CheckIn;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {
    @EntityGraph(attributePaths = "checkedInBy.member")
    List<CheckIn> findByMember_Id(UUID memberId);

    @Override
    @EntityGraph(attributePaths = "checkedInBy.member")
    List<CheckIn> findAll();

    // TODO: Add "..event" later
    boolean existsByMember_IdAndCreatedAtGreaterThanAndCreatedAtLessThan(
            UUID memberId,
            Instant start,
            Instant end
    );
}
