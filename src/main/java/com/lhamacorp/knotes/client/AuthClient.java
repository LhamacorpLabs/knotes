package com.lhamacorp.knotes.client;

import com.lhamacorp.knotes.context.UserContext;
import com.lhamacorp.knotes.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Objects;

@Component
public class AuthClient {

    private final RestClient rest;
    private final String baseUrl;
    private final String key;
    private final String secret;

    public AuthClient(RestClient rest,
                      @Value("${clients.api.auth.url}") String baseUrl,
                      @Value("${clients.secrets.key}") String key,
                      @Value("${clients.secrets.secret}") String secret) {
        this.rest = rest;
        this.baseUrl = baseUrl;
        this.key = key;
        this.secret = secret;
    }

    public String authenticate() {
        try {
            return Objects.requireNonNull(rest.post()
                            .uri(baseUrl + "/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(new AuthRequest(key, secret))
                            .retrieve()
                            .body(AuthResponse.class))
                    .token();
        } catch (RestClientResponseException e) {
            throw new UnauthorizedException("Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new UnauthorizedException("An error occurred: " + e.getMessage());
        }
    }

    @Cacheable(value = "current", key = "#token")
    public UserContext current(String token) {
        try {
            return rest.get()
                    .uri(baseUrl + "/users/current")
                    .header("Authorization", token)
                    .retrieve()
                    .body(UserContext.class);
        } catch (RestClientResponseException e) {
            throw new UnauthorizedException("Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new UnauthorizedException("An error occurred: " + e.getMessage());
        }
    }

}
