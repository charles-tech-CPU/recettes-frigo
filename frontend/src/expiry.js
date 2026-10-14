// Peremption d'un ingredient du frigo. expiresOn = "2026-10-20" (date sans heure, ou null).

// "Perime bientot" : aujourd'hui et les SOON_DAYS jours suivants
export const SOON_DAYS = 3

// Jours restants avant la date (0 = aujourd'hui, negatif = deja perime), null sans date
export function daysUntil(expiresOn, today = new Date()) {
  if (!expiresOn) return null
  const [year, month, day] = expiresOn.split('-').map(Number)
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  // Math.round : un passage a l'heure d'ete/d'hiver fait une journee de 23 ou 25 h
  return Math.round((new Date(year, month - 1, day) - start) / 86_400_000)
}

// 'expired' | 'soon' | 'ok' | null
export function expiryLevel(expiresOn) {
  const days = daysUntil(expiresOn)
  if (days === null) return null
  if (days < 0) return 'expired'
  return days <= SOON_DAYS ? 'soon' : 'ok'
}

export function isExpiringSoon(item) {
  const level = expiryLevel(item.expiresOn)
  return level === 'expired' || level === 'soon'
}

export function expiryLabel(expiresOn) {
  const days = daysUntil(expiresOn)
  if (days === null) return null
  if (days < -1) return `périmé depuis ${-days} j`
  if (days === -1) return 'périmé hier'
  if (days === 0) return "périme aujourd'hui"
  if (days === 1) return 'périme demain'
  if (days <= SOON_DAYS) return `périme dans ${days} j`
  const [year, month, day] = expiresOn.split('-').map(Number)
  return `jusqu'au ${new Date(year, month - 1, day).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}`
}
