import axios from 'axios'

// L'API tourne sur la meme machine que la page, port 8085 : on reprend l'hote
// courant pour que l'appli marche aussi depuis un autre appareil du reseau local.
const client = axios.create({ baseURL: `http://${window.location.hostname}:8085/api` })

export default {
  getFridgeItems() {
    return client.get('/fridge-items').then(r => r.data)
  },
  createFridgeItem(payload) {
    return client.post('/fridge-items', payload).then(r => r.data)
  },
  updateFridgeItem(id, payload) {
    return client.put(`/fridge-items/${id}`, payload).then(r => r.data)
  },
  deleteFridgeItem(id) {
    return client.delete(`/fridge-items/${id}`)
  },
  getSuggestions(limit = 12) {
    return client.get('/recipes/suggestions', { params: { limit } }).then(r => r.data)
  },
  getRecipeDetail(mealId) {
    return client.get(`/recipes/${mealId}`).then(r => r.data)
  },

  // Recettes perso (eventuellement Thermomix)
  getMyRecipes() {
    return client.get('/my-recipes').then(r => r.data)
  },
  getMyRecipe(id) {
    return client.get(`/my-recipes/${id}`).then(r => r.data)
  },
  createMyRecipe(payload) {
    return client.post('/my-recipes', payload).then(r => r.data)
  },
  updateMyRecipe(id, payload) {
    return client.put(`/my-recipes/${id}`, payload).then(r => r.data)
  },
  deleteMyRecipe(id) {
    return client.delete(`/my-recipes/${id}`)
  },
  // Recette lue sur la page d'un lien, non enregistree : pre-remplit le formulaire
  importRecipe(url) {
    return client.post('/my-recipes/import', { url }).then(r => r.data)
  },

  // Notes sur une recette : recipeKey = "mealdb:<id>" ou "perso:<id>"
  getNotes(recipeKey) {
    return client.get('/recipe-notes', { params: { recipe: recipeKey } }).then(r => r.data)
  },
  addNote(recipeKey, content) {
    return client.post('/recipe-notes', { recipeKey, content }).then(r => r.data)
  },
  deleteNote(id) {
    return client.delete(`/recipe-notes/${id}`)
  }
}
