package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class MyMemoryClientTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    /** Parametre de la requete, decode (les textes sont encodes dans l'URL). */
    private static RequestMatcher param(String name, String expected) {
        return request -> {
            String raw = UriComponentsBuilder.fromUri(request.getURI())
                    .build()
                    .getQueryParams()
                    .getFirst(name);
            assertThat(raw == null ? null : URLDecoder.decode(raw, StandardCharsets.UTF_8))
                    .isEqualTo(expected);
        };
    }

    private static String ok(String translated) {
        return "{\"responseData\":{\"translatedText\":\"" + translated + "\",\"match\":0.85},\"responseStatus\":200}";
    }

    @Test
    void lesTextesCourtsSontRegroupesUnParLigneEnUneRequete() {
        server.expect(param("q", "Chicken & Rice\nSoy Sauce\n3/4 cup"))
                .andExpect(param("langpair", "en|fr"))
                .andExpect(param("de", null))
                .andRespond(withSuccess(ok("Poulet & riz\\nSauce soja\\n3/4 tasse"), MediaType.APPLICATION_JSON));

        List<String> result = new MyMemoryClient("", builder)
                .translateToFrench(Arrays.asList("Chicken & Rice", "", "Soy Sauce", null, "3/4 cup"));

        assertThat(result).containsExactly("Poulet & riz", "", "Sauce soja", null, "3/4 tasse");
        server.verify();
    }

    @Test
    void lEmailEstTransmisPourLeQuotaEtLesEntitesSontDecodees() {
        server.expect(param("de", "moi@exemple.fr"))
                .andRespond(withSuccess(ok("Gousse d&#39;ail"), MediaType.APPLICATION_JSON));

        assertThat(new MyMemoryClient("moi@exemple.fr", builder).translateToFrench(List.of("Garlic Clove")))
                .containsExactly("Gousse d'ail");
    }

    @Test
    void siLeNombreDeLignesChangeChaqueTexteEstTraduitSeul() {
        server.expect(param("q", "Salt\nPepper"))
                .andRespond(withSuccess(ok("Sel et poivre"), MediaType.APPLICATION_JSON));
        server.expect(param("q", "Salt")).andRespond(withSuccess(ok("Sel"), MediaType.APPLICATION_JSON));
        server.expect(param("q", "Pepper")).andRespond(withSuccess(ok("Poivre"), MediaType.APPLICATION_JSON));

        assertThat(new MyMemoryClient("", builder).translateToFrench(List.of("Salt", "Pepper")))
                .containsExactly("Sel", "Poivre");
    }

    @Test
    void desInstructionsSurPlusieursLignesSontTraduitesLigneParLigne() {
        server.expect(param("q", "Boil water."))
                .andRespond(withSuccess(ok("Faire bouillir l'eau."), MediaType.APPLICATION_JSON));
        server.expect(param("q", "Add rice."))
                .andRespond(withSuccess(ok("Ajouter le riz."), MediaType.APPLICATION_JSON));

        assertThat(new MyMemoryClient("", builder).translateToFrench(List.of("Boil water.\r\n\r\nAdd rice.")))
                .containsExactly("Faire bouillir l'eau.\n\nAjouter le riz.");
    }

    @Test
    void unQuotaEpuiseEstUneErreurEtPasUneTraduction() {
        server.expect(param("q", "Salt"))
                .andRespond(withSuccess(
                        "{\"responseData\":{\"translatedText\":\"MYMEMORY WARNING: YOU USED ALL AVAILABLE FREE TRANSLATIONS FOR TODAY\"},"
                                + "\"responseStatus\":\"429\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> new MyMemoryClient("", builder).translateToFrench(List.of("Salt")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void uneLigneTropLongueEstDecoupeeParPhrasesSousLaLimite() {
        String sentence = "Stir the sauce gently over a low heat for a few minutes. ";
        List<String> chunks = MyMemoryClient.chunks(sentence.repeat(20).trim());

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allMatch(c -> c.getBytes(StandardCharsets.UTF_8).length <= MyMemoryClient.MAX_BYTES);
        assertThat(String.join(" ", chunks)).isEqualTo(sentence.repeat(20).trim());
    }
}
