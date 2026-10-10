import { createRouter, createWebHistory } from 'vue-router'
import RecipesView from '../views/RecipesView.vue'
import FridgeView from '../views/FridgeView.vue'

const routes = [
  { path: '/', component: RecipesView },
  { path: '/frigo', component: FridgeView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
