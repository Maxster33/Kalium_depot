// /config salon <type> [salon] et /config salons : salons de chaque fonction du bot (cahier des charges, section 3).
// Reserve a la permission « Gerer le serveur ». Un type sans salon = fonction coupee.

import { ChannelType, EmbedBuilder, MessageFlags, PermissionFlagsBits, SlashCommandBuilder } from 'discord.js';

/** Types de salons du cahier des charges (section 3) ; les fonctions arrivent etape par etape. */
export const TYPES = [
  { value: 'statut', name: 'Statut des serveurs' },
  { value: 'annonces', name: 'Annonces' },
  { value: 'classements', name: 'Classements' },
  { value: 'resultats', name: 'Résultats de parties' },
  { value: 'recaps', name: 'Récaps' },
  { value: 'demandes-publiques', name: 'Demandes publiques (Kixster)' },
  { value: 'wiki', name: 'Wiki' },
  { value: 'tickets', name: 'Tickets' },
  { value: 'nouveautes-projet', name: 'Nouveautés du projet' },
  { value: 'staff-alertes', name: 'Staff : alertes' },
  { value: 'staff-moderation', name: 'Staff : modération' },
  { value: 'staff-courrier', name: 'Staff : courrier' },
  { value: 'admin-suivi', name: 'Admin : suivi du projet' },
];

export const data = new SlashCommandBuilder()
  .setName('config')
  .setDescription('Réglages du bot')
  .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
  .addSubcommand((s) => s.setName('salon').setDescription("Choisir (ou retirer) le salon d'une fonction")
    .addStringOption((o) => o.setName('type').setDescription('La fonction').setRequired(true).addChoices(...TYPES))
    .addChannelOption((o) => o.setName('salon').setDescription('Le salon (vide = retirer)')
      .addChannelTypes(ChannelType.GuildText, ChannelType.GuildAnnouncement)))
  .addSubcommand((s) => s.setName('salons').setDescription('Salons choisis pour chaque fonction'));

export async function execute(interaction, { db }) {
  if (!db) {
    return interaction.reply({ content: 'Base locale du bot indisponible : réglage impossible pour le moment.', flags: MessageFlags.Ephemeral });
  }
  if (interaction.options.getSubcommand() === 'salons') {
    const choisis = new Map(db.salons().map((s) => [s.type, s.salon]));
    const lignes = TYPES.map((t) => `**${t.name}** : ${choisis.has(t.value) ? `<#${choisis.get(t.value)}>` : '—'}`);
    const embed = new EmbedBuilder().setTitle('Salons du bot').setDescription(lignes.join('\n')).setColor(0x09add3);
    return interaction.reply({ embeds: [embed], flags: MessageFlags.Ephemeral });
  }
  const type = interaction.options.getString('type', true);
  const nom = TYPES.find((t) => t.value === type)?.name ?? type;
  const salon = interaction.options.getChannel('salon');
  if (!salon) {
    const retire = db.retirerSalon(type);
    return interaction.reply({ content: retire ? `**${nom}** : salon retiré, fonction coupée.` : `**${nom}** n'avait pas de salon.`, flags: MessageFlags.Ephemeral });
  }
  db.choisirSalon(type, salon.id, interaction.user.id);
  return interaction.reply({ content: `**${nom}** : ${salon}.`, flags: MessageFlags.Ephemeral });
}
