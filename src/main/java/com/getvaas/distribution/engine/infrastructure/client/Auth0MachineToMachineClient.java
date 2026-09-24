package com.getvaas.distribution.engine.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

/**
 * Cliente Auth0 client-credentials (M2M) para llamar microservicios VAAS externos — mismo patrón
 * ya probado en {@code conciliation-engine-api} (VPR-9671), adaptado a este repo. El token se
 * cachea en memoria y se descarta cada 20hs; no hay invalidación por expiración real del token
 * (mismo límite conocido que la referencia).
 */
@Component
public class Auth0MachineToMachineClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String audience;

    private volatile String cachedToken;

    public Auth0MachineToMachineClient(
            @Value("${auth0.url}") String auth0Url,
            @Value("${auth0.client-id}") String clientId,
            @Value("${auth0.client-secret}") String clientSecret,
            RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(auth0Url).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.audience = auth0Url + "/api/v2/";
    }

    public String getAccessToken() {
        if (cachedToken == null) {
            refreshToken();
        }
        return cachedToken;
    }

    @Scheduled(fixedRate = 20, timeUnit = TimeUnit.HOURS)
    public void evictCache() {
        cachedToken = null;
    }

    private synchronized void refreshToken() {
        if (cachedToken != null) {
            return;
        }

        var request = new TokenRequest("client_credentials", clientId, clientSecret, audience);

        TokenResponse response = restClient.post()
                .uri("/oauth/token")
                .body(request)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Failed to obtain Auth0 M2M token");
        }

        cachedToken = response.accessToken();
    }

    private record TokenRequest(
            @JsonProperty("grant_type") String grantType,
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret,
            @JsonProperty("audience") String audience) {
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Long expiresIn) {
    }
}
