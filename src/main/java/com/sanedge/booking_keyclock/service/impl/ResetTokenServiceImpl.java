package com.sanedge.booking_keyclock.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sanedge.booking_keyclock.models.ResetToken;
import com.sanedge.booking_keyclock.models.User;
import com.sanedge.booking_keyclock.repository.ResetTokenRepository;
import com.sanedge.booking_keyclock.service.ResetTokenService;

@Service
public class ResetTokenServiceImpl implements ResetTokenService {
    @Autowired
    private ResetTokenRepository resetTokenRepository;

    @Override
    public ResetToken createResetToken(User user) {
        ResetToken resetToken = new ResetToken();
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiryDate(Instant.now().plus(24, ChronoUnit.HOURS)); 
        return resetTokenRepository.save(resetToken);
    }

    @Override
    public void deleteResetToken(String userId) {
        resetTokenRepository.deleteByUserId(userId);
    }

    @Override
    public Optional<ResetToken> findByToken(String token) {
        return resetTokenRepository.findByToken(token);
    }

    @Override
    public void updateExpiryDate(ResetToken resetToken) {
        resetToken.setExpiryDate(Instant.now().plus(24, ChronoUnit.HOURS));
        resetTokenRepository.save(resetToken);
    }
}
