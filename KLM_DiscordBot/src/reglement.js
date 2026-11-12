// Salons d'informations de KaLium publiés par le bot : des sections séparées par une image (barre animée), chacune =
// un gros titre (embed) + un texte (embed). Une section peut porter un groupe de fils (les règles, les concepts…) :
// son texte se termine par les liens des fils, et chaque fil (verrouillé) finit par « Revenir au sommaire ».
//
//   [image] [Bienvenue | description] [image] [Objectif | texte] [image] [Règles | liens] [image] [Concepts | liens] [image]
//
// Un salon = un dossier de textes : « informations » = reglement/, les autres = salons/<nom>/ (guide-acces,
// infos-concepts…). Commandes texte réservées au rôle « Le fameux fruit » :
//   k!rules set <id du salon> [nom]  publie le dossier <nom> (par défaut celui déjà publié dans ce salon, sinon
//                              « informations ») ; relancée, republie seulement ce qui a changé (rien n'est modifié sur
//                              place : Discord afficherait « (modifié) ») ; les autres salons publiés sont ensuite remis
//                              à jour (leurs liens vers ce salon ont pu changer) ;
//   k!rules edit [nom]         menu des encadrés (sections, fils) ; chacun s'ouvre dans un formulaire Discord ; à l'envoi
//                              le fichier est réécrit et tous les salons publiés sont mis à jour ;
//   k!rules images [nom]       même menu ; formulaire « image » (en bas de l'encadré) et « miniature » (en haut à
//                              droite) : un lien est téléchargé et gardé dans <dossier>/images/ (les liens d'images
//                              Discord expirent), un nom de fichier déjà présent est gardé, un champ vide retire l'image.
// Textes d'un dossier (relus à chaque commande, pas besoin de redémarrer le bot) :
//   NN-<section>.md : « # Gros titre » (ou « # » seul : pas de titre), lignes facultatives « couleur: #rrggbb » et
//                     « groupe: <sous-dossier> », puis le texte ; avec un groupe, une ligne « --- » sépare le texte
//                     placé avant les liens des fils de celui placé après ;
//   <groupe>/<fiche>.md : un fil par fichier (ordre alphabétique), « # Nom du fil », « couleur: » facultative, texte ;
//                     une ligne « --- » coupe le texte en plusieurs encadrés (un fil long) ; « liste: non » : fil
//                     absent de la liste de la section ; « retour: <fiche> » : « Revenir » mène à ce fil-là ;
//   dans les deux, lignes facultatives « image: <fichier> » et « miniature: <fichier> » (fichiers de <dossier>/images/) ;
//   liens dans les textes : {salon:<nom>} ; {fil:<groupe>/<fiche>} (même salon) ; {fil:<nom>/<groupe>/<fiche>} ;
//   _separation.gif (ou .png) : l'image entre les sections.
// k!rules edit réécrit ces fichiers SUR L'HÉBERGEMENT : les récupérer avant d'y renvoyer reglement/ ou salons/.
// Les identifiants Discord (salon, messages, fils) sont gardés dans reglement-etat.json, jamais commité.

import { createHash } from 'node:crypto';
import { readdirSync, readFileSync, writeFileSync, existsSync, statSync, mkdirSync, unlinkSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  ActionRowBuilder, ChannelType, MessageFlags, ModalBuilder, PermissionFlagsBits, StringSelectMenuBuilder,
  TextInputBuilder, TextInputStyle, ThreadAutoArchiveDuration,
} from 'discord.js';
import { PERMISSIONS_BOT, effacerAnnoncesDeFils, verrouillerSalon } from './wiki.js';

let RACINE = fileURLToPath(new URL('../', import.meta.url));
let FICHIER_ETAT = join(RACINE, 'reglement-etat.json');
/** Tests : textes et état ailleurs que dans le dossier du bot. */
export function utiliserRacine(racine) {
  RACINE = racine;
  FICHIER_ETAT = join(racine, 'reglement-etat.json');
}

export const PRINCIPAL = 'informations';
/** Rôle autorisé (nom, sans tenir compte des majuscules ni d'un emoji autour). */
const ROLE_AUTORISE = 'le fameux fruit';
const MAX_DESCRIPTION = 4096;
const MAX_MESSAGE = 6000;
const MAX_NOM_FIL = 100;
const MAX_CHAMP = 4000;
const MAX_PARTIES_FORMULAIRE = 3;
const COULEUR_DEFAUT = 0x8b3dff;
const SANS_PING = { parse: [] };
const META = /^(couleur|groupe|image|miniature|liste|retour)\s*:\s*(.*)$/i;
const SEPARATEUR = /\n-{3,}\n/;
const LIEN = /\{(salon|fil):([a-z0-9/-]+)\}/g;
const NOM = /^[a-z0-9-]+$/;
const IMAGES = 'images';
/** Taille maximale d'une image téléchargée (un bot envoie 10 Mo au plus par message). */
const MAX_IMAGE = 8 * 1024 * 1024;
const TYPES_IMAGE = { 'image/png': 'png', 'image/jpeg': 'jpg', 'image/gif': 'gif', 'image/webp': 'webp' };

let enCours = false;

function aLeRole(member) {
  return Boolean(member?.roles?.cache?.some((r) => r.name.toLowerCase().includes(ROLE_AUTORISE)));
}

/** Dossier des textes d'un salon. */
export function dossierDe(nom) {
  return nom === PRINCIPAL ? join(RACINE, 'reglement') : join(RACINE, 'salons', nom);
}

/** Les salons qui ont un dossier de textes : « informations » puis salons/<nom>/. */
export function nomsSalons() {
  const noms = [PRINCIPAL];
  const d = join(RACINE, 'salons');
  if (existsSync(d)) {
    noms.push(...readdirSync(d).filter((n) => NOM.test(n) && statSync(join(d, n)).isDirectory())
      .sort((a, b) => a.localeCompare(b, 'fr')));
  }
  return noms;
}

