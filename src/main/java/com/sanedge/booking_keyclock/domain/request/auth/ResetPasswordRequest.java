package com.sanedge.booking_keyclock.domain.request.auth;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;
    private String password;
}
