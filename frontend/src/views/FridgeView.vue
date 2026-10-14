<template>
  <section class="hero">
    <svg class="hero-art" viewBox="0 0 160 120" aria-hidden="true" focusable="false">
      <!-- Frigo ouvert, etageres garnies -->
      <circle cx="22" cy="24" r="3" fill="#fde68a" />
      <circle cx="144" cy="12" r="2.5" fill="#fde68a" />
      <circle cx="150" cy="96" r="2" fill="#fde68a" />
      <rect x="40" y="8" width="64" height="104" rx="8" fill="#e2e8f0" />
      <rect x="46" y="14" width="52" height="92" rx="4" fill="#1e293b" />
      <path d="M46 40 H98 M46 68 H98" stroke="#94a3b8" stroke-width="2" />
      <!-- etagere du haut : lait, pomme, oeufs -->
      <rect x="53" y="18" width="5" height="4" rx="1" fill="#60a5fa" />
      <rect x="51" y="22" width="9" height="18" rx="3" fill="#f8fafc" />
      <circle cx="72" cy="34" r="6" fill="#ef4444" />
      <path d="M72 28 C72 25 73 24 75 23" stroke="#7c4a1e" stroke-width="1.6" fill="none" stroke-linecap="round" />
      <ellipse cx="85" cy="35" rx="4" ry="5" fill="#fef3c7" />
      <ellipse cx="93" cy="35" rx="4" ry="5" fill="#fde68a" />
      <!-- etagere du milieu : fromage, tomate, bocal -->
      <path d="M50 68 L68 68 L68 55 Z" fill="#f5c451" />
      <circle cx="63" cy="64" r="1.6" fill="#e0a526" />
      <circle cx="79" cy="61" r="7" fill="#f87171" />
      <path d="M76 54.5 L79 56.5 L82 54.5" stroke="#4ade80" stroke-width="2" fill="none" stroke-linecap="round" />
      <rect x="88" y="52" width="8" height="16" rx="2" fill="#fb923c" />
      <!-- bac a legumes -->
      <circle cx="58" cy="86" r="6" fill="#4ade80" />
      <circle cx="68" cy="84" r="7" fill="#22c55e" />
      <circle cx="80" cy="86" r="6" fill="#4ade80" />
      <rect x="48" y="86" width="48" height="18" rx="3" fill="#334155" />
      <!-- porte ouverte -->
      <path d="M104 10 L130 18 V102 L104 110 Z" fill="#cbd5e1" />
      <path d="M108 40 L126 44 M108 72 L126 74" stroke="#94a3b8" stroke-width="2" />
      <rect x="122" y="50" width="3" height="20" rx="1.5" fill="#64748b" />
    </svg>
    <div class="hero-text">
      <h2>Mon frigo</h2>
      <p>
        Ce que tu as dans le frigo et le placard. La liste reste d'une fois sur
        l'autre : ajoute au fil des courses, retire quand c'est fini.
      </p>
      <div v-if="items.length" class="hero-stats">
        <span>{{ items.length }} ingrédient{{ items.length > 1 ? 's' : '' }}</span>
        <span>{{ sections.length }} catégorie{{ sections.length > 1 ? 's' : '' }}</span>
      </div>
    </div>
  </section>

  <section v-if="expiringSoon.length" class="expiring">
    <div class="expiring-head">
      <h3>⏳ À consommer vite</h3>
      <router-link to="/?filtre=peremption" class="expiring-link">Voir les recettes qui les utilisent →</router-link>
    </div>
    <div class="expiring-list">
      <span v-for="item in expiringSoon" :key="item.id" class="expiring-chip" :class="expiryLevel(item.expiresOn)">
        <strong>{{ item.name }}</strong> · {{ expiryLabel(item.expiresOn) }}
      </span>
    </div>
  </section>

  <AddIngredientForm :existing-items="items" @added="load" />

  <p v-if="error" class="error">{{ error }}</p>
  <p v-else-if="loaded && !items.length" class="empty-hint">Ton frigo est vide pour l'instant.</p>

  <section
    v-for="section in sections"
    :key="section.key"
    class="category-section"
    :style="{ '--cat-color': section.color }"
  >
    <header class="category-header">
      <span class="category-badge"><CategoryIcon :name="section.key" /></span>
      <h3>{{ section.label }}</h3>
      <span class="category-count">{{ section.items.length }}</span>
    </header>

    <div class="item-grid">
      <div v-for="item in section.items" :key="item.id" class="item-card">
        <div class="item-text">
          <div class="item-name">{{ item.name }}</div>
          <div class="item-meta">
            <span v-if="parseQuantity(item.quantity)" class="item-stepper">
              <button type="button" :aria-label="`Retirer un peu de ${item.name}`" :disabled="busyIds.has(item.id)" @click="changeQuantity(item, -1)">−</button>
              <span class="item-quantity">{{ item.quantity }}</span>
              <button type="button" :aria-label="`Ajouter un peu de ${item.name}`" :disabled="busyIds.has(item.id)" @click="changeQuantity(item, 1)">+</button>
            </span>
            <span v-else-if="item.quantity" class="item-quantity">{{ item.quantity }}</span>
            <span v-if="section.key === 'autres' && item.category">{{ item.category }}</span>
            <span v-if="editingExpiryId === item.id" class="expiry-edit">
              <input
                type="date"
                :value="item.expiresOn ?? ''"
                :aria-label="`Date de péremption de ${item.name}`"
                @change="setExpiry(item, $event.target.value || null)"
              />
              <button v-if="item.expiresOn" type="button" class="expiry-clear" @click="setExpiry(item, null)">Sans date</button>
            </span>
            <button
              v-else
              type="button"
              class="expiry-badge"
              :class="expiryLevel(item.expiresOn)"
              :aria-label="`Date de péremption de ${item.name}`"
              @click="editingExpiryId = item.id"
            >
              {{ item.expiresOn ? expiryLabel(item.expiresOn) : '📅' }}
            </button>
          </div>
        </div>
        <button type="button" class="remove-btn" :aria-label="`Retirer ${item.name}`" @click="remove(item)">✕</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import AddIngredientForm from '../components/AddIngredientForm.vue'
