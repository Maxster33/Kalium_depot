// Lanceur de KLM_DiscordBot : lit le fichier .env puis demarre le bot (src/index.js).
// Les hebergeurs de bots lancent souvent « node index.js » a la racine, sans l'option --env-file.
// Une variable deja definie (panneau de l'hebergeur) n'est pas remplacee par .env.

import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const FICHIER_ENV = fileURLToPath(new URL('./.env', import.meta.url));

if (existsSync(FICHIER_ENV)) {
  for (const ligne of readFileSync(FICHIER_ENV, 'utf8').split(/\r?\n/)) {
    const m = ligne.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/);
    if (!m || process.env[m[1]]) continue;
    process.env[m[1]] = m[2].replace(/^(["'])(.*)\1$/, '$2');
  }
}

await import('./src/index.js');
