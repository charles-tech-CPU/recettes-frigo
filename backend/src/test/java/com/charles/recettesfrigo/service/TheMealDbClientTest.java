package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Les reponses simulees reprennent le format reel de TheMealDB (champs en trop ignores, "meals": null). */
class TheMealDbClientTest {

    private static final String BASE = "https://www.themealdb.com/api/json/v1/1";

    private MockRestServiceServer server;
    private TheMealDbClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TheMealDbClient(builder.build());
    }

    @Test
    void filtrerParIngredient() {
        server.expect(requestTo(BASE + "/filter.php?i=chicken"))
                .andRespond(withSuccess("""
                        {"meals":[{"strMeal":"Brown Stew Chicken","strMealThumb":"https://img/x.jpg",
                        "idMeal":"52940","strArea":"Jamaican"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.filterByIngredient("chicken"))
                .containsExactly(new TheMealDbClient.MealSummary("52940", "Brown Stew Chicken", "https://img/x.jpg"));
    }

    @Test
    void aucunResultatDonneUneListeVide() {
        server.expect(requestTo(BASE + "/filter.php?i=poulet"))
                .andRespond(withSuccess("{\"meals\":null}", MediaType.APPLICATION_JSON));

        assertThat(client.filterByIngredient("poulet")).isEmpty();
    }

    @Test
    void detailDUneRecette() {
        server.expect(requestTo(BASE + "/lookup.php?i=52772"))
                .andRespond(withSuccess(
                        "{\"meals\":[{\"idMeal\":\"52772\",\"strMeal\":\"Teriyaki Chicken Casserole\"}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.lookupById("52772")).containsEntry("strMeal", "Teriyaki Chicken Casserole");
    }

    @Test
    void recetteInconnueDonneNull() {
        server.expect(requestTo(BASE + "/lookup.php?i=1"))
                .andRespond(withSuccess("{\"meals\":null}", MediaType.APPLICATION_JSON));

        assertThat(client.lookupById("1")).isNull();
    }

    @Test
    void uneErreurServeurDevientTheMealDbUnavailable() {
        server.expect(requestTo(BASE + "/filter.php?i=rice")).andRespond(withServerError());
        server.expect(requestTo(BASE + "/lookup.php?i=2")).andRespond(withServerError());

        assertThatThrownBy(() -> client.filterByIngredient("rice"))
                .isInstanceOf(TheMealDbClient.TheMealDbUnavailableException.class);
        assertThatThrownBy(() -> client.lookupById("2"))
                .isInstanceOf(TheMealDbClient.TheMealDbUnavailableException.class);
    }
}
