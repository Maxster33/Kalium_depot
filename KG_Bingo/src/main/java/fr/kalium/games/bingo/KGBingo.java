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
    private BingoResults results;
    /** 1.6.0 : joueurs en Bingo sur Serveur Jeux (bouton du menu de kal-games). */
    private RelayCounter players;
    private BingoMenus menus;
    /** 1.10.0 : a appeler pour chaque joueur envoye en partie (lien avec KLM_Contacts), ou null. */
    private java.util.function.BiConsumer<org.bukkit.entity.Player, BingoParty> contactsLink;

    /** 1.10.0 : ce joueur part en partie ; son groupe de jeu (KLM_Contacts) le suit s'il en est le chef. */
    void contactsEntered(org.bukkit.entity.Player player, BingoParty party) {
        if (contactsLink == null) {
            return;
        }
        try {
            contactsLink.accept(player, party);
        } catch (LinkageError | RuntimeException e) {
            getLogger().warning("Annonce a KLM_Contacts impossible : " + e);
        }
    }

    /** 1.10.0 : un membre d'un groupe de jeu rejoint la partie de son chef (meme chemin que « Rejoindre avec un code »). */
    void groupJoin(org.bukkit.entity.Player player, String code) {
        BingoParty party = parties.join(player, code);
        if (party == null) {
            player.sendMessage(kg.prefix().append(menus.t("bingo.join-invalid", "<red>Code invalide, partie pleine ou introuvable.")));
            return;
        }
        player.sendMessage(kg.prefix().append(menus.t("bingo.joined", "<green>Partie rejointe, transfert en cours…")));
        parties.transferToBingo(player);
    }

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
        // 1.5.0 : points du Bingo credites dans les classements (resultats lus sur le relais HTTP).
        results = new BingoResults(this, kg.ranking(), kg.getConfig().getBoolean("stats.exclude-operators", true));
        // Retire de la liste les parties Bingo demarrees/annulees, signal lu sur le relais HTTP (voir
        // BingoPartyManager.pollClosedParties) - toutes les 5 s.
        Bukkit.getScheduler().runTaskTimer(this, parties::pollClosedParties, 100L, 100L);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BingoNetworkListener(this, parties));

        PluginCommand command = getCommand("bingo");
        if (command != null) {
            command.setExecutor(new BingoCommand(this, parties));
        }

        players = new RelayCounter(this, "compteur-bingo");
        BingoMenus menus = new BingoMenus(this, kg);
        // 1.9.0 : « Rejouer » - le joueur revenu du serveur Bingo par l'objet « Rejouer » recree ou rejoint la partie.
        getServer().getPluginManager().registerEvents(new BingoReplay(this, kg, menus), this);
        // 1.10.0 : groupes de jeu de KLM_Contacts (softdepend). Sans lui, ou avec une version sans cette fonction
        // (avant la 1.2.0), rien ne change.
        this.menus = menus;
        Plugin klmContacts = getServer().getPluginManager().getPlugin("KLM_Contacts");
        if (klmContacts != null) {
            try {
                contactsLink = ContactsLink.create(this, klmContacts);
            } catch (LinkageError | RuntimeException e) {
                getLogger().warning("KLM_Contacts trop ancien (1.2.0 ou plus attendu) : les groupes de jeu ne suivent pas "
                        + "leur chef au Bingo (" + e + ").");
            }
        }
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
                // 1.8.0 (demande de Maxster33) : joueurs possibles, 1 a (equipes max x joueurs par equipe max).
                int max = Math.max(1, getConfig().getInt("bingo.max-team-count", 4))
                        * Math.max(1, getConfig().getInt("bingo.max-team-size", 4));
                net.kyori.adventure.text.Component tip = menus.t("bingo.hub-entry-tip",
                                "<gray>Mini-jeu sur serveur dédié : créez une partie ou rejoignez-en une avec un code.")
                        .append(net.kyori.adventure.text.Component.newline())
                        .append(menus.t("bingo.hub-entry-players", "<gray>Joueurs : <white>1 à <max>", "max", max));
                return java.util.List.of(new Entry(ENTRY_ID, menus.t("bingo.hub-entry", "<gold><bold>Bingo"),
                        tip, (p, back) -> menus.openBingoMenu(p), players.value()));
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

    BingoResults results() {
        return results;
    }
}
