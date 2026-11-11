// KLM_DiscordBot : bot Discord du reseau KaLium (cahier des charges : CAHIER_DES_CHARGES.md).
// Lancement : npm start ou node index.js (le lanceur lit le fichier .env, voir .env.example).
// Commandes slash (/classement, /config) et commandes texte du staff (k!wiki, voir src/wiki.js ; k!rules, voir src/reglement.js).

import { Client, Events, GatewayIntentBits, MessageFlags } from 'discord.js';
import { ApiError, HttpTransport, RelaisTransport, ScoreApi } from './api.js';
import * as classement from './commands/classement.js';
import * as configCommande from './commands/config.js';
import { ouvrir } from './db.js';
import * as reglement from './reglement.js';
import * as wiki from './wiki.js';

const VERSION = '0.7.1';
const PREFIXE = 'k!';

const config = {
  token: process.env.DISCORD_TOKEN,
  guildId: process.env.DISCORD_GUILD_ID,
  apiUrl: process.env.API_URL,
  apiToken: process.env.API_TOKEN,
  relaisUrl: process.env.RELAIS_URL,
  relaisToken: process.env.RELAIS_TOKEN,
};
const missing = Object.entries({ DISCORD_TOKEN: config.token, DISCORD_GUILD_ID: config.guildId })
  .filter(([, value]) => !value)
  .map(([name]) => name);
if (missing.length) {
  console.error(`Configuration incomplète dans .env : ${missing.join(', ')} (voir .env.example).`);
  process.exit(1);
}

// API de KG_ScoreBoards : par le relais (RELAIS_URL / RELAIS_TOKEN) de preference, sinon en direct (API_URL /
// API_TOKEN). Sans l'un ni l'autre, le bot tourne quand meme (wiki, /config) mais sans /classement.
const api = config.relaisUrl && config.relaisToken
  ? new ScoreApi(new RelaisTransport(config.relaisUrl, config.relaisToken))
  : config.apiUrl && config.apiToken ? new ScoreApi(new HttpTransport(config.apiUrl, config.apiToken)) : null;
const db = await ouvrir();
// Chaque commande slash : sa definition, et ses fonctions avec ce dont elle a besoin.
const commands = new Map();
if (api) {
  commands.set(classement.data.name, {
    data: classement.data,
    execute: (interaction) => classement.execute(interaction, api),
    autocomplete: (interaction) => classement.autocomplete(interaction, api),
  });
}
commands.set(configCommande.data.name, { data: configCommande.data, execute: (interaction) => configCommande.execute(interaction, { db }) });
const textCommands = new Map([['wiki', wiki.commande], ['rules', reglement.commande]]);

// Guilds : commandes slash. GuildMessages + MessageContent (intention privilegiee, a activer dans le portail
// developpeur) : lire les commandes texte k!… du staff ; le bot ignore tous les autres messages.
const client = new Client({ intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages, GatewayIntentBits.MessageContent] });

client.once(Events.ClientReady, async (ready) => {
  // Commandes enregistrees sur le serveur KaLium seulement : visibles tout de suite (pas d'attente globale).
  const guild = await ready.guilds.fetch(config.guildId);
  await guild.commands.set([...commands.values()].map((c) => c.data.toJSON()));
  console.log(`KLM_DiscordBot ${VERSION} connecté en tant que ${ready.user.tag} ; commandes enregistrées sur « ${guild.name} ».`);
  if (!api) {
    console.warn('RELAIS_URL / RELAIS_TOKEN (ou API_URL / API_TOKEN) vides dans .env : /classement désactivé.');
    return;
  }
  const chemin = config.relaisUrl && config.relaisToken ? 'par le relais' : 'en direct';
  try {
    // Aussi pour remplir le cache de l'autocompletion (jeux, mois archives).
    const status = await api.status();
    await api.archives().catch(() => {});
    console.log(`API de KG_ScoreBoards ${status.data.version} joignable ${chemin} : ${status.data.games.length} jeu(x).`);
  } catch (error) {
    console.warn(`API de KG_ScoreBoards injoignable ${chemin} au démarrage : ${error.message}`);
  }
});

client.on(Events.InteractionCreate, async (interaction) => {
  // Menu et formulaires de k!rules edit.
  if ((interaction.isMessageComponent() || interaction.isModalSubmit()) && interaction.customId.startsWith('rules:')) {
    await reglement.interaction(interaction).catch(async (error) => {
      console.error('k!rules edit :', error);
      const text = `Erreur : ${error.message}`;
      await (interaction.deferred || interaction.replied ? interaction.editReply(text)
        : interaction.reply({ content: text, flags: MessageFlags.Ephemeral })).catch(() => {});
    });
    return;
  }
  const command = commands.get(interaction.commandName);
  if (!command) return;
  if (interaction.isAutocomplete()) {
    await command.autocomplete?.(interaction).catch(() => {});
    return;
  }
  if (!interaction.isChatInputCommand()) return;
  try {
    await command.execute(interaction);
  } catch (error) {
    const text = error instanceof ApiError && error.status === 401
      ? "Le bot n'est pas autorisé par le serveur de jeu (jeton de l'API à vérifier)."
      : error instanceof ApiError
        ? `Serveur de jeu injoignable pour le moment (${error.message}).`
        : 'Une erreur est survenue.';
    if (!(error instanceof ApiError)) console.error(`/${interaction.commandName} :`, error);
    const reply = { content: text, flags: MessageFlags.Ephemeral };
    await (interaction.deferred || interaction.replied ? interaction.editReply({ content: text }) : interaction.reply(reply)).catch(() => {});
  }
});

client.on(Events.MessageCreate, async (message) => {
  if (message.author.bot || message.guildId !== config.guildId || !message.content.startsWith(PREFIXE)) return;
  const [name, ...args] = message.content.slice(PREFIXE.length).trim().split(/\s+/);
  const command = textCommands.get(name?.toLowerCase());
  if (!command) return;
  try {
    await command(message, args);
  } catch (error) {
    console.error(`${PREFIXE}${name} :`, error);
  }
});

client.login(config.token);
