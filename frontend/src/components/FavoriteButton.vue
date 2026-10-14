<template>
  <button
    type="button"
    class="fav-btn"
    :class="{ on: favorite, 'on-card': onCard }"
    :aria-pressed="favorite"
    :aria-label="favorite ? `Retirer ${recipe.title} des favoris` : `Ajouter ${recipe.title} aux favoris`"
    :title="favorite ? 'Retirer des favoris' : 'Ajouter aux favoris'"
    :disabled="saving"
    @click.stop="onClick"
  >
    {{ favorite ? '♥' : '♡' }}
  </button>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useFavorites } from '../favorites'

const props = defineProps({
  // { key: "mealdb:<id>" | "perso:<id>", title, imageUrl }
  recipe: { type: Object, required: true },
  // Pose sur la photo d'une carte plutot que dans le flux
  onCard: { type: Boolean, default: false }
})

const { isFavorite, toggle } = useFavorites()
const favorite = computed(() => isFavorite(props.recipe.key))
const saving = ref(false)

async function onClick() {
  saving.value = true
  try {
    await toggle(props.recipe)
  } catch {
    // le coeur garde simplement son etat precedent
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.fav-btn {
  flex-shrink: 0; width: 48px; padding: 0; font-size: 24px; line-height: 1;
  background: #22303f; border-color: #37485a; color: #9fb0c0;
}
.fav-btn.on { color: #f472b6; border-color: #9d3a6e; }
.fav-btn.on-card {
  position: absolute; top: 8px; right: 8px; width: 44px; min-height: 44px; border-radius: 999px;
  background: rgba(18, 24, 31, 0.75); border-color: transparent;
}
.fav-btn:disabled { opacity: 0.6; }
</style>
