-- Date de peremption optionnelle d'un ingredient du frigo : l'ecran "Mon frigo" met en
-- avant ce qui perime bientot et les suggestions font passer en tete les recettes qui l'utilisent.
ALTER TABLE fridge_item ADD COLUMN expires_on DATE;
