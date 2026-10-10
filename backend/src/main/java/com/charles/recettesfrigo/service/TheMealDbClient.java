package com.charles.recettesfrigo.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client minimal pour l'API publique et gratuite TheMealDB
 * (https://www.themealdb.com/api.php). Deux endpoints utilises :
 * - filter.php?i=<ingredient> : liste resumee des recettes contenant cet
 *   ingredient (idMeal, strMeal, strMealThumb uniquement).
 * - lookup.php?i=<idMeal> : detail complet d'une recette, avec jusqu'a 20
 *   couples strIngredientN/strMeasureN (structure "a plat" propre a cette
 *   API - la plupart sont vides/null pour une recette donnee).
 * <p>
 * Important : je n'ai pas pu verifier ces deux formats de reponse par un
 * appel reseau reel depuis mon bac a sable (sortie HTTP restreinte). Le
 * parsing ci-dessous suit la documentation publique et stable de TheMealDB
 * (largement utilisee dans des tutoriels depuis des annees), mais un premier
 * test reel chez toi (ou avec Claude Code) reste la meilleure verification.
 */
@Component
public class TheMealDbClient {

    private final RestClient restClient;

    public TheMealDbClient(RestClient theMealDbRestClient) {
        this.restClient = theMealDbRestClient;
    }

    public record MealSummary(String idMeal, String strMeal, String strMealThumb) {}

    private record FilterResponse(List<MealSummary> meals) {}

    private record LookupResponse(List<Map<String, Object>> meals) {}

    public List<MealSummary> filterByIngredient(String englishIngredient) {
        try {
            FilterResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/filter.php")
                            .queryParam("i", englishIngredient)
                            .build())
                    .retrieve()
                    .body(FilterResponse.class);
            return (response != null && response.meals() != null) ? response.meals() : List.of();
        } catch (RestClientException ex) {
            throw new TheMealDbUnavailableException(
                    "Impossible d'interroger TheMealDB pour l'ingredient '" + englishIngredient + "'.", ex);
        }
    }

    public Map<String, Object> lookupById(String mealId) {
        try {
            LookupResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/lookup.php")
                            .queryParam("i", mealId)
                            .build())
                    .retrieve()
                    .body(LookupResponse.class);
            if (response == null || response.meals() == null || response.meals().isEmpty()) {
                return null;
            }
            return response.meals().get(0);
        } catch (RestClientException ex) {
            throw new TheMealDbUnavailableException(
                    "Impossible de recuperer le detail de la recette " + mealId + ".", ex);
        }
    }

    public static class TheMealDbUnavailableException extends RuntimeException {
        public TheMealDbUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
