// Règlement de KaLium dans un salon Discord : la présentation et le sommaire (embed) dans le salon, une partie par
// fil verrouillé (embed), chaque fil finissant par un lien « Revenir au sommaire ».
// Commande texte réservée au rôle « Le fameux fruit » :
//   k!rules set <id du salon>  publie le règlement dans ce salon, ou le met à jour sur place s'il y est déjà.
// Les textes sont dans reglement/ (relus à chaque commande, pas besoin de redémarrer le bot) :
//   _sommaire.md : « # Titre », ligne « couleur: #rrggbb » facultative, la présentation, puis après une ligne « --- »
//                  le texte placé sous le sommaire ;
//   une partie par fichier (ordre alphabétique : a-…, b-…), « # Titre » = nom du fil, puis le texte.
// Les identifiants Discord (salon, fils, messages) sont gardés dans reglement-etat.json, jamais commité.

import { readdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { ChannelType, PermissionFlagsBits, ThreadAutoArchiveDuration } from 'discord.js';
import { PERMISSIONS_BOT, effacerAnnoncesDeFils, verrouillerSalon } from './wiki.js';

const DOSSIER = fileURLToPath(new URL('../reglement/', import.meta.url));
const FICHIER_ETAT = fileURLToPath(new URL('../reglement-etat.json', import.meta.url));

/** Rôle autorisé (nom, sans tenir compte des majuscules ni d'un emoji autour). */
const ROLE_AUTORISE = 'le fameux fruit';
const MAX_DESCRIPTION = 4096;
const MAX_NOM_FIL = 100;
const COULEUR_DEFAUT = 0x09add3;
const SANS_PING = { parse: [] };

let enCours = false;

export async function commande(message, args) {
  if (!message.inGuild()) return;
  const autorise = message.member?.roles.cache.some((r) => r.name.toLowerCase().includes(ROLE_AUTORISE));
  if (!autorise) return; // silence : la commande n'existe pas pour les autres
  const [action, idSalon] = args;
  const salonId = idSalon?.replace(/^<#(\d+)>$/, '$1');
  if (action?.toLowerCase() !== 'set' || !/^\d{17,20}$/.test(salonId ?? '')) {
    await message.reply({ content: "Usage : `k!rules set <id du salon>` (clic droit sur le salon > Copier l'identifiant).", allowedMentions: SANS_PING });
    return;
  }
  if (enCours) {
    await message.reply({ content: 'Une publication du règlement est déjà en cours.', allowedMentions: SANS_PING });
    return;
  }
  enCours = true;
  try {
    const rapport = await publier(message.guild, salonId);
    await message.reply({ content: rapport, allowedMentions: SANS_PING });
  } catch (error) {
    console.error('k!rules :', error);
    await message.reply({ content: `Règlement non publié : ${error.message}`, allowedMentions: SANS_PING }).catch(() => {});
  } finally {
    enCours = false;
  }
}

/** Lit reglement/ : la présentation et les parties, dans l'ordre des fichiers. */
export function lireReglement(dossier = DOSSIER) {
  if (!existsSync(join(dossier, '_sommaire.md'))) throw new Error(`reglement/_sommaire.md introuvable`);
  const s = lireFichier(readFileSync(join(dossier, '_sommaire.md'), 'utf8'), '_sommaire.md');
  const [intro, conclusion = ''] = s.texte.split(/\n-{3,}\n/).map((t) => t.trim());
  const parties = readdirSync(dossier)
    .filter((f) => f.endsWith('.md') && !f.startsWith('_'))
    .sort((a, b) => a.localeCompare(b, 'fr', { numeric: true }))
    .map((f) => {
      const p = lireFichier(readFileSync(join(dossier, f), 'utf8'), f);
      if (!p.texte) throw new Error(`${f} : partie vide`);
      if (p.titre.length > MAX_NOM_FIL) throw new Error(`${f} : titre trop long pour un nom de fil (${p.titre.length} > ${MAX_NOM_FIL})`);
      return { cle: f.replace(/\.md$/, ''), ...p, couleur: p.couleur ?? s.couleur ?? COULEUR_DEFAUT };
    });
  if (!parties.length) throw new Error('aucune partie dans reglement/');
  return { titre: s.titre, couleur: s.couleur ?? COULEUR_DEFAUT, intro, conclusion, parties };
}

function lireFichier(brut, origine) {
  const lignes = brut.replace(/^﻿/, '').replace(/\r\n?/g, '\n').split('\n');
  const premiere = lignes.shift() ?? '';
  if (!/^#\s+\S/.test(premiere)) throw new Error(`${origine} : la première ligne doit être « # Titre »`);
  let couleur = null;
  if (/^couleur\s*:/i.test(lignes[0] ?? '')) {
    const valeur = lignes.shift().replace(/^couleur\s*:\s*/i, '').trim();
    if (!/^#[0-9a-f]{6}$/i.test(valeur)) throw new Error(`${origine} : couleur « ${valeur} » invalide (format #rrggbb)`);
    couleur = parseInt(valeur.slice(1), 16);
  }
  return { titre: premiere.replace(/^#\s+/, '').trim(), couleur, texte: lignes.join('\n').trim() };
}

/** Description de l'embed du sommaire : présentation, liens des fils, conclusion. */
export function texteSommaire(reglement, idsFils) {
  const liens = reglement.parties.map((p) => `📖 ${idsFils[p.cle] ? `<#${idsFils[p.cle]}>` : `**${p.titre}**`}`);
  const texte = [reglement.intro, `## Sommaire\n${liens.join('\n')}`, reglement.conclusion].filter(Boolean).join('\n\n');
  if (texte.length > MAX_DESCRIPTION) throw new Error(`présentation trop longue (${texte.length} > ${MAX_DESCRIPTION} caractères)`);
  return texte;
}

/** Description de l'embed d'une partie, terminée par le lien de retour au sommaire. */
export function textePartie(partie, lienSommaire) {
  const texte = `${partie.texte}\n\n↩️ [Revenir au sommaire](${lienSommaire})`;
  if (texte.length > MAX_DESCRIPTION) throw new Error(`${partie.titre} : texte trop long (${texte.length} > ${MAX_DESCRIPTION} caractères)`);
  return texte;
}

async function publier(guild, salonId) {
  const reglement = lireReglement();

  const salon = await guild.channels.fetch(salonId).catch(() => null);
  if (!salon) throw new Error(`salon ${salonId} introuvable sur ce serveur Discord`);
  if (salon.type !== ChannelType.GuildText) throw new Error(`#${salon.name} n'est pas un salon textuel`);
  const moi = guild.members.me;
  const manquantes = Object.entries(PERMISSIONS_BOT)
    .filter(([cle]) => !salon.permissionsFor(moi).has(PermissionFlagsBits[cle]))
    .map(([, nom]) => nom);
  if (manquantes.length) throw new Error(`il manque au bot, dans #${salon.name} : ${manquantes.join(', ')}`);

  const etat = lireEtat();
  etat.salons[salonId] ??= { sommaire: null, fils: {} };
  const memo = etat.salons[salonId];

  const avertissements = [];
  await verrouillerSalon(salon, moi).catch((error) => {
    avertissements.push(`salon non verrouillé pour les membres (${error.message}) : retire-leur « Envoyer des messages » et « … dans les fils » à la main`);
  });

  // 1. Le sommaire d'abord : les fils ont besoin de son lien. Publié sans liens, complété à la fin.
  const embedSommaire = (ids) => ({ title: reglement.titre, color: reglement.couleur, description: texteSommaire(reglement, ids) });
  let sommaire = memo.sommaire ? await salon.messages.fetch(memo.sommaire).catch(() => null) : null;
  const nouveauSommaire = !sommaire;
  if (!sommaire) {
    sommaire = await salon.send({ embeds: [embedSommaire({})], allowedMentions: SANS_PING });
    memo.sommaire = sommaire.id;
    ecrireEtat(etat);
  }
  const lienSommaire = sommaire.url;

  // 2. Un fil par partie.
  const compte = { crees: 0, modifies: 0, inchanges: 0 };
  const idsFils = {};
  for (const partie of reglement.parties) {
    compte[await synchroniserFil(salon, partie, lienSommaire, memo)]++;
    idsFils[partie.cle] = memo.fils[partie.cle].id;
    ecrireEtat(etat);
  }
  await effacerAnnoncesDeFils(salon, moi.id);

  // 3. Le sommaire avec les liens des fils.
  await sommaire.edit({ embeds: [embedSommaire(idsFils)], allowedMentions: SANS_PING });

  const cles = new Set(reglement.parties.map((p) => p.cle));
  const orphelins = Object.entries(memo.fils).filter(([cle]) => !cles.has(cle));
  if (orphelins.length) {
    avertissements.push(`fils sans partie (gardés, à supprimer à la main) : ${orphelins.map(([, f]) => `<#${f.id}>`).join(', ')}`);
  }
  return [
    `Règlement ${nouveauSommaire ? 'publié' : 'mis à jour'} dans <#${salonId}> : ${compte.crees} fil(s) créé(s), `
      + `${compte.modifies} mis à jour, ${compte.inchanges} inchangé(s).`,
    ...avertissements.map((a) => `⚠️ ${a}`),
  ].join('\n').slice(0, 2000);
}

/** Crée ou met à jour le fil d'une partie. Renvoie « crees », « modifies » ou « inchanges ». */
async function synchroniserFil(salon, partie, lienSommaire, memo) {
  const embed = { title: partie.titre, color: partie.couleur, description: textePartie(partie, lienSommaire) };
  const connu = memo.fils[partie.cle];
  let fil = connu ? await salon.threads.fetch(connu.id).catch(() => null) : null;
  if (fil && fil.parentId !== salon.id) fil = null;

  if (!fil) {
    fil = await salon.threads.create({
      name: partie.titre,
      type: ChannelType.PublicThread,
      autoArchiveDuration: ThreadAutoArchiveDuration.OneWeek,
      reason: 'Règlement KaLium',
    });
    const m = await fil.send({ embeds: [embed], allowedMentions: SANS_PING });
    await fil.setLocked(true, 'Règlement KaLium : lecture seule');
    memo.fils[partie.cle] = { id: fil.id, message: m.id };
    return 'crees';
  }

  let modifie = false;
  // Un fil archivé (une semaine sans message) ne se modifie pas : on le rouvre, il reste verrouillé.
  if (fil.archived) await fil.setArchived(false, 'Règlement KaLium : mise à jour');
  if (fil.name !== partie.titre) {
    await fil.setName(partie.titre);
    modifie = true;
  }
  const ancien = await fil.messages.fetch(connu.message).catch(() => null);
  if (!ancien) {
    connu.message = (await fil.send({ embeds: [embed], allowedMentions: SANS_PING })).id;
    modifie = true;
  } else {
    const e = ancien.embeds[0];
    if (e?.title !== embed.title || e?.description !== embed.description || e?.color !== embed.color) {
      await ancien.edit({ embeds: [embed], allowedMentions: SANS_PING });
      modifie = true;
    }
  }
  if (!fil.locked) await fil.setLocked(true, 'Règlement KaLium : lecture seule');
  return modifie ? 'modifies' : 'inchanges';
}

function lireEtat() {
  if (!existsSync(FICHIER_ETAT)) return { salons: {} };
  const etat = JSON.parse(readFileSync(FICHIER_ETAT, 'utf8'));
  etat.salons ??= {};
  return etat;
}

function ecrireEtat(etat) {
  writeFileSync(FICHIER_ETAT, JSON.stringify(etat, null, 2) + '\n');
}