import CategoryIcon from '../components/CategoryIcon.vue'
import { groupByCategory } from '../categories'
import { parseQuantity, stepQuantity } from '../ingredients'
import { daysUntil, expiryLabel, expiryLevel, isExpiringSoon } from '../expiry'

const items = ref([])
const loaded = ref(false)
const error = ref('')
// Ingredients dont la quantite est en cours d'enregistrement : evite deux -/+ partis de la meme valeur
const busyIds = reactive(new Set())

// Ingredient dont la date de peremption est en cours de modification
const editingExpiryId = ref(null)

const sections = computed(() => groupByCategory(items.value))
// Perimes ou perimant bientot, le plus urgent d'abord
const expiringSoon = computed(() => items.value
  .filter(isExpiringSoon)
  .sort((a, b) => daysUntil(a.expiresOn) - daysUntil(b.expiresOn)))

async function load() {
  try {
    items.value = await api.getFridgeItems()
    error.value = ''
  } catch {
    error.value = 'Impossible de charger le frigo pour le moment.'
  } finally {
    loaded.value = true
  }
}

async function remove(item) {
  try {
    await api.deleteFridgeItem(item.id)
  } catch {
    error.value = `Impossible de retirer « ${item.name} ».`
    return
  }
  await load()
}

// Enregistre l'ingredient avec les champs modifies (le PUT remplace tout : on renvoie le reste tel quel)
async function save(item, changes, errorMessage) {
  busyIds.add(item.id)
  try {
    const updated = await api.updateFridgeItem(item.id, {
      name: item.name,
      quantity: item.quantity,
      category: item.category,
      expiresOn: item.expiresOn,
      ...changes
    })
    Object.assign(item, updated)
  } catch {
    error.value = errorMessage
  } finally {
    busyIds.delete(item.id)
  }
}

// -/+ sur la quantite : arrive a zero, l'ingredient est retire du frigo
async function changeQuantity(item, direction) {
  const quantity = stepQuantity(item.quantity, direction)
  if (quantity === null) {
    await remove(item)
    return
  }
  await save(item, { quantity }, `Impossible de modifier la quantité de « ${item.name} ».`)
}

async function setExpiry(item, expiresOn) {
  editingExpiryId.value = null
  await save(item, { expiresOn }, `Impossible de modifier la date de « ${item.name} ».`)
}

onMounted(load)
</script>

