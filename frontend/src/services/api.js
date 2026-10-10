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
  }
}
