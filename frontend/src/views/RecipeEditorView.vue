<template>
  <div class="editor">
    <router-link to="/mes-recettes" class="back">← Mes recettes</router-link>
    <h2>{{ isNew ? 'Nouvelle recette' : 'Modifier la recette' }}</h2>

    <!-- Import depuis un lien (creation seulement) : pre-remplit le formulaire -->
    <section v-if="isNew" class="card import">
      <h3>Importer depuis un lien</h3>
      <p class="muted">
        Colle le lien d'une recette (blog Thermomix, Marmiton, 750g…) : elle est lue sur la page
        pour pré-remplir le formulaire, que tu peux corriger avant d'enregistrer.
      </p>
      <form class="inline" @submit.prevent="importFromUrl">
        <input v-model="importUrl" type="url" placeholder="https://…" required style="flex:1; min-width:220px" />
        <button type="submit" :disabled="importing">{{ importing ? 'Lecture…' : 'Importer' }}</button>
      </form>
      <p v-if="importMessage" :class="importOk ? 'ok' : 'error'">{{ importMessage }}</p>
    </section>

    <form v-if="form" @submit.prevent="save">
      <section class="card">
        <label for="title">Titre</label>
        <input id="title" v-model="form.title" required maxlength="200" placeholder="ex : Soupe de carottes au cumin" />

        <div class="grid">
          <div>
            <label for="category">Catégorie</label>
            <input id="category" v-model="form.category" maxlength="80" placeholder="ex : Soupe" />
          </div>
          <div>
            <label for="servings">Personnes</label>
            <input id="servings" v-model.number="form.servings" type="number" min="1" inputmode="numeric" />
          </div>
          <div>
            <label for="time">Temps total (min)</label>
            <input id="time" v-model.number="form.totalTimeMinutes" type="number" min="1" inputmode="numeric" />
          </div>
        </div>

        <label for="image">Photo (lien, optionnel)</label>
        <input id="image" v-model="form.imageUrl" type="url" maxlength="1000" placeholder="https://…" />

        <label class="tm-toggle">
          <input v-model="form.thermomix" type="checkbox" />
          <span>Recette Thermomix <small>(réglage temps / température / vitesse par étape)</small></span>
        </label>
      </section>

      <section class="card">
        <h3>Ingrédients</h3>
        <div v-for="(ingredient, i) in form.ingredients" :key="i" class="row">
          <input v-model="ingredient.quantity" class="qty" maxlength="80" placeholder="Quantité" aria-label="Quantité" />
          <input v-model="ingredient.name" class="name" maxlength="200" placeholder="Ingrédient (ex : Carottes)" aria-label="Ingrédient" />
          <button type="button" class="secondary icon" aria-label="Retirer l'ingrédient" @click="form.ingredients.splice(i, 1)">✕</button>
        </div>
        <button type="button" class="secondary" @click="form.ingredients.push({ name: '', quantity: '' })">+ Ingrédient</button>
      </section>

      <section class="card">
        <h3>Étapes</h3>
        <div v-for="(step, i) in form.steps" :key="i" class="step">
          <div class="step-head">
            <span class="step-num">{{ i + 1 }}</span>
            <button type="button" class="secondary icon" aria-label="Monter" :disabled="i === 0" @click="move(i, -1)">↑</button>
            <button type="button" class="secondary icon" aria-label="Descendre" :disabled="i === form.steps.length - 1" @click="move(i, 1)">↓</button>
            <button type="button" class="secondary icon" aria-label="Retirer l'étape" @click="form.steps.splice(i, 1)">✕</button>
          </div>
          <textarea v-model="step.instruction" rows="3" placeholder="ex : Ajouter les carottes et l'eau."></textarea>
          <div v-if="form.thermomix" class="tm-row">
            <input v-model="step.tmTime" class="tm-time" maxlength="20" placeholder="Temps (ex : 20 min)" aria-label="Temps" />
            <select v-model="step.tmTemperature" aria-label="Température">
              <option value="">Sans chauffe</option>
              <option v-for="t in TM_TEMPERATURES" :key="t" :value="t">{{ t }}</option>
            </select>
            <select v-model="step.tmSpeed" aria-label="Vitesse">
              <option value="">Vitesse —</option>
              <option v-for="s in TM_SPEEDS" :key="s" :value="s">{{ s === 'cuillère' || s === 'Turbo' ? s : `vitesse ${s}` }}</option>
            </select>
            <label class="reverse"><input v-model="step.tmReverse" type="checkbox" /> ↺ sens inverse</label>
          </div>
        </div>
        <button type="button" class="secondary" @click="form.steps.push(emptyStep())">+ Étape</button>
      </section>

      <p v-if="error" class="error">{{ error }}</p>
      <div class="inline actions">
        <router-link to="/mes-recettes"><button type="button" class="secondary">Annuler</button></router-link>
        <button type="submit" :disabled="saving">{{ saving ? 'Enregistrement…' : 'Enregistrer la recette' }}</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../services/api'
import { TM_SPEEDS, TM_TEMPERATURES } from '../recipes'

const route = useRoute()
const router = useRouter()

const isNew = computed(() => !route.params.id)
const form = ref(null)
const error = ref('')
const saving = ref(false)
const importUrl = ref('')
const importing = ref(false)
const importMessage = ref('')
const importOk = ref(false)

