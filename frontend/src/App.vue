<template>
  <header class="topbar">
    <a :href="PORTAL_URL" class="portal-link" title="Retour au portail du foyer">← Retour au portail</a>
    <h1>🍲 Recettes Frigo</h1>
    <nav>
      <router-link to="/">Recettes</router-link>
      <router-link to="/mes-recettes">Mes recettes</router-link>
      <router-link to="/frigo">Mon frigo</router-link>
    </nav>
  </header>
  <main>
    <router-view />
  </main>
</template>

<script setup>
import { PORTAL_URL } from './portal'
</script>
