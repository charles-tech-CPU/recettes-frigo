<template>
  <header class="topbar">
    <h1>🍲 Recettes Frigo</h1>
    <nav>
      <router-link to="/">Recettes</router-link>
      <router-link to="/frigo">Mon frigo</router-link>
    </nav>
  </header>
  <main>
    <router-view />
  </main>
</template>
