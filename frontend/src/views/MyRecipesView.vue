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

  <section v-if="favorites.list.length" class="favorites">
    <h3>♥ Mes favoris</h3>
    <div class="recipe-grid">
      <div
        v-for="f in favorites.list"
        :key="f.recipeKey"
        class="recipe-card"
        :class="{ opening: opening === f.recipeKey }"
        @click="openFavorite(f)"
      >
        <FavoriteButton overlay :recipe="{ key: f.recipeKey, title: f.title, imageUrl: f.imageUrl }" />
        <img v-if="f.imageUrl" :src="f.imageUrl" :alt="f.title" />
        <div v-else class="no-image"><CategoryIcon name="autres" /></div>
        <div class="content">
          <div class="title">{{ f.title }}</div>
          <div class="missing">
            {{ opening === f.recipeKey ? 'Ouverture de la recette…' : f.recipeKey.startsWith('perso:') ? 'Ma recette' : 'TheMealDB' }}
          </div>
        </div>
      </div>
    </div>
    <h3 class="all-title">Toutes mes recettes</h3>
  </section>
  <p v-if="loaded && !recipes.length" class="empty-hint">
    Aucune recette perso pour l'instant — crée la première avec "+ Nouvelle recette".
  </p>

  <div class="recipe-grid">
    <div v-for="r in recipes" :key="r.id" class="recipe-card" @click="detail = fromCustom(r)">
      <FavoriteButton overlay :recipe="{ key: `perso:${r.id}`, title: r.title, imageUrl: r.imageUrl }" />
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
      <!-- Modifier / supprimer : recettes perso seulement (un favori TheMealDB s'ouvre aussi ici) -->
      <div v-if="detail.id" class="panel-actions">
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
import FavoriteButton from '../components/FavoriteButton.vue'
import RecipePanel from '../components/RecipePanel.vue'
import { formatDuration, fromCustom, fromMealDb } from '../recipes'
import { useFavorites } from '../favorites'

const recipes = ref([])
const detail = ref(null)
const loaded = ref(false)
const error = ref('')
const { favorites } = useFavorites()
// Favori en cours d'ouverture (recipeKey) : une recette TheMealDB est rechargee et traduite
const opening = ref(null)

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

async function openFavorite(favorite) {
  if (opening.value) return
  opening.value = favorite.recipeKey
  error.value = ''
  const [source, id] = favorite.recipeKey.split(':')
  try {
    detail.value = source === 'perso'
      ? fromCustom(await api.getMyRecipe(id))
      : fromMealDb(await api.getRecipeDetail(id))
  } catch (e) {
    error.value = e.response?.data?.error ?? 'Impossible de récupérer cette recette.'
  } finally {
    opening.value = null
  }
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
.favorites h3 { margin: 18px 0 0; }
.favorites .all-title { margin-top: 28px; }
.recipe-card.opening { opacity: 0.7; cursor: progress; }
</style>
