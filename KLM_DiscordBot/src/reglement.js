// Salon d'informations de KaLium : des sections séparées par une image (barre animée), chacune = un gros titre
// (embed) + un texte (embed). Une section peut porter un groupe de fils (les règles, les concepts) : son texte se
// termine par les liens des fils, et chaque fil (verrouillé, un embed) finit par « Revenir au sommaire ».
//
//   [image] [Bienvenue | description] [image] [Objectif | texte] [image] [Règles | liens] [image] [Concepts | liens] [image]
//
// Commandes texte réservées au rôle « Le fameux fruit » :
//   k!rules set <id du salon>  publie le salon ; relancée, republie seulement ce qui a changé (rien n'est modifié sur
//                              place : Discord afficherait « (modifié) ») ;
//   k!rules edit               menu des encadrés (sections, fils) ; chacun s'ouvre dans un formulaire Discord ; à l'envoi
//                              le fichier de reglement/ est réécrit et tous les salons publiés sont mis à jour ;
//   k!rules images             même menu ; formulaire « image » (en bas de l'encadré) et « miniature » (en haut à
//                              droite) : un lien est téléchargé et gardé dans reglement/images/ (les liens d'images
//                              Discord expirent), un nom de fichier déjà présent est gardé, un champ vide retire l'image.
// Textes dans reglement/ (relus à chaque commande, pas besoin de redémarrer le bot) :
//   NN-<section>.md : « # Gros titre » (ou « # » seul : pas de titre), lignes facultatives « couleur: #rrggbb » et
//                     « groupe: <dossier> », puis le texte ; avec un groupe, une ligne « --- » sépare le texte placé
//                     avant les liens des fils de celui placé après ;
//   <dossier>/<fiche>.md : un fil par fichier (ordre alphabétique), « # Nom du fil », « couleur: » facultative, texte ;
//   dans les deux, lignes facultatives « image: <fichier> » et « miniature: <fichier> » (fichiers de reglement/images/) ;
//   _separation.gif (ou .png) : l'image entre les sections.
// k!rules edit réécrit ces fichiers SUR L'HÉBERGEMENT : les récupérer avant d'y renvoyer reglement/ depuis le dépôt.
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

const DOSSIER = fileURLToPath(new URL('../reglement/', import.meta.url));
const FICHIER_ETAT = fileURLToPath(new URL('../reglement-etat.json', import.meta.url));

/** Rôle autorisé (nom, sans tenir compte des majuscules ni d'un emoji autour). */
const ROLE_AUTORISE = 'le fameux fruit';
const MAX_DESCRIPTION = 4096;
const MAX_NOM_FIL = 100;
const MAX_CHAMP = 4000;
const COULEUR_DEFAUT = 0x8b3dff;
const SANS_PING = { parse: [] };
const META = /^(couleur|groupe|image|miniature)\s*:\s*(.*)$/i;
const IMAGES = 'images';
/** Taille maximale d'une image téléchargée (un bot envoie 10 Mo au plus par message). */
const MAX_IMAGE = 8 * 1024 * 1024;
const TYPES_IMAGE = { 'image/png': 'png', 'image/jpeg': 'jpg', 'image/gif': 'gif', 'image/webp': 'webp' };

let enCours = false;

function aLeRole(member) {
  return Boolean(member?.roles?.cache?.some((r) => r.name.toLowerCase().includes(ROLE_AUTORISE)));
}

