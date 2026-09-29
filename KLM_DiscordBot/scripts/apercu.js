// Apercu des graphiques avec des donnees fictives (sans Discord ni serveur) : npm run apercu -> apercu-*.png
import { writeFile } from 'node:fs/promises';
import { renderRanking, gameColor } from '../src/charts.js';
import { formatPoints, formatTime } from '../src/format.js';

const names = ['LeKiwi06', 'Maaxster', '.PatientLime2170', 'Élodie_42', 'Zorglub', 'xX_Rush_Xx', 'Pomme', 'Kalium', 'Bedrock_Béa', 'Z'];
const points = [1234.56, 980, 812.5, 640, 512, 300.25, 250, 120, 45.5, 3];
const rows = names.map((name, i) => ({ label: `${i + 1}. ${name}`, value: points[i], text: `${formatPoints(points[i])} pts` }));
await writeFile('apercu-points.png', await renderRanking({
  title: 'PvP Kit — top 10 général', subtitle: 'Classement aux points · 29/09 à 01 h 58', rows, color: gameColor('pvpkit'),
}));

const laps = [37210, 37980, 38400, 39120, 41050];
const lapRows = laps.map((ms, i) => ({ label: `${i + 1}. ${names[i]}`, value: ms / 1000, text: formatTime(ms) }));
await writeFile('apercu-tours.png', await renderRanking({
  title: 'Course de bateau — meilleurs tours', subtitle: 'Septembre 2026', rows: lapRows, color: gameColor('boatrace'),
}));
console.log('apercu-points.png et apercu-tours.png écrits.');
