<template>
  <section class="cooked">
    <button v-if="!open" type="button" class="secondary" @click="start">🍳 J'ai cuisiné ça</button>

    <template v-else>
      <h3>🍳 Mettre à jour le frigo</h3>
      <p v-if="loading" class="muted">Recherche des ingrédients du frigo utilisés…</p>
      <p v-else-if="!rows.length" class="muted">Cette recette n'utilise aucun ingrédient de ton frigo.</p>

      <div v-for="row in rows" :key="row.item.id" class="cooked-row">
        <span class="cooked-name">
          {{ row.item.name }}<small v-if="row.item.quantity"> · {{ row.item.quantity }}</small>
        </span>
        <select v-model="row.action" :aria-label="`Que faire de ${row.item.name}`">
          <option v-if="row.next" value="step">Retirer un peu → {{ row.next }}</option>
          <option value="all">Tout utilisé (retirer du frigo)</option>
          <option value="keep">Pas touché</option>
        </select>
      </div>

      <div class="inline actions">
        <button v-if="rows.length" type="button" :disabled="saving" @click="apply">
          {{ saving ? 'Mise à jour…' : 'Mettre à jour le frigo' }}
        </button>
        <button type="button" class="secondary" :disabled="saving" @click="open = false">Annuler</button>
      </div>
    </template>

    <p v-if="message" class="ok">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import api from '../services/api'
import { stepQuantity } from '../ingredients'

const props = defineProps({
  // "mealdb:<id>" ou "perso:<id>"
  recipeKey: { type: String, required: true }
})
const emit = defineEmits(['fridge-changed'])

const open = ref(false)
const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const message = ref('')
const error = ref('')

// Action proposee par defaut : retirer un pas si la quantite est chiffree (tout si c'etait le
// dernier), rien sans quantite (sel, epices... on n'en vide pas le pot a chaque recette)
function toRow(item) {
  const next = item.quantity ? stepQuantity(item.quantity, -1) : null
  const counted = next !== null && next !== item.quantity
  const action = counted ? 'step' : item.quantity && next === null ? 'all' : 'keep'
  return { item, next: counted ? next : null, action }
}

async function start() {
  open.value = true
  loading.value = true
  message.value = ''
  error.value = ''
  try {
    rows.value = (await api.getFridgeUsage(props.recipeKey)).map(toRow)
  } catch (e) {
    error.value = e.response?.data?.error ?? 'Impossible de retrouver les ingrédients du frigo utilisés.'
    open.value = false
  } finally {
    loading.value = false
  }
}

async function apply() {
  saving.value = true
  error.value = ''
  let updated = 0
  let removed = 0
  try {
    for (const { item, next, action } of rows.value) {
      if (action === 'step') {
        await api.updateFridgeItem(item.id, { ...item, quantity: next })
        updated++
      } else if (action === 'all') {
        await api.deleteFridgeItem(item.id)
        removed++
      }
    }
    open.value = false
    rows.value = []
    message.value = updated || removed
      ? `Frigo mis à jour : ${[
        updated ? `${updated} quantité${updated > 1 ? 's' : ''} baissée${updated > 1 ? 's' : ''}` : null,
        removed ? `${removed} ingrédient${removed > 1 ? 's' : ''} retiré${removed > 1 ? 's' : ''}` : null
      ].filter(Boolean).join(', ')}.`
      : 'Rien à changer dans le frigo.'
  } catch {
    error.value = 'Le frigo n\'a été mis à jour qu\'en partie : vérifie dans « Mon frigo ».'
  } finally {
    saving.value = false
    if (updated || removed) emit('fridge-changed')
  }
}
</script>

<style scoped>
.cooked { margin-top: 18px; }
.cooked h3 { margin: 0 0 8px; }
.cooked-row {
  display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap;
  padding: 8px 0; border-bottom: 1px solid #2a3644;
}
.cooked-name { font-weight: 600; }
.cooked-name small { font-weight: 400; color: #9fb0c0; }
.cooked-row select { width: auto; max-width: 100%; min-height: 44px; padding: 8px; font-size: 14px; }
.actions { margin-top: 12px; }
.muted { color: #9fb0c0; font-size: 14px; }
.ok { color: #4ade80; font-size: 14px; margin: 10px 0 0; }
.error { color: #ff6b6b; font-size: 14px; margin: 10px 0 0; }
</style>
