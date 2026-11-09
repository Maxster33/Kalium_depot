// Wiki KaLium dans un salon Discord : un sommaire par serveur (embed) qui renvoie vers une fiche par fil verrouillé.
// Commandes texte (staff, permission « Gérer le serveur ») :
//   k!wiki setup <id du salon>  publie le wiki dans ce salon, ou le met à jour s'il y est déjà ;
//   k!wiki maj                  met à jour le dernier salon publié (après une modification des fichiers wiki/).
// Les fiches sont relues à chaque commande : pas besoin de redémarrer le bot après une modification.
// Les identifiants Discord (salon, fils, messages) sont gardés dans wiki-etat.json, jamais commité.

import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { ChannelType, MessageFlags, MessageType, PermissionFlagsBits, ThreadAutoArchiveDuration } from 'discord.js';
import { lireWiki, texteSommaire } from './wiki-fiches.js';

const DOSSIER_WIKI = fileURLToPath(new URL('../wiki/', import.meta.url));
const FICHIER_ETAT = fileURLToPath(new URL('../wiki-etat.json', import.meta.url));

/** Permissions du bot dans le salon du wiki, sans lesquelles la publication échoue. */
export const PERMISSIONS_BOT = {
  ViewChannel: 'voir le salon',
  SendMessages: 'envoyer des messages',
  SendMessagesInThreads: 'envoyer des messages dans les fils',
  CreatePublicThreads: 'créer des fils publics',
  ManageThreads: 'gérer les fils',
  ManageMessages: 'gérer les messages',
  EmbedLinks: 'intégrer des liens',
  ReadMessageHistory: "voir l'historique des messages",
};

const SANS_PING = { parse: [] };

let enCours = false;

