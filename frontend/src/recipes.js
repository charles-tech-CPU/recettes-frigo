// Reglages Thermomix proposes dans l'editeur de recette perso
export const TM_TEMPERATURES = [
  '37°C', '40°C', '45°C', '50°C', '55°C', '60°C', '65°C', '70°C', '75°C', '80°C',
  '85°C', '90°C', '95°C', '98°C', '100°C', '105°C', '110°C', '115°C', '120°C', 'Varoma'
]
export const TM_SPEEDS = [
  'cuillère', '0.5', '1', '1.5', '2', '2.5', '3', '3.5', '4', '4.5', '5',
  '5.5', '6', '6.5', '7', '7.5', '8', '8.5', '9', '9.5', '10', 'Turbo'
]

// Notation Thermomix d'une etape : "20 min / 100°C / ↺ / vitesse 1" (null si aucun reglage)
export function formatTm(step) {
  const speed = step.tmSpeed ? (step.tmSpeed === 'Turbo' ? 'Turbo' : `vitesse ${step.tmSpeed}`) : null
  const parts = [step.tmTime, step.tmTemperature, step.tmReverse ? '↺ sens inverse' : null, speed].filter(Boolean)
  return parts.length ? parts.join(' / ') : null
}

export function formatDuration(minutes) {
  if (!minutes) return null
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h ? `${h} h${m ? ` ${String(m).padStart(2, '0')}` : ''}` : `${m} min`
}

// Une recette TheMealDB ou perso, sous la forme affichee par RecipePanel
export function fromMealDb(detail) {
  return {
    key: `mealdb:${detail.mealId}`,
    title: detail.title,
    imageUrl: detail.thumbnailUrl,
    tags: [detail.category, detail.area].filter(Boolean),
    thermomix: false,
    ingredients: detail.ingredients.map(line => ({ name: line.ingredient, quantity: line.measure })),
    steps: null,
    instructions: detail.instructions,
    videoUrl: detail.videoUrl,
    sourceUrl: detail.sourceUrl,
    note: detail.translated
      ? 'Recette TheMealDB traduite automatiquement (MyMemory).'
      : 'Recette TheMealDB restée en anglais : traduction indisponible pour le moment (quota gratuit du jour atteint ?). Réessaie plus tard.'
  }
}

export function fromCustom(recipe) {
  return {
    key: `perso:${recipe.id}`,
    id: recipe.id,
    title: recipe.title,
    imageUrl: recipe.imageUrl,
    tags: [
      recipe.category,
      recipe.servings ? `${recipe.servings} pers.` : null,
      formatDuration(recipe.totalTimeMinutes)
    ].filter(Boolean),
    thermomix: recipe.thermomix,
    ingredients: recipe.ingredients,
    steps: recipe.steps,
    instructions: null,
    videoUrl: null,
    sourceUrl: recipe.sourceUrl,
    note: null
  }
}
