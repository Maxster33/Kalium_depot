package fr.kalium.core;

import fr.kalium.core.claims.ClaimsAdminMenu;
import fr.kalium.core.claims.ClaimsListener;
import fr.kalium.core.claims.ClaimsMenu;
import fr.kalium.core.claims.ClaimsService;
import fr.kalium.core.market.MarketAdminMenu;
import fr.kalium.core.market.MarketGui;
import fr.kalium.core.market.MarketGuiListener;
import fr.kalium.core.market.MarketService;
import fr.kalium.core.menu.DialogHelper;
import fr.kalium.core.menu.MenuService;
import fr.kalium.core.module.ModuleRegistry;
import fr.kalium.core.stats.CoreStatsSection;
import fr.kalium.core.stats.StatsListener;
import fr.kalium.core.stats.StatsService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Plugin de menu central pour serveurs de survie longue duree, base sur les Dialogs natifs de
 * Minecraft (meme approche que KaliumMenu, dont il reprend le modele). Concu pour etre reutilise
 * tel quel sur d'autres serveurs, y compris temporaires : pas de base de donnees externe, tout
 * est autonome dans le dossier du plugin.
 * <p>
 * Le menu s'ouvre par la commande /kalium (jamais par un objet fige en barre d'action, qui genait
 * trop en survie). Il propose le retour au lobby, le retour a la base (claim principal), un bouton
 * Statistiques et un bouton Claims (chacun desactivable independamment), et un bouton Parametres
 * reserve aux operateurs, d'ou sont pilotes tous les modules optionnels via ModuleRegistry.
 */
public final class KaliumCore extends JavaPlugin implements CommandExecutor, TabCompleter, PluginMessageListener {

    private static final String CHANNEL = "BungeeCord";

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Set<String> operators = new HashSet<>();

    private DialogHelper dialogs;
    private ModuleRegistry modules;
    private StatsService stats;
    private ClaimsService claims;
    private MenuService menu;
    private ClaimsMenu claimsMenu;
    private ClaimsAdminMenu claimsAdminMenu;
    private MarketService market;
    private MarketGui marketGui;
    private MarketAdminMenu marketAdminMenu;

    private String lobbyServer;
    private int buttonWidth;

    // ------------------------------------------------------------------ cycle de vie

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        dialogs = new DialogHelper(this);
        modules = new ModuleRegistry(this);

        stats = new StatsService(this);
        stats.registerSection(new CoreStatsSection(this));

        claims = new ClaimsService(this);
        claims.load();

        market = new MarketService(this);
        market.load();

        menu = new MenuService(this, dialogs);
        claimsMenu = new ClaimsMenu(this, dialogs);
        claimsAdminMenu = new ClaimsAdminMenu(this, dialogs);
        marketGui = new MarketGui(this, dialogs);
        marketAdminMenu = new MarketAdminMenu(this, dialogs);

        modules.register(new ModuleRegistry.ModuleInfo(
                "stats",
                mm.deserialize("Statistiques"),
                mm.deserialize(msg("stats-button")),
                mm.deserialize(msg("stats-description")),
                true,
                menu::openStats));
        modules.register(new ModuleRegistry.ModuleInfo(
                "claims",
                mm.deserialize("Claims"),
                mm.deserialize(msg("claims-button")),
                mm.deserialize(msg("claims-description")),
                true,
                claimsMenu::openMain,
                claimsAdminMenu::openSettings));
        modules.register(new ModuleRegistry.ModuleInfo(
                "market",
                mm.deserialize("Commerce"),
                mm.deserialize(msg("market-button")),
                mm.deserialize(msg("market-description")),
                true,
                marketGui::openHub,
                marketAdminMenu::openSettings));

        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getPluginManager().registerEvents(new StatsListener(this), this);
        getServer().getPluginManager().registerEvents(new ClaimsListener(this), this);
        getServer().getPluginManager().registerEvents(new MarketGuiListener(this), this);

