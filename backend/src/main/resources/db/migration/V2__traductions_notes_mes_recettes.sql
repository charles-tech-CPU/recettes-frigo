-- Traductions francaises des recettes TheMealDB (DeepL), faites une seule fois
-- par recette puis relues ici. ingredients = JSON [{"ingredient": "...", "measure": "..."}],
-- dans le meme ordre que strIngredient1..20 de TheMealDB.
CREATE TABLE recipe_translation (
    meal_id VARCHAR(20) PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    category VARCHAR(100),
    area VARCHAR(100),
    instructions TEXT,
    ingredients TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

-- Recettes personnelles (saisies a la main ou importees depuis un lien),
-- eventuellement Thermomix : chaque etape peut porter un reglage temps/temperature/vitesse.
CREATE TABLE custom_recipe (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(80),
    servings INTEGER,
    total_time_minutes INTEGER,
    thermomix BOOLEAN NOT NULL DEFAULT FALSE,
    image_url VARCHAR(1000),
    source_url VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE custom_recipe_ingredient (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES custom_recipe (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    name VARCHAR(200) NOT NULL,
    quantity VARCHAR(80)
);

CREATE TABLE custom_recipe_step (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES custom_recipe (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    instruction TEXT NOT NULL,
    tm_time VARCHAR(20),
    tm_temperature VARCHAR(20),
    tm_speed VARCHAR(20),
    tm_reverse BOOLEAN NOT NULL DEFAULT FALSE
);

-- Notes libres sur une recette ("cuisson 200°C, 25 min"...). recipe_key designe
-- la recette : "mealdb:<idMeal>" pour TheMealDB, "perso:<id>" pour une recette perso.
CREATE TABLE recipe_note (
    id BIGSERIAL PRIMARY KEY,
    recipe_key VARCHAR(40) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX recipe_note_recipe_key_idx ON recipe_note (recipe_key);
