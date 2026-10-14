import { createRouter, createWebHistory } from 'vue-router'
import RecipesView from '../views/RecipesView.vue'
import FridgeView from '../views/FridgeView.vue'
import MyRecipesView from '../views/MyRecipesView.vue'
import RecipeEditorView from '../views/RecipeEditorView.vue'
import ShoppingListView from '../views/ShoppingListView.vue'

const routes = [
  { path: '/', component: RecipesView },
  { path: '/mes-recettes', component: MyRecipesView },
  { path: '/mes-recettes/nouvelle', component: RecipeEditorView },
  { path: '/mes-recettes/:id(\\d+)', component: RecipeEditorView },
  { path: '/frigo', component: FridgeView },
  { path: '/courses', component: ShoppingListView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
