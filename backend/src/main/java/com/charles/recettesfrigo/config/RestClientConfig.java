package com.charles.recettesfrigo.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Client HTTP pour interroger l'API publique TheMealDB. Timeouts courts
     * pour ne jamais bloquer l'appli si TheMealDB est indisponible ou lent -
     * RecipeSuggestionService transforme un echec reseau en erreur claire
     * plutot que de laisser la requete pendre indefiniment.
     */
    @Bean
    public RestClient theMealDbRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);
        return RestClient.builder()
                .baseUrl("https://www.themealdb.com/api/json/v1/1")
                .requestFactory(factory)
                .build();
    }

    /**
     * Timeouts des autres clients HTTP (DeepL, import de recette par lien), construits
     * a partir du RestClient.Builder de Spring Boot : une page ou une API lente ne
     * doit jamais bloquer la requete indefiniment.
     */
    @Bean
    public RestClientCustomizer timeoutsCustomizer() {
        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(15000);
            builder.requestFactory(factory);
        };
    }
}