export async function commande(message, args) {
  if (!message.inGuild()) return;
  if (!aLeRole(message.member)) return; // silence : la commande n'existe pas pour les autres
  const [action, idSalon] = args;
  if (action?.toLowerCase() === 'edit' || action?.toLowerCase() === 'images') {
    const images = action.toLowerCase() === 'images';
    await message.reply({
      content: images ? 'Les images de quel encadré ?' : 'Quel encadré modifier ?',
      components: [menuEncadres(DOSSIER, images)], allowedMentions: SANS_PING,
    });
    return;
  }
  const salonId = idSalon?.replace(/^<#(\d+)>$/, '$1');
  if (action?.toLowerCase() !== 'set' || !/^\d{17,20}$/.test(salonId ?? '')) {
    await message.reply({
      content: "`k!rules set <id du salon>` publie le salon d'informations (clic droit sur le salon > Copier l'identifiant) ; `k!rules edit` modifie un encadré ; `k!rules images` change ses images.",
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
    const rapport = await publier(message.guild, salonId);
    await message.reply({ content: rapport, allowedMentions: SANS_PING });
  } catch (error) {
    console.error('k!rules :', error);
    await message.reply({ content: `Rien de publié : ${error.message}`, allowedMentions: SANS_PING }).catch(() => {});
  } finally {
    enCours = false;
  }
}

// ---------------------------------------------------------------------------------------------------------------
// Lecture des textes.

/** Lit reglement/ : les sections dans l'ordre des fichiers, les fils de leurs groupes, l'image de séparation. */
export function lireSalon(dossier = DOSSIER) {
  if (!existsSync(dossier)) throw new Error('dossier reglement/ introuvable');
  const md = (d) => readdirSync(d).filter((f) => f.endsWith('.md') && !f.startsWith('_'))
    .sort((a, b) => a.localeCompare(b, 'fr', { numeric: true }));
  const sections = md(dossier).map((f) => {
    const s = lireFichier(readFileSync(join(dossier, f), 'utf8'), f);
    const [avant, apres = ''] = s.texte.split(/\n-{3,}\n/).map((t) => t.trim());
    const section = {
      cle: f.replace(/\.md$/, ''), titre: s.titre, couleurPropre: s.couleur, couleur: s.couleur ?? COULEUR_DEFAUT,
      groupe: s.groupe, avant, apres, fils: [], image: s.image, miniature: s.miniature,
    };
    verifierImages(dossier, s, f);
    if (!section.titre && !avant && !s.groupe) throw new Error(`${f} : section vide`);
    if (s.groupe) {
      if (!/^[a-z0-9-]+$/.test(s.groupe)) throw new Error(`${f} : groupe « ${s.groupe} » invalide (minuscules, chiffres, tirets)`);
      const d = join(dossier, s.groupe);
      if (!existsSync(d) || !statSync(d).isDirectory()) throw new Error(`${f} : dossier reglement/${s.groupe}/ introuvable`);
      section.fils = md(d).map((g) => {
        const p = lireFichier(readFileSync(join(d, g), 'utf8'), `${s.groupe}/${g}`);
        if (!p.titre) throw new Error(`${s.groupe}/${g} : nom du fil vide`);
        if (p.titre.length > MAX_NOM_FIL) throw new Error(`${s.groupe}/${g} : nom du fil trop long (${p.titre.length} > ${MAX_NOM_FIL})`);
        if (!p.texte) throw new Error(`${s.groupe}/${g} : texte vide`);
        verifierImages(dossier, p, `${s.groupe}/${g}`);
        return {
          cle: `${s.groupe}/${g.replace(/\.md$/, '')}`, titre: p.titre, couleurPropre: p.couleur, couleur: p.couleur ?? section.couleur,
          texte: p.texte, image: p.image, miniature: p.miniature,
        };
      });
      if (!section.fils.length) throw new Error(`reglement/${s.groupe}/ : aucun fil`);
    }
    return section;
  });
  if (!sections.length) throw new Error('aucune section dans reglement/');
  const image = ['_separation.gif', '_separation.png'].map((f) => join(dossier, f)).find(existsSync) ?? null;
  return { sections, image };
}

function verifierImages(dossier, lu, origine) {
  for (const nom of [lu.image, lu.miniature].filter(Boolean)) {
    if (!/^[\w.-]+$/.test(nom) || !existsSync(join(dossier, IMAGES, nom))) throw new Error(`${origine} : image reglement/${IMAGES}/${nom} introuvable`);
  }
}

function lireFichier(brut, origine) {
  const lignes = brut.replace(/^﻿/, '').replace(/\r\n?/g, '\n').split('\n');
  const premiere = lignes.shift() ?? '';
  if (!/^#(\s.*)?$/.test(premiere)) throw new Error(`${origine} : la première ligne doit être « # Titre » (ou « # » seul)`);
  let couleur = null;
  let groupe = null;
  let image = null;
  let miniature = null;
  while (META.test(lignes[0] ?? '')) {
    const [, cle, valeurBrute] = lignes.shift().match(META);
    const valeur = valeurBrute.trim();
    if (cle.toLowerCase() === 'groupe') {
      groupe = valeur;
    } else if (cle.toLowerCase() === 'image') {
      image = valeur || null;
    } else if (cle.toLowerCase() === 'miniature') {
      miniature = valeur || null;
    } else {
      if (!/^#[0-9a-f]{6}$/i.test(valeur)) throw new Error(`${origine} : couleur « ${valeur} » invalide (format #rrggbb)`);
      couleur = parseInt(valeur.slice(1), 16);
    }
  }
  return { titre: premiere.replace(/^#/, '').trim(), couleur, groupe, image, miniature, texte: lignes.join('\n').trim() };
}

// ---------------------------------------------------------------------------------------------------------------
// Encadrés.

/** Les embeds d'une section : le gros titre (s'il y en a un) puis le texte (avec les liens de ses fils). */
export function embedsSection(section, idsFils) {
  const embeds = [];
  if (section.titre) embeds.push({ color: section.couleur, description: `# ${section.titre}` });
  const liens = section.fils.map((f) => `📖 ${idsFils[f.cle] ? `<#${idsFils[f.cle]}>` : `**${f.titre}**`}`).join('\n');
  const texte = [section.avant, liens, section.apres].filter(Boolean).join('\n\n');
  if (texte.length > MAX_DESCRIPTION) throw new Error(`${section.cle} : texte trop long (${texte.length} > ${MAX_DESCRIPTION} caractères)`);
  if (texte) embeds.push({ color: section.couleur, description: texte });
  avecImages(embeds[embeds.length - 1], section);
  return embeds;
}

/** Image (en bas) et miniature (en haut à droite) d'un encadré : fichiers joints au message (attachment://). */
function avecImages(embed, { image, miniature }) {
  if (image) embed.image = { url: `attachment://${image}` };
  if (miniature) embed.thumbnail = { url: `attachment://${miniature}` };
  return embed;
}

/** Les fichiers à joindre à un message pour ses embeds. */
function fichiersDe(embeds) {
  const noms = new Set(embeds.flatMap((e) => [e.image?.url, e.thumbnail?.url]).filter(Boolean)
    .map((u) => u.replace('attachment://', '')));
  return [...noms].map((nom) => ({ attachment: join(DOSSIER, IMAGES, nom), name: nom }));
}

/** L'embed d'un fil, terminé par le lien de retour vers la section qui le liste. */
export function embedFil(fil, lienSection) {
  const texte = `${fil.texte}\n\n↩️ [Revenir au sommaire](${lienSection})`;
  if (texte.length > MAX_DESCRIPTION) throw new Error(`${fil.titre} : texte trop long (${texte.length} > ${MAX_DESCRIPTION} caractères)`);
  // Un texte qui commence par un titre Markdown (« # … ») remplace le titre de l'encadré (plus gros).
  return avecImages({ title: /^#{1,3}\s/.test(fil.texte) ? undefined : fil.titre, color: fil.couleur, description: texte }, fil);
}

// ---------------------------------------------------------------------------------------------------------------
// Publication.

export async function publier(guild, salonId) {
  const { sections, image } = lireSalon();

  const salon = await guild.channels.fetch(salonId).catch(() => null);
  if (!salon) throw new Error(`salon ${salonId} introuvable sur ce serveur Discord`);
  if (salon.type !== ChannelType.GuildText) throw new Error(`#${salon.name} n'est pas un salon textuel`);
  const moi = guild.members.me;
  const avecFichiers = image || sections.some((s) => s.image || s.miniature || s.fils.some((f) => f.image || f.miniature));
  const permissions = { ...PERMISSIONS_BOT, ...(avecFichiers ? { AttachFiles: 'joindre des fichiers' } : {}) };
  const manquantes = Object.entries(permissions)
    .filter(([cle]) => !salon.permissionsFor(moi).has(PermissionFlagsBits[cle]))
    .map(([, nom]) => nom);
  if (manquantes.length) throw new Error(`il manque au bot, dans #${salon.name} : ${manquantes.join(', ')}`);

  const etat = lireEtat();
  etat.salons[salonId] ??= { messages: [], fils: {} };
  const memo = etat.salons[salonId];
  migrer(memo);

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
        memo.fils[f.cle] = { id: fil.id, message: null };
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

  // 2. La mise en page : [image] section [image] section … [image]. Republiée en entier dès qu'un texte, l'ordre ou
  //    l'image change, ou qu'un message a disparu.
  const plan = [];
  for (const s of sections) {
    if (image) plan.push({ type: 'image' });
    plan.push({ type: 'section', cle: s.cle, embeds: embedsSection(s, idsFils) });
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
        : await salon.send({ embeds: p.embeds, files: fichiersDe(p.embeds), allowedMentions: SANS_PING });
      memo.messages.push({ type: p.type, cle: p.cle, id: m.id });
      messages.push(m);
      ecrireEtat(etat);
    }
  }
  const lienDe = (cle) => messages[plan.findIndex((p) => p.cle === cle)].url;

  // 3. Le texte de chaque fil, republié s'il a changé (ou si le lien de sa section a changé).
  for (const s of sections) {
    for (const f of s.fils) {
      const fil = fils[f.cle];
      const connu = memo.fils[f.cle];
      const embed = embedFil(f, lienDe(s.cle));
      const ancien = connu.message ? await fil.messages.fetch(connu.message).catch(() => null) : null;
      if (ancien && pareil(ancien, [embed])) {
        compte.inchanges++;
      } else {
        compte[ancien || connu.message ? 'republies' : 'crees']++;
        await ancien?.delete().catch(() => {});
        connu.message = (await fil.send({ embeds: [embed], files: fichiersDe([embed]), allowedMentions: SANS_PING })).id;
        ecrireEtat(etat);
      }
      if (!fil.locked) await fil.setLocked(true, 'Lecture seule');
    }
  }

  const cles = new Set(sections.flatMap((s) => s.fils.map((f) => f.cle)));
  const orphelins = Object.entries(memo.fils).filter(([cle]) => !cles.has(cle));
  if (orphelins.length) {
    avertissements.push(`fils qui ne sont plus dans reglement/ (gardés, à supprimer à la main) : ${orphelins.map(([, f]) => `<#${f.id}>`).join(', ')}`);
  }
  if (!image) avertissements.push('pas d\'image de séparation (reglement/_separation.gif) : sections publiées sans');
  return [
    `Salon <#${salonId}> ${identique ? 'inchangé' : 'republié'} ; fils : ${compte.crees} nouveau(x), `
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

/** État de la 0.5 (un seul message de sommaire, fils « a-respect »…) -> état par sections, fils « regles/a-respect ». */
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
// k!rules edit : menu des encadrés -> formulaire Discord -> fichier réécrit -> salons publiés mis à jour.
// Valeurs du menu : « s:<section> » ou « f:<groupe>/<fil> ».

export function menuEncadres(dossier = DOSSIER, images = false) {
  const { sections } = lireSalon(dossier);
  const options = [];
  for (const s of sections) {
    options.push({ label: (s.titre || s.cle).slice(0, 100), value: `s:${s.cle}`, emoji: '📌', description: 'Section du salon' });
    for (const f of s.fils) options.push({ label: f.titre.slice(0, 100), value: `f:${f.cle}`, emoji: '📖', description: `Fil · ${(s.titre || s.cle).slice(0, 90)}` });
  }
  const menu = new StringSelectMenuBuilder().setCustomId(images ? 'rules:choixImages' : 'rules:choix').setPlaceholder('Choisir un encadré')
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
function trouver(valeur, dossier = DOSSIER) {
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

/** Formulaire prérempli avec le texte actuel de l'encadré. */
export function formulaire(valeur, dossier = DOSSIER) {
  const trouve = trouver(valeur, dossier);
  if (!trouve) return null;
  const modal = new ModalBuilder().setCustomId(`rules:modal:${valeur}`);
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
  return modal.setTitle(fil.titre.slice(0, 45)).addComponents(
    champ('titre', 'Nom du fil', TextInputStyle.Short, fil.titre,
      { max: MAX_NOM_FIL, placeholder: "Aussi le titre de l'encadré, sauf si le texte commence par « # »" }),
    champ('couleur', 'Couleur (#rrggbb, facultatif)', TextInputStyle.Short, hex(fil.couleurPropre),
      { requis: false, max: 7, placeholder: `vide = celle de la section (${hex(section.couleur)})` }),
    champ('texte', 'Texte', TextInputStyle.Paragraph, fil.texte,
      { max: 3900, placeholder: 'Le lien « Revenir au sommaire » est ajouté tout seul à la fin.' }),
  );
}

/** Formulaire des images d'un encadré, prérempli avec les noms des fichiers actuels. */
export function formulaireImages(valeur, dossier = DOSSIER) {
  const trouve = trouver(valeur, dossier);
  if (!trouve) return null;
  const e = trouve.fil ?? trouve.section;
  const conseil = "Lien d'image (https://…) ; vide = pas d'image";
  return new ModalBuilder().setCustomId(`rules:img:${valeur}`)
    .setTitle(`Images · ${e.titre || e.cle}`.slice(0, 45))
    .addComponents(
      champ('image', "Image (en bas de l'encadré)", TextInputStyle.Short, e.image, { requis: false, max: 1000, placeholder: conseil }),
      champ('miniature', 'Miniature (en haut à droite)', TextInputStyle.Short, e.miniature, { requis: false, max: 1000, placeholder: conseil }),
    );
}

/**
 * Valeur d'un champ d'image : vide = pas d'image ; le nom actuel = inchangé ; un lien = image téléchargée dans
 * reglement/images/, nommée d'après l'encadré et l'empreinte de son contenu. Renvoie le nom du fichier, ou null.
 */
export async function imageDuChamp(valeur, actuel, base, dossier = DOSSIER) {
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

/** Retire de reglement/images/ les fichiers qu'aucun encadré n'utilise plus. */
function nettoyerImages(dossier = DOSSIER) {
  const d = join(dossier, IMAGES);
  if (!existsSync(d)) return;
  const { sections } = lireSalon(dossier);
  const utilisees = new Set(sections.flatMap((s) => [s.image, s.miniature, ...s.fils.flatMap((f) => [f.image, f.miniature])]).filter(Boolean));
  for (const nom of readdirSync(d)) {
    if (!utilisees.has(nom) && /\.(png|jpe?g|gif|webp)$/i.test(nom)) unlinkSync(join(d, nom));
  }
}

/** Contenu d'un fichier de reglement/ à partir des champs du formulaire (sans l'écrire). */
export function contenuFichier({ estFil, groupe, titre, couleur, texte, apres, image, miniature }) {
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
  // Une ligne « --- » seule sépare les deux textes d'une section : interdite dans les textes eux-mêmes.
  if (/^-{3,}$/m.test(texte) || /^-{3,}$/m.test(apres)) throw new Error('une ligne « --- » seule est réservée, retire-la');
  // Une première ligne « couleur: », « image: »… serait prise pour un réglage.
  if (META.test(texte.split('\n')[0])) throw new Error('le texte ne peut pas commencer par « couleur: », « groupe: », « image: » ou « miniature: »');
  const lignes = [titre ? `# ${titre}` : '#'];
  if (couleur) lignes.push(`couleur: ${couleur.toLowerCase()}`);
  if (!estFil && groupe) lignes.push(`groupe: ${groupe}`);
  if (image) lignes.push(`image: ${image}`);
  if (miniature) lignes.push(`miniature: ${miniature}`);
  lignes.push('');
  if (texte) lignes.push(texte);
  if (!estFil && groupe && apres) lignes.push('', '---', '', apres);
  return `${lignes.join('\n').trimEnd()}\n`;
}

/** Clics du menu et envois de formulaire dont l'identifiant commence par « rules: ». */
export async function interaction(inter) {
  if (!inter.inGuild()) return;
  const member = aLeRole(inter.member) ? inter.member : await inter.guild.members.fetch(inter.user.id).catch(() => null);
  if (!aLeRole(member)) {
    await inter.reply({ content: 'Réservé au rôle Le fameux fruit.', flags: MessageFlags.Ephemeral });
    return;
  }

  if (inter.isStringSelectMenu() && (inter.customId === 'rules:choix' || inter.customId === 'rules:choixImages')) {
    const modal = inter.customId === 'rules:choix' ? formulaire(inter.values[0]) : formulaireImages(inter.values[0]);
    if (!modal) {
      await inter.reply({ content: "Cet encadré n'existe plus : relance `k!rules edit`.", flags: MessageFlags.Ephemeral });
      return;
    }
    await inter.showModal(modal);
    return;
  }

  if (!inter.isModalSubmit()) return;
  const [, genre, ...reste] = inter.customId.split(':');
  if (genre !== 'modal' && genre !== 'img') return;
  const valeur = reste.join(':');
  const trouve = trouver(valeur);
  if (!trouve) {
    await inter.reply({ content: "Cet encadré n'existe plus : relance `k!rules edit`.", flags: MessageFlags.Ephemeral });
    return;
  }
  await inter.deferReply({ flags: MessageFlags.Ephemeral });
  const champTexte = (id) => {
    try { return inter.fields.getTextInputValue(id); } catch { return ''; }
  };

  const { section, fil } = trouve;
  const e = fil ?? section;
  const fichier = join(DOSSIER, `${e.cle}.md`);
  const ancien = readFileSync(fichier, 'utf8');
  try {
    // Les champs du formulaire, et les valeurs actuelles pour ce que ce formulaire ne montre pas.
    const base = e.cle.replace(/[^a-z0-9-]/gi, '-');
    const champs = genre === 'modal'
      ? { titre: champTexte('titre'), couleur: champTexte('couleur'), texte: champTexte('texte'), apres: champTexte('apres'), image: e.image, miniature: e.miniature }
      : {
        titre: e.titre, couleur: hex(e.couleurPropre), texte: fil ? fil.texte : section.avant, apres: fil ? '' : section.apres,
        image: await imageDuChamp(champTexte('image'), e.image, `${base}-image`),
        miniature: await imageDuChamp(champTexte('miniature'), e.miniature, `${base}-miniature`),
      };
    writeFileSync(fichier, contenuFichier({ estFil: Boolean(fil), groupe: section.groupe, ...champs }));
    verifierLongueurs(lireSalon());
  } catch (error) {
    writeFileSync(fichier, ancien);
    await inter.editReply(`Modification refusée : ${error.message}. Rien n'a changé.`);
    return;
  }
  try { nettoyerImages(); } catch { /* au pire, un fichier inutile reste dans reglement/images/ */ }

  const salons = Object.keys(lireEtat().salons);
  if (enCours) {
    await inter.editReply('Enregistré, mais une publication est déjà en cours : relance `k!rules set` sur le salon dans un instant.');
    return;
  }
  enCours = true;
  const rapports = [];
  try {
    for (const salonId of salons) {
      const salon = await inter.guild.channels.fetch(salonId).catch(() => null);
      if (!salon) {
        // Salon supprimé (par exemple le salon de test) : on l'oublie.
        const etat = lireEtat();
        delete etat.salons[salonId];
        ecrireEtat(etat);
        continue;
      }
      rapports.push(await publier(inter.guild, salonId).catch((error) => `<#${salonId}> : non mis à jour (${error.message})`));
    }
  } finally {
    enCours = false;
  }
  const fait = genre === 'img' ? 'Images enregistrées.' : 'Texte enregistré.';
  await inter.editReply((rapports.length ? `${fait}\n${rapports.join('\n')}`
    : `${fait} Le salon n'est publié nulle part : \`k!rules set <id du salon>\` pour le publier.`).slice(0, 2000));
}

/** Les textes tiennent-ils dans un embed, avec des identifiants et un lien de la plus grande taille possible ? */
export function verifierLongueurs({ sections }) {
  const id = '9'.repeat(20);
  const lien = `https://discord.com/channels/${id}/${id}/${id}`;
  for (const s of sections) {
    const embeds = embedsSection(s, Object.fromEntries(s.fils.map((f) => [f.cle, id])));
    const total = embeds.reduce((n, e) => n + e.description.length, 0);
    if (total > 6000) throw new Error(`${s.cle} : textes trop longs pour un message (${total} > 6000 caractères)`);
    for (const f of s.fils) embedFil(f, lien);
  }
}
