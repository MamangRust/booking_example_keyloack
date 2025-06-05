package com.sanedge.booking_keyclock.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.http.client.HttpResponseException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanedge.booking_keyclock.config.KeycloakConfig;
import com.sanedge.booking_keyclock.domain.request.auth.ForgotRequest;
import com.sanedge.booking_keyclock.domain.request.auth.LoginRequest;
import com.sanedge.booking_keyclock.domain.request.auth.RegisterRequest;
import com.sanedge.booking_keyclock.domain.request.auth.ResetPasswordRequest;
import com.sanedge.booking_keyclock.domain.response.MessageResponse;
import com.sanedge.booking_keyclock.domain.response.auth.AuthResponse;
import com.sanedge.booking_keyclock.exception.ResourceNotFoundException;
import com.sanedge.booking_keyclock.models.ResetToken;
import com.sanedge.booking_keyclock.models.User;
import com.sanedge.booking_keyclock.service.AuthMailService;
import com.sanedge.booking_keyclock.service.AuthService;
import com.sanedge.booking_keyclock.service.ResetTokenService;
import com.sanedge.booking_keyclock.utils.RandomString;

import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthMailService authMailService;
    private final ResetTokenService resetTokenService;
    private final KeycloakConfig keycloakConfig;

    @Autowired
    public AuthServiceImpl(
            AuthMailService authMailService,
            ResetTokenService resetTokenService,
            KeycloakConfig keycloakConfig) {
        this.authMailService = authMailService;
        this.resetTokenService = resetTokenService;
        this.keycloakConfig = keycloakConfig;
    }

    @Override
    public MessageResponse login(LoginRequest request) throws HttpResponseException {
        log.info("Attempting login for username: {}", request.getUsername());
        log.info("Using Keycloak server URL: {}", keycloakConfig.getServerUrl());
        log.info("Using realm: {}", keycloakConfig.getRealm());
        log.info("Using client ID: {}", keycloakConfig.getClientId());

        try {
            Keycloak userKeycloakInstance = keycloakConfig.createUserKeycloakClient(
                    request.getUsername(),
                    request.getPassword());

            log.info("Keycloak client instance created successfully");

            AccessTokenResponse tokenResponse;
            try {
                tokenResponse = userKeycloakInstance.tokenManager().getAccessToken();
                log.info("Token obtained successfully from Keycloak");
            } catch (Exception tokenException) {
                log.error("Failed to obtain token from Keycloak: {}", tokenException.getMessage());

                if (tokenException.getMessage().contains("401") ||
                        tokenException.getMessage().contains("Unauthorized") ||
                        tokenException.getMessage().contains("invalid_grant")) {
                    throw new ResourceNotFoundException("Invalid username or password");
                } else {
                    throw new RuntimeException("Keycloak connection error: " + tokenException.getMessage());
                }
            }

            RealmResource realmResource = keycloakConfig.getRealmResource();
            UsersResource usersResource = realmResource.users();
            List<UserRepresentation> users;

            try {
                users = usersResource.searchByUsername(request.getUsername(), true);
                log.info("Found {} users with username: {}", users.size(), request.getUsername());
            } catch (Exception userSearchException) {
                log.error("Failed to search user in Keycloak admin: {}", userSearchException.getMessage());
                throw new RuntimeException("Failed to retrieve user information from Keycloak");
            }

            if (users.isEmpty()) {
                log.warn("User not found in Keycloak for username: {}", request.getUsername());
                throw new ResourceNotFoundException("User not found in Keycloak");
            }

            UserRepresentation userRepresentation = users.get(0);
            String username = userRepresentation.getUsername();

            log.info("User found: {}, Email verified: {}, Enabled: {}",
                    username, userRepresentation.isEmailVerified(), userRepresentation.isEnabled());

            if (!userRepresentation.isEnabled()) {
                throw new ResourceNotFoundException("User account is disabled");
            }

            AuthResponse authResponse = AuthResponse.builder()
                    .access_token(tokenResponse.getToken())
                    .refresh_token(tokenResponse.getRefreshToken())
                    .username(username)
                    .build();

            log.info("User successfully logged in: {}", username);

            return MessageResponse.builder()
                    .message("Berhasil login")
                    .data(authResponse)
                    .statusCode(200)
                    .build();

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for user ({}): {}",
                    request.getUsername(), e.getMessage(), e);
            log.error("Exception type: {}", e.getClass().getSimpleName());

            if (e.getMessage().contains("Connection refused") ||
                    e.getMessage().contains("UnknownHostException")) {
                throw new RuntimeException("Cannot connect to Keycloak server at: " + keycloakConfig.getServerUrl());
            }

            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    @Override
    public MessageResponse register(RegisterRequest registerRequest) {
        try {
            if (this.getUserByUsername(registerRequest.getUsername()).isPresent()) {
                throw new ResourceNotFoundException("Username is already taken");
            }

            if (this.getUserByEmail(registerRequest.getEmail()).isPresent()) {
                throw new ResourceNotFoundException("Email is already in use");
            }

            String token = RandomString.generateRandomString(50);

            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setEmailVerified(false);

            CredentialRepresentation credential = makePasswordCredentialRepresentation(registerRequest.getPassword());
            user.setCredentials(List.of(credential));

            Map<String, List<String>> attributes = makeUserAttributes(token);
            user.setAttributes(attributes);

            RealmResource realmResource = keycloakConfig.getRealmResource();
            UsersResource usersResource = realmResource.users();

            var response = usersResource.create(user);
            if (response.getStatus() != 201) {
                throw new ResourceNotFoundException("Failed to create user in Keycloak");
            }

            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

            Set<String> strRoles = registerRequest.getRole();
            if (strRoles == null || strRoles.isEmpty()) {
                strRoles = Set.of("user");
            }

            for (String role : strRoles) {
                try {
                    RoleRepresentation keycloakRole = realmResource.roles().get(role).toRepresentation();
                    log.info("Assigning role {} to user {}", role, registerRequest.getUsername());
                    usersResource.get(userId).roles().realmLevel().add(List.of(keycloakRole));
                } catch (Exception e) {
                    log.error("Failed to assign role {} to user {}: {}", role, registerRequest.getUsername(),
                            e.getMessage(), e);
                    throw new RuntimeException("Failed to assign role due to permission issue");
                }
            }

            authMailService.sendEmailVerify(registerRequest.getEmail(), token);

            log.info("User registered successfully: {}", registerRequest.getUsername());
            return MessageResponse.builder().message("Success create user").data(null).statusCode(200).build();

        } catch (ResourceNotFoundException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return MessageResponse.builder().message(e.getMessage()).data(null).statusCode(400).build();

        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage(), e);
            return MessageResponse.builder().message("Internal server error during registration").data(null)
                    .statusCode(500).build();
        }
    }

    @Override
    public MessageResponse forgotPassword(ForgotRequest request) {
        try {
            Optional<User> userOptional = this.getUserByEmail(request.getEmail());

            if (userOptional.isEmpty()) {
                throw new ResourceNotFoundException("User not found");
            }

            User user = userOptional.get();
            ResetToken resetToken = resetTokenService.createResetToken(user);

            String resetLink = "http://localhost:8080/api/auth/reset-password?token=" + resetToken.getToken();
            authMailService.sendResetPasswordEmail(user.getEmail(), resetLink);

            log.info("Forgot password request processed for user: {}", user.getEmail());

            return MessageResponse.builder()
                    .message("Success send email")
                    .statusCode(200)
                    .build();

        } catch (ResourceNotFoundException e) {
            log.warn("Forgot password failed: {}", e.getMessage());
            return MessageResponse.builder().message(e.getMessage()).statusCode(404).build();

        } catch (Exception e) {
            log.error("Unexpected error in forgotPassword: {}", e.getMessage(), e);
            return MessageResponse.builder().message("Internal server error").statusCode(500).build();
        }
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        try {
            ResetToken resetToken = resetTokenService.findByToken(request.getToken())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired token"));

            String userId = resetToken.getUserId();
            CredentialRepresentation credential = makePasswordCredentialRepresentation(request.getPassword());

            RealmResource realmResource = keycloakConfig.getRealmResource();
            UsersResource usersResource = realmResource.users();
            UserResource userResource = usersResource.get(userId);

            userResource.resetPassword(credential);

            UserRepresentation userRepresentation = userResource.toRepresentation();
            String username = userRepresentation.getUsername();

            resetTokenService.deleteResetToken(userId);

            log.info("Password reset successfully for user: {}", username);

            return MessageResponse.builder()
                    .message("Password reset successfully.")
                    .statusCode(200)
                    .build();

        } catch (ResourceNotFoundException e) {
            log.warn("Reset password failed: {}", e.getMessage());
            return MessageResponse.builder().message(e.getMessage()).statusCode(404).build();

        } catch (Exception e) {
            log.error("Unexpected error in resetPassword: {}", e.getMessage(), e);
            return MessageResponse.builder().message("Internal server error").statusCode(500).build();
        }
    }

    @Override
    public MessageResponse verifyEmail(String token) {
        try {
            RealmResource realmResource = keycloakConfig.getRealmResource();
            UsersResource usersResource = realmResource.users();
            List<UserRepresentation> userList = usersResource.search(null, null, null, null, 0, 100);

            for (UserRepresentation userRep : userList) {
                String code = userRep.firstAttribute("verificationCode");
                if (token.equals(code)) {
                    userRep.singleAttribute("verified", "true");
                    userRep.setEmailVerified(true);
                    usersResource.get(userRep.getId()).update(userRep);

                    log.info("Email verification successful for user: {}", userRep.getUsername());
                    return MessageResponse.builder().message("Success verify email").statusCode(200).build();
                }
            }

            log.info("Verification code not found: {}", token);
            return MessageResponse.builder().message("Verification code not found").statusCode(404).build();

        } catch (Exception e) {
            log.error("Unexpected error in verifyEmail: {}", e.getMessage(), e);
            return MessageResponse.builder().message("Internal server error").statusCode(500).build();
        }
    }

    public Optional<User> getUserByEmail(String email) {
        RealmResource realmResource = keycloakConfig.getRealmResource();
        UsersResource usersResource = realmResource.users();

        var usersByEmail = usersResource.searchByEmail(email, true);

        if (usersByEmail.isEmpty()) {
            return Optional.empty();
        }

        var userRepresentation = usersByEmail.get(0);
        return Optional.of(new User(userRepresentation));
    }

    public Optional<User> getUserByUsername(String username) {
        RealmResource realmResource = keycloakConfig.getRealmResource();
        UsersResource usersResource = realmResource.users();

        var usersByUsername = usersResource.searchByUsername(username, true);

        if (usersByUsername.isEmpty()) {
            return Optional.empty();
        }

        var userRepresentation = usersByUsername.get(0);
        return Optional.of(new User(userRepresentation));
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            log.error("No authenticated user found");
            throw new RuntimeException("No authenticated user found");
        }

        String userId = authentication.getName();
        log.info("Current user ID (from token): {}", userId);

        RealmResource realmResource = keycloakConfig.getRealmResource();
        UsersResource usersResource = realmResource.users();

        UserRepresentation userRepresentation;
        try {
            userRepresentation = usersResource.get(userId).toRepresentation();

            if (userRepresentation == null) {
                log.error("User not found in Keycloak with ID: {}", userId);
                throw new UsernameNotFoundException("User not found in Keycloak");
            }

            log.info("User found in Keycloak: {}", userRepresentation.getUsername());

        } catch (NotFoundException e) {
            log.error("User not found in Keycloak with ID: {}", userId);
            throw new UsernameNotFoundException("User not found in Keycloak", e);
        }

        log.info("User found in Keycloak: {}", userRepresentation.getUsername());

        return new User(userRepresentation);
    }

    private Map<String, List<String>> makeUserAttributes(String token) {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("verificationCode", List.of(token));
        return attributes;
    }

    private CredentialRepresentation makePasswordCredentialRepresentation(String password) {
        var passwordCredentialRepresentation = new CredentialRepresentation();
        passwordCredentialRepresentation.setTemporary(false);
        passwordCredentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        passwordCredentialRepresentation.setValue(password);
        return passwordCredentialRepresentation;
    }
}