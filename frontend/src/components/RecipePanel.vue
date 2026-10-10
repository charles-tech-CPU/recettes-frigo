<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="panel recipe-panel">
      <div class="panel-header">
        <h2>{{ recipe.title }}</h2>
        <button class="secondary" aria-label="Fermer" @click="emit('close')">✕</button>
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

      <RecipeNotes :recipe-key="recipe.key" />
    </div>
  </div>
</template>

<script setup>
import RecipeNotes from './RecipeNotes.vue'
import { formatTm } from '../recipes'

defineProps({
  // Recette au format de fromMealDb / fromCustom (src/recipes.js)
  recipe: { type: Object, required: true }
})
const emit = defineEmits(['close'])
</script>

<style scoped>
.panel-header h2 { margin: 0; }
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
