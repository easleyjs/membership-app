package com.example.adminportal.repository;

import com.example.adminportal.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {
}
