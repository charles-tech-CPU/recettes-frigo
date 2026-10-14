<template>
  <h2 style="margin:0">Liste de courses</h2>
  <p class="intro">
    Coche au fur et à mesure dans le magasin, puis range tes achats dans le frigo d'un seul geste.
    Depuis une recette, le bouton 🛒 ajoute ce qui te manque.
  </p>

  <form class="add-row" @submit.prevent="addManual">
    <input v-model="newName" class="name" maxlength="80" placeholder="Article (ex : Lait)" aria-label="Article" />
    <input v-model="newQuantity" class="qty" maxlength="80" placeholder="Quantité" aria-label="Quantité" />
    <button type="submit" :disabled="!newName.trim() || adding">+ Ajouter</button>
  </form>

  <p v-if="error" class="error">{{ error }}</p>
  <p v-if="message" class="ok">{{ message }}</p>
  <p v-if="loaded && !items.length && !error" class="empty-hint">
    Liste vide. Ajoute un article ici, ou les ingrédients manquants depuis une recette.
  </p>

  <ul v-if="toBuy.length" class="list">
    <li v-for="item in toBuy" :key="item.id" class="article">
      <label>
        <input type="checkbox" :checked="item.checked" @change="toggle(item)" />
        <span class="article-text">
          <span class="article-name">{{ item.name }}</span>
          <span v-if="item.quantity" class="article-qty">{{ item.quantity }}</span>
          <small v-if="item.recipeTitle" class="article-recipe">pour {{ item.recipeTitle }}</small>
        </span>
      </label>
      <button type="button" class="remove" :aria-label="`Retirer ${item.name}`" @click="remove(item)">✕</button>
    </li>
  </ul>

  <section v-if="bought.length" class="bought">
    <div class="bought-head">
      <h3>Dans le panier ({{ bought.length }})</h3>
      <div class="inline">
        <button type="button" :disabled="storing" @click="storeBought">
          {{ storing ? 'Rangement…' : '🧊 Ranger dans le frigo' }}
        </button>
        <button type="button" class="secondary" :disabled="storing" @click="clearBought">Retirer sans ranger</button>
      </div>
    </div>
    <ul class="list">
      <li v-for="item in bought" :key="item.id" class="article checked">
        <label>
          <input type="checkbox" :checked="item.checked" @change="toggle(item)" />
          <span class="article-text">
            <span class="article-name">{{ item.name }}</span>
            <span v-if="item.quantity" class="article-qty">{{ item.quantity }}</span>
          </span>
        </label>
        <button type="button" class="remove" :aria-label="`Retirer ${item.name}`" @click="remove(item)">✕</button>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import { storeInFridge } from '../shopping'

const items = ref([])
const loaded = ref(false)
const error = ref('')
const message = ref('')
const newName = ref('')
const newQuantity = ref('')
const adding = ref(false)
const storing = ref(false)

const toBuy = computed(() => items.value.filter(i => !i.checked))
const bought = computed(() => items.value.filter(i => i.checked))

async function load() {
  try {
    items.value = await api.getShoppingItems()
    error.value = ''
  } catch {
    error.value = 'Impossible de charger la liste de courses.'
  } finally {
    loaded.value = true
  }
}

async function addManual() {
  if (!newName.value.trim()) return
  adding.value = true
  message.value = ''
  try {
    const added = await api.addShoppingItems([{ name: newName.value.trim(), quantity: newQuantity.value.trim() || null }])
    if (!added.length) message.value = `« ${newName.value.trim()} » est déjà dans la liste.`
    newName.value = ''
    newQuantity.value = ''
    await load()
  } catch (e) {
    error.value = e.response?.data?.error ?? "Impossible d'ajouter cet article."
  } finally {
    adding.value = false
  }
}

// Coche tout de suite a l'ecran (dans le magasin, pas le temps d'attendre), annule si l'appel echoue
async function toggle(item) {
  item.checked = !item.checked
  message.value = ''
  try {
    await api.checkShoppingItem(item.id, item.checked)
  } catch {
    item.checked = !item.checked
    error.value = `Impossible de cocher « ${item.name} ».`
  }
}

async function remove(item) {
  try {
    await api.deleteShoppingItem(item.id)
    items.value = items.value.filter(i => i.id !== item.id)
  } catch {
    error.value = `Impossible de retirer « ${item.name} ».`
  }
}

async function storeBought() {
  storing.value = true
  error.value = ''
  message.value = ''
  const articles = bought.value
  let stored = 0
  try {
    const fridgeItems = await api.getFridgeItems()
    for (const article of articles) {
      await storeInFridge(article, fridgeItems)
      await api.deleteShoppingItem(article.id)
      stored++
    }
    message.value = `${stored} article${stored > 1 ? 's' : ''} rangé${stored > 1 ? 's' : ''} dans le frigo.`
  } catch {
    error.value = stored
      ? `${stored} article(s) rangé(s), puis une erreur : réessaie pour les autres.`
      : 'Impossible de ranger les achats dans le frigo pour le moment.'
  } finally {
    storing.value = false
    await load()
  }
}

async function clearBought() {
  try {
    await api.deleteCheckedShoppingItems()
    await load()
  } catch {
    error.value = 'Impossible de retirer les articles cochés.'
  }
}

onMounted(load)
</script>

<style scoped>
.intro { color: #9fb0c0; }
.add-row { display: flex; gap: 10px; flex-wrap: wrap; margin: 14px 0 18px; }
.add-row .name { flex: 2; min-width: 180px; }
.add-row .qty { flex: 1; min-width: 120px; }
.error { color: #ff6b6b; }
.ok { color: #4ade80; }

.list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.article {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 8px 6px 14px; border-radius: 12px;
  background: #1a2330; border: 1px solid #2a3644;
}
.article label { flex: 1; display: flex; align-items: center; gap: 14px; margin: 0; cursor: pointer; min-height: 48px; }
.article input[type="checkbox"] { width: 26px; height: 26px; min-height: 0; flex-shrink: 0; accent-color: #4ade80; }
.article-text { display: flex; flex-wrap: wrap; align-items: baseline; gap: 4px 10px; color: #e6ecf1; font-size: 16px; }
.article-name { font-weight: 600; }
.article-qty { color: #4ade80; font-size: 14px; }
.article-recipe { width: 100%; color: #9fb0c0; font-size: 12px; }
.article.checked .article-name { text-decoration: line-through; color: #9fb0c0; }
.remove { flex-shrink: 0; width: 44px; min-height: 44px; padding: 0; background: transparent; border-color: #37485a; color: #9fb0c0; }

.bought { margin-top: 26px; }
.bought-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap; margin-bottom: 10px; }
.bought-head h3 { margin: 0; }
</style>
