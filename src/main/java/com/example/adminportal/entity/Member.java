package com.example.adminportal.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false, unique = true, updatable = false)
    private Long memberNumber;

    @Column(nullable = false, unique = true)
    private String qrToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(nullable = false, unique = true)
    private String email;

    private String firstName;

    private String lastName;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // Remove after testing/deployment
    public Member(
            String firstName,
            String lastName,
            String email
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.memberNumber = (long) (Math.random() * 10);
        this.qrToken = UUID.randomUUID().toString();
    }

    public Member(
            String firstName,
            String lastName,
            String email,
            Long memberNumber,
            String qrToken
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.memberNumber = memberNumber;
        this.qrToken = qrToken;
    }
}