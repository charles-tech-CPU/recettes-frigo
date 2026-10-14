import api from './services/api'
import { CATEGORIES } from './categories'
import { catalogEntry, formatQuantity, parseQuantity, sameIngredient } from './ingredients'

// Range un article achete dans le frigo. Deja dans le frigo : on additionne les quantites
// si elles ont la meme unite ("2 boîtes" + "1 boîte"), sinon on n'y touche pas. Sinon on
// l'ajoute, avec le nom et la categorie du catalogue quand il y figure.
// fridgeItems est tenu a jour (un meme article peut etre range deux fois dans un lot).
export async function storeInFridge(article, fridgeItems) {
  const bought = parseQuantity(article.quantity)
  const existing = fridgeItems.find(i => sameIngredient(i.name, article.name))
  if (existing) {
    const stock = parseQuantity(existing.quantity)
    let quantity = null
    if (bought && !existing.quantity) quantity = formatQuantity(bought.amount, bought.unit)
    else if (bought && stock && bought.unit === stock.unit) quantity = formatQuantity(stock.amount + bought.amount, stock.unit)
    if (quantity && quantity.length <= 40) {
      Object.assign(existing, await api.updateFridgeItem(existing.id, { ...existing, quantity }))
    }
    return
  }
  const entry = catalogEntry(article.name)
  const quantity = bought ? formatQuantity(bought.amount, bought.unit) : null
  const created = await api.createFridgeItem({
    name: entry?.name ?? article.name.charAt(0).toUpperCase() + article.name.slice(1),
    quantity: quantity && quantity.length <= 40 ? quantity : null,
    category: entry && entry.categoryKey !== 'autres' ? CATEGORIES.find(c => c.key === entry.categoryKey).label : null,
    expiresOn: null
  })
  fridgeItems.push(created)
}
