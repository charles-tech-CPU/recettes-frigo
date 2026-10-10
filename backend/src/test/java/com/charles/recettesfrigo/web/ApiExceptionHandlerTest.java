package com.charles.recettesfrigo.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.charles.recettesfrigo.service.RecipeImportService;
import com.charles.recettesfrigo.service.TheMealDbClient;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void uneRessourceIntrouvableRenvoie404AvecLeMessage() {
        var response = handler.handleNotFound(new EntityNotFoundException("Ingredient introuvable: 5"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Ingredient introuvable: 5");
    }

    @Test
    void uneSaisieInvalideRenvoie400() {
        var response = handler.handleBadRequest(new IllegalArgumentException("Nom vide"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Nom vide");
    }

    @Test
    void theMealDbInjoignablePourToutLeFrigoRenvoie503() {
        var response = handler.handleConflict(new IllegalStateException("TheMealDB est injoignable"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("error", "TheMealDB est injoignable");
    }

    @Test
    void unImportDeRecetteImpossibleRenvoie422() {
        var response = handler.handleImportFailed(new RecipeImportService.RecipeImportException("Aucune recette"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).containsEntry("error", "Aucune recette");
    }

    @Test
    void unEchecDAppelTheMealDbRenvoie502() {
        var response = handler.handleMealDbUnavailable(
                new TheMealDbClient.TheMealDbUnavailableException("Panne", new RuntimeException()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("error", "Panne");
    }
}
