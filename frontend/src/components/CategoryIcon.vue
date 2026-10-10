<template>
  <svg class="category-icon" viewBox="0 0 64 64" aria-hidden="true" focusable="false">
    <!-- Legumes : carotte -->
    <g v-if="name === 'legumes'">
      <ellipse cx="50" cy="11" rx="3.5" ry="8" transform="rotate(35 50 11)" fill="#4ade80" />
      <ellipse cx="54" cy="16" rx="3.5" ry="8" transform="rotate(75 54 16)" fill="#22c55e" />
      <path d="M46 16 C52 18 52 24 48 28 L18 54 C15 56 12 53 14 50 L38 18 C40 15 43 14 46 16 Z" fill="#f97316" />
      <path d="M38 25 l5 3 M30 34 l5 3 M23 43 l4 3" stroke="#c2410c" stroke-width="2.4" stroke-linecap="round" />
    </g>

    <!-- Fruits : pomme -->
    <g v-else-if="name === 'fruits'">
      <path d="M32 20 C24 14 12 18 12 32 C12 46 22 56 32 52 C42 56 52 46 52 32 C52 18 40 14 32 20 Z" fill="#ef4444" />
      <path d="M32 20 C32 14 34 10 37 7" fill="none" stroke="#7c4a1e" stroke-width="3" stroke-linecap="round" />
      <ellipse cx="42" cy="12" rx="6" ry="3" transform="rotate(-30 42 12)" fill="#4ade80" />
      <ellipse cx="21" cy="30" rx="3" ry="6" fill="#ffffff" opacity="0.35" />
    </g>

    <!-- Viandes : pilon -->
    <g v-else-if="name === 'viandes'" transform="rotate(-40 32 32)">
      <rect x="29" y="34" width="6" height="18" fill="#f3e9df" />
      <circle cx="27.5" cy="54" r="4" fill="#f3e9df" />
      <circle cx="36.5" cy="54" r="4" fill="#f3e9df" />
      <ellipse cx="32" cy="21" rx="14" ry="17" fill="#c2703d" />
      <ellipse cx="27" cy="16" rx="5" ry="7" fill="#e3955f" opacity="0.7" />
    </g>

    <!-- Poissons : poisson -->
    <g v-else-if="name === 'poissons'">
      <path d="M43 32 L58 20 V44 Z" fill="#0ea5e9" />
      <path d="M25 22 L34 13 L37 23 Z" fill="#0ea5e9" />
      <ellipse cx="28" cy="32" rx="18" ry="11.5" fill="#38bdf8" />
      <path d="M22 25 C26 30 26 34 22 39" fill="none" stroke="#0369a1" stroke-width="2.4" stroke-linecap="round" />
      <circle cx="16.5" cy="29.5" r="2.6" fill="#0b1220" />
    </g>

    <!-- Produits laitiers : brique de lait -->
    <g v-else-if="name === 'laitiers'">
      <rect x="33" y="4" width="7" height="6" rx="1.5" fill="#3b82f6" />
      <path d="M20 20 L26 8 H38 L44 20 Z" fill="#cbd5e1" />
      <rect x="20" y="20" width="24" height="36" rx="2" fill="#f8fafc" />
      <rect x="20" y="30" width="24" height="14" fill="#60a5fa" />
      <path d="M32 32.5 C28.5 37 28.5 41 32 41 C35.5 41 35.5 37 32 32.5 Z" fill="#ffffff" />
    </g>

    <!-- Feculents : pain -->
    <g v-else-if="name === 'feculents'">
      <path d="M10 33 C10 22 20 17 32 17 C44 17 54 22 54 33 C54 37 51 39 48 39 V52 C48 54.5 46 56 44 56 H20 C18 56 16 54.5 16 52 V39 C13 39 10 37 10 33 Z" fill="#e0a526" />
      <path d="M14 31 C16 23 24 20 32 20 C40 20 48 23 50 31" fill="none" stroke="#f5c451" stroke-width="3" stroke-linecap="round" />
      <path d="M23 26 l4 7 M31 25 l4 7 M39 26 l4 7" stroke="#a16207" stroke-width="3" stroke-linecap="round" />
    </g>

    <!-- Epicerie : bocal -->
    <g v-else-if="name === 'epicerie'">
      <rect x="19" y="7" width="26" height="9" rx="2.5" fill="#ea580c" />
      <rect x="16" y="15" width="32" height="42" rx="8" fill="#f5e6c8" />
      <rect x="16" y="28" width="32" height="16" fill="#fb923c" />
      <path d="M23 34 H41 M23 39 H35" stroke="#ffffff" stroke-width="2.4" stroke-linecap="round" />
    </g>

    <!-- Epices : salière d'épices -->
    <g v-else-if="name === 'epices'">
      <path d="M20 19 C20 9 44 9 44 19 Z" fill="#a855f7" />
      <g fill="#12181f">
        <circle cx="27" cy="15" r="1.4" />
        <circle cx="32" cy="13.5" r="1.4" />
        <circle cx="37" cy="15" r="1.4" />
      </g>
      <rect x="20" y="19" width="24" height="37" rx="6" fill="#e9d5ff" />
      <path d="M20 36 H44 V50 A6 6 0 0 1 38 56 H26 A6 6 0 0 1 20 50 Z" fill="#b45309" />
      <rect x="24" y="23" width="3" height="9" rx="1.5" fill="#ffffff" opacity="0.6" />
    </g>

    <!-- Autres : bol fumant -->
    <g v-else>
      <path d="M24 21 C20 17 28 13 24 8 M34 21 C30 17 38 13 34 8" fill="none" stroke="#94a3b8" stroke-width="3" stroke-linecap="round" />
      <path d="M8 30 H56 C56 44 46 54 32 54 C18 54 8 44 8 30 Z" fill="#94a3b8" />
      <rect x="6" y="27" width="52" height="6" rx="3" fill="#cbd5e1" />
    </g>
  </svg>
</template>

<script setup>
defineProps({
  // legumes, fruits, viandes, poissons, laitiers, feculents, epicerie, epices, autres
  name: { type: String, required: true }
})
</script>
