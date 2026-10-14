<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="panel recipe-panel">
      <div class="panel-header">
        <h2>{{ recipe.title }}</h2>
        <div class="header-actions">
          <FavoriteButton :recipe="recipe" />
          <button class="secondary" aria-label="Fermer" @click="emit('close')">✕</button>
        </div>
      </div>

      <img v-if="recipe.imageUrl" :src="recipe.imageUrl" :alt="recipe.title" />

      <div class="tags">
        <span v-if="recipe.thermomix" class="tag tm">Thermomix</span>
        <span v-for="tag in recipe.tags" :key="tag" class="tag">{{ tag }}</span>
      </div>

      <slot name="actions" />

      <h3>Ingrédients</h3>
      <div v-for="(line, i) in recipe.ingredients" :key="i" class="ingredient-line">
        <span>{{ line.name }}</span>
        <span class="quantity">{{ line.quantity }}</span>
      </div>
      <p v-if="!recipe.ingredients.length" class="muted">Aucun ingrédient renseigné.</p>

      <div v-if="shoppingCandidates.length" class="shopping">
        <button type="button" class="secondary" :disabled="addingToList" @click="addToShoppingList">
          🛒 {{ missing ? `Ajouter ${shoppingCandidates.length > 1 ? `les ${shoppingCandidates.length} manquants` : 'le manquant'} à la liste de courses` : 'Ajouter les ingrédients à la liste de courses' }}
        </button>
        <p v-if="shoppingMessage" class="shopping-message">
          {{ shoppingMessage }} <router-link to="/courses">Voir la liste →</router-link>
        </p>
      </div>

      <h3>Préparation</h3>
      <ol v-if="recipe.steps" class="steps">
        <li v-for="(step, i) in recipe.steps" :key="i">
          <span v-if="formatTm(step)" class="tm-setting">{{ formatTm(step) }}</span>
          <div class="step-text">{{ step.instruction }}</div>
        </li>
      </ol>
      <p v-else class="instructions">{{ recipe.instructions }}</p>
      <p v-if="recipe.note" class="lang-note">{{ recipe.note }}</p>

      <div v-if="recipe.videoUrl || recipe.sourceUrl" class="links">
        <a v-if="recipe.videoUrl" :href="recipe.videoUrl" target="_blank" rel="noopener"><button type="button">▶ Vidéo</button></a>
        <a v-if="recipe.sourceUrl" :href="recipe.sourceUrl" target="_blank" rel="noopener"><button type="button" class="secondary">Source</button></a>
      </div>

      <CookedSection :recipe-key="recipe.key" @fridge-changed="emit('fridge-changed')" />

      <RecipeNotes :recipe-key="recipe.key" />
    </div>
  </div>
</template>

<script setup>
import CookedSection from './CookedSection.vue'
import FavoriteButton from './FavoriteButton.vue'
import RecipeNotes from './RecipeNotes.vue'
import { computed, ref } from 'vue'
import api from '../services/api'
import { formatTm } from '../recipes'

const props = defineProps({
  // Recette au format de fromMealDb / fromCustom (src/recipes.js)
  recipe: { type: Object, required: true },
  // Noms des ingredients qui manquent dans le frigo (suggestions) ; null = inconnu
  missing: { type: Array, default: null }
})
const emit = defineEmits(['close', 'fridge-changed'])

const addingToList = ref(false)
const shoppingMessage = ref('')

// Ce qui part dans la liste de courses : les manquants si on les connait, sinon tous les ingredients
const shoppingCandidates = computed(() => {
  const names = props.missing ?? props.recipe.ingredients.map(i => i.name)
  return names.map(name => {
    const quantity = props.recipe.ingredients.find(i => i.name === name)?.quantity
    return {
      name: name.slice(0, 80),
      quantity: quantity ? quantity.slice(0, 80) : null,
      recipeTitle: props.recipe.title.slice(0, 300)
    }
  })
})

async function addToShoppingList() {
  addingToList.value = true
  shoppingMessage.value = ''
  try {
    const added = await api.addShoppingItems(shoppingCandidates.value)
    shoppingMessage.value = added.length
      ? `${added.length} article${added.length > 1 ? 's' : ''} ajouté${added.length > 1 ? 's' : ''}.`
      : 'Tout est déjà dans la liste.'
  } catch {
    shoppingMessage.value = "Impossible d'ajouter à la liste de courses."
  } finally {
    addingToList.value = false
  }
}
</script>

<style scoped>
.panel-header h2 { margin: 0; }
.shopping { margin-top: 12px; }
.shopping-message { margin: 8px 0 0; font-size: 14px; color: #4ade80; }
.header-actions { display: flex; gap: 8px; flex-shrink: 0; margin-left: 10px; }
.tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 6px; }
.tag {
  font-size: 12px; padding: 3px 10px; border-radius: 999px;
  background: #1f3a2b; color: #4ade80;
}
.tag.tm { background: #3a1f3a; color: #f0abfc; font-weight: 700; }
h3 { margin: 18px 0 8px; }
.quantity { color: #9fb0c0; text-align: right; }
.muted { color: #9fb0c0; font-size: 14px; }
.steps { padding-left: 22px; margin: 0; display: flex; flex-direction: column; gap: 12px; }
.steps li { line-height: 1.5; font-size: 15px; }
.step-text { white-space: pre-line; }
.tm-setting {
  display: inline-block; margin-bottom: 4px; padding: 2px 10px; border-radius: 6px;
  font-size: 13px; font-weight: 700; background: #3a1f3a; color: #f0abfc;
}
.links { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 14px; }
</style>
