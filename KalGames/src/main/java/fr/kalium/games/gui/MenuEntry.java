package fr.kalium.games.gui;

import net.kyori.adventure.text.Component;

import java.util.function.Supplier;

/**
 * Bouton ajoute a un menu de KalGames par un AUTRE plugin (1.13.0 - ex. KG_Bingo ajoute son bouton "Bingo" au
 * menu Mini-jeux et a Parametres). KalGames n'a ainsi rien a savoir des jeux separes : chaque plugin declare son
 * bouton au demarrage (PlayerMenus.addGameEntry / AdminMenus.addSettingsEntry) et le retire a l'arret.
 * Ces "prises" suivront le menu quand il sera sorti dans son propre plugin (KG_Menu, prevu).
 *
 * @param id      identifiant unique (ex. "kg_bingo") : un nouvel ajout avec le meme id remplace l'ancien
 * @param label   texte du bouton (recalcule a chaque ouverture du menu : les textes peuvent etre recharges)
 * @param tooltip info-bulle, ou null
 * @param click   action au clic (sur le thread principal)
 */
public record MenuEntry(String id, Supplier<Component> label, Supplier<Component> tooltip, Gui.Click click) {
}
