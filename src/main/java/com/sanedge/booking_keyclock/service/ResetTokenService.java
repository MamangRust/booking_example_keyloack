package com.sanedge.booking_keyclock.service;

import java.util.Optional;

import com.sanedge.booking_keyclock.models.ResetToken;
import com.sanedge.booking_keyclock.models.User;

public interface ResetTokenService {
    ResetToken createResetToken(User user);

    void deleteResetToken(String userId);

    Optional<ResetToken> findByToken(String token);

    void updateExpiryDate(ResetToken resetToken);
}
