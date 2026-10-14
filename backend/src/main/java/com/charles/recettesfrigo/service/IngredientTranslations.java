package com.charles.recettesfrigo.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * TheMealDB est une base de recettes en anglais : ses noms d'ingredients
 * (utilises pour filter.php?i=...) sont en anglais, avec un underscore a la
 * place des espaces pour les noms composes (ex: "chicken_breast"). Charles
 * saisit son frigo en francais, donc on traduit cote serveur avant d'appeler
 * l'API - uniquement pour l'appel et le calcul des ingredients manquants,
 * jamais affiche tel quel a l'utilisateur.
 * <p>
 * Limite assumee (v1, voir README) : seuls les ingredients courants listes
 * ici sont traduits. Un ingredient absent de cette liste est envoye tel quel
 * a TheMealDB (en francais) et a de fortes chances de ne remonter aucun
 * resultat - c'est un compromis raisonnable pour une appli gratuite et sans
 * dependance a un service de traduction payant.
 */
public final class IngredientTranslations {

    private static final Map<String, String> FR_TO_EN = Map.ofEntries(
            // Viandes / volailles / poissons
            Map.entry("poulet", "chicken"),
            Map.entry("blanc de poulet", "chicken_breast"),
            Map.entry("cuisse de poulet", "chicken_thighs"),
            Map.entry("boeuf", "beef"),
            Map.entry("boeuf hache", "ground_beef"),
            Map.entry("steak hache", "ground_beef"),
            Map.entry("porc", "pork"),
            Map.entry("lardons", "bacon"),
            Map.entry("bacon", "bacon"),
            Map.entry("jambon", "ham"),
            Map.entry("saucisse", "sausages"),
            Map.entry("chorizo", "chorizo"),
            Map.entry("agneau", "lamb"),
            Map.entry("dinde", "turkey"),
            Map.entry("canard", "duck"),
            Map.entry("saumon", "salmon"),
            Map.entry("thon", "tuna"),
            Map.entry("cabillaud", "cod"),
            Map.entry("crevettes", "shrimp"),
            Map.entry("moules", "mussels"),

            // Produits laitiers / oeufs
            Map.entry("oeuf", "eggs"),
            Map.entry("oeufs", "eggs"),
            Map.entry("lait", "milk"),
            Map.entry("beurre", "butter"),
            Map.entry("creme fraiche", "cream"),
            Map.entry("creme", "cream"),
            Map.entry("fromage", "cheese"),
            Map.entry("fromage rape", "cheese"),
            Map.entry("parmesan", "parmesan_cheese"),
            Map.entry("mozzarella", "mozzarella_cheese"),
            Map.entry("feta", "feta_cheese"),
            Map.entry("yaourt", "yogurt"),

            // Legumes
            Map.entry("tomate", "tomato"),
            Map.entry("tomates", "tomatoes"),
            Map.entry("oignon", "onion"),
            Map.entry("oignons", "onions"),
            Map.entry("ail", "garlic"),
            Map.entry("gousse d'ail", "garlic"),
            Map.entry("pomme de terre", "potatoes"),
            Map.entry("pommes de terre", "potatoes"),
            Map.entry("carotte", "carrots"),
            Map.entry("carottes", "carrots"),
            Map.entry("poivron", "bell_pepper"),
            Map.entry("poivron rouge", "red_bell_pepper"),
            Map.entry("courgette", "zucchini"),
            Map.entry("aubergine", "eggplant"),
            Map.entry("champignon", "mushrooms"),
            Map.entry("champignons", "mushrooms"),
            Map.entry("brocoli", "broccoli"),
            Map.entry("chou-fleur", "cauliflower"),
            Map.entry("chou", "cabbage"),
            Map.entry("epinards", "spinach"),
            Map.entry("salade", "lettuce"),
            Map.entry("concombre", "cucumber"),
            Map.entry("celeri", "celery"),
            Map.entry("poireau", "leeks"),
            Map.entry("haricots verts", "green_beans"),
            Map.entry("petits pois", "peas"),
            Map.entry("mais", "sweetcorn"),
            Map.entry("avocat", "avocado"),
            Map.entry("citrouille", "pumpkin"),
            Map.entry("patate douce", "sweet_potato"),
            Map.entry("gingembre", "ginger"),
            Map.entry("piment", "chili"),

            // Fruits
            Map.entry("citron", "lemon"),
            Map.entry("citron vert", "lime"),
            Map.entry("pomme", "apple"),
            Map.entry("banane", "banana"),
            Map.entry("orange", "orange"),
            Map.entry("fraise", "strawberries"),
            Map.entry("fraises", "strawberries"),
            Map.entry("ananas", "pineapple"),
            Map.entry("mangue", "mango"),
            Map.entry("raisin", "grapes"),

            // Feculents / epicerie
            Map.entry("riz", "rice"),
            Map.entry("pates", "pasta"),
            Map.entry("spaghetti", "spaghetti"),
            Map.entry("farine", "flour"),
            Map.entry("sucre", "sugar"),
            Map.entry("sel", "salt"),
            Map.entry("poivre", "black_pepper"),
            Map.entry("huile d'olive", "olive_oil"),
            Map.entry("huile", "vegetable_oil"),
            Map.entry("vinaigre", "vinegar"),
            Map.entry("pain", "bread"),
            Map.entry("chapelure", "breadcrumbs"),
            Map.entry("levure", "yeast"),
            Map.entry("miel", "honey"),
            Map.entry("moutarde", "mustard"),
            Map.entry("mayonnaise", "mayonnaise"),
            Map.entry("ketchup", "ketchup"),
            Map.entry("sauce soja", "soy_sauce"),
            Map.entry("concentre de tomate", "tomato_paste"),
            Map.entry("bouillon", "stock"),
            Map.entry("lait de coco", "coconut_milk"),
            Map.entry("haricots rouges", "kidney_beans"),
            Map.entry("haricots blancs", "cannellini_beans"),
            Map.entry("pois chiches", "chickpeas"),
            Map.entry("lentilles", "lentils"),
            Map.entry("noix de coco", "coconut"),
            Map.entry("amandes", "almonds"),
            Map.entry("noix", "walnuts"),
            Map.entry("chocolat", "chocolate"),
            Map.entry("vanille", "vanilla"),
            Map.entry("cannelle", "cinnamon"),
            Map.entry("basilic", "basil"),
            Map.entry("persil", "parsley"),
            Map.entry("thym", "thyme"),
            Map.entry("romarin", "rosemary"),
            Map.entry("coriandre", "coriander"),
            Map.entry("cumin", "cumin"),
            Map.entry("paprika", "paprika"),
            Map.entry("curry", "curry_powder"),
            Map.entry("laurier", "bay_leaf"));

    private IngredientTranslations() {}

    /**
     * Traduit un nom d'ingredient francais vers le terme anglais attendu par
     * TheMealDB. Si l'ingredient n'est pas dans le dictionnaire, il est
     * renvoye tel quel (normalise), en dernier recours.
     */
    public static String toEnglishQueryTerm(String frenchName) {
        String normalized = normalize(frenchName);
        String translated = FR_TO_EN.get(normalized);
        return translated != null ? translated : normalized.replace(' ', '_');
    }

    public static boolean isKnown(String frenchName) {
        return FR_TO_EN.containsKey(normalize(frenchName));
    }

    /**
     * Les cles du dictionnaire sont sans accents ni ligatures : "Bœuf haché",
     * "Épinards" ou "huile d’olive" (apostrophe typographique) doivent les retrouver.
     */
    static String normalize(String frenchName) {
        String lower = frenchName.trim().toLowerCase(Locale.FRENCH);
        String ascii = lower.replace("œ", "oe").replace("æ", "ae").replace('’', '\'');
        return Normalizer.normalize(ascii, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
