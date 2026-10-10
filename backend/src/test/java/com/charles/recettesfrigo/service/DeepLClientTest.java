package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DeepLClientTest {

    @Test
    void sansCleLeClientEstDesactive() {
        assertThat(new DeepLClient("", RestClient.builder()).isEnabled()).isFalse();
        assertThat(new DeepLClient(null, RestClient.builder()).isEnabled()).isFalse();
    }

    @Test
    void uneCleGratuitePasseParApiFreeEtTraduitEnUneRequete() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DeepLClient client = new DeepLClient("cle-test:fx", builder);
        server.expect(requestTo("https://api-free.deepl.com/v2/translate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "DeepL-Auth-Key cle-test:fx"))
                .andExpect(content()
                        .json("{\"text\":[\"Chicken\",\"Rice\"],\"source_lang\":\"EN\",\"target_lang\":\"FR\"}"))
                .andRespond(withSuccess(
                        "{\"translations\":[{\"detected_source_language\":\"EN\",\"text\":\"Poulet\"},"
                                + "{\"detected_source_language\":\"EN\",\"text\":\"Riz\"}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.isEnabled()).isTrue();
        assertThat(client.translateToFrench(List.of("Chicken", "Rice"))).containsExactly("Poulet", "Riz");
        server.verify();
    }

    @Test
    void uneReponseIncompleteEstRefusee() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DeepLClient client = new DeepLClient("cle-payante", builder);
        server.expect(requestTo("https://api.deepl.com/v2/translate"))
                .andRespond(withSuccess("{\"translations\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.translateToFrench(List.of("Chicken")))
                .isInstanceOf(IllegalStateException.class);
    }
}
