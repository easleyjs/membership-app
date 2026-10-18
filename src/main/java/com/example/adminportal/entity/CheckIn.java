package com.example.adminportal.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
public class CheckIn {

    //Check-In IDs will be generated in the client to facilitate offline access.
    @Id
    UUID id;

    /*
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Event event;
     */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AppUser checkedInBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckInMethod method;

}
