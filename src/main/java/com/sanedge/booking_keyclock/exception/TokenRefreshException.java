package com.sanedge.booking_keyclock.exception;

public class TokenRefreshException extends RuntimeException {

    private final String token;

    public TokenRefreshException(String token, String message) {
        super(message);
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
