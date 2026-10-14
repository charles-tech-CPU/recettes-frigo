-- Liste de courses : ingredients manquants d'une recette (recipe_title) ou ajoutes a la main,
-- coches au magasin puis ranges dans le frigo.
CREATE TABLE shopping_item (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    quantity VARCHAR(80),
    recipe_title VARCHAR(300),
    checked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);
