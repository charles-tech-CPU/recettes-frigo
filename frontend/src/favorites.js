import { reactive } from 'vue'
import api from './services/api'

// Recettes favorites, partagees par tous les ecrans : chargees une fois, tenues a jour
// a chaque ajout/retrait. Une recette = { key: "mealdb:<id>" | "perso:<id>", title, imageUrl }.
const state = reactive({ list: [], loaded: false })
let loading = null

function ensureLoaded() {
  if (state.loaded || loading) return
  loading = api.getFavorites()
    .then(list => {
      state.list = list
      state.loaded = true
    })
    .catch(() => {}) // sans favoris, le reste de l'appli marche : on reessaiera au prochain ecran
    .finally(() => { loading = null })
}

function isFavorite(key) {
  return state.list.some(f => f.recipeKey === key)
}

async function toggle(recipe) {
  if (isFavorite(recipe.key)) {
    await api.removeFavorite(recipe.key)
    state.list = state.list.filter(f => f.recipeKey !== recipe.key)
  } else {
    const favorite = await api.addFavorite({ recipeKey: recipe.key, title: recipe.title, imageUrl: recipe.imageUrl ?? null })
    state.list = [favorite, ...state.list]
  }
}

export function useFavorites() {
  ensureLoaded()
  return { favorites: state, isFavorite, toggle }
}
