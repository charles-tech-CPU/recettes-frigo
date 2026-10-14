-- Recettes favorites : TheMealDB ("mealdb:<idMeal>") ou perso ("perso:<id>"). Titre et
-- photo sont gardes pour afficher la liste sans rappeler TheMealDB ni retraduire.
CREATE TABLE recipe_favorite (
    recipe_key VARCHAR(40) PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    image_url VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);