export async function commande(message, args) {
  if (!message.inGuild()) return;
  if (!message.member?.permissions.has(PermissionFlagsBits.ManageGuild)) {
    await message.reply({ content: 'Réservé au staff (permission « Gérer le serveur »).', allowedMentions: SANS_PING });
    return;
  }
  const [action, idSalon] = args;
  const etat = lireEtat();
  let salonId;
  if (action === 'setup') {
    salonId = idSalon?.replace(/^<#(\d+)>$/, '$1');
    if (!/^\d{17,20}$/.test(salonId ?? '')) {
      await message.reply({ content: 'Usage : `k!wiki setup <id du salon>` (clic droit sur le salon > Copier l\'identifiant).', allowedMentions: SANS_PING });
      return;
    }
  } else if (action === 'maj') {
    salonId = etat.dernier;
    if (!salonId) {
      await message.reply({ content: 'Aucun wiki publié : commence par `k!wiki setup <id du salon>`.', allowedMentions: SANS_PING });
      return;
    }
  } else {
    await message.reply({ content: '`k!wiki setup <id du salon>` publie le wiki ; `k!wiki maj` le met à jour.', allowedMentions: SANS_PING });
    return;
  }
  if (enCours) {
    await message.reply({ content: 'Une publication du wiki est déjà en cours.', allowedMentions: SANS_PING });
    return;
  }
  enCours = true;
  try {
    const rapport = await publier(message.guild, salonId, etat);
    await message.reply({ content: rapport, allowedMentions: SANS_PING });
  } catch (error) {
    console.error('k!wiki :', error);
    await message.reply({ content: `Wiki non publié : ${error.message}`, allowedMentions: SANS_PING }).catch(() => {});
  } finally {
    enCours = false;
  }
}

async function publier(guild, salonId, etat) {
  const serveurs = lireWiki(DOSSIER_WIKI);

  const salon = await guild.channels.fetch(salonId).catch(() => null);
  if (!salon) throw new Error(`salon ${salonId} introuvable sur ce serveur Discord`);
  if (salon.type !== ChannelType.GuildText) throw new Error(`#${salon.name} n'est pas un salon textuel`);
  const moi = guild.members.me;
  const manquantes = Object.entries(PERMISSIONS_BOT)
    .filter(([cle]) => !salon.permissionsFor(moi).has(PermissionFlagsBits[cle]))
    .map(([, nom]) => nom);
  if (manquantes.length) throw new Error(`il manque au bot, dans #${salon.name} : ${manquantes.join(', ')}`);

  etat.salons[salonId] ??= { fils: {}, sommaires: {} };
  etat.dernier = salonId;
  const memo = etat.salons[salonId];
  ecrireEtat(etat);

  const avertissements = [];
  await verrouillerSalon(salon, moi).catch((error) => {
    avertissements.push(`salon non verrouillé pour les joueurs (${error.message}) : retire-leur « Envoyer des messages » et « … dans les fils » à la main`);
  });

  const compte = { crees: 0, modifies: 0, inchanges: 0 };
  const idsFils = {};
  for (const serveur of serveurs) {
    for (const fiche of serveur.fiches) {
      const resultat = await synchroniserFil(salon, fiche, memo);
      compte[resultat]++;
      idsFils[fiche.cle] = memo.fils[fiche.cle].id;
      ecrireEtat(etat);
    }
  }
  await effacerAnnoncesDeFils(salon, moi.id);

  const sommaires = await synchroniserSommaires(salon, serveurs, idsFils, memo);
  ecrireEtat(etat);

  const cles = new Set(serveurs.flatMap((s) => s.fiches.map((f) => f.cle)));
  const orphelins = Object.entries(memo.fils).filter(([cle]) => !cles.has(cle));
  if (orphelins.length) {
    avertissements.push(`fils sans fiche (gardés, à supprimer à la main s'ils ne servent plus) : ${orphelins.map(([, f]) => `<#${f.id}>`).join(', ')}`);
  }

  const lignes = [
    `Wiki publié dans <#${salonId}> : ${serveurs.length} sommaire(s) ${sommaires}, `
      + `${compte.crees} fil(s) créé(s), ${compte.modifies} mis à jour, ${compte.inchanges} inchangé(s).`,
    ...avertissements.map((a) => `⚠️ ${a}`),
  ];
  return lignes.join('\n').slice(0, 2000);
}

/** Seul le staff écrit dans le salon et ses fils ; le bot garde ses droits (dérogation à son nom). */
export async function verrouillerSalon(salon, moi) {
  await salon.permissionOverwrites.edit(moi.id, {
    ViewChannel: true, SendMessages: true, SendMessagesInThreads: true, CreatePublicThreads: true,
    ManageThreads: true, ManageMessages: true, EmbedLinks: true, ReadMessageHistory: true,
  });
  await salon.permissionOverwrites.edit(salon.guild.roles.everyone, {
    SendMessages: false, SendMessagesInThreads: false, CreatePublicThreads: false, CreatePrivateThreads: false,
  });
}

/** Crée ou met à jour le fil d'une fiche. Renvoie « crees », « modifies » ou « inchanges ». */
async function synchroniserFil(salon, fiche, memo) {
  const connu = memo.fils[fiche.cle];
  let fil = connu ? await salon.threads.fetch(connu.id).catch(() => null) : null;
  if (fil && fil.parentId !== salon.id) fil = null;

  if (!fil) {
    fil = await salon.threads.create({
      name: fiche.nomFil,
      type: ChannelType.PublicThread,
      autoArchiveDuration: ThreadAutoArchiveDuration.OneWeek,
      reason: 'Wiki KaLium',
    });
    const ids = [];
    for (const texte of fiche.messages) ids.push((await envoyer(fil, texte)).id);
    await fil.setLocked(true, 'Wiki KaLium : lecture seule');
    memo.fils[fiche.cle] = { id: fil.id, messages: ids };
    return 'crees';
  }

  let modifie = false;
  // Un fil archivé (une semaine sans message) ne se modifie pas : on le rouvre, il reste verrouillé.
  if (fil.archived) await fil.setArchived(false, 'Wiki KaLium : mise à jour');
  if (fil.name !== fiche.nomFil) {
    await fil.setName(fiche.nomFil);
    modifie = true;
  }

  const anciens = [];
  for (const id of connu.messages) anciens.push(await fil.messages.fetch(id).catch(() => null));
  if (anciens.some((m) => !m)) {
    // Un message a disparu : on republie toute la fiche pour garder l'ordre.
    for (const m of anciens) await m?.delete().catch(() => {});
    connu.messages = [];
    for (const texte of fiche.messages) connu.messages.push((await envoyer(fil, texte)).id);
    modifie = true;
  } else {
    for (let i = 0; i < Math.max(anciens.length, fiche.messages.length); i++) {
      const ancien = anciens[i];
      const texte = fiche.messages[i];
      if (ancien && texte === undefined) {
        await ancien.delete();
        modifie = true;
      } else if (!ancien) {
        anciens[i] = await envoyer(fil, texte);
        modifie = true;
      } else if (ancien.content !== texte) {
        await ancien.edit({ content: texte, allowedMentions: SANS_PING });
        modifie = true;
      }
    }
    connu.messages = anciens.slice(0, fiche.messages.length).map((m) => m.id);
  }
  if (!fil.locked) await fil.setLocked(true, 'Wiki KaLium : lecture seule');
  return modifie ? 'modifies' : 'inchanges';
}

function envoyer(fil, texte) {
  return fil.send({ content: texte, allowedMentions: SANS_PING, flags: MessageFlags.SuppressEmbeds });
}

/** Retire du salon les messages « … a commencé un fil » laissés par le bot : le salon ne garde que les sommaires. */
export async function effacerAnnoncesDeFils(salon, botId) {
  const messages = await salon.messages.fetch({ limit: 100 });
  for (const m of messages.values()) {
    if (m.type === MessageType.ThreadCreated && m.author.id === botId) await m.delete().catch(() => {});
  }
}

/**
 * Un message (embed) par serveur, dans l'ordre des dossiers. Si un sommaire manque ou si la liste des serveurs a
 * changé, tous les sommaires sont republiés pour garder l'ordre ; sinon ils sont modifiés sur place.
 */
async function synchroniserSommaires(salon, serveurs, idsFils, memo) {
  const embeds = serveurs.map((s) => ({ title: s.titre, color: s.couleur, description: texteSommaire(s, idsFils) }));
  const memeListe = Object.keys(memo.sommaires).join('|') === serveurs.map((s) => s.cle).join('|');
  const existants = memeListe
    ? await Promise.all(serveurs.map((s) => salon.messages.fetch(memo.sommaires[s.cle]).catch(() => null)))
    : [];

  if (memeListe && existants.every(Boolean)) {
    for (let i = 0; i < serveurs.length; i++) {
      await existants[i].edit({ embeds: [embeds[i]], allowedMentions: SANS_PING });
    }
    return 'mis à jour';
  }
  for (const id of Object.values(memo.sommaires)) {
    await salon.messages.delete(id).catch(() => {});
  }
  memo.sommaires = {};
  for (let i = 0; i < serveurs.length; i++) {
    const m = await salon.send({ embeds: [embeds[i]], allowedMentions: SANS_PING });
    memo.sommaires[serveurs[i].cle] = m.id;
  }
  return 'publié(s)';
}

function lireEtat() {
  if (!existsSync(FICHIER_ETAT)) return { dernier: null, salons: {} };
  const etat = JSON.parse(readFileSync(FICHIER_ETAT, 'utf8'));
  etat.salons ??= {};
  return etat;
}

function ecrireEtat(etat) {
  writeFileSync(FICHIER_ETAT, JSON.stringify(etat, null, 2) + '\n');
}
