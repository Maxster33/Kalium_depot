// Base locale du bot (cahier des charges, section 2) : un seul fichier bot.sqlite a cote de package.json, jamais dans
// le depot. better-sqlite3 (Node 20 chez Dishost : pas de node:sqlite). Chaque table est creee au besoin ; les etapes
// suivantes y ajoutent les leurs.

import { fileURLToPath } from 'node:url';

const FICHIER = fileURLToPath(new URL('../bot.sqlite', import.meta.url));

/**
 * Ouvre la base ; renvoie null (avec un avertissement) si better-sqlite3 ne se charge pas, par exemple si
 * l'hebergeur n'a pas lance son script d'installation : le bot tourne alors sans les fonctions qui en ont besoin.
 */
export async function ouvrir() {
  let Database;
  try {
    ({ default: Database } = await import('better-sqlite3'));
  } catch (error) {
    console.warn(`Base locale indisponible (better-sqlite3 : ${error.message}) : /config désactivé.`);
    return null;
  }
  const db = new Database(FICHIER);
  db.pragma('journal_mode = WAL');
  db.exec(`CREATE TABLE IF NOT EXISTS salons (
    type TEXT PRIMARY KEY,
    salon TEXT NOT NULL,
    par TEXT NOT NULL,
    le TEXT NOT NULL
  )`);
  return new Base(db);
}

class Base {
  constructor(db) {
    this.db = db;
  }

  /** Salon choisi pour un type (identifiant Discord), ou null. */
  salon(type) {
    return this.db.prepare('SELECT salon FROM salons WHERE type = ?').get(type)?.salon ?? null;
  }

  salons() {
    return this.db.prepare('SELECT type, salon, par, le FROM salons ORDER BY type').all();
  }

  choisirSalon(type, salon, par) {
    this.db.prepare(`INSERT INTO salons (type, salon, par, le) VALUES (?, ?, ?, ?)
      ON CONFLICT(type) DO UPDATE SET salon = excluded.salon, par = excluded.par, le = excluded.le`)
      .run(type, salon, par, new Date().toISOString());
  }

  retirerSalon(type) {
    return this.db.prepare('DELETE FROM salons WHERE type = ?').run(type).changes > 0;
  }
}
