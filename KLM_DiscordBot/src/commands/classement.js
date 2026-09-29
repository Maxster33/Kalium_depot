// /classement <jeu> [periode] [tri] [mois] : top 10 d'un jeu en image (cahier des charges, section 5).

import { AttachmentBuilder, EmbedBuilder, SlashCommandBuilder } from 'discord.js';
import { gameColor, renderRanking } from '../charts.js';
import { formatPoints, formatTime, monthLabel, shortDate } from '../format.js';

export const data = new SlashCommandBuilder()
  .setName('classement')
  .setDescription("Top 10 d'un jeu, en image")
  .addStringOption((o) => o.setName('jeu').setDescription('Le jeu').setRequired(true).setAutocomplete(true))
  .addStringOption((o) => o.setName('periode').setDescription('Général (par défaut) ou mois en cours')
    .addChoices({ name: 'Général', value: 'general' }, { name: 'Mois en cours', value: 'month' }))
  .addStringOption((o) => o.setName('tri').setDescription('Points (par défaut) ou meilleur tour (course de bateau)')
    .addChoices({ name: 'Points', value: 'points' }, { name: 'Meilleur tour', value: 'lap' }))
  .addStringOption((o) => o.setName('mois').setDescription('Un mois archivé').setAutocomplete(true));

export async function autocomplete(interaction, api) {
  const focused = interaction.options.getFocused(true);
  const typed = focused.value.toLowerCase();
  try {
    if (focused.name === 'jeu') {
      const status = await api.status();
      const games = status.data.games.filter((g) => g.name.toLowerCase().includes(typed) || g.id.includes(typed));
      return interaction.respond(games.slice(0, 25).map((g) => ({ name: g.name, value: g.id })));
    }
    if (focused.name === 'mois') {
      const archives = await api.archives();
      const list = archives.data.filter((a) => a.label.toLowerCase().includes(typed) || a.id.includes(typed));
      return interaction.respond(list.slice(0, 25).map((a) => ({ name: a.label, value: a.id })));
    }
  } catch {
    // serveur injoignable : pas de suggestions
  }
  return interaction.respond([]);
}

export async function execute(interaction, api) {
  const game = interaction.options.getString('jeu', true);
  const month = interaction.options.getString('periode') === 'month';
  const lap = interaction.options.getString('tri') === 'lap';
  const archive = interaction.options.getString('mois');
  await interaction.deferReply();

  const result = archive ? await api.archive(archive, game, { lap }) : await api.ranking(game, { month, lap });
  if (!result) {
    return interaction.editReply(archive
      ? "Ce jeu n'a pas de classement dans ce mois archivé."
      : 'Jeu inconnu. Choisis-le dans la liste proposée.');
  }
  const ranking = result.data;
  if (ranking.rows.length === 0) {
    return interaction.editReply(`Personne n'est encore classé en ${ranking.game.name} pour cette période.`);
  }

  const period = archive ? monthLabel(archive.split('_')[0]) : month ? monthLabel(ranking.month) : 'général';
  const sortLabel = ranking.sort === 'lap' ? 'meilleurs tours' : 'points';
  const title = `${ranking.game.name} — top ${ranking.rows.length} ${archive || month ? 'de ' + period : 'général'}`;
  const updated = result.stale
    ? `données du ${shortDate(result.at)}, serveur de jeu hors ligne`
    : shortDate(ranking.time ? new Date(ranking.time) : result.at);
  const subtitle = `Classement : ${sortLabel} · ${updated}`;

  const best = ranking.rows[0].bestLapMs;
  const rows = ranking.rows.map((row) => {
    if (ranking.sort === 'lap') {
      const gap = row.bestLapMs - best;
      return {
        label: `${row.rank}. ${row.name}`,
        value: row.bestLapMs / 1000,
        text: gap > 0 ? `${formatTime(row.bestLapMs)} (+${(gap / 1000).toFixed(2).replace('.', ',')} s)` : formatTime(row.bestLapMs),
      };
    }
    return { label: `${row.rank}. ${row.name}`, value: row.points, text: `${formatPoints(row.points)} pts` };
  });

  const png = await renderRanking({ title, subtitle, rows, color: gameColor(ranking.game.id) });
  const file = new AttachmentBuilder(png, { name: 'classement.png' });
  // Le texte reprend le classement (lisible sans l'image : lecteurs d'ecran, apercus de notification).
  const embed = new EmbedBuilder()
    .setTitle(title)
    .setDescription(rows.map((r) => `**${r.label}** — ${r.text}`).join('\n'))
    .setImage('attachment://classement.png')
    .setFooter({ text: `${ranking.total} joueur(s) classé(s) · ${updated}` })
    .setColor(gameColor(ranking.game.id));
  return interaction.editReply({ embeds: [embed], files: [file] });
}
