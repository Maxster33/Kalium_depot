// Graphiques en images PNG (Chart.js dessine sur @napi-rs/canvas, sans navigateur).
// Charte : fond sombre fixe (l'image garde sa carte, lisible sur Discord clair comme sombre), traits fins, grille
// discrete, textes en encre neutre (jamais de la couleur du jeu), une couleur fixe par jeu.

import { createCanvas } from '@napi-rs/canvas';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const INK = {
  surface: '#1a1a19',
  primary: '#ffffff',
  secondary: '#c3c2b7',
  muted: '#898781',
  grid: '#2c2c2a',
  axis: '#383835',
};

// Palette categorielle validee (fond sombre), dans l'ordre fixe des emplacements.
const SERIES = ['#3987e5', '#d95926', '#199e70', '#c98500', '#d55181', '#008300', '#9085e9', '#e66767'];

const FONT = 'system-ui, "Segoe UI", "DejaVu Sans", Arial, sans-serif';
Chart.defaults.font.family = FONT;
Chart.defaults.color = INK.secondary;

/** Couleur fixe d'un jeu : toujours la meme pour un meme identifiant, sur tous les graphiques. */
export function gameColor(gameId) {
  let hash = 0;
  for (const char of gameId) hash = (hash * 31 + char.codePointAt(0)) >>> 0;
  return SERIES[hash % SERIES.length];
}

/** Fond de la carte + titre et sous-titre dessines a la main (au-dessus de la zone du graphique). */
const card = (title, subtitle) => ({
  id: 'kaliumCard',
  beforeDraw(chart) {
    const { ctx, width, height } = chart;
    ctx.save();
    ctx.fillStyle = INK.surface;
    ctx.fillRect(0, 0, width, height);
    ctx.fillStyle = INK.primary;
    ctx.font = `600 22px ${FONT}`;
    ctx.textBaseline = 'top';
    ctx.fillText(title, 24, 20);
    if (subtitle) {
      ctx.fillStyle = INK.muted;
      ctx.font = `14px ${FONT}`;
      ctx.fillText(subtitle, 24, 50);
    }
    ctx.restore();
  },
});

/** Valeur affichee au bout de chaque barre (encre secondaire). */
const barValues = (labels) => ({
  id: 'kaliumBarValues',
  afterDatasetsDraw(chart) {
    const { ctx } = chart;
    const meta = chart.getDatasetMeta(0);
    ctx.save();
    ctx.fillStyle = INK.secondary;
    ctx.font = `600 14px ${FONT}`;
    ctx.textBaseline = 'middle';
    meta.data.forEach((bar, i) => {
      ctx.fillText(labels[i], bar.x + 8, bar.y);
    });
    ctx.restore();
  },
});

/**
 * Classement en barres horizontales.
 * rows : [{ label: '1. Pseudo', value: nombre, text: 'valeur affichee' }] ; value sert a la longueur des barres.
 */
export async function renderRanking({ title, subtitle, rows, color }) {
  const width = 900;
  const height = 110 + Math.max(rows.length, 1) * 40;
  const canvas = createCanvas(width, height);
  const max = Math.max(...rows.map((r) => r.value), 1);
  const chart = new Chart(canvas.getContext('2d'), {
    type: 'bar',
    data: {
      labels: rows.map((r) => r.label),
      datasets: [{
        data: rows.map((r) => r.value),
        backgroundColor: color,
        borderRadius: { topRight: 4, bottomRight: 4 },
        borderSkipped: 'start',
        barPercentage: 0.7,
        categoryPercentage: 0.9,
      }],
    },
    options: {
      indexAxis: 'y',
      responsive: false,
      animation: false,
      devicePixelRatio: 1,
      layout: { padding: { top: 80, right: 24, bottom: 16, left: 16 } },
      plugins: { legend: { display: false }, tooltip: { enabled: false } },
      scales: {
        x: {
          beginAtZero: true,
          // Place pour la valeur ecrite au bout de la plus longue barre.
          suggestedMax: max * 1.18,
          grid: { color: INK.grid },
          border: { color: INK.axis },
          ticks: { display: false },
        },
        y: {
          grid: { display: false },
          border: { color: INK.axis },
          ticks: { color: INK.primary, font: { size: 15 } },
        },
      },
    },
    plugins: [card(title, subtitle), barValues(rows.map((r) => r.text))],
  });
  const png = await canvas.encode('png');
  chart.destroy();
  return png;
}
