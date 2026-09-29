// Affichage des valeurs, identique a KG_ScoreBoards (StatsService.formatPoints / formatTime) : les joueurs voient les
// memes nombres en jeu et sur Discord.

/** Points : 6 chiffres au plus, decimales (2 au plus) jusqu'a un million, puis M / Md avec 3 decimales au plus. */
export function formatPoints(points) {
  const abs = Math.abs(points);
  if (abs >= 1e9) return trim((points / 1e9).toFixed(3)) + ' Md';
  if (abs >= 999_999.5) return trim((points / 1e6).toFixed(3)) + ' M';
  const digits = abs < 1 ? 1 : Math.floor(Math.log10(abs)) + 1;
  const decimals = Math.max(0, Math.min(2, 6 - digits));
  return trim(points.toFixed(decimals));
}

function trim(text) {
  text = text.replace('.', ',');
  if (!text.includes(',')) return text;
  text = text.replace(/0+$/, '');
  return text.endsWith(',') ? text.slice(0, -1) : text;
}

/** Temps : m:ss.cc (comme en jeu). */
export function formatTime(ms) {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor(ms / 1000) % 60;
  const hundredths = Math.floor(ms / 10) % 100;
  return `${minutes}:${String(seconds).padStart(2, '0')}.${String(hundredths).padStart(2, '0')}`;
}

/** Libelle d'un mois (aaaa-mm -> « septembre 2026 »). */
export function monthLabel(key) {
  const [year, month] = key.split('-').map(Number);
  if (!year || !month) return key;
  const name = new Date(Date.UTC(year, month - 1, 15)).toLocaleDateString('fr-FR', { month: 'long', timeZone: 'UTC' });
  return `${name} ${year}`;
}

/** Date et heure courtes en heure de Paris (« 29/09 à 21 h 40 »). */
export function shortDate(date) {
  const parts = new Intl.DateTimeFormat('fr-FR', {
    timeZone: 'Europe/Paris', day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit',
  }).formatToParts(date);
  const get = (type) => parts.find((p) => p.type === type)?.value;
  return `${get('day')}/${get('month')} à ${get('hour')} h ${get('minute')}`;
}
