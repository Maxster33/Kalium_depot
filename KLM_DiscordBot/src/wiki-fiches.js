// Lecture des fiches du wiki (dossier wiki/ du bot), sans Discord : voir wiki/LISEZMOI.md pour le format.

import { readdirSync, readFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';

/** Limite d'un message Discord. */
export const MAX_MESSAGE = 2000;
/** Limite d'un nom de fil Discord. */
const MAX_NOM_FIL = 100;
/** Limite de la description d'un embed (sommaire). */
export const MAX_SOMMAIRE = 4096;

const COULEUR_DEFAUT = 0x5865f2;

/**
 * Lit tous les serveurs du wiki, dans l'ordre des dossiers (01-kixster, 02-kal-games…).
 * @returns {{cle: string, titre: string, couleur: number, intro: string, fiches: {cle: string, titre: string, resume: string, nomFil: string, messages: string[]}[]}[]}
 */
export function lireWiki(dossier) {
  if (!existsSync(dossier)) throw new Error(`dossier du wiki introuvable : ${dossier}`);
  const serveurs = [];
  for (const nomDossier of trier(readdirSync(dossier, { withFileTypes: true }).filter((e) => e.isDirectory()).map((e) => e.name))) {
    const chemin = join(dossier, nomDossier);
    const cheminSommaire = join(chemin, '_sommaire.md');
    if (!existsSync(cheminSommaire)) throw new Error(`${nomDossier} : _sommaire.md manquant`);
    const sommaire = lireSommaire(readFileSync(cheminSommaire, 'utf8'), nomDossier);
    const fiches = trier(readdirSync(chemin).filter((f) => f.endsWith('.md') && !f.startsWith('_')))
      .map((f) => lireFiche(readFileSync(join(chemin, f), 'utf8'), `${nomDossier}/${f}`, sommaire.titre));
    if (!fiches.length) throw new Error(`${nomDossier} : aucune fiche`);
    serveurs.push({ cle: sansOrdre(nomDossier), ...sommaire, fiches });
  }
  if (!serveurs.length) throw new Error('aucun serveur dans le wiki');
  return serveurs;
}

/** Sommaire d'un serveur : « # Titre », ligne « couleur: #rrggbb » facultative, puis le texte d'introduction. */
export function lireSommaire(texte, origine) {
  const lignes = normaliser(texte).split('\n');
  const titre = titreDe(lignes.shift(), origine);
  let couleur = COULEUR_DEFAUT;
  if (/^couleur\s*:/i.test(lignes[0] ?? '')) {
    const valeur = lignes.shift().replace(/^couleur\s*:\s*/i, '').trim();
    if (!/^#[0-9a-f]{6}$/i.test(valeur)) throw new Error(`${origine} : couleur « ${valeur} » invalide (format #rrggbb)`);
    couleur = parseInt(valeur.slice(1), 16);
  }
  return { titre, couleur, intro: lignes.join('\n').trim() };
}

/** Fiche : « # Titre », ligne « > résumé » facultative (pour le sommaire), puis le contenu publié dans le fil. */
export function lireFiche(texte, origine, titreServeur) {
  const lignes = normaliser(texte).split('\n');
  const titre = titreDe(lignes.shift(), origine);
  const resume = /^>\s?/.test(lignes[0] ?? '') ? lignes.shift().replace(/^>\s?/, '').trim() : '';
  const contenu = lignes.join('\n').trim();
  if (!contenu) throw new Error(`${origine} : fiche vide`);
  const nomFil = `${titreServeur} · ${titre}`;
  if (nomFil.length > MAX_NOM_FIL) throw new Error(`${origine} : nom de fil trop long (${nomFil.length} > ${MAX_NOM_FIL})`);
  const nomFichier = origine.split('/').pop();
  return { cle: `${sansOrdre(origine.split('/')[0])}/${sansOrdre(nomFichier.replace(/\.md$/, ''))}`, titre, resume, nomFil, messages: decouper(contenu, origine) };
}

/**
 * Découpe un contenu en messages Discord : une ligne « --- » seule force un nouveau message ; sinon les paragraphes
 * sont regroupés jusqu'à 2 000 caractères. Un paragraphe trop long est refusé (à couper dans le fichier).
 */
export function decouper(contenu, origine) {
  const messages = [];
  for (const bloc of contenu.split(/\n-{3,}\n/)) {
    let courant = '';
    for (const paragraphe of bloc.split(/\n{2,}/).map((p) => p.trim()).filter(Boolean)) {
      if (paragraphe.length > MAX_MESSAGE) throw new Error(`${origine} : paragraphe de ${paragraphe.length} caractères (2 000 au plus), à couper`);
      const suite = courant ? `${courant}\n\n${paragraphe}` : paragraphe;
      if (suite.length > MAX_MESSAGE) {
        messages.push(courant);
        courant = paragraphe;
      } else {
        courant = suite;
      }
    }
    if (courant) messages.push(courant);
  }
  return messages;
}

/** Texte d'un sommaire : introduction puis une ligne par fiche (lien du fil et résumé). */
export function texteSommaire(serveur, idsFils) {
  const lignes = serveur.fiches.map((f) => `• <#${idsFils[f.cle]}>${f.resume ? ` — ${f.resume}` : ''}`);
  const texte = [serveur.intro, lignes.join('\n')].filter(Boolean).join('\n\n');
  if (texte.length > MAX_SOMMAIRE) throw new Error(`sommaire de ${serveur.titre} trop long (${texte.length} > ${MAX_SOMMAIRE})`);
  return texte;
}

function titreDe(ligne, origine) {
  if (!/^#\s+\S/.test(ligne ?? '')) throw new Error(`${origine} : la première ligne doit être « # Titre »`);
  return ligne.replace(/^#\s+/, '').trim();
}

function normaliser(texte) {
  return texte.replace(/^﻿/, '').replace(/\r\n?/g, '\n');
}

/** « 01-kixster » -> « kixster » : le numéro sert à l'ordre, pas à reconnaître la fiche (renuméroter ne recrée rien). */
function sansOrdre(nom) {
  return nom.replace(/^\d+[-_ ]/, '');
}

function trier(noms) {
  return noms.sort((a, b) => a.localeCompare(b, 'fr', { numeric: true }));
}
