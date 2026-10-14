<template>
  <form class="add-form" @submit.prevent="submit">
    <h3>Ajouter un ingrédient</h3>

    <!-- 1. Categorie -->
    <div class="step"><span class="step-num">1</span>Catégorie</div>
    <div class="chip-row">
      <button
        v-for="c in CATEGORIES"
        :key="c.key"
        type="button"
        class="category-chip"
        :class="{ selected: c.key === categoryKey }"
        :style="{ '--cat-color': c.color }"
        :aria-pressed="c.key === categoryKey"
        @click="chooseCategory(c.key)"
      >
        <CategoryIcon :name="c.key" />
        {{ c.label }}
      </button>
    </div>

    <!-- 2. Ingredient de la categorie, ou saisi a la main -->
    <template v-if="category">
      <div class="step"><span class="step-num">2</span>Ingrédient</div>
      <input
        v-model="search"
        class="search"
        maxlength="80"
        :placeholder="catalog.length ? `Rechercher ou saisir (ex: ${catalog[0].name})` : 'Nom de l\'ingrédient'"
      />
      <div class="chip-row" :style="{ '--cat-color': category.color }">
        <button
          v-for="i in filteredCatalog"
          :key="i.name"
          type="button"
          class="ingredient-chip"
          :class="{ selected: i.name === name, owned: inFridge(i.name) }"
          :disabled="inFridge(i.name)"
          :title="inFridge(i.name) ? 'Déjà dans le frigo' : ''"
          @click="chooseIngredient(i.name, i.unit)"
        >
          <span v-if="inFridge(i.name)" aria-hidden="true">✓ </span>{{ i.name }}
        </button>
        <button
          v-if="customName"
          type="button"
          class="ingredient-chip custom"
          :class="{ selected: customName === name }"
          @click="chooseIngredient(customName, defaultUnitOf(categoryKey))"
        >
          + « {{ customName }} »
        </button>
      </div>
      <p v-if="customName && inFridge(customName)" class="hint">« {{ customName }} » est déjà dans le frigo.</p>
    </template>

    <!-- 3. Quantite et unite -->
    <template v-if="name">
      <div class="step"><span class="step-num">3</span>Quantité <small>(optionnel)</small></div>
      <div class="quantity-row">
        <div class="stepper">
          <button type="button" class="secondary" aria-label="Diminuer" @click="changeAmount(-1)">−</button>
          <input v-model="amount" type="number" inputmode="decimal" min="0" step="any" placeholder="—" aria-label="Quantité" />
          <button type="button" class="secondary" aria-label="Augmenter" @click="changeAmount(1)">+</button>
        </div>
        <select v-model="unit" class="unit" aria-label="Unité">
          <option v-for="u in UNITS" :key="u.value" :value="u.value">{{ u.label }}</option>
        </select>
        <label class="expiry-field">
          <span>Périme le <small>(optionnel)</small></span>
          <input v-model="expiresOn" type="date" />
        </label>
        <button type="submit" class="submit" :disabled="saving">+ Ajouter {{ name }}</button>
      </div>
    </template>

    <p v-if="error" class="error">{{ error }}</p>
  </form>
</template>

<script setup>
import { computed, ref } from 'vue'
import api from '../services/api'
import CategoryIcon from './CategoryIcon.vue'
import { CATEGORIES } from '../categories'
import { STEP_BY_UNIT, UNITS, defaultUnitOf, formatQuantity, ingredientsOf, normalizeName, sameIngredient } from '../ingredients'

const props = defineProps({
  // Ingredients deja dans le frigo : grises dans la liste (le nom est unique en base)
  existingItems: { type: Array, required: true }
})
const emit = defineEmits(['added'])

const categoryKey = ref(null)
const search = ref('')
const name = ref('')
const amount = ref('')
const unit = ref('pièce')
const expiresOn = ref('')
const error = ref('')
const saving = ref(false)

