-- Traduction en deux temps pour menager le quota gratuit de MyMemory :
-- la liste des suggestions ne traduit que le titre et les noms d'ingredients
-- (full_translation = FALSE, instructions et quantites non traduites) ; le detail
-- d'une recette complete la traduction (full_translation = TRUE).
ALTER TABLE recipe_translation ADD COLUMN full_translation BOOLEAN NOT NULL DEFAULT FALSE;
