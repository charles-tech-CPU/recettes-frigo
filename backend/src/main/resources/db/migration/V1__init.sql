CREATE TABLE fridge_item (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    quantity VARCHAR(40),
    category VARCHAR(40)
);

-- Quelques ingredients de depart a titre d'exemple (modifiables/supprimables ensuite).
INSERT INTO fridge_item (name, quantity, category) VALUES
    ('Poulet', '2', 'Viande'),
    ('Tomate', '4', 'Legumes'),
    ('Oignon', '2', 'Legumes'),
    ('Riz', '500g', 'Epicerie'),
    ('Oeuf', '6', 'Produits laitiers');
