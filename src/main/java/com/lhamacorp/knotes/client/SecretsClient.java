package com.lhamacorp.knotes.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
public class SecretsClient {

    private final RestClient rest;
    private final AuthClient authClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String principalId;

    public SecretsClient(RestClient rest,
                          AuthClient authClient,
                          ObjectMapper objectMapper,
                          @Value("${clients.secrets.url}") String baseUrl,
                          @Value("${clients.secrets.key}") String principalId) {
        this.rest = rest;
        this.authClient = authClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.principalId = principalId;
    }

    public <T> T fetch(String name, Class<T> valueType) {
        SecretResponse secret = fetchSecret(name);
        try {
            return objectMapper.readValue(secret.secretString(), valueType);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse secret '" + name + "': " + e.getMessage(), e);
        }
    }

    private SecretResponse fetchSecret(String name) {
        try {
            return rest.get()
                .uri(baseUrl + "/api/secrets/" + name)
                .header("X-Principal-Id", principalId)
                .header(AUTHORIZATION, "Bearer " + authClient.authenticate())
                .retrieve()
                .body(SecretResponse.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Failed to fetch secret '" + name + "': " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }

}
