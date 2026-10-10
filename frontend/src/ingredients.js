// Ingredients proposes dans le formulaire d'ajout, par categorie (cles de
// CATEGORIES). Ce sont les ingredients du dictionnaire FR -> EN du backend
// (IngredientTranslations.java) : ils sont donc tous reconnus par la recherche
// de recettes. Un ingredient hors liste reste saisissable a la main.
// "unit" : unite proposee par defaut, sinon celle de la categorie.

export const UNITS = [
  { value: 'pièce', label: 'pièce(s)' },
  { value: 'g', label: 'g' },
  { value: 'kg', label: 'kg' },
  { value: 'ml', label: 'ml' },
  { value: 'cl', label: 'cl' },
  { value: 'L', label: 'L' },
  { value: 'c. à soupe', label: 'c. à soupe' },
  { value: 'c. à café', label: 'c. à café' },
  { value: 'pincée', label: 'pincée(s)' },
  { value: 'tranche', label: 'tranche(s)' },
  { value: 'boîte', label: 'boîte(s)' },
  { value: 'sachet', label: 'sachet(s)' },
  { value: 'botte', label: 'botte(s)' }
]

const DEFAULT_UNIT_BY_CATEGORY = {
  legumes: 'pièce',
  fruits: 'pièce',
  viandes: 'g',
  poissons: 'g',
  laitiers: 'g',
  feculents: 'g',
  epicerie: 'g',
  epices: 'g',
  autres: 'pièce'
}

export const INGREDIENTS = {
  legumes: [
    'Ail', 'Aubergine', 'Avocat', 'Brocoli', 'Carottes', 'Céleri', 'Champignons',
    'Chou', 'Chou-fleur', 'Citrouille', 'Concombre', 'Courgette',
    { name: 'Épinards', unit: 'g' }, 'Gingembre', { name: 'Haricots verts', unit: 'g' },
    'Maïs', 'Oignons', 'Patate douce', { name: 'Petits pois', unit: 'g' }, 'Piment',
    'Poireau', 'Poivron', 'Poivron rouge', 'Pommes de terre', 'Salade', 'Tomates'
  ],
  fruits: [
    'Ananas', 'Banane', 'Citron', 'Citron vert', { name: 'Fraises', unit: 'g' },
    'Mangue', 'Noix de coco', 'Orange', 'Pomme', { name: 'Raisin', unit: 'g' }
  ],
  viandes: [
    'Agneau', 'Bacon', 'Blanc de poulet', 'Bœuf', 'Bœuf haché', 'Canard', 'Chorizo',
    'Cuisse de poulet', 'Dinde', { name: 'Jambon', unit: 'tranche' }, 'Lardons', 'Porc',
    'Poulet', { name: 'Saucisse', unit: 'pièce' }, { name: 'Steak haché', unit: 'pièce' }
  ],
  poissons: ['Cabillaud', 'Crevettes', 'Moules', 'Saumon', { name: 'Thon', unit: 'boîte' }],
  laitiers: [
    'Beurre', { name: 'Crème', unit: 'cl' }, { name: 'Crème fraîche', unit: 'cl' }, 'Feta',
    'Fromage', 'Fromage râpé', { name: 'Lait', unit: 'L' }, 'Mozzarella',
    { name: 'Œufs', unit: 'pièce' }, 'Parmesan', { name: 'Yaourt', unit: 'pièce' }
  ],
  feculents: ['Chapelure', 'Farine', { name: 'Pain', unit: 'pièce' }, 'Pâtes', 'Riz', 'Spaghetti'],
  epicerie: [
    'Amandes', { name: 'Bouillon', unit: 'pièce' }, 'Chocolat', 'Concentré de tomate',
    { name: "Huile d'olive", unit: 'cl' }, { name: 'Huile', unit: 'cl' }, { name: 'Ketchup', unit: 'c. à soupe' },
    { name: 'Lait de coco', unit: 'cl' }, 'Mayonnaise', 'Miel', 'Moutarde', 'Noix',
    { name: 'Sauce soja', unit: 'c. à soupe' }, 'Sucre', { name: 'Levure', unit: 'sachet' },
    { name: 'Vinaigre', unit: 'cl' }
  ],
  epices: [
    { name: 'Basilic', unit: 'botte' }, 'Cannelle', { name: 'Coriandre', unit: 'botte' }, 'Cumin', 'Curry',
    { name: 'Laurier', unit: 'pièce' }, 'Paprika', { name: 'Persil', unit: 'botte' }, 'Poivre',
    'Romarin', 'Sel', 'Thym', { name: 'Vanille', unit: 'pièce' }
  ],
  autres: []
}

// Ingredients d'une categorie, tries, avec leur unite par defaut
export function ingredientsOf(categoryKey) {
  return (INGREDIENTS[categoryKey] ?? [])
    .map(entry => (typeof entry === 'string' ? { name: entry } : entry))
    .map(entry => ({ name: entry.name, unit: entry.unit ?? DEFAULT_UNIT_BY_CATEGORY[categoryKey] }))
    .sort((a, b) => a.name.localeCompare(b.name, 'fr'))
}

export function defaultUnitOf(categoryKey) {
  return DEFAULT_UNIT_BY_CATEGORY[categoryKey] ?? 'pièce'
}

// "3" + "pièce" -> "3 pièces", "1.5" + "kg" -> "1,5 kg", "2" + "c. à soupe" -> "2 c. à soupe"
export function formatQuantity(amount, unit) {
  if (amount === null || amount === undefined || amount === '') return null
  const number = Number(amount)
  const text = number.toLocaleString('fr-FR', { maximumFractionDigits: 2 })
  const plural = number >= 2 && PLURAL_UNITS.includes(unit)
  return `${text} ${plural ? unit + 's' : unit}`
}

const PLURAL_UNITS = ['pièce', 'pincée', 'tranche', 'boîte', 'sachet', 'botte']

// Nom sans accents ni majuscules, pour comparer et rechercher ("Œufs" -> "oeufs")
export function normalizeName(name) {
  return name.trim().toLowerCase().replace(/œ/g, 'oe').normalize('NFD').replace(/[̀-ͯ]/g, '')
}

// Meme ingredient au singulier ou au pluriel ("Tomate" = "tomates", "Œuf" = "oeufs")
export function sameIngredient(a, b) {
  const singular = name => normalizeName(name).split(/\s+/).map(w => (w.length > 3 ? w.replace(/[sx]$/, '') : w)).join(' ')
  return singular(a) === singular(b)
}
