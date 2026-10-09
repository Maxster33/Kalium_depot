package fr.kalium.contacts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.MenuSection;
import net.kyori.adventure.text.Component;

/**
 * KLM_Contacts - étape 1 « amis » (demande de LeKiwi06, 09/10/2026 : « un KLM_Contacts pour ajouter nos amis, créer des
 * groupes avec eux, voir dans quels serveurs de KaLium ils sont »). Présent sur chaque serveur Paper : menus et commande
 * /amis. Les données, la présence, les déplacements et les messages privés (/mp, /r) sont sur le proxy (KaliumRelay
 * 1.6.0), voir {@link Relais}.
 *
 * - /amis : ouvre le menu ; /amis ajouter | accepter | refuser | retirer | rejoindre &lt;pseudo&gt;.
 * - /bloquer &lt;pseudo&gt;, /debloquer &lt;pseudo&gt;.
 * - Bouton « Contacts » du comparateur « Informations » (KLM_Menu 2.10.0 ; avec une version plus ancienne, le plugin
 *   fonctionne par ses commandes, sans le bouton).
 * Groupes de jeu et invitations en partie : étapes 2 et 3 du cahier des charges, pas dans cette version.
 */
public final class KlmContacts extends JavaPlugin {

    private static final List<String> SOUS_COMMANDES = List.of("ajouter", "accepter", "refuser", "retirer", "rejoindre");