<style scoped>
/* Bandeau d'accueil : illustration du frigo et resume de l'inventaire */
.hero {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  margin-bottom: 18px;
  border-radius: 16px;
  border: 1px solid #2a3644;
  background: linear-gradient(120deg, #1f3a2b 0%, #1a2e45 60%, #2b2346 100%);
}
.hero-art { width: 150px; flex-shrink: 0; }
.hero-text h2 { margin: 0 0 6px; font-size: 26px; }
.hero-text p { margin: 0; color: #c3d0dc; line-height: 1.45; max-width: 560px; }
.hero-stats { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px; }
.hero-stats span {
  font-size: 13px; font-weight: 600; padding: 4px 10px; border-radius: 999px;
  background: rgba(18, 24, 31, 0.55); color: #4ade80;
}

/* Une section par categorie */
.category-section { margin-bottom: 26px; }
.category-header {
  display: flex; align-items: center; gap: 12px;
  padding-bottom: 10px; margin-bottom: 12px;
  border-bottom: 2px solid color-mix(in srgb, var(--cat-color) 35%, transparent);
}
.category-header h3 { margin: 0; font-size: 19px; }
.category-badge {
  display: inline-flex; align-items: center; justify-content: center;
  width: 46px; height: 46px; border-radius: 12px; flex-shrink: 0;
  background: color-mix(in srgb, var(--cat-color) 16%, #1a2330);
  border: 1px solid color-mix(in srgb, var(--cat-color) 40%, transparent);
}
.category-badge :deep(.category-icon) { width: 34px; height: 34px; }
.category-count {
  margin-left: auto; min-width: 28px; text-align: center;
  font-size: 13px; font-weight: 700; padding: 3px 9px; border-radius: 999px;
  background: color-mix(in srgb, var(--cat-color) 18%, #1a2330); color: var(--cat-color);
}

.item-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 12px;
}
.item-card {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  padding: 10px 10px 10px 16px; border-radius: 12px;
  background: #1a2330; border: 1px solid #2a3644;
  border-left: 4px solid var(--cat-color);
}
.item-text { min-width: 0; }
.item-name { font-weight: 600; font-size: 16px; overflow-wrap: anywhere; }
.item-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 4px; font-size: 12px; color: #9fb0c0; }
.item-quantity {
  padding: 1px 8px; border-radius: 999px; font-weight: 600;
  background: color-mix(in srgb, var(--cat-color) 16%, #1a2330); color: var(--cat-color);
}
.item-stepper { display: inline-flex; align-items: center; gap: 6px; }
.item-stepper button {
  min-height: 34px; width: 34px; padding: 0; font-size: 18px; line-height: 1;
  background: #22303f; border-color: #37485a; color: #e6ecf1;
}
.item-stepper button:disabled { opacity: 0.5; }
.item-stepper button:hover { border-color: var(--cat-color); }
.error { color: #ff6b6b; text-align: center; }

/* A consommer vite */
.expiring {
  margin-bottom: 18px; padding: 14px 16px; border-radius: 14px;
  background: #2e2416; border: 1px solid #6b4f1d;
}
.expiring-head { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; flex-wrap: wrap; }
.expiring-head h3 { margin: 0 0 10px; font-size: 17px; }
.expiring-link { font-size: 14px; font-weight: 600; color: #fbbf24; }
.expiring-list { display: flex; flex-wrap: wrap; gap: 8px; }
.expiring-chip { padding: 6px 12px; border-radius: 999px; font-size: 14px; background: #3d2f17; color: #fde68a; }
.expiring-chip.expired { background: #4a1f1f; color: #fca5a5; }

/* Date de peremption sur une carte */
.expiry-badge {
  min-height: 28px; padding: 1px 9px; border-radius: 999px; font-size: 12px; font-weight: 600;
  background: transparent; border: 1px dashed #37485a; color: #9fb0c0;
}
.expiry-badge.ok { border-style: solid; }
.expiry-badge.soon { border: 1px solid #b45309; background: #3d2f17; color: #fbbf24; }
.expiry-badge.expired { border: 1px solid #b91c1c; background: #4a1f1f; color: #fca5a5; }
.expiry-edit { display: inline-flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.expiry-edit input { min-height: 34px; padding: 4px 8px; font-size: 14px; width: auto; }
.expiry-clear { min-height: 34px; padding: 4px 10px; font-size: 13px; background: #22303f; border-color: #37485a; }
.remove-btn {
  flex-shrink: 0; width: 48px; padding: 0;
  background: #22303f; border-color: #37485a; color: #9fb0c0;
}
.remove-btn:hover { background: #7a2b2b; border-color: #7a2b2b; color: #fff; }

@media (max-width: 560px) {
  .hero { flex-direction: column; text-align: center; padding: 18px 16px; }
  .hero-art { width: 120px; }
  .hero-stats { justify-content: center; }
}
</style>
