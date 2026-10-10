// Sections de l'ecran "Mon frigo", dans l'ordre d'affichage. La categorie d'un
// ingredient est un texte libre : on la range dans une section via des mots-cles,
// sans tenir compte des majuscules, des accents ni du pluriel ("Produits laitiers",
// "laitier", "Crèmerie" -> Produits laitiers). Inconnue ou absente -> "Autres".
export const CATEGORIES = [
  { key: 'legumes', label: 'Légumes', color: '#4ade80' },
  { key: 'fruits', label: 'Fruits', color: '#f472b6' },
  { key: 'viandes', label: 'Viandes', color: '#f87171' },
  { key: 'poissons', label: 'Poissons', color: '#38bdf8' },
  { key: 'laitiers', label: 'Produits laitiers', color: '#93c5fd' },
  { key: 'feculents', label: 'Féculents', color: '#f5c451' },
  { key: 'epicerie', label: 'Épicerie', color: '#fb923c' },
  { key: 'epices', label: 'Épices', color: '#c084fc' },
  { key: 'autres', label: 'Autres', color: '#94a3b8' }
]

// Ordre de recherche (different de l'ordre d'affichage) : "fruits de mer" doit
// tomber dans Poissons avant d'etre pris pour un fruit, et "epicerie" contient "epice".
const KEYWORDS = [
  ['poissons', ['poisson', 'fruit de mer', 'crustace', 'coquillage']],
  ['legumes', ['legume', 'crudite', 'salade']],
  ['fruits', ['fruit']],
  ['viandes', ['viande', 'volaille', 'charcuterie', 'boucherie']],
  ['laitiers', ['laitier', 'laitage', 'cremerie', 'fromage', 'oeuf', 'lait']],
  ['feculents', ['feculent', 'cereale', 'pate', 'riz', 'pain', 'boulangerie', 'legumineuse']],
  ['epicerie', ['epicerie', 'conserve', 'placard', 'condiment', 'sauce']],
  ['epices', ['epice', 'herbe', 'aromate', 'assaisonnement']]
]

// "Produits Laitiers " -> "produit laitier"
function normalize(text) {
  return text
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/œ/g, 'oe')
    .split(/\s+/)
    .filter(Boolean)
    .map(word => (word.length > 3 ? word.replace(/[sx]$/, '') : word))
    .join(' ')
}

export function categoryKeyOf(category) {
  const normalized = normalize(category ?? '')
  if (!normalized) return 'autres'
  const match = KEYWORDS.find(([, words]) => words.some(word => normalized.includes(word)))
  return match ? match[0] : 'autres'
}

// Sections non vides, dans l'ordre de CATEGORIES, ingredients tries par nom
export function groupByCategory(items) {
  return CATEGORIES
    .map(category => ({
      ...category,
      items: items
        .filter(item => categoryKeyOf(item.category) === category.key)
        .sort((a, b) => a.name.localeCompare(b.name, 'fr'))
    }))
    .filter(section => section.items.length)
}