    private Lang lang;
    private Relais relais;
    private Menus menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        relais = new Relais(this);
        menus = new Menus(this, relais, lang, new Gui(this, lang));
        Component titre = lang.c("section.titre", "<light_purple><bold>Contacts");
        Component description = lang.c("section.description", "<gray>Tes amis, le serveur où ils sont, les demandes d'ami.");
        getServer().getServicesManager().register(MenuSection.class, new MenuSection() {
            @Override
            public String id() {
                return "contacts";
            }

            @Override
            public Plugin owner() {
                return KlmContacts.this;
            }

            @Override
            public Component title() {
                return titre;
            }

            @Override
            public Component description() {
                return description;
            }

            @Override
            public boolean informations() {
                return true;
            }

            @Override
            public int order() {
                return 50;
            }

            @Override
            public void open(Player joueur, Consumer<Player> retour) {
                menus.ouvrir(joueur, retour);
            }
        }, this, ServicePriority.Normal);
        lang.saveIfNeeded();
        if (getConfig().getString("relay-token", "").isBlank()) {
            getLogger().warning("relay-token est vide dans config.yml : les contacts sont indisponibles tant qu'il n'est pas "
                    + "rempli (même valeur que sur les autres plugins reliés au relais).");
        }
    }

    @Override
    public void onDisable() {
        if (lang != null) {
            lang.saveIfNeeded();
        }
    }

    Component prefixe() {
        return lang.c("prefixe", "<light_purple>Contacts » ");
    }

    void dire(Player joueur, String cle, String def, Object... paires) {
        joueur.sendMessage(prefixe().append(lang.c(cle, def, paires)));
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ commandes

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "bloquer" -> {
                if (args.length != 1) {
                    dire(joueur, "usage.bloquer", "<gray>Utilisation : <white>/bloquer pseudo");
                } else {
                    agir(joueur, "block", args[0], null);
                }
            }
            case "debloquer" -> {
                if (args.length != 1) {
                    dire(joueur, "usage.debloquer", "<gray>Utilisation : <white>/debloquer pseudo");
                } else {
                    agir(joueur, "unblock", args[0], null);
                }
            }
            default -> {
                if (args.length == 0) {
                    menus.ouvrir(joueur, null);
                } else if (args.length != 2 || !SOUS_COMMANDES.contains(args[0].toLowerCase(Locale.ROOT))) {
                    dire(joueur, "usage.amis",
                            "<gray>Utilisation : <white>/amis</white> (menu), <white>/amis ajouter | accepter | refuser | retirer | rejoindre pseudo");
                } else {
                    switch (args[0].toLowerCase(Locale.ROOT)) {
                        case "ajouter" -> agir(joueur, "add", args[1], null);
                        case "accepter" -> agir(joueur, "accept", args[1], null);
                        case "refuser" -> agir(joueur, "deny", args[1], null);
                        case "retirer" -> agir(joueur, "remove", args[1], null);
                        default -> rejoindre(joueur, args[1]);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> propositions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("amis") && args.length == 1) {
            for (String sous : SOUS_COMMANDES) {
                if (sous.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    propositions.add(sous);
                }
            }
            return propositions;
        }
        return null; // pseudo : joueurs de ce serveur
    }

    // ------------------------------------------------------------------ actions

    /**
     * Action sur un joueur (add, accept, deny, cancel, remove, block, unblock), désigné par son pseudo ou son UUID : le
     * résultat est dit dans le tchat, puis {@code ensuite} est appelé (retour au menu), s'il est fourni.
     */
    void agir(Player joueur, String action, String cible, Consumer<Player> ensuite) {
        relais.appeler(joueur, action, Map.of("target", cible), reponse -> {
            if (reponse == null) {
                return;
            }
            if (reponse.ok()) {
                // « ok:sent:<pseudo> », « ok:friends:<pseudo> » (add, accept) ou « ok:<pseudo> ».
                String detail = reponse.partie(0);
                switch (action) {
                    case "add", "accept" -> {
                        if (detail.equals("friends")) {
                            dire(joueur, "fait.amis", "<green><white><nom></white> et toi êtes maintenant amis.", "nom", reponse.partie(1));
                        } else {
                            dire(joueur, "fait.demande", "<green>Demande d'ami envoyée à <white><nom></white>.", "nom", reponse.partie(1));
                        }
                    }
                    case "deny" -> dire(joueur, "fait.refuse", "<gray>Demande de <white><nom></white> refusée.", "nom", detail);
                    case "cancel" -> dire(joueur, "fait.annule", "<gray>Demande à <white><nom></white> annulée.", "nom", detail);
                    case "remove" -> dire(joueur, "fait.retire", "<gray><white><nom></white> n'est plus dans tes amis.", "nom", detail);
                    case "block" -> dire(joueur, "fait.bloque", "<gray><white><nom></white> est bloqué.", "nom", detail);
                    case "unblock" -> dire(joueur, "fait.debloque", "<gray><white><nom></white> est débloqué.", "nom", detail);
                    default -> {
                    }
                }
            } else {
                refus(joueur, action, reponse.partie(0));
            }
            if (ensuite != null) {
                ensuite.accept(joueur);
            }
        });
    }

    private void refus(Player joueur, String action, String code) {
        switch (code) {
            case "unknown" -> dire(joueur, "refus.inconnu", "<red>Joueur inconnu : il doit être venu au moins une fois sur KaLium.");
            case "self" -> dire(joueur, "refus.soi", "<red>C'est toi.");
            case "already" -> dire(joueur, "refus.deja-ami", "<yellow>Ce joueur est déjà ton ami.");
            case "pending" -> dire(joueur, "refus.deja-demande", "<yellow>Tu lui as déjà envoyé une demande.");
            case "blocked" -> dire(joueur, "refus.bloque", "<red>Tu as bloqué ce joueur : débloque-le d'abord (/debloquer).");
            case "full" -> dire(joueur, "refus.plein", "<red>Ta liste d'amis est pleine.");
            case "target-full" -> dire(joueur, "refus.plein-autre", "<red>Sa liste d'amis est pleine.");
            case "too-many" -> dire(joueur, "refus.trop-demandes", "<red>Trop de demandes en attente : annules-en d'abord.");
            case "none" -> {
                switch (action) {
                    case "remove" -> dire(joueur, "refus.pas-ami", "<red>Ce joueur n'est pas dans tes amis.");
                    case "unblock" -> dire(joueur, "refus.pas-bloque", "<red>Ce joueur n'est pas bloqué.");
                    default -> dire(joueur, "refus.pas-demande", "<red>Aucune demande d'ami avec ce joueur.");
                }
            }
            default -> dire(joueur, "refus.autre", "<red>Action impossible.");
        }
    }

    /** « Rejoindre son serveur » : le proxy déplace le joueur vers le serveur de son ami. */
    void rejoindre(Player joueur, String cible) {
        relais.appeler(joueur, "join", Map.of("target", cible), reponse -> {
            if (reponse == null) {
                return;
            }
            if (reponse.ok()) {
                dire(joueur, "rejoindre.fait", "<gray>Connexion à <white><serveur></white>…", "serveur", menus.serveur(reponse.partie(0)));
                return;
            }
            switch (reponse.partie(0)) {
                case "unknown" -> dire(joueur, "refus.inconnu", "<red>Joueur inconnu : il doit être venu au moins une fois sur KaLium.");
                case "not-friend" -> dire(joueur, "refus.pas-ami", "<red>Ce joueur n'est pas dans tes amis.");
                case "offline" -> dire(joueur, "rejoindre.hors-ligne", "<red>Cet ami n'est pas en ligne.");
                case "same" -> dire(joueur, "rejoindre.meme", "<yellow>Vous êtes déjà sur le même serveur.");
                case "closed" -> dire(joueur, "rejoindre.ferme", "<red>On n'entre pas librement sur <white><serveur></white> : il faut y être invité par une partie.",
                        "serveur", menus.serveur(reponse.partie(1)));
                default -> dire(joueur, "refus.autre", "<red>Action impossible.");
            }
        });
    }
}
