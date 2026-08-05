package in.ac.iiitb.ca.identity;

import in.ac.iiitb.ca.common.error.ApiException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final RestClient restClient;
    private final String serverUrl;
    private final String realm;
    private final String adminUser;
    private final String adminPassword;
    private final boolean enabled;

    public KeycloakAdminClient(
            @Value("${app.keycloak.admin.server-url:http://localhost:8081}") String serverUrl,
            @Value("${app.keycloak.admin.realm:college-admin}") String realm,
            @Value("${app.keycloak.admin.username:admin}") String adminUser,
            @Value("${app.keycloak.admin.password:admin}") String adminPassword,
            @Value("${app.keycloak.admin.enabled:true}") boolean enabled) {
        this.serverUrl = serverUrl.replaceAll("/$", "");
        this.realm = realm;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
        this.enabled = enabled;
        this.restClient = RestClient.builder().baseUrl(this.serverUrl).build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String createUser(
            String username,
            String email,
            String displayName,
            String temporaryPassword,
            UUID tenantId,
            List<String> realmRoles) {
        if (!enabled) {
            throw ApiException.badRequest("Keycloak admin provisioning is disabled");
        }
        String token = adminToken();
        try {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("username", username);
            payload.put("email", email);
            payload.put("enabled", true);
            payload.put("emailVerified", true);
            payload.put("requiredActions", List.of());
            String firstName = displayName == null || displayName.isBlank() ? username : displayName.trim();
            String lastName = "User";
            int space = firstName.indexOf(' ');
            if (space > 0) {
                lastName = firstName.substring(space + 1).trim();
                firstName = firstName.substring(0, space).trim();
                if (lastName.isBlank()) {
                    lastName = "User";
                }
            }
            payload.put("firstName", firstName);
            payload.put("lastName", lastName);
            if (tenantId != null) {
                payload.put("attributes", Map.of("tenant_id", List.of(tenantId.toString())));
            }
            restClient
                    .post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw ApiException.conflict("Keycloak user already exists: " + username);
            }
            log.error("Keycloak create user failed: {}", ex.getResponseBodyAsString());
            throw ApiException.badRequest("Failed to create Keycloak user: " + ex.getStatusCode().value());
        }

        String userId = findUserIdByUsername(token, username);
        setPassword(token, userId, temporaryPassword, false);
        clearRequiredActions(token, userId);
        assignRealmRoles(token, userId, realmRoles);
        return userId;
    }

    private void clearRequiredActions(String token, String userId) {
        try {
            Map<?, ?> user = restClient
                    .get()
                    .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);
            if (user == null) {
                return;
            }
            java.util.HashMap<String, Object> payload = new java.util.HashMap<>();
            for (Map.Entry<?, ?> entry : user.entrySet()) {
                if (entry.getKey() != null) {
                    payload.put(entry.getKey().toString(), entry.getValue());
                }
            }
            payload.put("emailVerified", true);
            payload.put("enabled", true);
            payload.put("requiredActions", List.of());
            if (payload.get("lastName") == null || String.valueOf(payload.get("lastName")).isBlank()) {
                payload.put("lastName", "User");
            }
            if (payload.get("firstName") == null || String.valueOf(payload.get("firstName")).isBlank()) {
                payload.put("firstName", "User");
            }
            restClient
                    .put()
                    .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Failed to clear Keycloak required actions for {}: {}", userId, ex.getStatusCode().value());
        }
    }

    public void setUserEnabled(String keycloakSub, boolean enabledFlag) {
        if (!enabled || keycloakSub == null || keycloakSub.isBlank()) {
            return;
        }
        String token = adminToken();
        try {
            Map<?, ?> user = restClient
                    .get()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakSub)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);
            if (user == null) {
                return;
            }
            java.util.HashMap<String, Object> payload = new java.util.HashMap<>();
            for (Map.Entry<?, ?> entry : user.entrySet()) {
                if (entry.getKey() != null) {
                    payload.put(entry.getKey().toString(), entry.getValue());
                }
            }
            payload.put("enabled", enabledFlag);
            restClient
                    .put()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakSub)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Keycloak enable/disable failed for {}: {}", keycloakSub, ex.getStatusCode().value());
            throw ApiException.badRequest("Failed to sync user status to Keycloak");
        }
    }

    public void resetPassword(String keycloakSub, String newPassword, boolean temporary) {
        if (!enabled) {
            throw ApiException.badRequest("Keycloak admin provisioning is disabled");
        }
        if (keycloakSub == null || keycloakSub.isBlank()) {
            throw ApiException.badRequest("Missing Keycloak subject");
        }
        String token = adminToken();
        try {
            setPassword(token, keycloakSub, newPassword, temporary);
        } catch (RestClientResponseException ex) {
            log.warn("Keycloak password reset failed for {}: {}", keycloakSub, ex.getStatusCode().value());
            throw ApiException.badRequest("Failed to reset Keycloak password");
        }
    }

    public void syncRealmRoles(String keycloakSub, List<String> roleNames) {
        if (!enabled || keycloakSub == null || keycloakSub.isBlank()) {
            return;
        }
        String token = adminToken();
        try {
            // Replace realm role mappings with the requested set.
            List<?> current = restClient
                    .get()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, keycloakSub)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(List.class);
            if (current != null && !current.isEmpty()) {
                restClient
                        .method(org.springframework.http.HttpMethod.DELETE)
                        .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, keycloakSub)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(current)
                        .retrieve()
                        .toBodilessEntity();
            }
            assignRealmRoles(token, keycloakSub, roleNames);
        } catch (RestClientResponseException ex) {
            log.warn("Keycloak role sync failed for {}: {}", keycloakSub, ex.getStatusCode().value());
            throw ApiException.badRequest("Failed to sync roles to Keycloak");
        }
    }

    private String adminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUser);
        form.add("password", adminPassword);
        try {
            Map<?, ?> body = restClient
                    .post()
                    .uri("/realms/master/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            if (body == null || body.get("access_token") == null) {
                throw ApiException.badRequest("Unable to obtain Keycloak admin token");
            }
            return body.get("access_token").toString();
        } catch (RestClientResponseException ex) {
            throw ApiException.badRequest("Keycloak admin login failed");
        }
    }

    private String findUserIdByUsername(String token, String username) {
        List<?> users = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("username", username)
                        .queryParam("exact", true)
                        .build(realm))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(List.class);
        if (users == null || users.isEmpty()) {
            throw ApiException.notFound("Created Keycloak user not found");
        }
        Object first = users.get(0);
        if (first instanceof Map<?, ?> map && map.get("id") != null) {
            return map.get("id").toString();
        }
        throw ApiException.notFound("Created Keycloak user id missing");
    }

    private void setPassword(String token, String userId, String password) {
        setPassword(token, userId, password, false);
    }

    private void setPassword(String token, String userId, String password, boolean temporary) {
        restClient
                .put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", realm, userId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "password", "value", password, "temporary", temporary))
                .retrieve()
                .toBodilessEntity();
    }

    private void assignRealmRoles(String token, String userId, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }
        List<?> available = restClient
                .get()
                .uri("/admin/realms/{realm}/roles", realm)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(List.class);
        if (available == null) {
            return;
        }
        List<Map<String, Object>> toAssign = available.stream()
                .filter(Map.class::isInstance)
                .map(o -> (Map<String, Object>) o)
                .filter(m -> roleNames.contains(String.valueOf(m.get("name"))))
                .toList();
        if (toAssign.isEmpty()) {
            return;
        }
        restClient
                .post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(toAssign)
                .retrieve()
                .toBodilessEntity();
    }
}
