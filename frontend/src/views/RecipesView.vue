<template>
  <div class="inline" style="justify-content:space-between">
    <h2 style="margin:0">Recettes possibles avec ton frigo</h2>
    <button class="secondary" :disabled="loading" @click="load">{{ loading ? 'Recherche...' : '↻ Actualiser' }}</button>
  </div>
  <p style="color:#9fb0c0">
    Tes recettes et celles de TheMealDB, classées par nombre d'ingrédients de ton frigo qu'elles utilisent.
  </p>

  <div v-if="suggestions.length" class="filters">
    <button
      v-for="f in FILTERS"
      :key="f.key"
      type="button"
      class="filter-chip"
      :class="{ active: activeFilters.has(f.key) }"
      :aria-pressed="activeFilters.has(f.key)"
      @click="toggleFilter(f.key)"
    >
      {{ f.label }}
    </button>
  </div>

  <p v-if="error" style="color:#ff6b6b">{{ error }}</p>
  <p v-if="!loading && !error && !suggestions.length" class="empty-hint">
    Aucune suggestion pour l'instant — ajoute des ingrédients dans "Mon frigo", ou réessaie.
  </p>
  <p v-else-if="!loading && suggestions.length && !visible.length" class="empty-hint">
    Aucune recette ne correspond à ces filtres.
  </p>

  <div class="recipe-grid">
    <div
      v-for="r in visible"
      :key="`${r.source}:${r.id}`"
      class="recipe-card"
      :class="{ opening: opening === `${r.source}:${r.id}` }"
      @click="openDetail(r)"
    >
      <FavoriteButton overlay :recipe="{ key: `${r.source}:${r.id}`, title: r.title, imageUrl: r.thumbnailUrl }" />
      <img v-if="r.thumbnailUrl" :src="r.thumbnailUrl" :alt="r.title" />
      <div v-else class="no-image"><CategoryIcon name="autres" /></div>
      <div class="content">
        <div class="badges">
          <span class="match-badge">{{ r.matchedIngredientCount }} ingrédient(s) du frigo</span>
          <span v-if="r.source === 'perso'" class="mine-badge">Ma recette</span>
          <span v-if="r.thermomix" class="tm-badge">Thermomix</span>
        </div>
        <div v-if="expiringNames(r).length" class="expiring-uses">⏳ Écoule : {{ expiringNames(r).join(', ') }}</div>
        <div class="title">{{ r.title }}</div>
        <div v-if="opening === `${r.source}:${r.id}`" class="opening-hint">Ouverture de la recette…</div>
        <div v-if="r.missingIngredients.length" class="missing">
          Manque : {{ r.missingIngredients.slice(0, 3).join(', ') }}<span v-if="r.missingIngredients.length > 3">...</span>
        </div>
        <div v-else class="missing">Tu as tout !</div>
      </div>
    </div>
  </div>

  <RecipePanel v-if="detail" :recipe="detail" :missing="detailMissing" @close="detail = null" />
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../services/api'
import CategoryIcon from '../components/CategoryIcon.vue'
import FavoriteButton from '../components/FavoriteButton.vue'
import RecipePanel from '../components/RecipePanel.vue'
import { fromCustom, fromMealDb } from '../recipes'
import { daysUntil, isExpiringSoon } from '../expiry'
import { useFavorites } from '../favorites'

// Filtres cumulables ; ?filtre=a,b dans l'adresse (lien "A consommer vite" du frigo)
const FILTERS = [
  { key: 'complet', label: "✓ J'ai tout", test: r => !r.missingIngredients.length },
  { key: 'favoris', label: '♥ Favoris', test: r => isFavorite(`${r.source}:${r.id}`) },
  { key: 'perso', label: 'Mes recettes', test: r => r.source === 'perso' },
  { key: 'thermomix', label: 'Thermomix', test: r => r.thermomix },
  { key: 'peremption', label: '⏳ Écoule ce qui périme', test: r => expiringNames(r).length > 0 }
]

