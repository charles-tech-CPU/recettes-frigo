package com.charles.recettesfrigo.service;

import com.charles.recettesfrigo.dto.CustomRecipeDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Pre-remplit une recette perso a partir du lien d'une page de recette (blog
 * Thermomix, Marmiton, 750g...). La plupart des sites publient leur recette sous une
 * forme structuree lisible par les moteurs de recherche (schema.org "Recipe", en
 * JSON-LD) : on lit ce bloc plutot que la mise en page, propre a chaque site.
 * <p>
 * Rien n'est enregistre ici : la recette renvoyee sert a remplir le formulaire,
 * que l'utilisateur relit et corrige avant d'enregistrer.
 */
@Service
public class RecipeImportService {

    // "200 g de carottes", "2 c. à soupe d'huile", "3 oeufs" -> quantite + nom
    private static final Pattern QUANTITY = Pattern.compile(
            "^([\\d.,/½¼¾⅓⅔]+(?:\\s*-\\s*[\\d.,/]+)?\\s*"
                    + "(?:kg|g|mg|ml|cl|dl|l|c\\.?\\s*à\\s*(?:s|c|soupe|café)\\.?|cuill[eè]res?\\s+à\\s+(?:soupe|café)"
                    + "|pinc[ée]es?|tranches?|sachets?|bo[iî]tes?|gousses?|bottes?|verres?|pots?|brins?|feuilles?)?\\.?)"
                    + "\\s+(?:de\\s+|d['’]\\s*)?(.+)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TM_TIME = Pattern.compile("(\\d+\\s*(?:min|sec|s)\\b(?:\\s*\\d+\\s*(?:sec|s)\\b)?)");
    private static final Pattern TM_TEMPERATURE =
            Pattern.compile("(\\d{2,3}\\s*°\\s*C?|varoma)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TM_SPEED =
            Pattern.compile("vitesse\\s*(cuill[eè]re|turbo|\\d+(?:[.,]5)?)", Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RecipeImportService(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder.defaultHeader("User-Agent", "Mozilla/5.0 (compatible; RecettesFrigo/1.0)")
                .defaultHeader("Accept-Language", "fr-FR,fr;q=0.9")
                .build();
        this.objectMapper = objectMapper;
    }

    public CustomRecipeDto importFromUrl(String url) {
        URI uri = parseUrl(url);
        ResponseEntity<byte[]> response;
        try {
            response = restClient.get().uri(uri).retrieve().toEntity(byte[].class);
        } catch (RestClientResponseException ex) {
            throw new RecipeImportException("Impossible d'ouvrir cette page (le site a répondu "
                    + ex.getStatusCode().value() + ").");
        } catch (RestClientException ex) {
            throw new RecipeImportException("Impossible d'ouvrir cette page (site injoignable).");
        }
        // Octets bruts : l'encodage vient de l'en-tete HTTP s'il le precise, sinon jsoup
        // le lit dans la page (<meta charset>), UTF-8 par defaut. Lire en String ici
        // supposerait ISO-8859-1 sans en-tete et abimerait les accents.
        byte[] body = response.getBody() == null ? new byte[0] : response.getBody();
        MediaType contentType = response.getHeaders().getContentType();
        String charset = contentType != null && contentType.getCharset() != null
                ? contentType.getCharset().name()
                : null;
        try {
            return parse(Jsoup.parse(new ByteArrayInputStream(body), charset, uri.toString()), uri.toString());
        } catch (IOException ex) {
            throw new RecipeImportException("Page illisible.");
        }
    }

    CustomRecipeDto parse(String html, String url) {
        return parse(Jsoup.parse(html, url), url);
    }

    private CustomRecipeDto parse(Document document, String url) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            JsonNode recipe;
            try {
                recipe = findRecipe(objectMapper.readTree(script.data()));
            } catch (JsonProcessingException ex) {
                continue; // bloc JSON-LD mal forme : on essaie le suivant
            }
            if (recipe != null && !text(recipe.get("name")).isEmpty()) {
                return toDto(recipe, url);
            }
        }
        throw new RecipeImportException(
                "Aucune recette lisible sur cette page : le site ne publie pas sa recette sous forme structurée. "
                        + "Tu peux la saisir à la main.");
    }

    private static URI parseUrl(String url) {
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null) {
                return uri;
            }
        } catch (IllegalArgumentException ex) {
            // traite ci-dessous
        }
        throw new IllegalArgumentException("Lien invalide : il doit commencer par http:// ou https://");
    }

    /** Le bloc Recipe, a la racine, dans un tableau ou dans un "@graph". */
    private static JsonNode findRecipe(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findRecipe(child);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        if (isRecipe(node.get("@type"))) {
            return node;
        }
        return findRecipe(node.get("@graph"));
    }

    private static boolean isRecipe(JsonNode type) {
        if (type == null) {
            return false;
        }
        if (type.isArray()) {
            for (JsonNode t : type) {
                if ("Recipe".equals(t.asText())) {
                    return true;
                }
            }
            return false;
        }
        return "Recipe".equals(type.asText());
    }

    private CustomRecipeDto toDto(JsonNode recipe, String url) {
        List<CustomRecipeDto.Ingredient> ingredients = new ArrayList<>();
        for (String line : texts(recipe.get("recipeIngredient"))) {
            ingredients.add(splitIngredient(line));
        }
        List<CustomRecipeDto.Step> steps = new ArrayList<>();
        collectSteps(recipe.get("recipeInstructions"), steps);
        boolean thermomix = steps.stream().anyMatch(s -> s.tmSpeed() != null);

        return new CustomRecipeDto(
                null,
                limit(text(recipe.get("name")), 200),
                limit(first(recipe.get("recipeCategory")), 80),
                servings(recipe.get("recipeYield")),
                totalMinutes(recipe),
                thermomix,
                limit(image(recipe.get("image")), 1000),
                limit(url, 1000),
                ingredients,
                steps);
    }

    static CustomRecipeDto.Ingredient splitIngredient(String line) {
        Matcher m = QUANTITY.matcher(line);
        if (m.matches()) {
            return new CustomRecipeDto.Ingredient(
                    limit(capitalize(m.group(2)), 200), limit(m.group(1).trim(), 80));
        }
        return new CustomRecipeDto.Ingredient(limit(capitalize(line), 200), null);
    }

    /** Etapes : texte, liste de textes, HowToStep {text} ou HowToSection {itemListElement}. */
    private static void collectSteps(JsonNode node, List<CustomRecipeDto.Step> steps) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectSteps(child, steps));
        } else if (node.isTextual()) {
            for (String line : clean(node.asText()).split("\\s*\\n+\\s*")) {
                addStep(line, steps);
            }
        } else if (node.has("itemListElement")) {
            collectSteps(node.get("itemListElement"), steps);
        } else if (node.has("text")) {
            addStep(clean(node.get("text").asText()), steps);
        }
    }

    /** Reglage Thermomix lu dans le texte de l'etape s'il mentionne une vitesse ("5 min/100°C/vitesse 1"). */
    static void addStep(String text, List<CustomRecipeDto.Step> steps) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher speed = TM_SPEED.matcher(text);
        if (!speed.find()) {
            steps.add(new CustomRecipeDto.Step(text, null, null, null, false));
            return;
        }
        Matcher time = TM_TIME.matcher(text);
        Matcher temperature = TM_TEMPERATURE.matcher(text);
        String speedValue = speed.group(1).toLowerCase(Locale.FRENCH).replace(',', '.');
        steps.add(new CustomRecipeDto.Step(
                text,
                time.find() ? time.group(1).trim() : null,
                temperature.find() ? normalizeTemperature(temperature.group(1)) : null,
                speedValue.equals("turbo") ? "Turbo" : speedValue.replace("cuillere", "cuillère"),
                text.toLowerCase(Locale.FRENCH).contains("sens inverse")));
    }

    private static String normalizeTemperature(String raw) {
        if (raw.equalsIgnoreCase("varoma")) {
            return "Varoma";
        }
        return raw.replaceAll("[^0-9]", "") + "°C";
    }

    private static Integer servings(JsonNode yield) {
        String value = first(yield);
        Matcher m = Pattern.compile("(\\d+)").matcher(value == null ? "" : value);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private static Integer totalMinutes(JsonNode recipe) {
        Integer total = minutes(recipe.get("totalTime"));
        if (total != null) {
            return total;
        }
        Integer prep = minutes(recipe.get("prepTime"));
        Integer cook = minutes(recipe.get("cookTime"));
        if (prep == null && cook == null) {
            return null;
        }
        return (prep == null ? 0 : prep) + (cook == null ? 0 : cook);
    }

    /** Duree ISO 8601 ("PT1H30M") en minutes. */
    private static Integer minutes(JsonNode node) {
        String value = text(node);
        if (value.isEmpty()) {
            return null;
        }
        try {
            long minutes = Duration.parse(value).toMinutes();
            return minutes > 0 ? (int) minutes : null;
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /** "image" : texte, liste, ou objet ImageObject {url}. */
    private static String image(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            return node.isEmpty() ? null : image(node.get(0));
        }
        if (node.isObject()) {
            return image(node.get("url"));
        }
        String url = node.asText();
        return url.isBlank() ? null : url;
    }

    private static List<String> texts(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node == null || node.isNull()) {
            return result;
        }
        if (node.isArray()) {
            node.forEach(child -> {
                String value = clean(child.asText());
                if (!value.isEmpty()) {
                    result.add(value);
                }
            });
        } else if (!clean(node.asText()).isEmpty()) {
            result.add(clean(node.asText()));
        }
        return result;
    }

    private static String first(JsonNode node) {
        List<String> values = texts(node);
        return values.isEmpty() ? null : values.get(0);
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? "" : clean(node.asText());
    }

    /** Retire les balises et entites HTML parfois presentes dans le JSON-LD. */
    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        String withBreaks = value.replaceAll("(?i)<br\\s*/?>|</p>", "\n");
        return Jsoup.parse(withBreaks).wholeText().replace(' ', ' ').trim();
    }

    private static String capitalize(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? trimmed : Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private static String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    public static class RecipeImportException extends RuntimeException {
        public RecipeImportException(String message) {
            super(message);
        }
    }
}
