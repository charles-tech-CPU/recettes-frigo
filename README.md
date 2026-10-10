# Recettes Frigo

Appli qui suggère des recettes à partir de ce que tu as réellement dans ton
frigo/placard : tu tiens à jour un inventaire (ajouté/retiré au fil des
courses), l'appli interroge l'API gratuite **TheMealDB** et classe les
recettes par nombre d'ingrédients de ton frigo qu'elles utilisent. Backend
Java / Spring Boot, frontend Vue 3, base PostgreSQL.

Sixième projet **indépendant** de `lol-results`, `foot-results`,
`tennis-results`, `budget-foyer` et `calendrier-foyer` : repo séparé, base
séparée, ports différents (backend 8085, frontend 5178).

## 1. Prérequis

Identiques aux cinq autres projets : Java 21 (JDK), Maven, Node.js 20+/npm,
PostgreSQL 14+, Git. Il faut en plus un **accès internet** pour le serveur
qui fait tourner le backend (pour interroger TheMealDB) - contrairement aux
5 autres projets qui fonctionnent entièrement en local.

## 2. Principe du projet

- **"Mon frigo"** : un inventaire simple et persistant (nom, quantité libre,
  catégorie libre) que tu modifies au fil de l'eau - comme demandé, rien à
  ressaisir à chaque recherche.
- **"Recettes"** : l'écran principal interroge TheMealDB pour chaque
  ingrédient de ton frigo, compte combien de fois chaque recette ressort
  (= combien d'ingrédients de ton frigo elle utilise), et affiche les
  recettes triées par ce score - en indiquant pour chacune les ingrédients
  qui te manquent réellement (calculés à partir de la vraie liste
  d'ingrédients de la recette, jamais devinés).
- TheMealDB est **entièrement gratuite et sans limite de requêtes** à
  surveiller (contrairement à Spoonacular), ce qui correspond à ton choix.

### Limite importante assumée (v1) : la langue

TheMealDB est une base de recettes **en anglais**. Deux conséquences :
- **Titres et instructions des recettes s'affichent en anglais** - aucune
  API de traduction gratuite fiable n'existe pour ça, et en ajouter une
  payante aurait changé le budget du projet. L'écran de détail l'indique
  clairement.
- **Pour que la recherche fonctionne avec un frigo saisi en français**,
  j'ai intégré un dictionnaire d'environ 100 ingrédients courants
  (`IngredientTranslations.java`) qui traduit côté serveur avant d'appeler
  l'API (ex: "poulet" → "chicken"). Un ingrédient absent de ce dictionnaire
  est envoyé tel quel à TheMealDB et a de fortes chances de ne remonter
  aucun résultat. Si tu tombes souvent sur ce cas, dis à Claude Code
  d'ajouter les mots qui manquent dans cette liste - c'est un fichier très
  simple à étendre.

### Autre simplification (v1)

- TheMealDB ne sait filtrer que **par un seul ingrédient à la fois** (pas de
  "donne-moi les recettes avec poulet ET tomate ET riz" en une requête). Le
  classement par "nombre d'ingrédients du frigo utilisés" est donc calculé
  côté serveur en interrogeant l'API une fois par ingrédient du frigo, puis
  en comptant les recettes qui reviennent plusieurs fois - une bonne
  approximation, pas un vrai filtre combiné natif.

## 3. Base de données

```bash
psql -U postgres
```

```sql
CREATE USER recettes_user WITH PASSWORD 'recettes_password';
CREATE DATABASE recettes_frigo OWNER recettes_user;
\q
```

## 4. Git

```bash
cd recettes-frigo
git init
git add .
git commit -m "Scaffold initial : backend Spring Boot, frontend Vue 3, suggestions de recettes via TheMealDB"
```

## 5. Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

Flyway applique automatiquement `V1__init.sql` (schéma + 5 ingrédients de
départ à titre d'exemple). L'API démarre sur `http://localhost:8085`.

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/fridge-items` | Inventaire du frigo |
| POST | `/api/fridge-items` | Ajouter un ingrédient |
| PUT | `/api/fridge-items/{id}` | Modifier un ingrédient |
| DELETE | `/api/fridge-items/{id}` | Retirer un ingrédient |
| GET | `/api/recipes/suggestions?limit=12` | Recettes suggérées, triées par nombre d'ingrédients du frigo utilisés |
| GET | `/api/recipes/{mealId}` | Détail complet d'une recette (ingrédients, instructions, vidéo) |

Compilation, tests (`mvn verify`) et appels réels à TheMealDB vérifiés :
le format des réponses `filter.php`/`lookup.php` correspond bien au code.

## 6. Frontend (Vue 3)

```bash
cd frontend
npm install
npm run dev
```

Démarre sur `http://localhost:5178`, appelle l'API sur `http://localhost:8085`
(CORS déjà configuré). Deux écrans, pensés pour le tactile :

- **Recettes** (écran d'accueil) : grille de cartes recettes (photo, nombre
  d'ingrédients du frigo utilisés, ingrédients manquants en aperçu). Tape
  sur une carte pour voir le détail complet (tous les ingrédients avec
  quantités, instructions, lien vidéo/source).
- **Mon frigo** : liste de l'inventaire avec formulaire d'ajout rapide
  (ingrédient, quantité et catégorie optionnelles) et suppression au doigt.

## 7. Lancer depuis IntelliJ

Trois configurations partagées (`.idea/runConfigurations/`) apparaissent
dans la liste déroulante en haut à droite : **Backend (Spring Boot)**,
**Frontend (Vite)** et **Tout démarrer** (les deux à la fois). Lance
`npm install` une fois dans `frontend/` avant le premier démarrage.

## 8. Qualité et intégration continue

- **Spotless** (formatage Java, palantir-java-format) : `mvn spotless:apply`
  reformate ; `mvn verify` échoue si un fichier n'est pas formaté.
- **JaCoCo** : `mvn verify` produit `backend/target/site/jacoco/jacoco.xml`.
- **ESLint** : `npm run lint` vérifie, `npm run lint:fix` corrige.
- **GitHub Actions** (`.github/workflows/ci.yml`, branche `main`) : build +
  tests backend, lint + build frontend, puis analyse **SonarQube Cloud**
  (`sonar-project.properties`). Il faut créer le projet
  `charles-tech-CPU_recettes-frigo` sur sonarcloud.io et ajouter le secret
  `SONAR_TOKEN` dans le repo GitHub.

## 9. Pistes d'évolution (hors v1)

- Traduction automatique des titres/instructions (nécessiterait une API de
  traduction, gratuite avec quota ou payante).
- Étendre le dictionnaire français → anglais au fur et à mesure des
  ingrédients qui ne matchent pas.
- Liste de courses générée à partir des ingrédients manquants d'une recette
  choisie.
- Historique des recettes déjà cuisinées / favoris.
- Basculer vers une API multi-ingrédients native (ex: Spoonacular) si le
  dictionnaire de traduction ou le système "un ingrédient à la fois" montre
  ses limites à l'usage.

## Structure du repo

```
recettes-frigo/
├── backend/    Spring Boot (Java 21, Maven, PostgreSQL, Flyway, appel TheMealDB)
└── frontend/   Vue 3 + Vite (interface tactile, grille de recettes)
```
