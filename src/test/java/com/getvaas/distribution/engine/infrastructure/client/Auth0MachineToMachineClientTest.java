package com.getvaas.distribution.engine.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Auth0MachineToMachineClientTest {

    private static final String AUTH0_URL = "https://auth0.test";

    private record Fixture(Auth0MachineToMachineClient client, MockRestServiceServer server) {
    }

    private Fixture fixture() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new Auth0MachineToMachineClient(AUTH0_URL, "client-id", "client-secret", builder);
        return new Fixture(client, server);
    }

    @Test
    void getAccessToken_fetchesAndCachesToken() {
        var fixture = fixture();
        fixture.server().expect(requestTo(AUTH0_URL + "/oauth/token"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.grant_type").value("client_credentials"))
                .andExpect(jsonPath("$.client_id").value("client-id"))
                .andExpect(jsonPath("$.client_secret").value("client-secret"))
                .andExpect(jsonPath("$.audience").value(AUTH0_URL + "/api/v2/"))
                .andRespond(withSuccess("{\"access_token\":\"token-123\",\"token_type\":\"Bearer\",\"expires_in\":86400}",
                        MediaType.APPLICATION_JSON));

        var token = fixture.client().getAccessToken();
        assertThat(token).isEqualTo("token-123");

        var cachedToken = fixture.client().getAccessToken();
        assertThat(cachedToken).isEqualTo("token-123");

        fixture.server().verify();
    }

    @Test
    void getAccessToken_throwsWhenAuth0DoesNotReturnAccessToken() {
        var fixture = fixture();
        fixture.server().expect(requestTo(AUTH0_URL + "/oauth/token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.client().getAccessToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Auth0 M2M token");
    }

    @Test
    void evictCache_forcesNewTokenOnNextCall() {
        var fixture = fixture();
        fixture.server().expect(requestTo(AUTH0_URL + "/oauth/token"))
                .andRespond(withSuccess("{\"access_token\":\"first\"}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(AUTH0_URL + "/oauth/token"))
                .andRespond(withSuccess("{\"access_token\":\"second\"}", MediaType.APPLICATION_JSON));

        assertThat(fixture.client().getAccessToken()).isEqualTo("first");
        fixture.client().evictCache();
        assertThat(fixture.client().getAccessToken()).isEqualTo("second");

        fixture.server().verify();
    }
}
