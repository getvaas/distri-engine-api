package com.getvaas.distribution.engine.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.getvaas.distribution.engine.domain.model.NotificationTemplate;
import com.getvaas.distribution.engine.domain.port.NotificationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    // Verificado contra NotificationHttpProvider.kt (sistema real): la query solo manda
    // type+page+limit (sin channel_id), la respuesta viene envuelta en {"data": [...]}, y el
    // filtro por channelId se hace acá, del lado del cliente — no del lado del servidor.
    @Override
    public List<NotificationTemplate> getTemplates(String type, Integer channelId) {
        var response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/templates")
                        .queryParam("type", type)
                        .queryParam("page", 0)
                        .queryParam("limit", 1)
                        .build())
                .header("Authorization", "Bearer " + auth0Client.getAccessToken())
                .retrieve()
                .body(TemplatesPageResponse.class);

        if (response == null || response.data() == null) {
            return List.of();
        }
        return response.data().stream()
                .filter(template -> Objects.equals(template.channelId(), channelId))
                .map(template -> new NotificationTemplate(template.id(), template.type(), template.channelId()))
                .toList();
    }

    @Override
    public Long createTemplate(String text, String type, Integer channelId) {
        var response = restClient.post()
                .uri("/templates")
                .header("Authorization", "Bearer " + auth0Client.getAccessToken())
                .body(new CreateTemplateRequest(text, type, channelId))
                .retrieve()
                .body(NotificationTemplateDto.class);

        if (response == null || response.id() == null) {
            throw new IllegalStateException("notifications-api no devolvió un id de template [type=" + type + "]");
        }
        return response.id();
    }

    private record NotifyRequest(
            String type,
            List<String> recipients,
            Map<String, String> context,
            List<String> attachmentsUrls) {
    }

    private record TemplatesPageResponse(List<NotificationTemplateDto> data) {
    }

    private record NotificationTemplateDto(
            Long id,
            String type,
            @JsonProperty("channel_id") Integer channelId) {
    }

    private record CreateTemplateRequest(
            String text,
            String type,
            @JsonProperty("channel_id") Integer channelId) {
    }
}
