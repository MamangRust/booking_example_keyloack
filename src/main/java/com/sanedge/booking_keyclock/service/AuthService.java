package com.sanedge.booking_keyclock.service;

import com.sanedge.booking_keyclock.domain.request.auth.ForgotRequest;
import com.sanedge.booking_keyclock.domain.request.auth.LoginRequest;
import com.sanedge.booking_keyclock.domain.request.auth.RegisterRequest;
import com.sanedge.booking_keyclock.domain.request.auth.ResetPasswordRequest;
import com.sanedge.booking_keyclock.domain.response.MessageResponse;
import com.sanedge.booking_keyclock.models.User;

public interface AuthService {
    public MessageResponse login(LoginRequest loginRequest);

    public MessageResponse register(RegisterRequest registerRequest);
    User getCurrentUser();

    public MessageResponse forgotPassword(ForgotRequest request);

    public MessageResponse resetPassword(ResetPasswordRequest request);

    public MessageResponse verifyEmail(String token);
}
