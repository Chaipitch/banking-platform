package com.pm.accountservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "customers")
public class Customer {
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String nationalId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
