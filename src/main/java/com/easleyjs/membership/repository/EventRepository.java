package com.easleyjs.membership.repository;

import com.easleyjs.membership.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event,Long> {
    boolean existsByName(String name);
}
