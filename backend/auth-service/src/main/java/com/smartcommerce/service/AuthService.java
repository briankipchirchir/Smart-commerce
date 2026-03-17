package com.smartcommerce.service;

import com.smartcommerce.dto.*;
import com.smartcommerce.exception.BadRequestException;
import com.smartcommerce.exception.DuplicateResourceException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.exception.UnauthorizedException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final Keycloak keycloak;
    private final RestTemplate restTemplate;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    // ── Register ──────────────────────────────────────────────
    public void register(RegisterRequest request) {
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

        List<UserRepresentation> existing = usersResource.searchByEmail(request.getEmail(), true);
        if (!existing.isEmpty()) {
            throw new DuplicateResourceException("Email is already registered");
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(true);

        if (request.getPhone() != null) {
            user.setAttributes(Map.of("phone", List.of(request.getPhone())));
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));

        Response response = usersResource.create(user);
        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatus());
        }

        String userId = response.getLocation().getPath().replaceAll(".*/", "");
        RoleRepresentation customerRole = realmResource.roles().get("CUSTOMER").toRepresentation();
        realmResource.users().get(userId).roles().realmLevel()
                .add(Collections.singletonList(customerRole));

        log.info("User registered successfully: {}", request.getEmail());
    }

    // ── Login ─────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public AuthResponse login(LoginRequest request) {
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", request.getEmail());
        body.add("password", request.getPassword());
        body.add("scope", "openid profile email");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tokenUrl, new HttpEntity<>(body, headers), Map.class);

            Map<String, Object> tokenData = response.getBody();
            List<UserRepresentation> users = keycloak.realm(realm).users()
                    .searchByEmail(request.getEmail(), true);

            UserRepresentation user = users.get(0);
            String role = getUserRole(user.getId());

            return AuthResponse.builder()
                    .accessToken((String) tokenData.get("access_token"))
                    .refreshToken((String) tokenData.get("refresh_token"))
                    .expiresIn(((Number) tokenData.get("expires_in")).longValue())
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(role)
                    .build();

        } catch (HttpClientErrorException e) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    // ── Refresh Token ─────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public AuthResponse refreshToken(String refreshToken) {
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tokenUrl, new HttpEntity<>(body, headers), Map.class);

            Map<String, Object> tokenData = response.getBody();
            return AuthResponse.builder()
                    .accessToken((String) tokenData.get("access_token"))
                    .refreshToken((String) tokenData.get("refresh_token"))
                    .expiresIn(((Number) tokenData.get("expires_in")).longValue())
                    .tokenType("Bearer")
                    .build();

        } catch (HttpClientErrorException e) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
    }

    // ── Logout ────────────────────────────────────────────────
    public void logout(String refreshToken) {
        String logoutUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        restTemplate.postForEntity(logoutUrl, new HttpEntity<>(body, headers), Void.class);
        log.info("User logged out successfully");
    }

    // ── Get Profile ───────────────────────────────────────────
    public UserProfileResponse getProfile(String userId) {
        UserRepresentation user = keycloak.realm(realm).users().get(userId)
                .toRepresentation();

        if (user == null) throw new ResourceNotFoundException("User not found");

        String phone = null;
        if (user.getAttributes() != null && user.getAttributes().containsKey("phone")) {
            phone = user.getAttributes().get("phone").get(0);
        }

        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(phone)
                .role(getUserRole(userId))
                .build();
    }

    // ── Update Profile ────────────────────────────────────────
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        UserRepresentation user = keycloak.realm(realm).users().get(userId)
                .toRepresentation();

        if (user == null) throw new ResourceNotFoundException("User not found");

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        if (request.getPhone() != null) {
            user.setAttributes(Map.of("phone", List.of(request.getPhone())));
        }

        keycloak.realm(realm).users().get(userId).update(user);
        log.info("Profile updated for user: {}", userId);

        return getProfile(userId);
    }

    // ── Update Password ───────────────────────────────────────
    public void updatePassword(String userId, UpdatePasswordRequest request) {
        // First verify current password by attempting login
        UserRepresentation user = keycloak.realm(realm).users().get(userId)
                .toRepresentation();

        if (user == null) throw new ResourceNotFoundException("User not found");

        // Verify current password via token endpoint
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", user.getEmail());
        body.add("password", request.getCurrentPassword());

        try {
            restTemplate.postForEntity(tokenUrl, new HttpEntity<>(body, headers), Map.class);
        } catch (HttpClientErrorException e) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Set new password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getNewPassword());
        credential.setTemporary(false);

        keycloak.realm(realm).users().get(userId).resetPassword(credential);
        log.info("Password updated for user: {}", userId);
    }

    // ── Get All Users ────────────────────────────────────
    public List<Map<String, Object>> getAllUsers() {
        return keycloak.realm(realm).users().list().stream()
                .map(user -> {
                    Map<String, Object> u = new java.util.HashMap<>();
                    u.put("userId", user.getId());
                    u.put("email", user.getEmail());
                    u.put("firstName", user.getFirstName());
                    u.put("lastName", user.getLastName());
                    u.put("enabled", user.isEnabled());
                    u.put("createdAt", user.getCreatedTimestamp());
                    u.put("role", getUserRole(user.getId()));
                    String phone = null;
                    if (user.getAttributes() != null && user.getAttributes().containsKey("phone")) {
                        phone = user.getAttributes().get("phone").get(0);
                    }
                    u.put("phone", phone);
                    return u;
                })
                .toList();
    }

    // ── Helper ────────────────────────────────────────────────
    private String getUserRole(String userId) {
        return keycloak.realm(realm).users().get(userId)
                .roles().realmLevel().listEffective()
                .stream()
                .map(RoleRepresentation::getName)
                .filter(r -> r.equals("ADMIN") || r.equals("CUSTOMER"))
                .findFirst()
                .orElse("CUSTOMER");
    }
}