function emptyStep() {
  return { instruction: '', tmTime: '', tmTemperature: '', tmSpeed: '', tmReverse: false }
}

function emptyRecipe() {
  return {
    title: '', category: '', servings: null, totalTimeMinutes: null, thermomix: false,
    imageUrl: '', sourceUrl: '',
    ingredients: [{ name: '', quantity: '' }],
    steps: [emptyStep()]
  }
}

// Champs null -> '' pour les inputs, et au moins une ligne d'ingredient / d'etape
function toForm(recipe) {
  return {
    ...recipe,
    category: recipe.category ?? '',
    imageUrl: recipe.imageUrl ?? '',
    sourceUrl: recipe.sourceUrl ?? '',
    ingredients: recipe.ingredients.length
      ? recipe.ingredients.map(i => ({ name: i.name, quantity: i.quantity ?? '' }))
      : [{ name: '', quantity: '' }],
    steps: recipe.steps.length
      ? recipe.steps.map(s => ({
        instruction: s.instruction,
        tmTime: s.tmTime ?? '',
        tmTemperature: s.tmTemperature ?? '',
        tmSpeed: s.tmSpeed ?? '',
        tmReverse: s.tmReverse
      }))
      : [emptyStep()]
  }
}

function move(index, direction) {
  const steps = form.value.steps
  const [step] = steps.splice(index, 1)
  steps.splice(index + direction, 0, step)
}

async function importFromUrl() {
  importing.value = true
  importMessage.value = ''
  try {
    const recipe = await api.importRecipe(importUrl.value)
    form.value = toForm(recipe)
    importOk.value = true
    importMessage.value = `« ${recipe.title} » importée : vérifie et corrige si besoin, puis enregistre.`
  } catch (e) {
    importOk.value = false
    importMessage.value = e.response?.data?.error ?? 'Import impossible.'
  } finally {
    importing.value = false
  }
}

async function save() {
  error.value = ''
  // Lignes vides ignorees ; nombres vides -> null
  const payload = {
    ...form.value,
    servings: form.value.servings || null,
    totalTimeMinutes: form.value.totalTimeMinutes || null,
    ingredients: form.value.ingredients.filter(i => i.name.trim()),
    steps: form.value.steps.filter(s => s.instruction.trim())
  }
  saving.value = true
  try {
    if (isNew.value) {
      await api.createMyRecipe(payload)
    } else {
      await api.updateMyRecipe(route.params.id, payload)
    }
    router.push('/mes-recettes')
  } catch (e) {
    error.value = e.response?.data?.error ?? "Impossible d'enregistrer : vérifie le titre et les champs."
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (isNew.value) {
    form.value = emptyRecipe()
    return
  }
  try {
    form.value = toForm(await api.getMyRecipe(route.params.id))
  } catch {
    error.value = 'Recette introuvable.'
  }
})
</script>

<style scoped>
.editor { max-width: 820px; margin: 0 auto; }
.back { display: inline-block; margin-bottom: 8px; font-size: 14px; }
.editor h2 { margin: 0 0 16px; }
.card h3 { margin: 0 0 12px; }
.card label:first-child { margin-top: 0; }
.muted { color: #9fb0c0; font-size: 14px; margin: 0 0 12px; }
.ok { color: #4ade80; margin: 10px 0 0; }
.error { color: #ff6b6b; margin: 10px 0 0; }

.grid { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 10px; }

.tm-toggle {
  display: flex; align-items: center; gap: 10px; margin-top: 16px;
  padding: 12px; border-radius: 10px; background: #2a1f2e; border: 1px solid #4a2f4e;
  font-size: 15px; color: #f0abfc; cursor: pointer;
}
.tm-toggle input, .reverse input { width: 22px; min-height: 22px; height: 22px; }
.tm-toggle small { color: #9fb0c0; }

.row { display: flex; gap: 8px; margin-bottom: 8px; }
.row .qty { flex: 0 0 140px; }
.row .name { flex: 1; }
button.icon { width: 48px; padding: 0; flex-shrink: 0; }
button.icon:disabled { opacity: 0.35; }

.step { padding: 12px; margin-bottom: 10px; border-radius: 10px; background: #12181f; border: 1px solid #2a3644; }
.step-head { display: flex; gap: 6px; align-items: center; margin-bottom: 8px; }
.step-num {
  display: inline-flex; align-items: center; justify-content: center; margin-right: auto;
  width: 28px; height: 28px; border-radius: 50%; background: #2b6cb0; font-weight: 700; font-size: 14px;
}
.step textarea { resize: vertical; font-family: inherit; }
.tm-row { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; align-items: center; }
.tm-row .tm-time, .tm-row select { flex: 1; min-width: 130px; width: auto; }
.reverse { display: flex; align-items: center; gap: 8px; margin: 0; padding: 0 6px; font-size: 14px; color: #f0abfc; white-space: nowrap; cursor: pointer; }

.actions { justify-content: flex-end; margin: 6px 0 30px; }

@media (max-width: 560px) {
  .grid { grid-template-columns: 1fr 1fr; }
  .grid > div:first-child { grid-column: 1 / -1; }
  .row .qty { flex-basis: 100px; }
}
</style>
