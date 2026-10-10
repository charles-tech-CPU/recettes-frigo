<template>
  <div class="inline" style="justify-content:space-between">
    <h2 style="margin:0">Recettes possibles avec ton frigo</h2>
    <button class="secondary" :disabled="loading" @click="load">{{ loading ? 'Recherche...' : '↻ Actualiser' }}</button>
  </div>
  <p style="color:#9fb0c0">
    Classées par nombre d'ingrédients de ton frigo qu'elles utilisent.
    Les titres et instructions viennent de TheMealDB, en anglais.
  </p>

  <p v-if="error" style="color:#ff6b6b">{{ error }}</p>
  <p v-if="!loading && !error && !suggestions.length" class="empty-hint">
    Aucune suggestion pour l'instant — ajoute des ingrédients dans "Mon frigo", ou réessaie.
  </p>

  <div class="recipe-grid">
    <div v-for="r in suggestions" :key="r.mealId" class="recipe-card" @click="openDetail(r.mealId)">
      <img :src="r.thumbnailUrl" :alt="r.title" />
      <div class="content">
        <div class="match-badge">{{ r.matchedIngredientCount }} ingrédient(s) du frigo</div>
        <div class="title">{{ r.title }}</div>
        <div v-if="r.missingIngredients.length" class="missing">
          Manque : {{ r.missingIngredients.slice(0, 3).join(', ') }}<span v-if="r.missingIngredients.length > 3">...</span>
        </div>
        <div v-else class="missing">Tu as tout !</div>
      </div>
    </div>
  </div>

  <div v-if="detail" class="overlay" @click.self="detail = null">
    <div class="panel">
      <div class="panel-header">
        <h2 style="margin:0">{{ detail.title }}</h2>
        <button class="secondary" @click="detail = null">✕</button>
      </div>
      <img :src="detail.thumbnailUrl" :alt="detail.title" />
      <div class="inline">
        <span v-if="detail.category" class="match-badge">{{ detail.category }}</span>
        <span v-if="detail.area" class="match-badge">{{ detail.area }}</span>
      </div>

      <h3>Ingrédients</h3>
      <div v-for="(line, i) in detail.ingredients" :key="i" class="ingredient-line">
        <span>{{ line.ingredient }}</span>
        <span style="color:#9fb0c0">{{ line.measure }}</span>
      </div>

      <h3>Instructions</h3>
      <p class="instructions">{{ detail.instructions }}</p>
      <p class="lang-note">Titre et instructions fournis en anglais par TheMealDB.</p>

      <div class="inline" style="margin-top:14px">
        <a v-if="detail.videoUrl" :href="detail.videoUrl" target="_blank"><button type="button">▶ Vidéo</button></a>
        <a v-if="detail.sourceUrl" :href="detail.sourceUrl" target="_blank"><button type="button" class="secondary">Source</button></a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'

const suggestions = ref([])
const detail = ref(null)
const loading = ref(false)
const error = ref('')

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

async function openDetail(mealId) {
  try {
    detail.value = await api.getRecipeDetail(mealId)
  } catch (e) {
    error.value = e.response?.data?.error ?? 'Impossible de récupérer cette recette.'
  }
}

onMounted(load)
</script>