const { isFavorite } = useFavorites()
const route = useRoute()
const router = useRouter()
const suggestions = ref([])
const fridgeItems = ref([])
const activeFilters = reactive(new Set(String(route.query.filtre ?? '').split(',').filter(key => FILTERS.some(f => f.key === key))))
const detail = ref(null)
// Ingredients manquants de la recette ouverte (pour la liste de courses)
const detailMissing = ref(null)
const loading = ref(false)
const error = ref('')
// Recette en cours d'ouverture ("source:id") : une seule a la fois
const opening = ref(null)

// Ingredients du frigo perimes ou perimant bientot, par id
const expiringById = computed(() => new Map(fridgeItems.value.filter(isExpiringSoon).map(i => [i.id, i])))

function expiringNames(suggestion) {
  return suggestion.usedFridgeItemIds
    .map(id => expiringById.value.get(id))
    .filter(Boolean)
    .sort((a, b) => daysUntil(a.expiresOn) - daysUntil(b.expiresOn))
    .map(i => i.name)
}

// Les recettes qui ecoulent ce qui perime passent devant (tri stable : l'ordre du serveur reste sinon)
const visible = computed(() => suggestions.value
  .filter(r => FILTERS.every(f => !activeFilters.has(f.key) || f.test(r)))
  .map(r => ({ r, expiring: expiringNames(r).length }))
  .sort((a, b) => b.expiring - a.expiring)
  .map(({ r }) => r))

function toggleFilter(key) {
  if (activeFilters.has(key)) activeFilters.delete(key)
  else activeFilters.add(key)
  router.replace({ query: activeFilters.size ? { filtre: [...activeFilters].join(',') } : {} })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [found, fridge] = await Promise.all([
      api.getSuggestions(12),
      // sans le frigo, on perd seulement la mise en avant de ce qui perime
      api.getFridgeItems().catch(() => [])
    ])
    suggestions.value = found
    fridgeItems.value = fridge
  } catch (e) {
    error.value = e.response?.data?.error ?? 'Impossible de récupérer des suggestions pour le moment.'
  } finally {
    loading.value = false
  }
}

async function openDetail(suggestion) {
  if (opening.value) return
  opening.value = `${suggestion.source}:${suggestion.id}`
  error.value = ''
  try {
    detail.value = suggestion.source === 'perso'
      ? fromCustom(await api.getMyRecipe(suggestion.id))
      : fromMealDb(await api.getRecipeDetail(suggestion.id))
    detailMissing.value = suggestion.missingIngredients
  } catch (e) {
    error.value = e.response?.data?.error ?? 'Impossible de récupérer cette recette.'
  } finally {
    opening.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.badges { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 4px; }
.badges span { font-size: 12px; padding: 3px 8px; border-radius: 10px; }
.recipe-card .match-badge { margin-bottom: 0; }
.mine-badge { background: #1e3350; color: #7fd0ff; }
.tm-badge { background: #3a1f3a; color: #f0abfc; font-weight: 700; }
.no-image {
  width: 100%; aspect-ratio: 1; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1f3a2b, #1a2e45);
}
.no-image :deep(.category-icon) { width: 45%; height: 45%; }
.expiring-uses { margin-top: 6px; font-size: 13px; font-weight: 600; color: #fbbf24; }
.filters { display: flex; flex-wrap: wrap; gap: 8px; margin: 4px 0 16px; }
.filter-chip {
  min-height: 40px; padding: 6px 14px; border-radius: 999px; font-size: 14px;
  background: #22303f; border-color: #37485a; color: #e6ecf1;
}
.filter-chip.active { background: #2b6cb0; border-color: #2b6cb0; font-weight: 700; }
.recipe-card.opening { opacity: 0.7; cursor: progress; }
.opening-hint { margin-top: 6px; font-size: 13px; color: #7fd0ff; }
</style>
