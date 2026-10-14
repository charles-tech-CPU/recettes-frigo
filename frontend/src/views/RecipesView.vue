<template>
  <div class="inline" style="justify-content:space-between">
    <h2 style="margin:0">Recettes possibles avec ton frigo</h2>
    <button class="secondary" :disabled="loading" @click="load">{{ loading ? 'Recherche...' : '↻ Actualiser' }}</button>
  </div>
  <p style="color:#9fb0c0">
    Tes recettes et celles de TheMealDB, classées par nombre d'ingrédients de ton frigo qu'elles utilisent.
  </p>

  <p v-if="error" style="color:#ff6b6b">{{ error }}</p>
  <p v-if="!loading && !error && !suggestions.length" class="empty-hint">
    Aucune suggestion pour l'instant — ajoute des ingrédients dans "Mon frigo", ou réessaie.
  </p>

  <div class="recipe-grid">
    <div
      v-for="r in suggestions"
      :key="`${r.source}:${r.id}`"
      class="recipe-card"
      :class="{ opening: opening === `${r.source}:${r.id}` }"
      @click="openDetail(r)"
    >
      <img v-if="r.thumbnailUrl" :src="r.thumbnailUrl" :alt="r.title" />
      <div v-else class="no-image"><CategoryIcon name="autres" /></div>
      <div class="content">
        <div class="badges">
          <span class="match-badge">{{ r.matchedIngredientCount }} ingrédient(s) du frigo</span>
          <span v-if="r.source === 'perso'" class="mine-badge">Ma recette</span>
          <span v-if="r.thermomix" class="tm-badge">Thermomix</span>
        </div>
        <div class="title">{{ r.title }}</div>
        <div v-if="opening === `${r.source}:${r.id}`" class="opening-hint">Ouverture de la recette…</div>
        <div v-if="r.missingIngredients.length" class="missing">
          Manque : {{ r.missingIngredients.slice(0, 3).join(', ') }}<span v-if="r.missingIngredients.length > 3">...</span>
        </div>
        <div v-else class="missing">Tu as tout !</div>
      </div>
    </div>
  </div>

  <RecipePanel v-if="detail" :recipe="detail" @close="detail = null" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'
import CategoryIcon from '../components/CategoryIcon.vue'
import RecipePanel from '../components/RecipePanel.vue'
import { fromCustom, fromMealDb } from '../recipes'

const suggestions = ref([])
const detail = ref(null)
const loading = ref(false)
const error = ref('')
// Recette en cours d'ouverture ("source:id") : une seule a la fois
const opening = ref(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    suggestions.value = await api.getSuggestions(12)
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
.recipe-card.opening { opacity: 0.7; cursor: progress; }
.opening-hint { margin-top: 6px; font-size: 13px; color: #7fd0ff; }
</style>
