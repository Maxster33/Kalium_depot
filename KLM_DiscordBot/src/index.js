// KLM_DiscordBot : bot Discord du reseau KaLium (cahier des charges : CAHIER_DES_CHARGES.md).
// Lancement : npm start (lit le fichier .env, voir .env.example).
// Commandes slash (/classement) et commandes texte du staff (k!wiki, voir src/wiki.js).

import { Client, Events, GatewayIntentBits, MessageFlags } from 'discord.js';
import { ApiError, ScoreApi } from './api.js';
import * as classement from './commands/classement.js';
import * as wiki from './wiki.js';

const VERSION = '0.2.0';
const PREFIXE = 'k!';

const config = {
  token: process.env.DISCORD_TOKEN,
  guildId: process.env.DISCORD_GUILD_ID,
  apiUrl: process.env.API_URL,
  apiToken: process.env.API_TOKEN,
};
const missing = Object.entries({ DISCORD_TOKEN: config.token, DISCORD_GUILD_ID: config.guildId })
  .filter(([, value]) => !value)
  .map(([name]) => name);
if (missing.length) {
  console.error(`Configuration incomplète dans .env : ${missing.join(', ')} (voir .env.example).`);
  process.exit(1);
}

// Sans API_URL / API_TOKEN, le bot tourne quand même (wiki) mais sans /classement.
const api = config.apiUrl && config.apiToken ? new ScoreApi(config.apiUrl, config.apiToken) : null;
const commands = new Map(api ? [[classement.data.name, classement]] : []);
const textCommands = new Map([['wiki', wiki.commande]]);

// Guilds : commandes slash. GuildMessages + MessageContent (intention privilegiee, a activer dans le portail
// developpeur) : lire les commandes texte k!… du staff ; le bot ignore tous les autres messages.
const client = new Client({ intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages, GatewayIntentBits.MessageContent] });

client.once(Events.ClientReady, async (ready) => {
  // Commandes enregistrees sur le serveur KaLium seulement : visibles tout de suite (pas d'attente globale).
  const guild = await ready.guilds.fetch(config.guildId);
  await guild.commands.set([...commands.values()].map((c) => c.data.toJSON()));
  console.log(`KLM_DiscordBot ${VERSION} connecté en tant que ${ready.user.tag} ; commandes enregistrées sur « ${guild.name} ».`);
  if (!api) {
    console.warn('API_URL ou API_TOKEN vide dans .env : /classement désactivé.');
    return;
  }
  try {
    const status = await api.status();
    console.log(`API de KG_ScoreBoards ${status.data.version} joignable : ${status.data.games.length} jeu(x).`);
  } catch (error) {
    console.warn(`API de KG_ScoreBoards injoignable au démarrage : ${error.message}`);
  }
});

client.on(Events.InteractionCreate, async (interaction) => {
  const command = commands.get(interaction.commandName);
  if (!command) return;
  if (interaction.isAutocomplete()) {
    await command.autocomplete?.(interaction, api).catch(() => {});
    return;
  }
  if (!interaction.isChatInputCommand()) return;
  try {
    await command.execute(interaction, api);
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
