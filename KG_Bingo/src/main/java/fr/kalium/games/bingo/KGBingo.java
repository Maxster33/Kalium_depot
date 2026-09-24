package fr.kalium.games.bingo;

import fr.kalium.games.KalGames;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * KG_Bingo : tout ce que le hub kal-games gere pour le Bingo (creer / rejoindre une partie, liste des parties en
 * salle d'attente, transfert vers le serveur Bingo, relais HTTP, /bingo, Parametres > Bingo). Sorti de KalGames
 * (package fr.kalium.games.bingo + menus Bingo) en 1.0.0, le 24/09/2026 - regle "un plugin = un role"
 * (REGLES.md, section 2). Le jeu lui-meme tourne sur le serveur Bingo (plugin KalBingo, futur KG_BingoGame).
 *
 * Depend de KalGames (plugin.yml : depend) : utilise ses menus (assistant Gui, textes lang.yml) et y ajoute ses
 * boutons (1.3.0) en les fournissant a KG_Menu, le menu du serveur kal-games (MenuProvider).
 */
public final class KGBingo extends JavaPlugin {

    private static final String ENTRY_ID = "kg_bingo";

    private KalGames kg;
    private BingoPartyManager parties;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Plugin found = getServer().getPluginManager().getPlugin("KalGames");
        if (!(found instanceof KalGames kalGames)) {
            getLogger().severe("KalGames introuvable : KG_Bingo desactive.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        kg = kalGames;

        parties = new BingoPartyManager(this);
        // Retire de la liste les parties Bingo demarrees/annulees, signal lu sur le relais HTTP (voir
        // BingoPartyManager.pollClosedParties) - toutes les 5 s.
        Bukkit.getScheduler().runTaskTimer(this, parties::pollClosedParties, 100L, 100L);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BingoNetworkListener(this, parties));

        PluginCommand command = getCommand("bingo");
        if (command != null) {
            command.setExecutor(new BingoCommand(this, parties));
        }

        BingoMenus menus = new BingoMenus(this, kg);
        // Bingo : serveur dedie separe, pas un Minigame/Arena classique de KalGames - bouton visible de tous
        // (comme /bingo create et /bingo join).
        // 1.3.0 : boutons fournis a KG_Menu (menu du serveur kal-games), decouverts au demarrage - remplace les prises
        // MenuEntry de KalGames et l'inscription directe dans KLM_Menu (1.2.0).
        getServer().getServicesManager().register(fr.kalium.kgmenu.api.MenuProvider.class, new fr.kalium.kgmenu.api.MenuProvider() {
            @Override
            public org.bukkit.plugin.Plugin owner() {
                return KGBingo.this;
            }

            @Override
            public int order() {
                return 20;
            }

            @Override
            public java.util.List<Entry> games(org.bukkit.entity.Player player) {
                return java.util.List.of(new Entry(ENTRY_ID, menus.t("bingo.hub-entry", "<gold><bold>Bingo"),
                        menus.t("bingo.hub-entry-tip", "<gray>Mini-jeu sur serveur dédié : créez une partie ou rejoignez-en une avec un code."),
                        (p, back) -> menus.openBingoMenu(p)));
            }

            @Override
            public java.util.List<Entry> settings(org.bukkit.entity.Player player) {
                if (!kg.isAdmin(player)) {
                    return java.util.List.of();
                }
                return java.util.List.of(new Entry(ENTRY_ID, menus.t("admin.home-bingo", "<light_purple>Bingo"),
                        menus.t("admin.home-bingo-tip", "<gray>Durée maximale d'une partie."), (p, back) -> menus.openBingoSettings(p)));
            }
        }, this, org.bukkit.plugin.ServicePriority.Normal);
        getLogger().info("KG_Bingo actif.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    public BingoPartyManager parties() {
        return parties;
    }
}
