package com.example.adminportal.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    private boolean membershipRequired;
    private boolean coverRequired;

    @Enumerated(EnumType.STRING)
    private EventLocation location;
}
