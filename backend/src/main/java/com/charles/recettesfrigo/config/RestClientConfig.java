package com.charles.recettesfrigo.config;

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
}
