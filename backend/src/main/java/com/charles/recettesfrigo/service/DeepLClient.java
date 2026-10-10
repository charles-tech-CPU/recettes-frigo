package com.charles.recettesfrigo.service;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client minimal de l'API DeepL (https://developers.deepl.com) pour traduire les
 * recettes TheMealDB de l'anglais vers le francais. La cle vient de la variable
 * d'environnement DEEPL_API_KEY ; une cle gratuite (suffixe ":fx") passe par
 * api-free.deepl.com, une cle payante par api.deepl.com. Sans cle, isEnabled()
 * renvoie false et l'appli garde les recettes en anglais.
 */
@Component
public class DeepLClient {

    private final String apiKey;
    private final RestClient restClient;

    public DeepLClient(@Value("${app.deepl.api-key:}") String apiKey, RestClient.Builder builder) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        String baseUrl = this.apiKey.endsWith(":fx") ? "https://api-free.deepl.com/v2" : "https://api.deepl.com/v2";
        this.restClient = builder.baseUrl(baseUrl)
                .defaultHeader("Authorization", "DeepL-Auth-Key " + this.apiKey)
                .build();
    }

    public boolean isEnabled() {
        return !apiKey.isEmpty();
    }

    private record TranslateResponse(List<Translation> translations) {}

    private record Translation(String text) {}

    /** Traduit les textes de l'anglais vers le francais, en une seule requete, dans le meme ordre. */
    public List<String> translateToFrench(List<String> texts) {
        TranslateResponse response = restClient
                .post()
                .uri("/translate")
                .body(Map.of("text", texts, "source_lang", "EN", "target_lang", "FR"))
                .retrieve()
                .body(TranslateResponse.class);
        if (response == null
                || response.translations() == null
                || response.translations().size() != texts.size()) {
            throw new IllegalStateException("Reponse DeepL inattendue.");
        }
        return response.translations().stream().map(Translation::text).toList();
    }
}
