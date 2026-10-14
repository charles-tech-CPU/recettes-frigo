<template>
  <div class="inline" style="justify-content:space-between">
    <h2 style="margin:0">Mes recettes</h2>
    <router-link to="/mes-recettes/nouvelle"><button type="button">+ Nouvelle recette</button></router-link>
  </div>
  <p style="color:#9fb0c0">
    Tes recettes à toi, classiques ou Thermomix : saisies à la main ou importées depuis un lien.
    Elles sont aussi proposées dans "Recettes" quand ton frigo s'y prête.
  </p>

  <p v-if="error" style="color:#ff6b6b">{{ error }}</p>
  <p v-if="loaded && !recipes.length" class="empty-hint">
    Aucune recette perso pour l'instant — crée la première avec "+ Nouvelle recette".
  </p>

  <div class="recipe-grid">
    <div v-for="r in recipes" :key="r.id" class="recipe-card" @click="detail = fromCustom(r)">
      <img v-if="r.imageUrl" :src="r.imageUrl" :alt="r.title" />
      <div v-else class="no-image"><CategoryIcon name="autres" /></div>
      <div class="content">
        <div class="badges">
          <span v-if="r.thermomix" class="tm-badge">Thermomix</span>
          <span v-if="r.category" class="match-badge">{{ r.category }}</span>
        </div>
        <div class="title">{{ r.title }}</div>
        <div class="missing">
          {{ r.ingredients.length }} ingrédient(s)<span v-if="r.totalTimeMinutes"> · {{ formatDuration(r.totalTimeMinutes) }}</span>
        </div>
      </div>
    </div>
  </div>

  <RecipePanel v-if="detail" :recipe="detail" @close="detail = null">
    <template #actions>
      <div class="panel-actions">
        <router-link :to="`/mes-recettes/${detail.id}`"><button type="button" class="secondary">✎ Modifier</button></router-link>
        <button type="button" class="danger" @click="remove(detail)">
          {{ detail.confirmDelete ? 'Confirmer la suppression ?' : 'Supprimer' }}
        </button>
      </div>
    </template>
  </RecipePanel>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'
import CategoryIcon from '../components/CategoryIcon.vue'
import RecipePanel from '../components/RecipePanel.vue'
import { formatDuration, fromCustom } from '../recipes'

const recipes = ref([])
const detail = ref(null)
const loaded = ref(false)
const error = ref('')

async function load() {
  try {
    recipes.value = await api.getMyRecipes()
  } catch {
    error.value = 'Impossible de charger tes recettes.'
  } finally {
    loaded.value = true
  }
}

// Suppression en deux temps (pas de boite de dialogue du navigateur) : premier
// appui = demande de confirmation sur le bouton, second appui = suppression.
async function remove(recipe) {
  if (!recipe.confirmDelete) {
    detail.value = { ...recipe, confirmDelete: true }
    return
  }
  try {
    await api.deleteMyRecipe(recipe.id)
  } catch {
    error.value = `Impossible de supprimer « ${recipe.title} ».`
    return
  } finally {
    detail.value = null
  }
  await load()
}

onMounted(load)
</script>

<style scoped>
.badges { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 4px; min-height: 22px; }
.badges span { font-size: 12px; padding: 3px 8px; border-radius: 10px; }
.recipe-card .match-badge { margin-bottom: 0; }
.tm-badge { background: #3a1f3a; color: #f0abfc; font-weight: 700; }
.no-image {
  width: 100%; aspect-ratio: 1; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #2b2346, #1a2e45);
}
.no-image :deep(.category-icon) { width: 45%; height: 45%; }
.panel-actions { display: flex; gap: 10px; flex-wrap: wrap; margin: 8px 0 4px; }
</style>
