<template>
  <section class="notes">
    <h3>📝 Mes notes</h3>
    <p v-if="!notes.length" class="muted">
      Aucune note pour l'instant. Garde ici tes astuces : cuisson, temps, variantes…
    </p>
    <ul v-else class="note-list">
      <li v-for="note in notes" :key="note.id" class="note">
        <div class="note-body">
          <div class="note-content">{{ note.content }}</div>
          <div class="note-date">{{ formatDate(note.createdAt) }}</div>
        </div>
        <button type="button" class="note-remove" :aria-label="'Supprimer la note'" @click="remove(note)">✕</button>
      </li>
    </ul>

    <form class="note-form" @submit.prevent="add">
      <textarea
        v-model="draft"
        rows="2"
        maxlength="2000"
        placeholder="ex : cuisson 200°C, 25 min au lieu de 35"
      ></textarea>
      <button type="submit" :disabled="!draft.trim() || saving">Ajouter la note</button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
  </section>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '../services/api'

const props = defineProps({
  // "mealdb:<id>" ou "perso:<id>"
  recipeKey: { type: String, required: true }
})

const notes = ref([])
const draft = ref('')
const saving = ref(false)
const error = ref('')

async function load() {
  error.value = ''
  try {
    notes.value = await api.getNotes(props.recipeKey)
  } catch {
    error.value = 'Impossible de charger les notes.'
  }
}

async function add() {
  if (!draft.value.trim()) return
  saving.value = true
  error.value = ''
  try {
    await api.addNote(props.recipeKey, draft.value)
    draft.value = ''
    await load()
  } catch (e) {
    error.value = e.response?.data?.error ?? "Impossible d'enregistrer la note."
  } finally {
    saving.value = false
  }
}

async function remove(note) {
  try {
    await api.deleteNote(note.id)
  } catch {
    error.value = 'Impossible de supprimer la note.'
    return
  }
  await load()
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })
}

watch(() => props.recipeKey, load, { immediate: true })
</script>

<style scoped>
.notes {
  margin-top: 20px; padding: 14px 16px; border-radius: 12px;
  background: #1f2a1c; border: 1px solid #3b4a2a;
}
.notes h3 { margin: 0 0 10px; font-size: 17px; }
.muted { color: #9fb0c0; font-size: 14px; margin: 0 0 10px; }
.note-list { list-style: none; margin: 0 0 12px; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.note {
  display: flex; gap: 10px; align-items: flex-start; justify-content: space-between;
  padding: 10px 12px; border-radius: 10px; background: #12181f; border-left: 3px solid #f5c451;
}
.note-content { white-space: pre-line; line-height: 1.45; font-size: 15px; }
.note-date { font-size: 12px; color: #9fb0c0; margin-top: 4px; }
.note-remove {
  flex-shrink: 0; width: 44px; min-height: 44px; padding: 0;
  background: transparent; border-color: #37485a; color: #9fb0c0;
}
.note-remove:hover { background: #7a2b2b; border-color: #7a2b2b; color: #fff; }
.note-form { display: flex; gap: 10px; flex-wrap: wrap; align-items: stretch; }
.note-form textarea { flex: 1; min-width: 200px; resize: vertical; font-family: inherit; }
.note-form button:disabled { opacity: 0.5; }
.error { color: #ff6b6b; margin: 8px 0 0; }
</style>
