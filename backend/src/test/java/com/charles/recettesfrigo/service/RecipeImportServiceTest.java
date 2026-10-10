package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.charles.recettesfrigo.dto.CustomRecipeDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RecipeImportServiceTest {

    private static final String URL = "https://blog-thermomix.example/soupe";

    /** Page type blog Thermomix : recette dans un "@graph", etapes groupees en sections. */
    private static final String THERMOMIX_PAGE = """
            <html><head>
            <script type="application/ld+json">{ pas du json }</script>
            <script type="application/ld+json">
            {"@context":"https://schema.org","@graph":[
              {"@type":"WebPage","name":"Blog"},
              {"@type":["Recipe"],"name":"Soupe de carottes &amp; cumin",
               "image":[{"@type":"ImageObject","url":"https://img.example/soupe.jpg"}],
               "recipeCategory":["Soupe","Entrée"],"recipeYield":"4 personnes",
               "prepTime":"PT10M","cookTime":"PT25M",
               "recipeIngredient":["500 g de carottes","1 oignon","2 c. à soupe d'huile d'olive","Sel"],
               "recipeInstructions":[{"@type":"HowToSection","name":"Cuisson","itemListElement":[
                 {"@type":"HowToStep","text":"Mettre l'oignon et mixer 5 sec/vitesse 5."},
                 {"@type":"HowToStep","text":"Ajouter les carottes, cuire 20 min/100°C/sens inverse/vitesse 1."},
                 {"@type":"HowToStep","text":"Servir chaud."}]}]}
            ]}
            </script></head><body></body></html>
            """;

    private MockRestServiceServer server;
    private RecipeImportService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new RecipeImportService(builder, new ObjectMapper());
    }

    @Test
    void uneRecetteThermomixEstLueAvecSesReglages() {
        server.expect(requestTo(URL)).andRespond(withSuccess(THERMOMIX_PAGE, MediaType.TEXT_HTML));

        CustomRecipeDto recipe = service.importFromUrl(URL);

        assertThat(recipe.id()).isNull();
        assertThat(recipe.title()).isEqualTo("Soupe de carottes & cumin");
        assertThat(recipe.category()).isEqualTo("Soupe");
        assertThat(recipe.servings()).isEqualTo(4);
        assertThat(recipe.totalTimeMinutes()).isEqualTo(35);
        assertThat(recipe.imageUrl()).isEqualTo("https://img.example/soupe.jpg");
        assertThat(recipe.sourceUrl()).isEqualTo(URL);
        assertThat(recipe.thermomix()).isTrue();
        assertThat(recipe.ingredients())
                .containsExactly(
                        new CustomRecipeDto.Ingredient("Carottes", "500 g"),
                        new CustomRecipeDto.Ingredient("Oignon", "1"),
                        new CustomRecipeDto.Ingredient("Huile d'olive", "2 c. à soupe"),
                        new CustomRecipeDto.Ingredient("Sel", null));
        assertThat(recipe.steps())
                .containsExactly(
                        new CustomRecipeDto.Step(
                                "Mettre l'oignon et mixer 5 sec/vitesse 5.", "5 sec", null, "5", false),
                        new CustomRecipeDto.Step(
                                "Ajouter les carottes, cuire 20 min/100°C/sens inverse/vitesse 1.",
                                "20 min",
                                "100°C",
                                "1",
                                true),
                        new CustomRecipeDto.Step("Servir chaud.", null, null, null, false));
    }

    @Test
    void desInstructionsEnTexteSimpleSontDecoupeesParLigne() {
        String page = """
                <script type="application/ld+json">
                [{"@type":"Recipe","name":"Pâtes au beurre","totalTime":"PT15M","image":"https://img/p.jpg",
                  "recipeIngredient":"200 g de pâtes",
                  "recipeInstructions":"Cuire les pâtes.<br>Ajouter le beurre."}]
                </script>
                """;

        CustomRecipeDto recipe = service.parse(page, URL);

        assertThat(recipe.thermomix()).isFalse();
        assertThat(recipe.totalTimeMinutes()).isEqualTo(15);
        assertThat(recipe.imageUrl()).isEqualTo("https://img/p.jpg");
        assertThat(recipe.ingredients()).containsExactly(new CustomRecipeDto.Ingredient("Pâtes", "200 g"));
        assertThat(recipe.steps())
                .extracting(CustomRecipeDto.Step::instruction)
                .containsExactly("Cuire les pâtes.", "Ajouter le beurre.");
    }

    @Test
    void unePageSansRecetteStructureeEstRefusee() {
        assertThatThrownBy(() -> service.parse("<html><body>Rien</body></html>", URL))
                .isInstanceOf(RecipeImportService.RecipeImportException.class);
    }

    @Test
    void unePageInaccessibleEstSignalee() {
        server.expect(requestTo(URL)).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> service.importFromUrl(URL))
                .isInstanceOf(RecipeImportService.RecipeImportException.class)
                .hasMessageContaining("Impossible d'ouvrir");
    }

    @Test
    void unLienQuiNEstPasHttpEstRefuse() {
        assertThatThrownBy(() -> service.importFromUrl("file:///etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.importFromUrl("pas un lien")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reglagesThermomixVaroumaEtCuillere() {
        List<CustomRecipeDto.Step> steps = new ArrayList<>();
        RecipeImportService.addStep("Cuire 30 min/Varoma/vitesse cuillère.", steps);
        RecipeImportService.addStep("Pulvériser 10 sec/vitesse Turbo.", steps);

        assertThat(steps)
                .containsExactly(
                        new CustomRecipeDto.Step(
                                "Cuire 30 min/Varoma/vitesse cuillère.", "30 min", "Varoma", "cuillère", false),
                        new CustomRecipeDto.Step("Pulvériser 10 sec/vitesse Turbo.", "10 sec", null, "Turbo", false));
    }
}
