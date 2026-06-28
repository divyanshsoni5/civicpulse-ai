package com.civicpulse.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String locality;

    @Enumerated(EnumType.STRING)
    private Role role = Role.CITIZEN;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (role == null) {
            role = Role.CITIZEN;
        }
        createdAt = LocalDateTime.now();
    }

    public enum Role {
        CITIZEN, ADMIN
    }
}