        for (String name : List.of("kalium", "kaliumcore")) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(this);
                command.setTabCompleter(this);
            }
        }

        // Si le plugin est (re)charge alors que des joueurs sont deja connectes, on demarre quand
        // meme le suivi de leur temps de jeu au lieu d'attendre leur prochaine connexion.
        for (Player online : getServer().getOnlinePlayers()) {
            stats.onJoin(online);
        }

        long autosaveTicks = Math.max(20L, getConfig().getLong("stats-autosave-seconds", 300) * 20L);
        getServer().getScheduler().runTaskTimer(this, () -> {
            stats.tick();
            stats.saveDirty();
            market.tick();
        }, autosaveTicks, autosaveTicks);

        getLogger().info("KaliumCore actif.");
    }

    @Override
    public void onDisable() {
        if (stats != null) {
            stats.saveAllOnline(getServer().getOnlinePlayers());
        }
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    public void loadSettings() {
        reloadConfig();
        lobbyServer = getConfig().getString("lobby-server", "lobby");
        buttonWidth = Math.max(1, Math.min(1024, getConfig().getInt("button-width", 220)));

        operators.clear();
        for (String value : getConfig().getStringList("operators")) {
            if (value != null && !value.isBlank()) {
                operators.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
    }

    // ------------------------------------------------------------------ accesseurs (utilises par menu/stats/claims)

    public MiniMessage mm() {
        return mm;
    }

    public DialogHelper dialogs() {
        return dialogs;
    }

    public ModuleRegistry modules() {
        return modules;
    }

    public StatsService stats() {
        return stats;
    }

    public ClaimsService claims() {
        return claims;
    }

    public MenuService menu() {
        return menu;
    }

    public ClaimsMenu claimsMenu() {
        return claimsMenu;
    }

    public ClaimsAdminMenu claimsAdminMenu() {
        return claimsAdminMenu;
    }

    public MarketService market() {
        return market;
    }

    public MarketGui marketGui() {
        return marketGui;
    }

    public MarketAdminMenu marketAdminMenu() {
        return marketAdminMenu;
    }

    public String lobbyServer() {
        return lobbyServer;
    }

    public int buttonWidth() {
        return buttonWidth;
    }

    /** Voit le bouton Parametres et les commandes d'administration : permission, ou liste "operators" du config.yml. */
    public boolean isAdmin(Player player) {
        return player.hasPermission("kaliumcore.admin")
                || operators.contains(player.getName().toLowerCase(Locale.ROOT))
                || operators.contains(player.getUniqueId().toString().toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------ commandes

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        if (name.equals("kaliumcore")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                loadSettings();
                sender.sendMessage(mm.deserialize(msg("reloaded")));
            } else {
                sender.sendMessage(mm.deserialize("<gray>Usage : /" + label + " reload"));
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        if (name.equals("kalium")) {
            if (!menu.onCooldown(player)) {
                menu.openMain(player);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("kaliumcore") && args.length == 1) {
            List<String> result = new ArrayList<>();
            if ("reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                result.add("reload");
            }
            return result;
        }
        return List.of();
    }

    // ------------------------------------------------------------------ canal BungeeCord / Velocity

    public void connectToLobby(Player player) {
        if (!player.isOnline()) {
            return;
        }
        byte[] data = pluginMessage("Connect", lobbyServer);
        if (data == null) {
            return;
        }
        player.sendMessage(mm.deserialize(msg("connecting"), Placeholder.unparsed("server", lobbyServer)));
        player.sendPluginMessage(this, CHANNEL, data);
    }

    private byte[] pluginMessage(String subChannel, String argument) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(subChannel);
            out.writeUTF(argument);
        } catch (IOException e) {
            getLogger().warning("Impossible de construire le message " + subChannel + " : " + e.getMessage());
            return null;
        }
        return bytes.toByteArray();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // Rien a lire pour l'instant : KaliumCore n'affiche pas de compteur de joueurs.
    }

    // ------------------------------------------------------------------ textes

    public String msg(String key) {
        String value = getConfig().getString("messages." + key);
        if (value != null) {
            return value;
        }
        return switch (key) {
            case "menu-title" -> "<#09add3><bold>KaLium</bold> <dark_gray>- <white>Menu";
            case "menu-header" -> "<gray>Que veux-tu faire ?";
            case "close-button" -> "<red>Fermer";
            case "back-button" -> "<#09add3><bold>Retour au lobby";
            case "back-description" -> "<gray>Clique pour retourner au lobby.";
            case "back-menu-button" -> "<gray>Retour au menu";
            case "home-button" -> "<#09add3><bold>Retour à la base";
            case "home-description" -> "<gray>Te téléporte sur ton claim principal.";
            case "home-none" -> "<red>Tu n'as pas encore de base définie.";
            case "home-teleported" -> "<green>Direction ta base !";
            case "stats-button" -> "<yellow><bold>Statistiques";
            case "stats-description" -> "<gray>Consulte tes statistiques sur ce serveur.";
            case "settings-button" -> "<gray><bold>Paramètres";
            case "settings-description" -> "<gray>Réservé aux opérateurs : active/désactive les options du menu.";
            case "connecting" -> "<gray>Connexion à <white><server><gray>...";
            case "already-in-lobby" -> "<yellow>Tu es déjà au lobby.";
            case "reloaded" -> "<green>Configuration rechargée.";
            case "no-permission" -> "<red>Tu n'as pas la permission d'utiliser cette commande.";
            case "stats-title" -> "<yellow><bold>Statistiques";
            case "stats-header" -> "<gray>Voici tes statistiques sur ce serveur.";
            case "stats-playtime" -> "<#09add3>Temps de jeu <dark_gray>: <white><value>";
            case "stats-levels-spent" -> "<#09add3>Niveaux dépensés <dark_gray>: <white><value>";
            case "stats-blocks-broken" -> "<#09add3>Blocs cassés <dark_gray>: <white><value>";
            case "stats-blocks-placed" -> "<#09add3>Blocs posés <dark_gray>: <white><value>";
            case "stats-monsters-killed" -> "<#09add3>Monstres tués <dark_gray>: <white><value>";
            case "settings-title" -> "<gray><bold>Paramètres</bold> <dark_gray>- <white>KaliumCore";
            case "settings-header" -> "<gray>Active ou désactive les options du menu.";
            case "settings-open-button" -> "<#09add3><bold><module> <gray>›";
            case "settings-reload-button" -> "<#09add3><bold>Recharger la configuration";
            case "settings-options-button" -> "<#09add3><bold>Options actives";
            case "settings-options-title" -> "<gray><bold>Paramètres</bold> <dark_gray>- <white>Options actives";
            case "settings-options-header" -> "<gray>Coche ou décoche chaque option, puis clique sur Enregistrer.";
            case "settings-options-save-button" -> "<green><bold>Enregistrer";
            case "settings-options-saved" -> "<green>Options mises à jour.";
            case "toggle-on" -> "Activé";
            case "toggle-off" -> "Désactivé";

            // ---------------------------------------------------------- claims (joueur)
            case "claims-button" -> "<gold><bold>Claims";
            case "claims-description" -> "<gray>Réclame ce chunk, gère ta base et tes emplacements.";
            case "claims-title" -> "<gold><bold>Claims";
            case "claims-header" -> "<gray>Territoire du chunk où tu te trouves.";
            case "claims-slots" -> "<#09add3>Emplacements <dark_gray>: <white><used> / <total> utilisés";
            case "claims-chunk-free" -> "<green>Ce chunk est libre.";
            case "claims-chunk-mine" -> "<green>Ce chunk t'appartient.";
            case "claims-chunk-other" -> "<red>Ce chunk appartient à <white><owner><red>.";
            case "claims-claim-button" -> "<green><bold>Réclamer ce chunk";
            case "claims-claim-ok" -> "<green>Chunk réclamé.";
            case "claims-claim-taken" -> "<red>Ce chunk est déjà réclamé.";
            case "claims-claim-no-slots" -> "<red>Tu n'as plus d'emplacement disponible.";
            case "claims-unclaim-button" -> "<red><bold>Libérer ce chunk";
            case "claims-unclaim-ok" -> "<green>Chunk libéré.";
            case "claims-unclaim-not-owner" -> "<red>Ce chunk ne t'appartient pas.";
            case "claims-home-set-button" -> "<#09add3><bold>Définir comme base";
            case "claims-home-set-ok" -> "<green>Nouvelle base définie.";
            case "claims-home-set-not-owner" -> "<red>Tu dois te trouver sur un de tes chunks pour le définir comme base.";
            case "claims-token-to-item-button" -> "<gray><bold>Convertir un emplacement en jeton";
            case "claims-token-to-item-description" -> "<gray>Transforme un emplacement libre en objet transportable.";
            case "claims-token-to-item-ok" -> "<green>Emplacement converti en jeton.";
            case "claims-token-to-item-none" -> "<red>Tu n'as pas d'emplacement disponible à convertir.";
            case "claims-item-to-token-button" -> "<gray><bold>Convertir un jeton en emplacement";
            case "claims-item-to-token-description" -> "<gray>Consomme un jeton de ton inventaire pour gagner un emplacement.";
            case "claims-item-to-token-ok" -> "<green>Jeton converti en emplacement.";
            case "claims-item-to-token-none" -> "<red>Tu n'as pas de jeton d'emplacement dans ton inventaire.";
            case "claims-protected-build" -> "<red>Ce chunk appartient à <white><owner><red> : tu ne peux rien y casser ni poser.";
            case "claims-protected-interact" -> "<red>Ce chunk appartient à <white><owner><red> : tu ne peux pas utiliser ce bloc.";
            case "claims-protected-pvp" -> "<red>Le combat est bloqué : <white><owner><red> est protégé dans son chunk.";

            // ---------------------------------------------------------- claims (parametres)
            case "settings-claims-header" -> "<gray>Emplacements par défaut et gestion des joueurs. Le bouton \"Options\" ci-dessous regroupe les réglages fins (claim, unclaim, protection...).";
            case "settings-claims-title" -> "<gold><bold>Paramètres</bold> <dark_gray>- <white>Claims";
            case "settings-claims-toggle-master" -> "Système de claim";
            case "settings-claims-toggle-claim" -> "Réclamer un chunk (claim)";
            case "settings-claims-toggle-unclaim" -> "Libérer un chunk (unclaim)";
            case "settings-claims-toggle-protection-break" -> "Protection - casse de blocs";
            case "settings-claims-toggle-protection-place" -> "Protection - dépôt de blocs";
            case "settings-claims-toggle-protection-interact" -> "Protection - utilisation de blocs (coffres, établis...)";
            case "settings-claims-toggle-protection-pvp" -> "Protection - PvP";
            case "settings-claims-toggle-home" -> "Chunk principal (base)";
            case "settings-claims-toggle-item" -> "Conversion en jeton (emplacement ↔ objet)";
            case "settings-claims-options-button" -> "<#09add3><bold>Options";
            case "settings-claims-options-title" -> "<gold><bold>Claims</bold> <dark_gray>- <white>Options";
            case "settings-claims-options-header" -> "<gray>Coche ou décoche chaque option, puis clique sur Enregistrer.";
            case "settings-claims-default-slots-label" -> "<gray>Emplacements par défaut (nouveaux joueurs)";
            case "settings-claims-default-slots-current" -> "<gray>Valeur actuelle : <white><value>";
            case "settings-claims-default-slots-save" -> "<#09add3><bold>Enregistrer les emplacements par défaut";
            case "settings-claims-default-slots-saved" -> "<green>Emplacements par défaut réglés sur <white><value><green>.";
            case "settings-claims-manage-button" -> "<#09add3><bold>Gérer les emplacements d'un joueur";
            case "settings-claims-manage-title" -> "<gold><bold>Claims</bold> <dark_gray>- <white>Gestion joueur";
            case "settings-claims-manage-header" -> "<gray>Tape un pseudo, consulte ses emplacements, puis ajuste-les.";
            case "settings-claims-manage-player-label" -> "<gray>Pseudo du joueur";
            case "settings-claims-manage-delta-label" -> "<gray>Ajustement (+/-)";
            case "settings-claims-manage-consult-button" -> "<#09add3><bold>Consulter";
            case "settings-claims-manage-apply-button" -> "<green><bold>Appliquer l'ajustement";
            case "settings-claims-manage-unknown-player" -> "<red>Joueur introuvable.";
            case "settings-claims-manage-info" -> "<gray><player> <dark_gray>: <white><used><gray>/<white><total> <gray>utilisés (<white><available> <gray>libres)";
            case "settings-claims-manage-applied" -> "<green>Emplacements de <white><player><green> réglés sur <white><total><green>.";

            // ---------------------------------------------------------- commerce (joueur)
            case "market-button" -> "<dark_green><bold>Commerce";
            case "market-description" -> "<gray>Achète, vends et gère tes objets contre des émeraudes.";
            case "market-hub-title" -> "<gold><bold>Commerce";
            case "market-hub-header" -> "<gray>Achète, vends et gère tes objets contre des émeraudes.";
            case "market-hub-sell-button" -> "<green><bold>Mettre en vente";
            case "market-hub-mesventes-button" -> "<#09add3><bold>Mes ventes";
            case "market-hub-search-button" -> "<#09add3><bold>Recherche";
            case "market-hub-marche-button" -> "<#09add3><bold>Marché";
            case "market-hub-flash-button" -> "<gold><bold>Vente flash";
            case "market-hub-history-sales-button" -> "<#09add3><bold>Historique des ventes";
            case "market-hub-history-purchases-button" -> "<#09add3><bold>Historique des achats";
            case "market-hub-rewards-button" -> "<gold><bold>Rewards <dark_gray>(<white><count><dark_gray>)";
            case "market-reward-title" -> "<gray><bold>Commerce</bold> <dark_gray>- <white>Récompenses";
            case "market-title-flash" -> "<gold><bold>Commerce</bold> <dark_gray>- <white>Vente flash";
            case "market-title-browse" -> "<gold><bold>Commerce</bold> <dark_gray>- <white>Marché";
            case "market-title-mesventes" -> "<gold><bold>Commerce</bold> <dark_gray>- <white>Mes ventes";
            case "market-title-recherche" -> "<gold><bold>Commerce</bold> <dark_gray>- <white>Recherche";
            case "market-page-prev" -> "<#09add3><bold>« Page précédente";
            case "market-page-next" -> "<#09add3><bold>Page suivante »";
            case "market-page-info" -> "<gray>Page <white><page></white> / <white><total></white>";
            case "market-back-menu" -> "<gray>« Retour au menu";
            case "market-empty-flash" -> "<gray>Aucune vente flash en cours.";
            case "market-empty-browse" -> "<gray>Aucune annonce en vente.";
            case "market-empty-sell" -> "<gray>Tu n'as aucune annonce active.";
            case "market-empty-search" -> "<gray>Aucun objet en vente ne correspond à ta recherche.";
            case "market-empty-rewards" -> "<gray>Ton coffre de récompenses est vide.";
            case "market-lore-price" -> "<#09add3>Prix <dark_gray>: <white><price> émeraude(s)";
            case "market-lore-average" -> "<#09add3>Prix de vente moyen <dark_gray>: <white><value> émeraude(s)";
            case "market-lore-average-none" -> "<#09add3>Prix de vente moyen <dark_gray>: <gray>Aucune donnée";
            case "market-lore-seller" -> "<#09add3>Vendeur <dark_gray>: <white><seller>";
            case "market-lore-buy-hint" -> "<green>Clique pour acheter";
            case "market-lore-cancel-hint" -> "<red>Clique pour retirer cette annonce";
            case "market-lore-expires" -> "<#09add3>Expire le <dark_gray>: <white><date>";
            case "market-lore-flash-stock" -> "<#09add3>Stock restant <dark_gray>: <white><remaining> / <total>";
            case "market-lore-flash-window-active" -> "<#09add3>Se termine le <dark_gray>: <white><date>";
            case "market-lore-flash-window-upcoming" -> "<#09add3>Disponible à partir du <dark_gray>: <white><date>";
            case "market-lore-flash-soon" -> "<yellow>Pas encore disponible";
            case "market-sell-dialog-title" -> "<gold><bold>Commerce</bold> <dark_gray>- <white>Mettre en vente";
            case "market-sell-dialog-header" -> "<gray>Choisis le prix, la quantité et la durée, puis confirme.";
            case "market-sell-dialog-price-label" -> "<gray>Prix (émeraudes)";
            case "market-sell-dialog-quantity-label" -> "<gray>Quantité";
            case "market-sell-dialog-duration-label" -> "<gray>Durée de la vente (jours, 7 max)";
            case "market-sell-dialog-confirm-button" -> "<green><bold>Mettre en vente";
            case "market-sell-ok" -> "<green>Objet mis en vente.";
            case "market-sell-empty-hand" -> "<red>Tu dois tenir l'objet à vendre en main.";
            case "market-sell-limit" -> "<red>Tu as atteint ta limite d'annonces actives.";
            case "market-sell-disabled" -> "<red>Le système de commerce est désactivé.";
            case "market-sell-bad-price" -> "<red>Prix invalide.";
            case "market-cancel-ok" -> "<green>Annonce retirée, objet récupéré.";
            case "market-buy-ok" -> "<green>Achat effectué ! Récupère ton objet dans le coffre de récompenses.";
            case "market-buy-not-enough" -> "<red>Tu n'as pas assez d'émeraudes.";
            case "market-buy-own-listing" -> "<red>Tu ne peux pas acheter ta propre annonce.";
            case "market-buy-not-found" -> "<red>Cette annonce n'est plus disponible.";
            case "market-buy-disabled" -> "<red>Le système de commerce est désactivé.";
            case "market-search-dialog-title" -> "<gold><bold>Commerce</bold> <dark_gray>- <white>Recherche";
            case "market-search-dialog-header" -> "<gray>Tape un mot-clé (nom français ou anglais), puis confirme.";
            case "market-search-dialog-label" -> "<gray>Mot-clé";
            case "market-search-dialog-confirm-button" -> "<green><bold>Rechercher";
            case "market-history-ventes-title" -> "<gold><bold>Commerce</bold> <dark_gray>- <white>Historique des ventes";
            case "market-history-ventes-header" -> "<gray>Tes <white>ventes</white> les plus récentes (40 max).";
            case "market-history-ventes-empty" -> "<gray>Tu n'as encore rien vendu.";
            case "market-history-achats-title" -> "<gold><bold>Commerce</bold> <dark_gray>- <white>Historique des achats";
            case "market-history-achats-header" -> "<gray>Tes <white>achats</white> les plus récents (40 max).";
            case "market-history-achats-empty" -> "<gray>Tu n'as encore rien acheté.";
            case "market-history-line-vente" -> "<white><item> <gray>x<qty> <dark_gray>- <green><price> émeraude(s) <dark_gray>- <gray>à <white><counterpart> <dark_gray>- <gray><date>";
            case "market-history-line-achat" -> "<white><item> <gray>x<qty> <dark_gray>- <red><price> émeraude(s) <dark_gray>- <gray>à <white><counterpart> <dark_gray>- <gray><date>";
            case "market-history-line-flash" -> "<white><item> <gray>x<qty> <dark_gray>- <gold><price> émeraude(s) <dark_gray>- <gray>vente flash de <white><counterpart> <dark_gray>- <gray><date>";

            // ---------------------------------------------------------- commerce (parametres)
            case "settings-market-header" -> "<gray>Limites du commerce et ventes flash programmées.";
            case "settings-market-title" -> "<gold><bold>Paramètres</bold> <dark_gray>- <white>Commerce";
            case "settings-market-toggle-master" -> "Système de commerce";
            case "settings-market-max-listings-label" -> "<gray>Annonces actives max par joueur (0 = illimité)";
            case "settings-market-max-listings-current" -> "<gray>Valeur actuelle : <white><value>";
            case "settings-market-max-listings-save" -> "<#09add3><bold>Enregistrer les limites";
            case "settings-market-max-listings-saved" -> "<green>Limite d'annonces réglée sur <white><value><green>.";
            case "settings-market-duration-label" -> "<gray>Durée par défaut proposée à la mise en vente (jours, 7 max)";
            case "settings-market-schedule-flash-button" -> "<#09add3><bold>Programmer une vente flash";
            case "settings-market-flash-list-button" -> "<#09add3><bold>Ventes flash programmées";
            case "settings-market-flash-schedule-title" -> "<gold><bold>Commerce</bold> <dark_gray>- <white>Programmer une vente flash";
            case "settings-market-flash-schedule-header" -> "<gray>Tiens l'objet à proposer en main, choisis prix/stock/délai/durée, puis confirme.";
            case "settings-market-flash-price-label" -> "<gray>Prix (émeraudes) par lot";
            case "settings-market-flash-stock-label" -> "<gray>Stock (nombre de lots)";
            case "settings-market-flash-delay-label" -> "<gray>Délai avant démarrage (minutes)";
            case "settings-market-flash-duration-label" -> "<gray>Durée de la vente (minutes)";
            case "settings-market-flash-confirm-button" -> "<green><bold>Programmer";
            case "settings-market-flash-ok" -> "<green>Vente flash programmée.";
            case "settings-market-flash-empty-hand" -> "<red>Tu dois tenir l'objet à proposer en main.";
            case "settings-market-flash-bad-params" -> "<red>Paramètres invalides.";
            case "settings-market-flash-list-title" -> "<gold><bold>Commerce</bold> <dark_gray>- <white>Ventes flash programmées";
            case "settings-market-flash-list-header" -> "<gray>Clique sur une vente pour l'annuler.";
            case "settings-market-flash-list-empty" -> "<gray>Aucune vente flash programmée.";
            case "settings-market-flash-list-entry" -> "<white><item> <dark_gray>- <gray><status>";
            case "settings-market-flash-cancel-ok" -> "<green>Vente flash annulée.";
            default -> "<red>[" + key + "]";
        };
    }
}