const category = computed(() => CATEGORIES.find(c => c.key === categoryKey.value))
const catalog = computed(() => ingredientsOf(categoryKey.value))
const filteredCatalog = computed(() => {
  const query = normalizeName(search.value)
  return catalog.value.filter(i => normalizeName(i.name).includes(query))
})
// Texte saisi qui ne correspond exactement a aucun ingredient propose : ajout libre
const customName = computed(() => {
  const typed = search.value.trim()
  if (!typed) return ''
  const exists = catalog.value.some(i => normalizeName(i.name) === normalizeName(typed))
  return exists ? '' : typed.charAt(0).toUpperCase() + typed.slice(1)
})

function inFridge(ingredientName) {
  return props.existingItems.some(item => sameIngredient(item.name, ingredientName))
}

function chooseCategory(key) {
  categoryKey.value = key
  search.value = ''
  name.value = ''
  amount.value = ''
  error.value = ''
}

function chooseIngredient(ingredientName, defaultUnit) {
  name.value = ingredientName
  unit.value = defaultUnit
  amount.value = ''
  error.value = ''
}

function changeAmount(direction) {
  const step = STEP_BY_UNIT[unit.value] ?? 1
  const current = Number(amount.value) || 0
  const next = Math.max(0, Math.round((current + direction * step) * 100) / 100)
  amount.value = next === 0 ? '' : next
}

async function submit() {
  if (!name.value) return
  if (inFridge(name.value)) {
    error.value = `« ${name.value} » est déjà dans le frigo.`
    return
  }
  error.value = ''
  saving.value = true
  try {
    await api.createFridgeItem({
      name: name.value,
      quantity: formatQuantity(amount.value, unit.value),
      category: categoryKey.value === 'autres' ? null : category.value.label,
      expiresOn: expiresOn.value || null
    })
    // On garde la categorie choisie : on ajoute souvent plusieurs legumes d'affilee
    search.value = ''
    name.value = ''
    amount.value = ''
    expiresOn.value = ''
    emit('added')
  } catch (e) {
    error.value = e.response?.data?.error ?? "Erreur lors de l'ajout (cet ingrédient existe peut-être déjà)."
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.add-form {
  background: #1a2330; border: 1px solid #2a3644; border-radius: 14px;
  padding: 16px 18px; margin-bottom: 26px;
}
.add-form h3 { margin: 0 0 4px; font-size: 17px; }

.step {
  display: flex; align-items: center; gap: 8px;
  margin: 16px 0 10px; font-size: 14px; font-weight: 600; color: #9fb0c0;
}
.step small { font-weight: 400; }
.step-num {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; border-radius: 50%;
  background: #2b6cb0; color: #fff; font-size: 12px;
}

.chip-row { display: flex; flex-wrap: wrap; gap: 8px; }

.category-chip {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 6px 14px 6px 8px; border-radius: 999px;
  background: #22303f; border: 1px solid #37485a; color: #e6ecf1; font-size: 15px;
}
.category-chip :deep(.category-icon) { width: 30px; height: 30px; }
.category-chip.selected {
  border-color: var(--cat-color); font-weight: 700;
  background: color-mix(in srgb, var(--cat-color) 20%, #1a2330);
}

.search { margin-bottom: 10px; }

.ingredient-chip {
  min-height: 44px; padding: 8px 14px; border-radius: 999px; font-size: 15px;
  background: #22303f; border: 1px solid #37485a; color: #e6ecf1;
}
.ingredient-chip:hover:not(:disabled) { border-color: var(--cat-color); }
.ingredient-chip.selected {
  background: var(--cat-color); border-color: var(--cat-color); color: #12181f; font-weight: 700;
}
.ingredient-chip.owned { opacity: 0.45; cursor: default; }
.ingredient-chip.custom { border-style: dashed; }

.quantity-row { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
.stepper { display: flex; gap: 6px; }
.stepper button { width: 48px; padding: 0; font-size: 22px; }
.stepper input { width: 96px; text-align: center; }
.unit { width: auto; min-width: 140px; }
.expiry-field { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #9fb0c0; }
.expiry-field input { width: auto; }
.submit { margin-left: auto; }
.submit:disabled { opacity: 0.6; }

.hint { margin: 8px 0 0; font-size: 13px; color: #9fb0c0; }
.error { color: #ff6b6b; margin: 12px 0 0; }

@media (max-width: 560px) {
  .submit { margin-left: 0; width: 100%; }
}
</style>
