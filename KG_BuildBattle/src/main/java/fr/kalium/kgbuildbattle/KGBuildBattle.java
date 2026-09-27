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
                return List.of(new Entry("kg_buildbattle", menus.t("menu.bouton", "<gold><bold>Build Battle"),
                        menus.t("menu.bouton-info", "<gray>Construis sur un thème, puis vote. File publique ou partie privée."),
                        menus::ouvrir));
            }
        }, this, ServicePriority.Normal);
        lang.saveIfNeeded();
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }
}
