package com.getvaas.distribution.engine.infrastructure.client;

import com.getvaas.distribution.engine.domain.port.NotificationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class NotificationApiClient implements NotificationProvider {

    private final RestClient restClient;
    private final Auth0MachineToMachineClient auth0Client;

    public NotificationApiClient(@Value("${notifications-api.base-url}") String baseUrl,
                                  Auth0MachineToMachineClient auth0Client,
                                  RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.auth0Client = auth0Client;
    }

    @Override
    public void notify(String type, List<String> recipients, Map<String, String> context, List<String> attachmentsUrls) {
        restClient.post()
                .uri("/notifications")
                .header("Authorization", "Bearer " + auth0Client.getAccessToken())
                .body(new NotifyRequest(type, recipients, context, attachmentsUrls))
                .retrieve()
                .toBodilessEntity();
    }

    private record NotifyRequest(
            String type,
            List<String> recipients,
            Map<String, String> context,
            List<String> attachmentsUrls) {
    }
}
