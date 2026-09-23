package com.getvaas.distribution.engine.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class NotificationApiClientTest {

    private static final String BASE_URL = "https://notifications-api.test";

    @Mock
    private Auth0MachineToMachineClient auth0Client;

    @Test
    void notify_postsRequestWithBearerTokenAndBody() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new NotificationApiClient(BASE_URL, auth0Client, builder);

        when(auth0Client.getAccessToken()).thenReturn("token-abc");

        server.expect(requestTo(BASE_URL + "/notifications"))
                .andExpect(header("Authorization", "Bearer token-abc"))
                .andExpect(jsonPath("$.type").value("DISTRIBUTION_SUCCEEDED"))
                .andExpect(jsonPath("$.recipients[0]").value("owner@example.com"))
                .andExpect(jsonPath("$.context.distributionId").value("427"))
                .andExpect(jsonPath("$.attachmentsUrls").isEmpty())
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.notify("DISTRIBUTION_SUCCEEDED", List.of("owner@example.com"),
                Map.of("distributionId", "427"), List.of());

        server.verify();
    }
}