function verifierNom(nom) {
  if (!nomsSalons().includes(nom)) throw new Error(`salon « ${nom} » inconnu (dossiers : ${nomsSalons().join(', ')})`);
  return nom;
}

export async function commande(message, args) {
  if (!message.inGuild()) return;
  if (!aLeRole(message.member)) return; // silence : la commande n'existe pas pour les autres
  const [action, idSalon, nomDemande] = args;
  const act = action?.toLowerCase();
  if (act === 'edit' || act === 'images') {
    const mode = act === 'images' ? 'i' : 'e';
    try {
      const nom = idSalon?.toLowerCase();
      const noms = nomsSalons();
      if (nom) verifierNom(nom);
      const components = !nom && noms.length > 1 ? [menuSalons(mode)] : [menuEncadres(nom ?? PRINCIPAL, mode)];
      await message.reply({
        content: mode === 'i' ? 'Les images de quel encadré ?' : 'Quel encadré modifier ?', components, allowedMentions: SANS_PING,
      });
    } catch (error) {
      await message.reply({ content: `Impossible : ${error.message}`, allowedMentions: SANS_PING });
    }
    return;
  }
  const salonId = idSalon?.replace(/^<#(\d+)>$/, '$1');
  if (act !== 'set' || !/^\d{17,20}$/.test(salonId ?? '')) {
    await message.reply({
      content: "`k!rules set <id du salon> [nom]` publie un salon (clic droit sur le salon > Copier l'identifiant ; "
        + `noms : ${nomsSalons().join(', ')}) ; \`k!rules edit [nom]\` modifie un encadré ; \`k!rules images [nom]\` change ses images.`,
      allowedMentions: SANS_PING,
    });
    return;
  }
  if (enCours) {
    await message.reply({ content: 'Une publication est déjà en cours.', allowedMentions: SANS_PING });
    return;
  }
  enCours = true;
  try {
    const rapport = await publier(message.guild, salonId, nomDemande?.toLowerCase());
    const autres = await publierTout(message.guild, salonId);
    await message.reply({ content: [rapport, ...autres].join('\n').slice(0, 2000), allowedMentions: SANS_PING });
  } catch (error) {
    console.error('k!rules :', error);
    await message.reply({ content: `Rien de publié : ${error.message}`, allowedMentions: SANS_PING }).catch(() => {});
  } finally {
    enCours = false;
  }
}

// ---------------------------------------------------------------------------------------------------------------
// Lecture des textes.

/** Lit un dossier : les sections dans l'ordre des fichiers, les fils de leurs groupes, l'image de séparation. */
export function lireSalon(dossier = dossierDe(PRINCIPAL)) {
  if (!existsSync(dossier)) throw new Error(`dossier ${dossier} introuvable`);
  const md = (d) => readdirSync(d).filter((f) => f.endsWith('.md') && !f.startsWith('_'))
    .sort((a, b) => a.localeCompare(b, 'fr', { numeric: true }));
  const sections = md(dossier).map((f) => {
    const s = lireFichier(readFileSync(join(dossier, f), 'utf8'), f);
    const [avant, apres = '', ...trop] = s.texte.split(SEPARATEUR).map((t) => t.trim());
    if (trop.length) throw new Error(`${f} : une seule ligne « --- » par section`);
    const section = {
      cle: f.replace(/\.md$/, ''), titre: s.titre, couleurPropre: s.couleur, couleur: s.couleur ?? COULEUR_DEFAUT,
      groupe: s.groupe, avant, apres, fils: [], image: s.image, miniature: s.miniature,
    };
    verifierImages(dossier, s, f);
    if (!section.titre && !avant && !s.groupe) throw new Error(`${f} : section vide`);
    if (s.groupe) {
      if (!NOM.test(s.groupe)) throw new Error(`${f} : groupe « ${s.groupe} » invalide (minuscules, chiffres, tirets)`);
      const d = join(dossier, s.groupe);
      if (!existsSync(d) || !statSync(d).isDirectory()) throw new Error(`${f} : dossier ${s.groupe}/ introuvable`);
      section.fils = md(d).map((g) => {
        const origine = `${s.groupe}/${g}`;
        const p = lireFichier(readFileSync(join(d, g), 'utf8'), origine);
        if (!p.titre) throw new Error(`${origine} : nom du fil vide`);
        if (p.titre.length > MAX_NOM_FIL) throw new Error(`${origine} : nom du fil trop long (${p.titre.length} > ${MAX_NOM_FIL})`);
        if (!p.texte) throw new Error(`${origine} : texte vide`);
        const parties = p.texte.split(SEPARATEUR).map((t) => t.trim());
        if (parties.some((t) => !t)) throw new Error(`${origine} : partie vide (deux « --- » de suite ?)`);
        verifierImages(dossier, p, origine);
        return {
          cle: `${s.groupe}/${g.replace(/\.md$/, '')}`, titre: p.titre, couleurPropre: p.couleur, couleur: p.couleur ?? section.couleur,
          texte: p.texte, parties, image: p.image, miniature: p.miniature, liste: p.liste, retour: p.retour,
        };
      });
      if (!section.fils.length) throw new Error(`${s.groupe}/ : aucun fil`);
      for (const fil of section.fils) {
        if (fil.retour && !section.fils.some((x) => x.cle === `${s.groupe}/${fil.retour}`)) {
          throw new Error(`${fil.cle} : « retour: ${fil.retour} » ne correspond à aucun fil de ${s.groupe}/`);
        }
      }
    }
    return section;
  });
  if (!sections.length) throw new Error(`aucune section dans ${dossier}`);
  const image = ['_separation.gif', '_separation.png'].map((f) => join(dossier, f)).find(existsSync) ?? null;
  return { sections, image, dossier };
}

function verifierImages(dossier, lu, origine) {
  for (const nom of [lu.image, lu.miniature].filter(Boolean)) {
    if (!/^[\w.-]+$/.test(nom) || !existsSync(join(dossier, IMAGES, nom))) throw new Error(`${origine} : image ${IMAGES}/${nom} introuvable`);
  }
}

function lireFichier(brut, origine) {
  const lignes = brut.replace(/^﻿/, '').replace(/\r\n?/g, '\n').split('\n');
  const premiere = lignes.shift() ?? '';
  if (!/^#(\s.*)?$/.test(premiere)) throw new Error(`${origine} : la première ligne doit être « # Titre » (ou « # » seul)`);
  const lu = { couleur: null, groupe: null, image: null, miniature: null, liste: true, retour: null };
  while (META.test(lignes[0] ?? '')) {
    const [, cleBrute, valeurBrute] = lignes.shift().match(META);
    const cle = cleBrute.toLowerCase();
    const valeur = valeurBrute.trim();
    if (cle === 'couleur') {
      if (!/^#[0-9a-f]{6}$/i.test(valeur)) throw new Error(`${origine} : couleur « ${valeur} » invalide (format #rrggbb)`);
      lu.couleur = parseInt(valeur.slice(1), 16);
    } else if (cle === 'liste') {
      if (!/^(oui|non)$/i.test(valeur)) throw new Error(`${origine} : « liste: ${valeur} » invalide (oui ou non)`);
      lu.liste = valeur.toLowerCase() === 'oui';
    } else if (cle === 'retour') {
      if (valeur && !NOM.test(valeur)) throw new Error(`${origine} : « retour: ${valeur} » invalide (nom de fichier sans .md)`);
      lu.retour = valeur || null;
    } else {
      lu[cle] = valeur || null;
    }
  }
  return { titre: premiere.replace(/^#/, '').trim(), ...lu, texte: lignes.join('\n').trim() };
}

// ---------------------------------------------------------------------------------------------------------------
// Liens entre salons et fils : {salon:<nom>}, {fil:<groupe>/<fiche>}, {fil:<nom>/<groupe>/<fiche>}.

/**
 * Fonction qui remplace les liens d'un texte du salon <nom>. ids(nom, cleFil) donne l'identifiant d'un fil publié
 * (ou null), idSalon(nom) celui d'un salon publié. Un lien vers un fil ou un salon pas encore publié devient son nom
 * en gras ; un lien vers un fil ou un salon qui n'existe pas est une erreur. Avec « pire », chaque lien prend la
 * plus grande place possible (vérification des longueurs).
 */
export function resolveur(nom, { ids = () => null, idSalon = () => null, pire = false } = {}) {
  const lus = new Map();
  const lire = (n) => {
    if (!lus.has(n)) lus.set(n, lireSalon(dossierDe(verifierNom(n))));
    return lus.get(n);
  };
  const idMax = `<#${'9'.repeat(20)}>`;
  return (texte) => texte.replace(LIEN, (tout, genre, chemin) => {
    const morceaux = chemin.split('/');
    if (genre === 'salon') {
      if (morceaux.length !== 1) throw new Error(`lien ${tout} : forme {salon:<nom>}`);
      verifierNom(chemin);
      const repli = `**#${chemin}**`;
      if (pire) return repli.length > idMax.length ? repli : idMax;
      const id = idSalon(chemin);
      return id ? `<#${id}>` : repli;
    }
    if (morceaux.length !== 2 && morceaux.length !== 3) throw new Error(`lien ${tout} : forme {fil:<groupe>/<fiche>} ou {fil:<salon>/<groupe>/<fiche>}`);
    const salon = morceaux.length === 3 ? morceaux[0] : nom;
    const cle = morceaux.slice(-2).join('/');
    const fil = lire(salon).sections.flatMap((s) => s.fils).find((f) => f.cle === cle);
    if (!fil) throw new Error(`lien ${tout} : fil ${cle} introuvable dans le salon « ${salon} »`);
    const repli = `**${fil.titre}**`;
    if (pire) return repli.length > idMax.length ? repli : idMax;
    const id = ids(salon, cle);
    return id ? `<#${id}>` : repli;
  });
}

// ---------------------------------------------------------------------------------------------------------------
// Encadrés.

const tel = (t) => t;

/** Les embeds d'une section : le gros titre (s'il y en a un) puis le texte (avec les liens de ses fils listés). */
export function embedsSection(section, idsFils, resoudre = tel) {
  const embeds = [];
  if (section.titre) embeds.push({ color: section.couleur, description: `# ${section.titre}` });
  const liens = section.fils.filter((f) => f.liste !== false)
    .map((f) => `📖 ${idsFils[f.cle] ? `<#${idsFils[f.cle]}>` : `**${f.titre}**`}`).join('\n');
  const texte = [resoudre(section.avant), liens, resoudre(section.apres)].filter(Boolean).join('\n\n');
  if (texte.length > MAX_DESCRIPTION) throw new Error(`${section.cle} : texte trop long (${texte.length} > ${MAX_DESCRIPTION} caractères)`);
  if (texte) embeds.push({ color: section.couleur, description: texte });
  // Miniature sur l'encadré du gros titre, image en bas de celui du texte (un seul encadré : les deux dessus).
  avecImages(embeds[0], { miniature: section.miniature });
  avecImages(embeds[embeds.length - 1], { image: section.image });
  return embeds;
}

/** Image (en bas) et miniature (en haut à droite) d'un encadré : fichiers joints au message (attachment://). */
function avecImages(embed, { image, miniature }) {
  if (image) embed.image = { url: `attachment://${image}` };
  if (miniature) embed.thumbnail = { url: `attachment://${miniature}` };
  return embed;
}

/** Les fichiers à joindre à un message pour ses embeds. */
function fichiersDe(embeds, dossier) {
  const noms = new Set(embeds.flatMap((e) => [e.image?.url, e.thumbnail?.url]).filter(Boolean)
    .map((u) => u.replace('attachment://', '')));
  return [...noms].map((nom) => ({ attachment: join(dossier, IMAGES, nom), name: nom }));
}

/**
 * Les messages d'un fil (listes d'embeds) : un embed par partie, le dernier terminé par le lien de retour ; plusieurs
 * messages si les parties dépassent ce qu'un message peut porter (6 000 caractères, 10 encadrés).
 */
export function messagesFil(fil, lienRetour, resoudre = tel) {
  const textes = fil.parties.map(resoudre);
  textes[textes.length - 1] += `\n\n↩️ [Revenir au sommaire](${lienRetour})`;
  // Un texte qui commence par un titre Markdown (« # … ») remplace le titre de l'encadré (plus gros).
  const titre = /^#{1,3}\s/.test(textes[0]) ? undefined : fil.titre;
  const embeds = textes.map((t, i) => {
    if (t.length > MAX_DESCRIPTION) throw new Error(`${fil.titre} : partie ${i + 1} trop longue (${t.length} > ${MAX_DESCRIPTION} caractères)`);
    return { title: i === 0 ? titre : undefined, color: fil.couleur, description: t };
  });
  avecImages(embeds[0], { miniature: fil.miniature });
  avecImages(embeds[embeds.length - 1], { image: fil.image });
  const messages = [];
  let lot = [];
  let taille = 0;
  for (const e of embeds) {
    const n = e.description.length + (e.title?.length ?? 0);
    if (lot.length && (taille + n > MAX_MESSAGE || lot.length === 10)) {
      messages.push(lot);
      lot = [];
      taille = 0;
    }
    lot.push(e);
    taille += n;
  }
  messages.push(lot);
  return messages;
}

/** Compatibilité : l'embed d'un fil d'une seule partie. */
export function embedFil(fil, lienSection, resoudre = tel) {
  return messagesFil({ ...fil, parties: fil.parties ?? [fil.texte] }, lienSection, resoudre)[0][0];
}

// ---------------------------------------------------------------------------------------------------------------
// Publication.

/** Republie les salons connus autres que <sauf> (salons supprimés oubliés) ; renvoie leurs rapports. */
async function publierTout(guild, sauf = null) {
  const rapports = [];
  for (const salonId of Object.keys(lireEtat().salons)) {
    if (salonId === sauf) continue;
    const salon = await guild.channels.fetch(salonId).catch(() => null);
    if (!salon) {
      // Salon supprimé (par exemple un salon de test) : on l'oublie.
      const etat = lireEtat();
      delete etat.salons[salonId];
      ecrireEtat(etat);
      continue;
    }
    rapports.push(await publier(guild, salonId).catch((error) => `<#${salonId}> : non mis à jour (${error.message})`));
  }
  return rapports;
}

export async function publier(guild, salonId, nomDemande) {
  const etat = lireEtat();
  const dejaPublie = etat.salons[salonId]?.dossier ?? (etat.salons[salonId] ? PRINCIPAL : null);
  if (nomDemande && dejaPublie && nomDemande !== dejaPublie) {
    throw new Error(`<#${salonId}> publie déjà « ${dejaPublie} » : choisis un autre salon (ou vide celui-ci et oublie-le dans reglement-etat.json)`);
  }
  const nom = verifierNom(nomDemande ?? dejaPublie ?? PRINCIPAL);
  const dossier = dossierDe(nom);
  const { sections, image } = lireSalon(dossier);

  const salon = await guild.channels.fetch(salonId).catch(() => null);
  if (!salon) throw new Error(`salon ${salonId} introuvable sur ce serveur Discord`);
  if (salon.type !== ChannelType.GuildText) throw new Error(`#${salon.name} n'est pas un salon textuel`);
  const moi = guild.members.me;
  const avecFichiers = image || sections.some((s) => s.image || s.miniature || s.fils.some((f) => f.image || f.miniature));
  const permissions = { ...PERMISSIONS_BOT, ...(avecFichiers ? { AttachFiles: 'joindre des fichiers' } : {}) };
  const manquantes = Object.entries(permissions)
    .filter(([cle]) => !salon.permissionsFor(moi).has(PermissionFlagsBits[cle]))
    .map(([, n]) => n);
  if (manquantes.length) throw new Error(`il manque au bot, dans #${salon.name} : ${manquantes.join(', ')}`);

  etat.salons[salonId] ??= { messages: [], fils: {} };
  const memo = etat.salons[salonId];
  memo.dossier = nom;
  migrer(memo);
  ecrireEtat(etat);

  const avertissements = [];
  await verrouillerSalon(salon, moi).catch((error) => {
    avertissements.push(`salon non verrouillé pour les membres (${error.message}) : retire-leur « Envoyer des messages » et « … dans les fils » à la main`);
  });

  // Aucun message n'est jamais modifié (Discord afficherait « (modifié) ») : ce qui change est supprimé et republié.
  // Ordre : 1. les fils (vides) pour connaître leurs identifiants ; 2. la mise en page, sections avec les liens des
  // fils ; 3. le texte de chaque fil, avec le lien de sa section.

  // 1. Les fils de chaque groupe, créés vides au besoin.
  const compte = { crees: 0, republies: 0, inchanges: 0 };
  const fils = {};
  for (const s of sections) {
    for (const f of s.fils) {
      const connu = memo.fils[f.cle];
      let fil = connu ? await salon.threads.fetch(connu.id).catch(() => null) : null;
      if (fil && fil.parentId !== salon.id) fil = null;
      if (!fil) {
        fil = await salon.threads.create({
          name: f.titre,
          type: ChannelType.PublicThread,
          autoArchiveDuration: ThreadAutoArchiveDuration.OneWeek,
          reason: "Salon d'informations KaLium",
        });
        memo.fils[f.cle] = { id: fil.id, messages: [] };
        ecrireEtat(etat);
      } else {
        // Un fil archivé (une semaine sans message) ne se modifie pas : on le rouvre, il reste verrouillé.
        if (fil.archived) await fil.setArchived(false, 'Mise à jour');
        if (fil.name !== f.titre) await fil.setName(f.titre);
      }
      fils[f.cle] = fil;
    }
  }
  await effacerAnnoncesDeFils(salon, moi.id);
  const idsFils = Object.fromEntries(Object.entries(fils).map(([cle, fil]) => [cle, fil.id]));

  // Les liens : fils de ce salon (créés ci-dessus), fils et salons des autres salons publiés.
  const salonDe = (n) => Object.entries(etat.salons).find(([, m]) => (m.dossier ?? PRINCIPAL) === n);
  const resoudre = resolveur(nom, {
    ids: (n, cle) => (n === nom ? idsFils[cle] : salonDe(n)?.[1].fils?.[cle]?.id) ?? null,
    idSalon: (n) => (n === nom ? salonId : salonDe(n)?.[0]) ?? null,
  });

  // 2. La mise en page : [image] section [image] section … [image]. Republiée en entier dès qu'un texte, l'ordre ou
  //    l'image change, ou qu'un message a disparu.
  const plan = [];
  for (const s of sections) {
    if (image) plan.push({ type: 'image' });
    plan.push({ type: 'section', cle: s.cle, embeds: embedsSection(s, idsFils, resoudre) });
  }
  if (image) plan.push({ type: 'image' });
  const memePlan = memo.messages.length === plan.length
    && plan.every((p, i) => memo.messages[i].type === p.type && memo.messages[i].cle === p.cle);
  let messages = memePlan ? await Promise.all(memo.messages.map((m) => salon.messages.fetch(m.id).catch(() => null))) : [];
  const identique = memePlan && messages.every(Boolean) && plan.every((p, i) => p.type === 'image' || pareil(messages[i], p.embeds));
  if (!identique) {
    for (const m of memo.messages) await salon.messages.delete(m.id).catch(() => {});
    memo.messages = [];
    messages = [];
    for (const p of plan) {
      const m = p.type === 'image'
        ? await salon.send({ files: [{ attachment: image, name: `separation.${image.split('.').pop()}` }] })
        : await salon.send({ embeds: p.embeds, files: fichiersDe(p.embeds, dossier), allowedMentions: SANS_PING });
      memo.messages.push({ type: p.type, cle: p.cle, id: m.id });
      messages.push(m);
      ecrireEtat(etat);
    }
  }
  const lienDe = (cle) => messages[plan.findIndex((p) => p.cle === cle)].url;

  // 3. Le texte de chaque fil, republié s'il a changé (ou si un lien a changé).
  for (const s of sections) {
    for (const f of s.fils) {
      const fil = fils[f.cle];
      const connu = memo.fils[f.cle];
      const retour = f.retour ? `https://discord.com/channels/${guild.id}/${idsFils[`${s.groupe}/${f.retour}`]}` : lienDe(s.cle);
      const voulus = messagesFil(f, retour, resoudre);
      const anciens = await Promise.all(connu.messages.map((id) => fil.messages.fetch(id).catch(() => null)));
      if (anciens.length === voulus.length && anciens.every((m, i) => m && pareil(m, voulus[i]))) {
        compte.inchanges++;
      } else {
        compte[connu.messages.length ? 'republies' : 'crees']++;
        for (const m of anciens) await m?.delete().catch(() => {});
        connu.messages = [];
        for (const embeds of voulus) {
          connu.messages.push((await fil.send({ embeds, files: fichiersDe(embeds, dossier), allowedMentions: SANS_PING })).id);
          ecrireEtat(etat);
        }
      }
      if (!fil.locked) await fil.setLocked(true, 'Lecture seule');
    }
  }

  const cles = new Set(sections.flatMap((s) => s.fils.map((f) => f.cle)));
  const orphelins = Object.entries(memo.fils).filter(([cle]) => !cles.has(cle));
  if (orphelins.length) {
    avertissements.push(`fils qui ne sont plus dans les textes (gardés, à supprimer à la main) : ${orphelins.map(([, f]) => `<#${f.id}>`).join(', ')}`);
  }
  if (!image) avertissements.push(`pas d'image de séparation (_separation.gif) : sections publiées sans`);
  return [
    `Salon <#${salonId}> (« ${nom} ») ${identique ? 'inchangé' : 'republié'} ; fils : ${compte.crees} nouveau(x), `
      + `${compte.republies} republié(s), ${compte.inchanges} inchangé(s).`,
    ...avertissements.map((a) => `⚠️ ${a}`),
  ].join('\n').slice(0, 2000);
}

/** Le message publié a-t-il les embeds voulus (titre, texte, couleur, images), sans mention « (modifié) » ? */
function pareil(message, voulus) {
  const publies = message.embeds;
  // Une image jointe est servie à une adresse qui finit par son nom de fichier (qui contient l'empreinte de l'image).
  const memeImage = (publiee, voulue) => (voulue
    ? Boolean(publiee?.url?.split('?')[0].endsWith(`/${voulue.url.replace('attachment://', '')}`))
    : !publiee);
  return !message.editedTimestamp && publies.length === voulus.length && voulus.every((e, i) => (publies[i].title ?? undefined) === e.title
    && publies[i].description === e.description && publies[i].color === e.color
    && memeImage(publies[i].image, e.image) && memeImage(publies[i].thumbnail, e.thumbnail));
}

/** États anciens -> état actuel : 0.5 (un seul message de sommaire, fils « a-respect »), 0.7 (un message par fil). */
function migrer(memo) {
  memo.messages ??= [];
  memo.fils ??= {};
  if (memo.sommaire) {
    memo.messages = [{ type: 'ancien', id: memo.sommaire }]; // supprimé à la republication
    delete memo.sommaire;
  }
  for (const cle of Object.keys(memo.fils)) {
    if (!cle.includes('/')) {
      memo.fils[`regles/${cle}`] ??= memo.fils[cle];
      delete memo.fils[cle];
    }
  }
  for (const f of Object.values(memo.fils)) {
    if (!Array.isArray(f.messages)) f.messages = f.message ? [f.message] : [];
    delete f.message;
  }
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

// ---------------------------------------------------------------------------------------------------------------
// k!rules edit : (salon ->) (section ->) encadré -> formulaire Discord -> fichier réécrit -> salons publiés mis à jour.
// Valeurs des menus : « s:<section> » ou « f:<groupe>/<fil> ». Mode : « e » (texte) ou « i » (images).
// customId : rules:salon:<mode> ; rules:section:<mode>:<salon> ; rules:choix:<mode>:<salon> ;
//            rules:modal:<salon>:<valeur> ; rules:img:<salon>:<valeur>.

export function menuSalons(mode) {
  const menu = new StringSelectMenuBuilder().setCustomId(`rules:salon:${mode}`).setPlaceholder('Choisir un salon')
    .addOptions(nomsSalons().slice(0, 25).map((n) => ({ label: n, value: n, emoji: '📂' })));
  return new ActionRowBuilder().addComponents(menu);
}

/** Menu des encadrés d'un salon ; s'il y en a plus de 25, d'abord le menu des sections (puis celui d'une section). */
export function menuEncadres(nom = PRINCIPAL, mode = 'e', cleSection = null) {
  const { sections } = lireSalon(dossierDe(nom));
  const options = [];
  const ajouter = (s) => {
    options.push({ label: (s.titre || s.cle).slice(0, 100), value: `s:${s.cle}`, emoji: '📌', description: 'Section du salon' });
    for (const f of s.fils) options.push({ label: f.titre.slice(0, 100), value: `f:${f.cle}`, emoji: '📖', description: `Fil · ${(s.titre || s.cle).slice(0, 90)}` });
  };
  if (cleSection) {
    const s = sections.find((x) => x.cle === cleSection);
    if (!s) throw new Error('cette section n\'existe plus');
    ajouter(s);
  } else {
    sections.forEach(ajouter);
  }
  if (options.length > 25 && !cleSection) {
    const menu = new StringSelectMenuBuilder().setCustomId(`rules:section:${mode}:${nom}`).setPlaceholder('Choisir une section')
      .addOptions(sections.slice(0, 25).map((s) => ({
        label: (s.titre || s.cle).slice(0, 100), value: s.cle, emoji: '📌', description: `${s.fils.length} fil(s)`,
      })));
    return new ActionRowBuilder().addComponents(menu);
  }
  const menu = new StringSelectMenuBuilder().setCustomId(`rules:choix:${mode}:${nom}`).setPlaceholder('Choisir un encadré')
    .addOptions(options.slice(0, 25));
  return new ActionRowBuilder().addComponents(menu);
}

const hex = (couleur) => (couleur == null ? '' : `#${couleur.toString(16).padStart(6, '0')}`);

function champ(id, label, style, valeur, { requis = true, max = MAX_CHAMP, placeholder } = {}) {
  const input = new TextInputBuilder().setCustomId(id).setLabel(label).setStyle(style).setRequired(requis).setMaxLength(max);
  if (valeur) input.setValue(valeur.slice(0, max));
  if (placeholder) input.setPlaceholder(placeholder);
  return new ActionRowBuilder().addComponents(input);
}

/** Retrouve l'encadré d'une valeur du menu : { section } ou { section, fil }, ou null. */
function trouver(valeur, dossier) {
  const { sections } = lireSalon(dossier);
  if (valeur.startsWith('s:')) {
    const section = sections.find((s) => s.cle === valeur.slice(2));
    return section ? { section } : null;
  }
  for (const section of sections) {
    const fil = section.fils.find((f) => f.cle === valeur.slice(2));
    if (fil) return { section, fil };
  }
  return null;
}

/** Formulaire prérempli avec le texte actuel de l'encadré (null : encadré disparu ; erreur : trop long à modifier). */
export function formulaire(valeur, nom = PRINCIPAL) {
  const trouve = trouver(valeur, dossierDe(nom));
  if (!trouve) return null;
  const modal = new ModalBuilder().setCustomId(`rules:modal:${nom}:${valeur}`);
  const { section, fil } = trouve;
  if (!fil) {
    const rows = [
      champ('titre', 'Gros titre (facultatif)', TextInputStyle.Short, section.titre, { requis: false, max: 256 }),
      champ('couleur', 'Couleur (#rrggbb)', TextInputStyle.Short, hex(section.couleurPropre),
        { requis: false, max: 7, placeholder: `vide = ${hex(COULEUR_DEFAUT)} ; aussi celle de ses fils sans couleur` }),
      champ('texte', section.groupe ? 'Texte avant la liste des fils' : 'Texte', TextInputStyle.Paragraph, section.avant,
        { requis: !section.groupe, max: 3500 }),
    ];
    if (section.groupe) {
      rows.push(champ('apres', 'Texte après la liste des fils (facultatif)', TextInputStyle.Paragraph, section.apres,
        { requis: false, max: 1000 }));
    }
    return modal.setTitle((section.titre || 'Section').slice(0, 45)).addComponents(...rows);
  }
  if (fil.parties.length > MAX_PARTIES_FORMULAIRE) {
    throw new Error(`ce fil a ${fil.parties.length} parties, le formulaire en montre ${MAX_PARTIES_FORMULAIRE} au plus : modifier son fichier ${fil.cle}.md`);
  }
  const rows = [
    champ('titre', 'Nom du fil', TextInputStyle.Short, fil.titre,
      { max: MAX_NOM_FIL, placeholder: "Aussi le titre de l'encadré, sauf si le texte commence par « # »" }),
    champ('couleur', 'Couleur (#rrggbb, facultatif)', TextInputStyle.Short, hex(fil.couleurPropre),
      { requis: false, max: 7, placeholder: `vide = celle de la section (${hex(section.couleur)})` }),
  ];
  for (let i = 0; i < MAX_PARTIES_FORMULAIRE; i++) {
    rows.push(champ(`partie${i}`, i === 0 ? 'Texte' : `Suite (encadré ${i + 1}, facultatif)`, TextInputStyle.Paragraph, fil.parties[i],
      { requis: i === 0, max: 3900, placeholder: i === 0 ? 'Le lien « Revenir au sommaire » est ajouté tout seul à la fin.' : 'Vide = pas d\'encadré de plus' }));
  }
  return modal.setTitle(fil.titre.slice(0, 45)).addComponents(...rows);
}

/** Formulaire des images d'un encadré, prérempli avec les noms des fichiers actuels. */
export function formulaireImages(valeur, nom = PRINCIPAL) {
  const trouve = trouver(valeur, dossierDe(nom));
  if (!trouve) return null;
  const e = trouve.fil ?? trouve.section;
  const conseil = "Lien d'image (https://…) ; vide = pas d'image";
  return new ModalBuilder().setCustomId(`rules:img:${nom}:${valeur}`)
    .setTitle(`Images · ${e.titre || e.cle}`.slice(0, 45))
    .addComponents(
      champ('image', "Image (en bas de l'encadré)", TextInputStyle.Short, e.image, { requis: false, max: 1000, placeholder: conseil }),
      champ('miniature', 'Miniature (en haut à droite)', TextInputStyle.Short, e.miniature, { requis: false, max: 1000, placeholder: conseil }),
    );
}

/**
 * Valeur d'un champ d'image : vide = pas d'image ; le nom actuel = inchangé ; un lien = image téléchargée dans
 * <dossier>/images/, nommée d'après l'encadré et l'empreinte de son contenu. Renvoie le nom du fichier, ou null.
 */
export async function imageDuChamp(valeur, actuel, base, dossier = dossierDe(PRINCIPAL)) {
  valeur = (valeur ?? '').trim();
  if (!valeur) return null;
  if (valeur === actuel) return actuel;
  if (!/^https?:\/\//i.test(valeur)) throw new Error(`« ${valeur.slice(0, 80)} » n'est pas un lien d'image (https://…)`);
  const reponse = await fetch(valeur, { signal: AbortSignal.timeout(15000) }).catch((error) => {
    throw new Error(`image injoignable (${error.message})`);
  });
  if (!reponse.ok) throw new Error(`image injoignable (HTTP ${reponse.status})`);
  const extension = TYPES_IMAGE[(reponse.headers.get('content-type') ?? '').split(';')[0].trim().toLowerCase()];
  if (!extension) throw new Error("ce lien ne mène pas à une image PNG, JPEG, GIF ou WebP (sur Discord : clic droit sur l'image > Copier le lien)");
  const octets = Buffer.from(await reponse.arrayBuffer());
  if (octets.length > MAX_IMAGE) throw new Error(`image trop lourde (${(octets.length / 1048576).toFixed(1)} Mo, 8 Mo au plus)`);
  const nom = `${base}-${createHash('sha1').update(octets).digest('hex').slice(0, 10)}.${extension}`;
  mkdirSync(join(dossier, IMAGES), { recursive: true });
  writeFileSync(join(dossier, IMAGES, nom), octets);
  return nom;
}

/** Retire de <dossier>/images/ les fichiers qu'aucun encadré n'utilise plus. */
function nettoyerImages(dossier) {
  const d = join(dossier, IMAGES);
  if (!existsSync(d)) return;
  const { sections } = lireSalon(dossier);
  const utilisees = new Set(sections.flatMap((s) => [s.image, s.miniature, ...s.fils.flatMap((f) => [f.image, f.miniature])]).filter(Boolean));
  for (const nom of readdirSync(d)) {
    if (!utilisees.has(nom) && /\.(png|jpe?g|gif|webp)$/i.test(nom)) unlinkSync(join(d, nom));
  }
}

/** Contenu d'un fichier à partir des champs du formulaire (sans l'écrire). Fil : texte = parties jointes par « --- ». */
export function contenuFichier({ estFil, groupe, titre, couleur, texte, apres, image, miniature, liste = true, retour = null }) {
  titre = (titre ?? '').replace(/\s+/g, ' ').trim();
  if (!titre && estFil) throw new Error('nom du fil vide');
  couleur = couleur?.trim() ?? '';
  if (couleur && !couleur.startsWith('#')) couleur = `#${couleur}`;
  if (couleur && !/^#[0-9a-f]{6}$/i.test(couleur)) throw new Error(`couleur « ${couleur} » invalide (format #rrggbb, par exemple #8b3dff)`);
  const net = (t) => (t ?? '').replace(/\r\n?/g, '\n').trim();
  texte = net(texte);
  apres = net(apres);
  if (estFil && !texte) throw new Error('texte vide');
  if (!estFil && !titre && !texte && !groupe) throw new Error('section vide');
  // Une ligne « --- » seule sépare les deux textes d'une section (et les encadrés d'un fil, où le formulaire la pose).
  if (!estFil && (/^-{3,}$/m.test(texte) || /^-{3,}$/m.test(apres))) throw new Error('une ligne « --- » seule est réservée, retire-la');
  // Une première ligne « couleur: », « image: »… serait prise pour un réglage.
  if (META.test(texte.split('\n')[0])) throw new Error('le texte ne peut pas commencer par « couleur: », « groupe: », « image: »…');
  const lignes = [titre ? `# ${titre}` : '#'];
  if (couleur) lignes.push(`couleur: ${couleur.toLowerCase()}`);
  if (!estFil && groupe) lignes.push(`groupe: ${groupe}`);
  if (estFil && liste === false) lignes.push('liste: non');
  if (estFil && retour) lignes.push(`retour: ${retour}`);
  if (image) lignes.push(`image: ${image}`);
  if (miniature) lignes.push(`miniature: ${miniature}`);
  lignes.push('');
  if (texte) lignes.push(texte);
  if (!estFil && groupe && apres) lignes.push('', '---', '', apres);
  return `${lignes.join('\n').trimEnd()}\n`;
}

/** Clics des menus et envois de formulaire dont l'identifiant commence par « rules: ». */
export async function interaction(inter) {
  if (!inter.inGuild()) return;
  const member = aLeRole(inter.member) ? inter.member : await inter.guild.members.fetch(inter.user.id).catch(() => null);
  if (!aLeRole(member)) {
    await inter.reply({ content: 'Réservé au rôle Le fameux fruit.', flags: MessageFlags.Ephemeral });
    return;
  }
  const disparu = async () => inter.reply({ content: "Cet encadré n'existe plus : relance `k!rules edit`.", flags: MessageFlags.Ephemeral });
  const [, genre, ...reste] = inter.customId.split(':');

  if (inter.isStringSelectMenu()) {
    try {
      if (genre === 'salon') {
        const nom = verifierNom(inter.values[0]);
        await inter.update({ content: `Salon « ${nom} » : quel encadré ?`, components: [menuEncadres(nom, reste[0])] });
      } else if (genre === 'section') {
        const [mode, nom] = reste;
        await inter.update({ content: `Salon « ${nom} » : quel encadré ?`, components: [menuEncadres(verifierNom(nom), mode, inter.values[0])] });
      } else if (genre === 'choix') {
        const [mode, nom] = reste;
        const modal = mode === 'i' ? formulaireImages(inter.values[0], verifierNom(nom)) : formulaire(inter.values[0], verifierNom(nom));
        if (!modal) await disparu();
        else await inter.showModal(modal);
      }
    } catch (error) {
      await inter.reply({ content: `Impossible : ${error.message}`, flags: MessageFlags.Ephemeral }).catch(() => {});
    }
    return;
  }

  if (!inter.isModalSubmit()) return;
  if (genre !== 'modal' && genre !== 'img') return;
  const [nom, ...v] = reste;
  if (!nomsSalons().includes(nom)) {
    await disparu();
    return;
  }
  const valeur = v.join(':');
  const dossier = dossierDe(nom);
  const trouve = trouver(valeur, dossier);
  if (!trouve) {
    await disparu();
    return;
  }
  await inter.deferReply({ flags: MessageFlags.Ephemeral });
  const champTexte = (id) => {
    try { return inter.fields.getTextInputValue(id); } catch { return ''; }
  };

  const { section, fil } = trouve;
  const e = fil ?? section;
  const fichier = join(dossier, `${e.cle}.md`);
  const ancien = readFileSync(fichier, 'utf8');
  try {
    // Les champs du formulaire, et les valeurs actuelles pour ce que ce formulaire ne montre pas.
    const base = e.cle.replace(/[^a-z0-9-]/gi, '-');
    const texteFil = () => [0, 1, 2].map((i) => champTexte(`partie${i}`).trim()).filter(Boolean).join('\n\n---\n\n');
    const champs = genre === 'modal'
      ? {
        titre: champTexte('titre'), couleur: champTexte('couleur'), texte: fil ? texteFil() : champTexte('texte'),
        apres: champTexte('apres'), image: e.image, miniature: e.miniature,
      }
      : {
        titre: e.titre, couleur: hex(e.couleurPropre), texte: fil ? fil.texte : section.avant, apres: fil ? '' : section.apres,
        image: await imageDuChamp(champTexte('image'), e.image, `${base}-image`, dossier),
        miniature: await imageDuChamp(champTexte('miniature'), e.miniature, `${base}-miniature`, dossier),
      };
    writeFileSync(fichier, contenuFichier({ estFil: Boolean(fil), groupe: section.groupe, liste: fil?.liste, retour: fil?.retour, ...champs }));
    verifierLongueurs(lireSalon(dossier), nom);
  } catch (error) {
    writeFileSync(fichier, ancien);
    await inter.editReply(`Modification refusée : ${error.message}. Rien n'a changé.`);
    return;
  }
  try { nettoyerImages(dossier); } catch { /* au pire, un fichier inutile reste dans images/ */ }

  if (enCours) {
    await inter.editReply('Enregistré, mais une publication est déjà en cours : relance `k!rules set` sur le salon dans un instant.');
    return;
  }
  enCours = true;
  let rapports;
  try {
    rapports = await publierTout(inter.guild);
  } finally {
    enCours = false;
  }
  const fait = genre === 'img' ? 'Images enregistrées.' : 'Texte enregistré.';
  await inter.editReply((rapports.length ? `${fait}\n${rapports.join('\n')}`
    : `${fait} Aucun salon n'est publié : \`k!rules set <id du salon> ${nom}\` pour le publier.`).slice(0, 2000));
}

/** Les textes tiennent-ils dans les encadrés, avec des liens de la plus grande taille possible ? */
export function verifierLongueurs({ sections }, nom = PRINCIPAL) {
  const id = '9'.repeat(20);
  const lien = `https://discord.com/channels/${id}/${id}/${id}`;
  const resoudre = resolveur(nom, { pire: true });
  for (const s of sections) {
    const embeds = embedsSection(s, Object.fromEntries(s.fils.map((f) => [f.cle, id])), resoudre);
    const total = embeds.reduce((n, e) => n + e.description.length, 0);
    if (total > MAX_MESSAGE) throw new Error(`${s.cle} : textes trop longs pour un message (${total} > ${MAX_MESSAGE} caractères)`);
    for (const f of s.fils) messagesFil(f, lien, resoudre);
  }
}
