package com.sanedge.booking_keyclock.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    private final AuthMailService authMailService;
    private final ResetTokenService resetTokenService;

    private Keycloak keycloakAdminInstance;

    @Value("${keycloak.admin-password}")
    private String keycloakAdminPassword;

    @Value("${keycloak.admin-username}")
    private String keycloakAdminUsername;

    @Value("${keycloak.client-id}")
    private String keycloakClientId;

    @Value("${keycloak.client-secret}")
    private String keycloakClientSecret;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;


    @Autowired
    public AuthServiceImpl(
            AuthMailService authMailService, ResetTokenService resetTokenService) {
        this.authMailService = authMailService;
        this.resetTokenService = resetTokenService;
    }

    @Override
    public MessageResponse login(LoginRequest request) {
        try {
            Keycloak userKeycloakInstance = KeycloakBuilder.builder()
                    .serverUrl(keycloakServerUrl)
                    .realm(keycloakRealm)
                    .clientId(keycloakClientId)
                    .clientSecret(keycloakClientSecret)
                    .grantType(OAuth2Constants.PASSWORD)
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .build();

            AccessTokenResponse tokenResponse = userKeycloakInstance.tokenManager().getAccessToken();

            UsersResource usersResource = keycloakAdminInstance.realm(keycloakRealm).users();
            List<UserRepresentation> users = usersResource.searchByUsername(request.getUsername(), true);

            if (users.isEmpty()) {
                throw new ResourceNotFoundException("User not found in Keycloak");
            }

            UserRepresentation userRepresentation = users.getFirst();
            String username = userRepresentation.getUsername();

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
        } catch (Exception e) {
            log.error("Login failed for user: {}", request.getUsername(), e);
            throw new ResourceNotFoundException("Invalid username or password");
        }
    }

    @Override
    public MessageResponse register(RegisterRequest registerRequest) {
        if (this.getUserByUsername(registerRequest.getUsername()).isPresent()) {
            throw new ResourceNotFoundException("Username is already taken");
        }

        if (this.getUserByEmail(registerRequest.getEmail()).isPresent()) {
            throw new ResourceNotFoundException("Email is already in use");
        }

        String token = RandomString.generateRandomString(50);

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());

        CredentialRepresentation credential = makePasswordCredentialRepresentation(registerRequest.getPassword());
        user.setCredentials(List.of(credential));

        Map<String, List<String>> attributes = makeUserAttributes(token);
        user.setAttributes(attributes);

        RealmResource realmResource = keycloakAdminInstance.realm(keycloakRealm);
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
            RoleRepresentation keycloakRole = realmResource.roles().get(role).toRepresentation();
            usersResource.get(userId).roles().realmLevel().add(List.of(keycloakRole));
        }

        authMailService.sendEmailVerify(registerRequest.getEmail(), token);

        log.info("User registered successfully: {}", registerRequest.getUsername());
        return MessageResponse.builder().message("Success create user").data(null).statusCode(200).build();
    }

    @Override
    public MessageResponse forgotPassword(ForgotRequest request) {
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
    }
    

    @Override
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        ResetToken resetToken = resetTokenService.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired token"));

        String userId = resetToken.getUserId();

        CredentialRepresentation credential = makePasswordCredentialRepresentation(request.getPassword());

        UsersResource usersResource = keycloakAdminInstance.realm(keycloakRealm).users();
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
    }

    @Override
    public MessageResponse verifyEmail(String token) {
        var usersResource = keycloakAdminInstance.realm(keycloakRealm).users();
        List<UserRepresentation> userList = usersResource.search(null, null, null, null, 0, 100);

        for (UserRepresentation userRep : userList) {
            String code = userRep.firstAttribute("verification_code");
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
    }

    public Optional<User> getUserByEmail(String email) {
        var usersResource = keycloakAdminInstance.realm(keycloakRealm).users();

        var usersByEmail = usersResource.searchByEmail(email, true);

        if (usersByEmail.isEmpty()) {
            return Optional.empty();
        }

        var userRepresentation = usersByEmail.getFirst();

        return Optional.of(new User(userRepresentation));
    }

    public Optional<User> getUserByUsername(String username) {
        var usersResource = keycloakAdminInstance.realm(keycloakRealm).users();

        var usersByEmail = usersResource.searchByUsername(username, true);

        if (usersByEmail.isEmpty()) {
            return Optional.empty();
        }

        var userRepresentation = usersByEmail.getFirst();

        return Optional.of(new User(userRepresentation));
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        var usersResource = keycloakAdminInstance.realm(keycloakRealm).users();

        var usersByUsername = usersResource.searchByUsername(authentication.getName(), true);

        var userRepresentation = usersByUsername.getFirst();

        return new User(userRepresentation);
    }

    private Map<String, List<String>> makeUserAttributes(String token) {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("verification_code", List.of(token));
        attributes.put("verified", List.of("false"));
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