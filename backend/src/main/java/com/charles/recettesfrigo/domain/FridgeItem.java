package com.charles.recettesfrigo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un ingredient present dans le frigo/placard, saisi en francais par
 * l'utilisateur et conserve d'une fois sur l'autre (inventaire persistant,
 * comme demande) - ajoute/retire au fil des courses plutot que ressaisi a
 * chaque recherche de recette.
 */
@Entity
@Table(name = "fridge_item")
@Getter
@Setter
@NoArgsConstructor
public class FridgeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    /** Quantite libre, purement informative (ex: "2", "500g"). */
    @Column(length = 40)
    private String quantity;

    /** Categorie libre pour organiser l'affichage (ex: "Legumes", "Viande"). */
    @Column(length = 40)
    private String category;
}
