package com.sanedge.booking_keyclock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sanedge.booking_keyclock.models.ResetToken;

@Repository
public interface ResetTokenRepository extends JpaRepository<ResetToken, Long> {
    void deleteByUserId(String userId);

    Optional<ResetToken> findByToken(String token);
}
