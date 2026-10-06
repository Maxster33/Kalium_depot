package fr.kalium.kgbuildbattle;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import fr.kalium.kgmenu.api.MenuProvider;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;

/**
 * KG_BuildBattle : le Build Battle côté kal-games (cahier des charges : KV_BuildBattle/CAHIER_DES_CHARGES.md). Bouton
 * « Build Battle » dans le menu du serveur (KG_Menu) ; file publique et parties privées ; les joueurs sont envoyés sur
 * Kanvas, où KV_BuildBattle fait jouer la partie. Même principe que KG_Bingo / KG_BingoGame.
 */
public final class KGBuildBattle extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Parties parties = new Parties(this);
        Lang lang = new Lang(this);
        Menus menus = new Menus(parties, lang, new Gui(this, lang));
        getServer().getScheduler().runTaskTimer(this, parties::verifierFermees, 100L, 100L);
        // 0.2.0 : nombre de joueurs en Build Battle (publie par KV_BuildBattle), affiche sur le bouton par KG_Menu.
        RelayCounter joueurs = new RelayCounter(this, "compteur-buildbattle");

        getServer().getServicesManager().register(MenuProvider.class, new MenuProvider() {
            @Override
            public Plugin owner() {
                return KGBuildBattle.this;
            }

            @Override
            public int order() {
                return 30;
            }

            @Override
            public List<Entry> games(Player joueur) {
                // 0.3.0 (demande de Maxster33) : joueurs possibles, de 2 (deux equipes d'un joueur, minimum de
                // KV_BuildBattle) a equipes max x 4 (equipes de 4 au plus).
                net.kyori.adventure.text.Component info = menus.t("menu.bouton-info",
                                "<gray>Construis sur un thème, puis vote. File publique ou partie privée.")
                        .append(net.kyori.adventure.text.Component.newline())
                        .append(menus.t("menu.bouton-joueurs", "<gray>Joueurs : <white>2 à <max>", "max", parties.equipesMax() * 4));
                return List.of(new Entry("kg_buildbattle", menus.t("menu.bouton", "<gold><bold>Build Battle"),
                        info, menus::ouvrir, joueurs.value()));
            }
        }, this, ServicePriority.Normal);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }
}
