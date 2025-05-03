package com.sanedge.booking_keyclock.models;

import java.util.Optional;
import org.keycloak.representations.idm.UserRepresentation;

public class User {
    private final String id;
    private final String email;
    private final String username;
    private final Optional<String> verificationCode;
    private final boolean verified;

    public User(
            String id,
            String email,
            String username,
            Optional<String> verificationCode,
            boolean verified) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.verificationCode = verificationCode;
        this.verified = verified;
    }

    public User(UserRepresentation userRepresentation) {
        this(
                userRepresentation.getId(),
                userRepresentation.getEmail(),
                userRepresentation.getUsername(),
                Optional.ofNullable(userRepresentation.firstAttribute("verification_code")),
                Boolean.parseBoolean(userRepresentation.firstAttribute("verified")));
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public Optional<String> getVerificationCode() {
        return verificationCode;
    }

    public boolean isVerified() {
        return verified;
    }
}
