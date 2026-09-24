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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void getTemplates_wrapsResultInDataEnvelope_andFiltersByChannelIdClientSide() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new NotificationApiClient(BASE_URL, auth0Client, builder);

        when(auth0Client.getAccessToken()).thenReturn("token-abc");

        server.expect(requestTo(BASE_URL + "/templates?type=999601_DISTRIBUTION_SUCCEEDED&page=0&limit=1"))
                .andExpect(header("Authorization", "Bearer token-abc"))
                .andRespond(withSuccess(
                        "{\"data\":[{\"id\":42,\"type\":\"999601_DISTRIBUTION_SUCCEEDED\",\"channel_id\":3},"
                                + "{\"id\":43,\"type\":\"999601_DISTRIBUTION_SUCCEEDED\",\"channel_id\":1}]}",
                        MediaType.APPLICATION_JSON));

        var result = client.getTemplates("999601_DISTRIBUTION_SUCCEEDED", 3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(42L);
        assertThat(result.get(0).channelId()).isEqualTo(3);
        server.verify();
    }

    @Test
    void getTemplates_emptyData_returnsEmptyList() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new NotificationApiClient(BASE_URL, auth0Client, builder);

        when(auth0Client.getAccessToken()).thenReturn("token-abc");

        server.expect(requestTo(BASE_URL + "/templates?type=unknown&page=0&limit=1"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        var result = client.getTemplates("unknown", 3);

        assertThat(result).isEmpty();
    }

    @Test
    void createTemplate_postsRequestAndReturnsId() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new NotificationApiClient(BASE_URL, auth0Client, builder);

        when(auth0Client.getAccessToken()).thenReturn("token-abc");

        server.expect(requestTo(BASE_URL + "/templates"))
                .andExpect(header("Authorization", "Bearer token-abc"))
                .andExpect(jsonPath("$.text").value("<html></html>"))
                .andExpect(jsonPath("$.type").value("999601_DISTRIBUTION_SUCCEEDED"))
                .andExpect(jsonPath("$.channel_id").value(3))
                .andRespond(withSuccess("{\"id\":99,\"type\":\"999601_DISTRIBUTION_SUCCEEDED\",\"channel_id\":3}",
                        MediaType.APPLICATION_JSON));

        var id = client.createTemplate("<html></html>", "999601_DISTRIBUTION_SUCCEEDED", 3);

        assertThat(id).isEqualTo(99L);
        server.verify();
    }

    @Test
    void createTemplate_noIdInResponse_throwsIllegalState() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new NotificationApiClient(BASE_URL, auth0Client, builder);

        when(auth0Client.getAccessToken()).thenReturn("token-abc");

        server.expect(requestTo(BASE_URL + "/templates"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createTemplate("<html></html>", "999601_DISTRIBUTION_SUCCEEDED", 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("999601_DISTRIBUTION_SUCCEEDED");
    }
}
