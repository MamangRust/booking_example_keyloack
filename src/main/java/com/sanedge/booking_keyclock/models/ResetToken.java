package com.sanedge.booking_keyclock.models;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "reset_tokens")
@Data
public class ResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId; 

    private String token;

    private Instant expiryDate;
}
