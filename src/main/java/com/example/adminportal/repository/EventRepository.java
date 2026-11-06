package com.example.adminportal.repository;

import com.example.adminportal.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event,Long> {
    boolean existsByName(String name);
}
