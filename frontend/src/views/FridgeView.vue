<template>
  <h2>Mon frigo</h2>
  <p style="color:#9fb0c0">
    Ajoute ici ce que tu as dans le frigo/placard. Cette liste reste d'une fois
    sur l'autre : ajoute au fil des courses, retire quand c'est fini.
  </p>

  <form class="card inline" @submit.prevent="addItem">
    <input v-model="form.name" placeholder="Ingrédient (ex: Poulet)" required style="flex:2; min-width:160px" />
    <input v-model="form.quantity" placeholder="Quantité (optionnel)" style="flex:1; min-width:120px" />
    <input v-model="form.category" placeholder="Catégorie (optionnel)" style="flex:1; min-width:140px" />
    <button type="submit">+ Ajouter</button>
  </form>
  <p v-if="error" style="color:#ff6b6b">{{ error }}</p>

  <p v-if="!items.length" class="empty-hint">Ton frigo est vide pour l'instant.</p>

  <div class="fridge-grid">
    <div v-for="item in items" :key="item.id" class="fridge-item-card">
      <div>
        <div class="name">{{ item.name }}</div>
        <div class="meta">
          <span v-if="item.quantity">{{ item.quantity }}</span>
          <span v-if="item.category"> · {{ item.category }}</span>
        </div>
      </div>
      <button class="danger" @click="remove(item)">✕</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../services/api'

const items = ref([])
const form = reactive({ name: '', quantity: '', category: '' })
const error = ref('')

async function load() {
  items.value = await api.getFridgeItems()
}

async function addItem() {
  error.value = ''
  try {
    await api.createFridgeItem({
      name: form.name,
      quantity: form.quantity || null,
      category: form.category || null
    })
    form.name = ''
    form.quantity = ''
    form.category = ''
    await load()
  } catch (e) {
    error.value = e.response?.data?.error ?? "Erreur lors de l'ajout (cet ingrédient existe peut-être déjà)."
  }
}

async function remove(item) {
  await api.deleteFridgeItem(item.id)
  await load()
}

onMounted(load)
</script>
