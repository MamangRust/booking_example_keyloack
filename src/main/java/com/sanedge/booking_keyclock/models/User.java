package com.sanedge.booking_keyclock.models;

import java.util.Optional;
import org.keycloak.representations.idm.UserRepresentation;

public class User {
    private final String id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String username;
    private final Optional<String> verificationCode;

    public User(
            String id,
            String firstName,
            String lastName,
            String email,
            String username,
            Optional<String> verificationCode) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.username = username;
        this.verificationCode = verificationCode;
    }

    public User(UserRepresentation userRepresentation) {
        this(
                userRepresentation.getId(),
                userRepresentation.getFirstName(),
                userRepresentation.getLastName(),
                userRepresentation.getEmail(),
                userRepresentation.getUsername(),
                Optional.ofNullable(userRepresentation.firstAttribute("verification_code")));
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
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
}
