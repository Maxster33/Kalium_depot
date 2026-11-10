// Règlement de KaLium dans un salon Discord : la présentation et le sommaire (embed) dans le salon, une partie par
// fil verrouillé (embed), chaque fil finissant par un lien « Revenir au sommaire ».
// Commandes texte réservées au rôle « Le fameux fruit » :
//   k!rules set <id du salon>  publie le règlement dans ce salon, ou le met à jour sur place s'il y est déjà ;
//   k!rules edit               menu des encadrés (présentation, parties) ; chacun s'ouvre dans un formulaire Discord
//                              (titre, couleur, texte) ; à l'envoi, le fichier de reglement/ est réécrit et tous les
//                              salons où le règlement est publié sont mis à jour.
// Les textes sont dans reglement/ (relus à chaque commande, pas besoin de redémarrer le bot) :
//   _sommaire.md : « # Titre », ligne « couleur: #rrggbb » facultative, la présentation, puis après une ligne « --- »
//                  le texte placé sous le sommaire ;
//   une partie par fichier (ordre alphabétique : a-…, b-…), « # Titre » = nom du fil, puis le texte.
// k!rules edit réécrit ces fichiers SUR L'HÉBERGEMENT : les récupérer avant d'y renvoyer reglement/ depuis le dépôt.
// Les identifiants Discord (salon, fils, messages) sont gardés dans reglement-etat.json, jamais commité.

import { readdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs';
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
/** Limite d'un champ de formulaire Discord. */
const MAX_CHAMP = 4000;
const COULEUR_DEFAUT = 0x09add3;
const SANS_PING = { parse: [] };
const SOMMAIRE = '_sommaire';

let enCours = false;

function aLeRole(member) {
  return Boolean(member?.roles?.cache?.some((r) => r.name.toLowerCase().includes(ROLE_AUTORISE)));
}

export async function commande(message, args) {
  if (!message.inGuild()) return;
  if (!aLeRole(message.member)) return; // silence : la commande n'existe pas pour les autres
  const [action, idSalon] = args;
  if (action?.toLowerCase() === 'edit') {
    await message.reply({ content: 'Quel encadré du règlement modifier ?', components: [menuEncadres()], allowedMentions: SANS_PING });
    return;
  }
  const salonId = idSalon?.replace(/^<#(\d+)>$/, '$1');
  if (action?.toLowerCase() !== 'set' || !/^\d{17,20}$/.test(salonId ?? '')) {
    await message.reply({
      content: "`k!rules set <id du salon>` publie le règlement (clic droit sur le salon > Copier l'identifiant) ; `k!rules edit` modifie un encadré.",
      allowedMentions: SANS_PING,
    });
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
      if (!p.titre) throw new Error(`${f} : titre vide (c'est le nom du fil)`);
      if (p.titre.length > MAX_NOM_FIL) throw new Error(`${f} : titre trop long pour un nom de fil (${p.titre.length} > ${MAX_NOM_FIL})`);
      return { cle: f.replace(/\.md$/, ''), ...p, couleurPropre: p.couleur, couleur: p.couleur ?? s.couleur ?? COULEUR_DEFAUT };
    });
  if (!parties.length) throw new Error('aucune partie dans reglement/');
  return { titre: s.titre, couleur: s.couleur ?? COULEUR_DEFAUT, couleurPropre: s.couleur, intro, conclusion, parties };
}

function lireFichier(brut, origine) {
  const lignes = brut.replace(/^﻿/, '').replace(/\r\n?/g, '\n').split('\n');
  const premiere = lignes.shift() ?? '';
  // « # » seul : pas de titre (permis pour la présentation seulement, voir lireReglement).
  if (!/^#(\s.*)?$/.test(premiere)) throw new Error(`${origine} : la première ligne doit être « # Titre » (ou « # » seul)`);
  let couleur = null;
  if (/^couleur\s*:/i.test(lignes[0] ?? '')) {
    const valeur = lignes.shift().replace(/^couleur\s*:\s*/i, '').trim();
    if (!/^#[0-9a-f]{6}$/i.test(valeur)) throw new Error(`${origine} : couleur « ${valeur} » invalide (format #rrggbb)`);
    couleur = parseInt(valeur.slice(1), 16);
  }
  return { titre: premiere.replace(/^#/, '').trim(), couleur, texte: lignes.join('\n').trim() };
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
  const embedSommaire = (ids) => ({ title: reglement.titre || undefined, color: reglement.couleur, description: texteSommaire(reglement, ids) });
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
  // Un texte qui commence par un titre Markdown (« # … ») remplace le titre de l'encadré (plus gros).
  const embed = { title: /^#{1,3}\s/.test(partie.texte) ? undefined : partie.titre, color: partie.couleur, description: textePartie(partie, lienSommaire) };
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
    if ((e?.title ?? undefined) !== embed.title || e?.description !== embed.description || e?.color !== embed.color) {
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

// ---------------------------------------------------------------------------------------------------------------
// k!rules edit : menu des encadrés -> formulaire Discord -> fichier réécrit -> salons publiés mis à jour.

/** Menu déroulant : la présentation puis chaque partie. */
export function menuEncadres() {
  const reglement = lireReglement();
  const menu = new StringSelectMenuBuilder()
    .setCustomId('rules:choix')
    .setPlaceholder('Choisir un encadré')
    .addOptions(
      { label: 'Présentation et sommaire', value: SOMMAIRE, emoji: '🏠' },
      ...reglement.parties.map((p) => ({ label: p.titre.slice(0, 100), value: p.cle, emoji: '📖' })),
    );
  return new ActionRowBuilder().addComponents(menu);
}

const hex = (couleur) => (couleur == null ? '' : `#${couleur.toString(16).padStart(6, '0')}`);

function champ(id, label, style, valeur, { requis = true, max = MAX_CHAMP, placeholder } = {}) {
  const input = new TextInputBuilder().setCustomId(id).setLabel(label).setStyle(style).setRequired(requis).setMaxLength(max);
  if (valeur) input.setValue(valeur.slice(0, max));
  if (placeholder) input.setPlaceholder(placeholder);
  return new ActionRowBuilder().addComponents(input);
}

/** Formulaire prérempli avec le texte actuel de l'encadré. */
export function formulaire(cle) {
  const reglement = lireReglement();
  const modal = new ModalBuilder().setCustomId(`rules:modal:${cle}`);
  if (cle === SOMMAIRE) {
    return modal.setTitle('Présentation et sommaire').addComponents(
      champ('titre', 'Titre (facultatif)', TextInputStyle.Short, reglement.titre,
        { requis: false, max: 256, placeholder: 'Vide = pas de titre (tu peux écrire « # Titre » dans le texte)' }),
      champ('couleur', 'Couleur (#rrggbb)', TextInputStyle.Short, hex(reglement.couleurPropre),
        { requis: false, max: 7, placeholder: '#09add3 (aussi celle des parties sans couleur)' }),
      champ('texte', 'Présentation (au-dessus du sommaire)', TextInputStyle.Paragraph, reglement.intro, { max: 3000 }),
      champ('conclusion', 'Texte sous le sommaire (facultatif)', TextInputStyle.Paragraph, reglement.conclusion,
        { requis: false, max: 800 }),
    );
  }
  const partie = reglement.parties.find((p) => p.cle === cle);
  if (!partie) return null;
  return modal.setTitle(partie.titre.slice(0, 45)).addComponents(
    champ('titre', 'Nom du fil', TextInputStyle.Short, partie.titre,
      { max: MAX_NOM_FIL, placeholder: "Aussi le titre de l'encadré, sauf si le texte commence par « # »" }),
    champ('couleur', 'Couleur (#rrggbb, facultatif)', TextInputStyle.Short, hex(partie.couleurPropre),
      { requis: false, max: 7, placeholder: `vide = celle de la présentation (${hex(reglement.couleur)})` }),
    champ('texte', 'Texte', TextInputStyle.Paragraph, partie.texte,
      { max: 3900, placeholder: 'Le lien « Revenir au sommaire » est ajouté tout seul à la fin.' }),
  );
}

/** Contenu d'un fichier de reglement/ à partir des champs du formulaire (sans l'écrire). */
export function contenuFichier(cle, { titre, couleur, texte, conclusion }) {
  titre = titre.replace(/\s+/g, ' ').trim();
  if (!titre && cle !== SOMMAIRE) throw new Error('nom du fil vide');
  couleur = couleur?.trim() ?? '';
  if (couleur && !couleur.startsWith('#')) couleur = `#${couleur}`;
  if (couleur && !/^#[0-9a-f]{6}$/i.test(couleur)) throw new Error(`couleur « ${couleur} » invalide (format #rrggbb, par exemple #09add3)`);
  texte = texte.replace(/\r\n?/g, '\n').trim();
  if (!texte) throw new Error('texte vide');
  // Une ligne « --- » seule sépare présentation et conclusion : interdite dans les textes eux-mêmes.
  if (/^-{3,}$/m.test(texte) || /^-{3,}$/m.test(conclusion ?? '')) throw new Error('une ligne « --- » seule est réservée, retire-la');
  const lignes = [titre ? `# ${titre}` : '#'];
  if (couleur) lignes.push(`couleur: ${couleur.toLowerCase()}`);
  lignes.push('', texte);
  if (cle === SOMMAIRE && conclusion?.trim()) lignes.push('', '---', '', conclusion.replace(/\r\n?/g, '\n').trim());
  return `${lignes.join('\n')}\n`;
}

/** Clics du menu et envois de formulaire dont l'identifiant commence par « rules: ». */
export async function interaction(inter) {
  if (!inter.inGuild()) return;
  const member = aLeRole(inter.member) ? inter.member : await inter.guild.members.fetch(inter.user.id).catch(() => null);
  if (!aLeRole(member)) {
    await inter.reply({ content: 'Réservé au rôle Le fameux fruit.', flags: MessageFlags.Ephemeral });
    return;
  }

  if (inter.isStringSelectMenu() && inter.customId === 'rules:choix') {
    const modal = formulaire(inter.values[0]);
    if (!modal) {
      await inter.reply({ content: "Cet encadré n'existe plus : relance `k!rules edit`.", flags: MessageFlags.Ephemeral });
      return;
    }
    await inter.showModal(modal);
    return;
  }

  if (!inter.isModalSubmit() || !inter.customId.startsWith('rules:modal:')) return;
  const cle = inter.customId.slice('rules:modal:'.length);
  const reglement = lireReglement();
  if (cle !== SOMMAIRE && !reglement.parties.some((p) => p.cle === cle)) {
    await inter.reply({ content: "Cet encadré n'existe plus : relance `k!rules edit`.", flags: MessageFlags.Ephemeral });
    return;
  }
  await inter.deferReply({ flags: MessageFlags.Ephemeral });
  const valeur = (id) => {
    try { return inter.fields.getTextInputValue(id); } catch { return ''; }
  };

  const fichier = join(DOSSIER, `${cle}.md`);
  const ancien = readFileSync(fichier, 'utf8');
  try {
    writeFileSync(fichier, contenuFichier(cle, {
      titre: valeur('titre'), couleur: valeur('couleur'), texte: valeur('texte'), conclusion: valeur('conclusion'),
    }));
    verifierLongueurs(lireReglement());
  } catch (error) {
    writeFileSync(fichier, ancien);
    await inter.editReply(`Modification refusée : ${error.message}. Rien n'a changé.`);
    return;
  }

  const salons = Object.keys(lireEtat().salons);
  if (!salons.length) {
    await inter.editReply('Texte enregistré. Le règlement n\'est publié nulle part : `k!rules set <id du salon>` pour le publier.');
    return;
  }
  if (enCours) {
    await inter.editReply('Texte enregistré, mais une publication est déjà en cours : relance `k!rules set` sur le salon dans un instant.');
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
  await inter.editReply(`Texte enregistré.\n${rapports.join('\n')}`.slice(0, 2000));
}

/** Les textes tiennent-ils dans un embed, avec des identifiants et un lien de la plus grande taille possible ? */
function verifierLongueurs(reglement) {
  const ids = Object.fromEntries(reglement.parties.map((p) => [p.cle, '9'.repeat(20)]));
  texteSommaire(reglement, ids);
  const lien = `https://discord.com/channels/${'9'.repeat(20)}/${'9'.repeat(20)}/${'9'.repeat(20)}`;
  for (const p of reglement.parties) textePartie(p, lien);
}
