package fr.kalium.claim;

import fr.kalium.economy.KSEconomy;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Menus de KS_Claim (Dialogs de la boîte à outils de KLM_Menu). Textes des boutons et des champs courts : ils ne doivent
 * jamais défiler (demande de LeKiwi06) ; les détails sont écrits en texte en haut des menus.
 */
final class Menus {

    private static final int PAR_PAGE = 10;
    private static final int MEMBRES_MAX = 5;

    /** Noms français des réglages de SCS (clé de SCS -> nom affiché). */
    private static final Map<String, String> REGLAGES = new LinkedHashMap<>();

    static {
        String[][] noms = {
                {"Build", "Construire"}, {"Destroy", "Casser"}, {"Buttons", "Boutons"}, {"Items", "Utiliser des objets"},
                {"InteractBlocks", "Contenants"}, {"Levers", "Leviers"}, {"Plates", "Plaques de pression"},
                {"Doors", "Portes"}, {"Trapdoors", "Trappes"}, {"Fencegates", "Portillons"},
                {"Tripwires", "Fils de déclenchement"}, {"RepeatersComparators", "Répéteurs, comparateurs"},
                {"Bells", "Cloches"}, {"Entities", "Entités"}, {"Frostwalker", "Semelles givrantes"},
                {"Teleportations", "Téléportations"}, {"Damages", "Dégâts aux entités"}, {"Fly", "Vol"},
                {"GuiTeleport", "Téléportation par menu"}, {"Portals", "Portails"}, {"Enter", "Entrer"},
                {"ItemsPickup", "Ramasser des objets"}, {"ItemsDrop", "Jeter des objets"},
                {"SpecialBlocks", "Blocs spéciaux"}, {"Elytra", "Élytres"}, {"Windcharges", "Charges de vent"},
                {"Weather", "Météo"}, {"Explosions", "Explosions"}, {"Liquids", "Liquides"}, {"Redstone", "Redstone"},
                {"Firespread", "Propagation du feu"}, {"Pvp", "PvP"}, {"Monsters", "Monstres"}};
        for (String[] nom : noms) {
            REGLAGES.put(nom[0], nom[1]);
        }
    }

    private final KSClaim plugin;
    private final Lang lang;
    private final Gui gui;

    Menus(KSClaim plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private static String pts(long valeur) {
        return valeur == 0 ? "gratuit" : nombre(valeur) + (valeur > 1 ? " points" : " point");
    }

    private static String nombre(long valeur) {
        String chiffres = String.valueOf(valeur);
        StringBuilder sortie = new StringBuilder();
        for (int i = 0; i < chiffres.length(); i++) {
            if (i > 0 && (chiffres.length() - i) % 3 == 0) {
                sortie.append(' ');
            }
            sortie.append(chiffres.charAt(i));
        }
        return sortie.toString();
    }

    private void message(Player joueur, Component texte, Consumer<Player> ensuite) {
        gui.notice(joueur, t("titre", "<aqua><bold>Claims"), texte, ensuite::accept);
        lang.saveIfNeeded();
    }

    private ActionButton retour(Consumer<Player> vers) {
        return gui.button(t("bouton.retour", "<gray>Retour"), null, vers::accept);
    }

    private static String monde(World monde) {
        return switch (monde.getEnvironment()) {
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> "Overworld";
        };
    }

    /** « Overworld, x 120 z -40 » (centre du chunk). */
    private static String position(Claim claim) {
        Chunk chunk = claim.getChunks().iterator().next();
        return monde(chunk.getWorld()) + ", x " + (chunk.getX() * 16 + 8) + " z " + (chunk.getZ() * 16 + 8);
    }

    // ------------------------------------------------------------------ accueil

    void ouvrir(Player joueur) {
        UUID uuid = joueur.getUniqueId();
        int nombre = KSClaim.nombreDeClaims(uuid);
        List<Component> corps = List.of(
                t("accueil.nombre", "<white>Claims : <green><nombre></green> / <max>", "nombre", nombre, "max",
                        plugin.claimsMax()),
                t("accueil.prochain", "<white>Prochain claim : <yellow><prix>", "prix", pts(plugin.prochainPrix(uuid))),
                t("accueil.aide", "<gray>1 claim = 1 chunk (16 x 16 blocs, toute la hauteur). « Claimer ici » "
                        + "claime le chunk où tu te trouves (aussi : /ksclaim)."));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("bouton.claimer", "<green>Claimer ici"), null, this::claimerIci));
        boutons.add(gui.button(t("bouton.mes-claims", "<white>Mes claims (<nombre>)", "nombre", nombre), null,
                p -> listeClaims(p, 0)));
        boutons.add(gui.button(t("bouton.groupes", "<white>Mes groupes"), null, this::listeGroupes));
        boutons.add(gui.button(t("bouton.prix", "<white>Prix des claims"), null, this::prixSuivants));
        gui.open(joueur, t("titre", "<aqua><bold>Claims"), corps, List.of(), boutons, null, 2);
        lang.saveIfNeeded();
    }

    private void prixSuivants(Player joueur) {
        int n = KSClaim.nombreDeClaims(joueur.getUniqueId()) + 1;
        List<Component> corps = new ArrayList<>();
        corps.add(t("prix.regle", "<gray>Les <gratuits> premiers claims sont gratuits, puis le n-ième coûte 16n + n²/2 "
                + "points (arrondi au-dessus) ; <max> claims au plus. Supprimer un claim rembourse le dernier prix payé.",
                "gratuits", plugin.claimsGratuits(), "max", plugin.claimsMax()));
        for (int i = n; i < n + 5 && i <= plugin.claimsMax(); i++) {
            corps.add(t("prix.ligne", "<white><n>e claim : <yellow><prix>", "n", i, "prix", pts(plugin.prix(i))));
        }
        gui.open(joueur, t("prix.titre", "<aqua><bold>Prix des claims"), corps, List.of(), List.of(retour(this::ouvrir)),
                null, 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ claimer

    /** Chunk du joueur : à lui -> son claim ; à vendre -> achat ; libre -> création (prix, confirmation). */
    void claimerIci(Player joueur) {
        Chunk chunk = joueur.getLocation().getChunk();
        Claim claim = KSClaim.api().getClaimAtChunk(chunk);
        if (claim != null) {
            if (joueur.getUniqueId().equals(claim.getUUID())) {
                menuClaim(joueur, claim.getId());
            } else if (claim.getSale()) {
                proposerAchat(joueur, claim);
            } else {
                message(joueur, t("claimer.pris", "<red>Ce chunk est déjà claimé par <proprio>.", "proprio",
                        claim.getOwner()), this::ouvrir);
            }
            return;
        }
        Component refus = refusCreation(joueur, chunk);
        if (refus != null) {
            message(joueur, refus, this::ouvrir);
            return;
        }
        long prix = plugin.prochainPrix(joueur.getUniqueId());
        gui.confirm(joueur, t("claimer.titre", "<green><bold>Claimer ce chunk"),
                t("claimer.texte", "<white>Chunk <x>, <z> (<monde>). Prix : <yellow><prix></yellow>.", "x", chunk.getX(),
                        "z", chunk.getZ(), "monde", monde(chunk.getWorld()), "prix", pts(prix)),
                p -> creer(p, chunk), this::ouvrir);
        lang.saveIfNeeded();
    }

    private Component refusCreation(Player joueur, Chunk chunk) {
        String refus = plugin.refusChunk(chunk);
        if ("deja".equals(refus)) {
            return t("claimer.deja", "<red>Ce chunk est déjà claimé.");
        }
        if ("monde".equals(refus)) {
            return t("claimer.monde", "<red>Les claims sont désactivés dans ce monde.");
        }
        if ("protege".equals(refus)) {
            return t("claimer.protege", "<red>Impossible de claimer dans cette zone protégée.");
        }
        if (KSClaim.nombreDeClaims(joueur.getUniqueId()) >= plugin.claimsMax()) {
            return t("claimer.max", "<red>Tu as déjà <max> claims (le maximum).", "max", plugin.claimsMax());
        }
        CPlayer cPlayer = KSClaim.scs().getPlayerMain().getCPlayer(joueur.getUniqueId());
        if (cPlayer != null && !cPlayer.canClaim()) {
            return t("claimer.max-scs", "<red>Tu ne peux plus claimer (limite de SimpleClaimSystem).");
        }
        long prix = plugin.prochainPrix(joueur.getUniqueId());
        if (KSEconomy.solde(joueur.getUniqueId()) < prix) {
            return t("claimer.solde", "<red>Solde insuffisant : ce claim coûte <prix>.", "prix", pts(prix));
        }
        return null;
    }

    private void creer(Player joueur, Chunk chunk) {
        Component refus = refusCreation(joueur, chunk);
        if (refus != null) {
            message(joueur, refus, this::ouvrir);
            return;
        }
        UUID uuid = joueur.getUniqueId();
        long prix = plugin.prochainPrix(uuid);
        if (!KSEconomy.debiter(uuid, prix)) {
            message(joueur, t("claimer.solde", "<red>Solde insuffisant : ce claim coûte <prix>.", "prix", pts(prix)),
                    this::ouvrir);
            return;
        }
        CPlayer cPlayer = KSClaim.scs().getPlayerMain().getCPlayer(uuid);
        int distance = cPlayer == null ? 0 : cPlayer.getClaimDistance();
        boolean[] ok = {false, false};
        plugin.async(() -> {
            ok[0] = KSClaim.scs().getMain().isAreaClaimFree(chunk, distance, joueur.getName()).join();
            if (ok[0]) {
                ok[1] = KSClaim.scs().getMain().createClaim(joueur, chunk).join();
            }
        }, () -> {
            if (ok[1]) {
                plugin.noterPrixPaye(uuid, prix);
                Claim claim = KSClaim.api().getClaimAtChunk(chunk);
                message(joueur, t("claimer.fait", "<green>Chunk claimé (<prix>).", "prix", pts(prix)),
                        p -> {
                            if (claim != null) {
                                menuClaim(p, claim.getId());
                            } else {
                                ouvrir(p);
                            }
                        });
            } else {
                KSEconomy.crediter(uuid, prix);
                message(joueur, ok[0] ? t("claimer.erreur", "<red>Erreur de SimpleClaimSystem : rien n'a été payé.")
                        : t("claimer.trop-pres", "<red>Trop près du claim d'un autre joueur."), this::ouvrir);
            }
        });
    }

    // ------------------------------------------------------------------ mes claims

    private void listeClaims(Player joueur, int page) {
        List<Claim> claims = KSClaim.claimsDe(joueur);
        int pages = Math.max(1, (claims.size() + PAR_PAGE - 1) / PAR_PAGE);
        int p = Math.max(0, Math.min(page, pages - 1));
        List<ActionButton> boutons = new ArrayList<>();
        for (int i = p * PAR_PAGE; i < Math.min(claims.size(), (p + 1) * PAR_PAGE); i++) {
            Claim claim = claims.get(i);
            String groupe = nomGroupe(joueur.getUniqueId(), plugin.groupeDuClaim(joueur.getUniqueId(), claim.getId()));
            boutons.add(gui.button(Component.text(claim.getName()),
                    t("claims.info", "<gray><position><groupe>", "position", position(claim), "groupe",
                            groupe == null ? "" : " - groupe " + groupe),
                    j -> menuClaim(j, claim.getId())));
        }
        if (p > 0) {
            boutons.add(gui.button(t("bouton.precedent", "<yellow>Page précédente"), null, j -> listeClaims(j, p - 1)));
        }
        if (p < pages - 1) {
            boutons.add(gui.button(t("bouton.suivant", "<yellow>Page suivante"), null, j -> listeClaims(j, p + 1)));
        }
        boutons.add(retour(this::ouvrir));
        List<Component> corps = claims.isEmpty() ? List.of(t("claims.aucun", "<gray>Tu n'as encore aucun claim."))
                : List.of(t("claims.page", "<gray>Page <page> / <pages>", "page", p + 1, "pages", pages));
        gui.open(joueur, t("claims.titre", "<aqua><bold>Mes claims"), corps, List.of(), boutons, null, 2);
        lang.saveIfNeeded();
    }

    private String nomGroupe(UUID proprietaire, String groupe) {
        if (groupe == null) {
            return null;
        }
        KSClaim.Groupe g = plugin.joueur(proprietaire).groupes.get(groupe);
        return g == null ? null : g.nom;
    }

    private Claim claim(Player joueur, int id) {
        return KSClaim.claimParId(joueur, id);
    }

    private void menuClaim(Player joueur, int id) {
        Claim claim = claim(joueur, id);
        if (claim == null) {
            ouvrir(joueur);
            return;
        }
        String groupe = nomGroupe(joueur.getUniqueId(), plugin.groupeDuClaim(joueur.getUniqueId(), id));
        List<Component> corps = new ArrayList<>();
        corps.add(t("claim.nom", "<white><nom> <gray>- <position>", "nom", claim.getName(), "position", position(claim)));
        corps.add(t("claim.description", "<gray><description>", "description", claim.getDescription()));
        corps.add(t("claim.membres", "<white>Membres : <nombre> / <max>", "nombre", membres(claim).size(), "max",
                MEMBRES_MAX));
        corps.add(groupe == null ? t("claim.sans-groupe", "<white>Groupe : <gray>aucun")
                : t("claim.groupe", "<white>Groupe : <aqua><groupe>", "groupe", groupe));
        if (claim.getSale()) {
            corps.add(t("claim.vente", "<gold>En vente : <prix>", "prix", pts(claim.getPrice())));
        }
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("bouton.nom-description", "<white>Nom et description"), null,
                p -> nomDescription(p, id)));
        boutons.add(gui.button(t("bouton.membres", "<white>Membres"), null, p -> membresClaim(p, id)));
        boutons.add(gui.button(t("bouton.bannis", "<white>Bannis"), null, p -> bannis(p, id)));
        boutons.add(gui.button(t("bouton.groupe", "<white>Groupe"), null, p -> choisirGroupe(p, id)));
        boutons.add(gui.button(t("bouton.reglages-membres", "<white>Réglages des membres"), null,
                p -> reglages(p, List.of(id), null, "members")));
        boutons.add(gui.button(t("bouton.reglages-visiteurs", "<white>Réglages des visiteurs"), null,
                p -> reglages(p, List.of(id), null, "visitors")));
        boutons.add(claim.getSale()
                ? gui.button(t("bouton.retirer-vente", "<gold>Retirer de la vente"), null, p -> retirerVente(p, id))
                : gui.button(t("bouton.vendre", "<gold>Vendre"), null, p -> vendre(p, id)));
        boutons.add(gui.button(t("bouton.supprimer", "<red>Supprimer"), null, p -> supprimer(p, id)));
        boutons.add(retour(p -> listeClaims(p, 0)));
        gui.open(joueur, t("claim.titre", "<aqua><bold>Claim <nom>", "nom", claim.getName()), corps, List.of(), boutons,
                null, 2);
        lang.saveIfNeeded();
    }

    private void nomDescription(Player joueur, int id) {
        Claim claim = claim(joueur, id);
        if (claim == null) {
            ouvrir(joueur);
            return;
        }
        List<DialogInput> champs = List.of(gui.text("nom", t("champ.nom", "Nom (16 caractères)"), claim.getName(), 16),
                gui.text("description", t("champ.description", "Description (50 caractères)"), claim.getDescription(), 50));
        ActionButton enregistrer = gui.form(t("bouton.enregistrer", "<green>Enregistrer"), null, (p, vue) -> {
            Claim c = claim(p, id);
            String nom = vue.getText("nom") == null ? "" : vue.getText("nom").trim();
            String description = vue.getText("description") == null ? "" : vue.getText("description").trim();
            if (c == null) {
                ouvrir(p);
                return;
            }
            if (nom.isEmpty() || !nom.matches("[A-Za-z0-9_\\-]+")) {
                message(p, t("nom.invalide", "<red>Nom : lettres sans accents, chiffres, - et _ seulement."),
                        j -> nomDescription(j, id));
                return;
            }
            if (!nom.equals(c.getName()) && KSClaim.api().getClaimByName(nom, p.getUniqueId()) != null) {
                message(p, t("nom.pris", "<red>Tu as déjà un claim de ce nom."), j -> nomDescription(j, id));
                return;
            }
            plugin.async(() -> {
                if (!nom.equals(c.getName())) {
                    KSClaim.api().setClaimName(c, nom);
                }
                if (!description.equals(c.getDescription())) {
                    KSClaim.api().setClaimDescription(c, description);
                }
            }, () -> menuClaim(p, id));
        });
        gui.open(joueur, t("nom.titre", "<aqua><bold>Nom et description"), List.of(), champs,
                List.of(enregistrer, retour(p -> menuClaim(p, id))), null, 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ membres et bannis

    /** Membres d'un claim, sans le propriétaire. */
    private static List<UUID> membres(Claim claim) {
        List<UUID> liste = new ArrayList<>(claim.getMembers());
        liste.remove(claim.getUUID());
        return liste;
    }

    private static String nom(UUID joueur) {
        OfflinePlayer hors = Bukkit.getOfflinePlayer(joueur);
        return hors.getName() == null ? joueur.toString().substring(0, 8) : hors.getName();
    }

    /** Joueur déjà venu sur le serveur, ou null. */
    private static OfflinePlayer connu(String pseudo) {
        if (pseudo == null || pseudo.isBlank()) {
            return null;
        }
        Player enLigne = Bukkit.getPlayerExact(pseudo.trim());
        if (enLigne != null) {
            return enLigne;
        }
        OfflinePlayer hors = Bukkit.getOfflinePlayerIfCached(pseudo.trim());
        return hors != null && hors.hasPlayedBefore() ? hors : null;
    }

    private void membresClaim(Player joueur, int id) {
        Claim claim = claim(joueur, id);
        if (claim == null) {
            ouvrir(joueur);
            return;
        }
        membres(joueur, List.of(id), null, membres(claim), p -> menuClaim(p, id));
    }

    /**
     * Membres d'un claim ou d'un groupe (ids : ses claims ; groupe : son identifiant ou null). Ajouter / retirer
     * s'applique à tous les claims ; 5 membres au plus par claim.
     */
    private void membres(Player joueur, List<Integer> ids, String groupe, List<UUID> actuels, Consumer<Player> retour) {
        List<ActionButton> boutons = new ArrayList<>();
        for (UUID membre : actuels) {
            boutons.add(gui.button(t("bouton.retirer-membre", "<red>Retirer <pseudo>", "pseudo", nom(membre)), null,
                    p -> {
                        String pseudo = nom(membre);
                        List<Claim> claims = claims(p, ids);
                        plugin.async(() -> claims.forEach(c -> KSClaim.api().removePlayerFromClaim(c, pseudo)),
                                () -> rouvrirMembres(p, ids, groupe, retour));
                    }));
        }
        boutons.add(gui.button(t("bouton.ajouter-membre", "<green>Ajouter un membre"), null,
                p -> ajouterMembre(p, ids, groupe, retour)));
        boutons.add(retour(retour));
        List<Component> corps = List.of(t("membres.aide", "<gray>Les membres ont les réglages des membres ; <max> "
                + "membres au plus par claim.", "max", MEMBRES_MAX));
        gui.open(joueur, t("membres.titre", "<aqua><bold>Membres"), corps, List.of(), boutons, null, 2);
        lang.saveIfNeeded();
    }

    private List<Claim> claims(Player joueur, List<Integer> ids) {
        List<Claim> liste = new ArrayList<>();
        for (int id : ids) {
            Claim claim = claim(joueur, id);
            if (claim != null) {
                liste.add(claim);
            }
        }
        return liste;
    }

    private void rouvrirMembres(Player joueur, List<Integer> ids, String groupe, Consumer<Player> retour) {
        if (groupe != null) {
            membresGroupe(joueur, groupe);
        } else if (!ids.isEmpty()) {
            membresClaim(joueur, ids.get(0));
        } else {
            retour.accept(joueur);
        }
    }

    private void ajouterMembre(Player joueur, List<Integer> ids, String groupe, Consumer<Player> retour) {
        ActionButton ajouter = gui.form(t("bouton.ajouter", "<green>Ajouter"), null, (p, vue) -> {
            OfflinePlayer cible = connu(vue.getText("pseudo"));
            if (cible == null || cible.getName() == null) {
                message(p, t("membre.inconnu", "<red>Joueur inconnu (il doit être déjà venu sur le serveur)."),
                        j -> ajouterMembre(j, ids, groupe, retour));
                return;
            }
            if (cible.getUniqueId().equals(p.getUniqueId())) {
                message(p, t("membre.toi", "<red>Tu es déjà le propriétaire."), j -> rouvrirMembres(j, ids, groupe, retour));
                return;
            }
            List<Claim> claims = claims(p, ids);
            List<Claim> pleins = new ArrayList<>();
            List<Claim> cibles = new ArrayList<>();
            for (Claim c : claims) {
                if (c.isMember(cible.getUniqueId())) {
                    continue;
                }
                if (membres(c).size() >= MEMBRES_MAX) {
                    pleins.add(c);
                } else {
                    cibles.add(c);
                }
            }
            String pseudo = cible.getName();
            plugin.async(() -> cibles.forEach(c -> KSClaim.api().addPlayerToClaim(c, pseudo)), () -> {
                if (pleins.isEmpty()) {
                    rouvrirMembres(p, ids, groupe, retour);
                } else {
                    message(p, t("membre.pleins", "<yellow><pseudo> ajouté, sauf dans <nombre> claim(s) qui ont déjà "
                            + "<max> membres.", "pseudo", pseudo, "nombre", pleins.size(), "max", MEMBRES_MAX),
                            j -> rouvrirMembres(j, ids, groupe, retour));
                }
            });
        });
        gui.open(joueur, t("membre.titre", "<aqua><bold>Ajouter un membre"), List.of(),
                List.of(gui.text("pseudo", t("champ.pseudo", "Pseudo du joueur"), "", 16)),
                List.of(ajouter, retour(p -> rouvrirMembres(p, ids, groupe, retour))), null, 1);
        lang.saveIfNeeded();
    }

    private void bannis(Player joueur, int id) {
        Claim claim = claim(joueur, id);
        if (claim == null) {
            ouvrir(joueur);
            return;
        }
        List<ActionButton> boutons = new ArrayList<>();
        for (UUID banni : claim.getBans()) {
            boutons.add(gui.button(t("bouton.debannir", "<green>Débannir <pseudo>", "pseudo", nom(banni)), null, p -> {
                Claim c = claim(p, id);
                String pseudo = nom(banni);
                plugin.async(() -> {
                    if (c != null) {
                        KSClaim.api().unbanPlayerFromClaim(c, pseudo);
                    }
                }, () -> bannis(p, id));
            }));
        }
        boutons.add(gui.button(t("bouton.bannir", "<red>Bannir un joueur"), null, p -> bannirOuExpulser(p, id, true)));
        boutons.add(gui.button(t("bouton.expulser", "<yellow>Expulser un joueur"), null,
                p -> bannirOuExpulser(p, id, false)));
        boutons.add(retour(p -> menuClaim(p, id)));
        gui.open(joueur, t("bannis.titre", "<aqua><bold>Bannis"),
                List.of(t("bannis.aide", "<gray>Un joueur banni ne peut plus entrer dans le claim ; expulser le fait "
                        + "seulement sortir.")), List.of(), boutons, null, 2);
        lang.saveIfNeeded();
    }

    private void bannirOuExpulser(Player joueur, int id, boolean bannir) {
        ActionButton valider = gui.form(bannir ? t("bouton.bannir-court", "<red>Bannir")
                : t("bouton.expulser-court", "<yellow>Expulser"), null, (p, vue) -> {
            OfflinePlayer cible = connu(vue.getText("pseudo"));
            Claim c = claim(p, id);
            if (c == null) {
                ouvrir(p);
                return;
            }
            if (cible == null || cible.getName() == null || cible.getUniqueId().equals(p.getUniqueId())) {
                message(p, t("membre.inconnu", "<red>Joueur inconnu (il doit être déjà venu sur le serveur)."),
                        j -> bannis(j, id));
                return;
            }
            String pseudo = cible.getName();
            plugin.async(() -> {
                if (bannir) {
                    KSClaim.api().banPlayerFromClaim(c, pseudo);
                }
            }, () -> {
                if (!bannir) {
                    KSClaim.api().kickPlayerFromClaim(c, pseudo);
                }
                bannis(p, id);
            });
        });
        gui.open(joueur, bannir ? t("bannir.titre", "<aqua><bold>Bannir") : t("expulser.titre", "<aqua><bold>Expulser"),
                List.of(), List.of(gui.text("pseudo", t("champ.pseudo", "Pseudo du joueur"), "", 16)),
                List.of(valider, retour(p -> bannis(p, id))), null, 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ réglages

    /**
     * Réglages d'un rôle (members, visitors) pour des claims (un claim ou tous ceux d'un groupe). Seuls les réglages
     * que la configuration de SCS laisse modifier (status-settings) sont proposés.
     */
    private void reglages(Player joueur, List<Integer> ids, String groupe, String role) {
        List<Claim> claims = claims(joueur, ids);
        Consumer<Player> retour = groupe != null ? p -> menuGroupe(p, groupe)
                : p -> menuClaim(p, ids.isEmpty() ? -1 : ids.get(0));
        if (claims.isEmpty()) {
            message(joueur, t("reglages.vide", "<red>Aucun claim."), retour);
            return;
        }
        Map<String, Boolean> modifiables = KSClaim.scs().getSettings().getStatusSettings();
        Map<String, LinkedHashMap<String, Boolean>> defauts = KSClaim.scs().getSettings().getDefaultValues();
        Set<String> cles = defauts.getOrDefault(role, new LinkedHashMap<>()).keySet();
        List<String> proposes = new ArrayList<>();
        List<DialogInput> champs = new ArrayList<>();
        for (String cle : cles) {
            if (!Boolean.TRUE.equals(modifiables.get(cle))) {
                continue;
            }
            proposes.add(cle);
            champs.add(gui.toggle(cle, Component.text(REGLAGES.getOrDefault(cle, cle)),
                    claims.get(0).getPermission(cle, role)));
        }
        if (proposes.isEmpty()) {
            message(joueur, t("reglages.aucun", "<gray>Aucun réglage modifiable."), retour);
            return;
        }
        ActionButton enregistrer = gui.form(t("bouton.enregistrer", "<green>Enregistrer"), null, (p, vue) -> {
            List<Claim> cibles = claims(p, ids);
            Map<String, Boolean> valeurs = new LinkedHashMap<>();
            for (String cle : proposes) {
                Boolean valeur = vue.getBoolean(cle);
                if (valeur != null) {
                    valeurs.put(cle, valeur);
                }
            }
            plugin.async(() -> {
                for (Claim c : cibles) {
                    valeurs.forEach((cle, valeur) -> {
                        if (c.getPermission(cle, role) != valeur) {
                            KSClaim.api().setClaimPerm(c, cle, valeur, role);
                        }
                    });
                }
            }, () -> retour.accept(p));
        });
        Component titre = "members".equals(role) ? t("reglages.titre-membres", "<aqua><bold>Réglages des membres")
                : t("reglages.titre-visiteurs", "<aqua><bold>Réglages des visiteurs");
        List<Component> corps = List.of(groupe != null
                ? t("reglages.groupe", "<gray>Appliqués à tous les claims du groupe (<nombre>).", "nombre", claims.size())
                : t("reglages.claim", "<gray>Coché : autorisé."));
        gui.open(joueur, titre, corps, champs, List.of(enregistrer, retour(retour)), null, 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ groupes

    private void listeGroupes(Player joueur) {
        Map<String, KSClaim.Groupe> groupes = plugin.joueur(joueur.getUniqueId()).groupes;
        List<ActionButton> boutons = new ArrayList<>();
        groupes.forEach((id, g) -> boutons.add(gui.button(t("bouton.groupe-liste", "<aqua><nom> <gray>(<nombre>)",
                "nom", g.nom, "nombre", plugin.claimsDuGroupe(joueur, id).size()), null, p -> menuGroupe(p, id))));
        boutons.add(gui.button(t("bouton.creer-groupe", "<green>Créer un groupe"), null, this::creerGroupe));
        boutons.add(retour(this::ouvrir));
        gui.open(joueur, t("groupes.titre", "<aqua><bold>Mes groupes"),
                List.of(t("groupes.aide", "<gray>Les membres et les réglages d'un groupe s'appliquent à tous ses claims. "
                        + "Un claim n'est que dans un seul groupe.")), List.of(), boutons, null, 2);
        lang.saveIfNeeded();
    }

    private void creerGroupe(Player joueur) {
        ActionButton creer = gui.form(t("bouton.creer", "<green>Créer"), null, (p, vue) -> {
            String nom = vue.getText("nom") == null ? "" : vue.getText("nom").trim();
            if (nom.isEmpty()) {
                listeGroupes(p);
                return;
            }
            String id = plugin.creerGroupe(p.getUniqueId(), nom);
            if (id == null) {
                message(p, t("groupe.pris", "<red>Tu as déjà un groupe de ce nom."), this::creerGroupe);
            } else {
                menuGroupe(p, id);
            }
        });
        gui.open(joueur, t("groupe.creer-titre", "<aqua><bold>Nouveau groupe"), List.of(),
                List.of(gui.text("nom", t("champ.nom-groupe", "Nom du groupe"), "", 24)),
                List.of(creer, retour(this::listeGroupes)), null, 1);
        lang.saveIfNeeded();
    }

    private void menuGroupe(Player joueur, String groupe) {
        KSClaim.Groupe g = plugin.joueur(joueur.getUniqueId()).groupes.get(groupe);
        if (g == null) {
            listeGroupes(joueur);
            return;
        }
        List<Claim> claims = plugin.claimsDuGroupe(joueur, groupe);
        List<Integer> ids = claims.stream().map(Claim::getId).toList();
        List<Component> corps = new ArrayList<>();
        StringBuilder liste = new StringBuilder();
        for (Claim c : claims) {
            liste.append(liste.isEmpty() ? "" : ", ").append(c.getName());
        }
        corps.add(claims.isEmpty() ? t("groupe.vide", "<gray>Aucun claim dans ce groupe.")
                : t("groupe.claims", "<white>Claims : <gray><liste>", "liste", liste.toString()));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("bouton.ajouter-claim", "<green>Ajouter un claim"), null,
                p -> rangerClaim(p, groupe, true)));
        boutons.add(gui.button(t("bouton.retirer-claim", "<red>Retirer un claim"), null,
                p -> rangerClaim(p, groupe, false)));
        boutons.add(gui.button(t("bouton.membres", "<white>Membres"), null, p -> membresGroupe(p, groupe)));
        boutons.add(gui.button(t("bouton.reglages-membres", "<white>Réglages des membres"), null,
                p -> reglages(p, ids, groupe, "members")));
        boutons.add(gui.button(t("bouton.reglages-visiteurs", "<white>Réglages des visiteurs"), null,
                p -> reglages(p, ids, groupe, "visitors")));
        boutons.add(gui.button(t("bouton.renommer", "<white>Renommer"), null, p -> renommerGroupe(p, groupe)));
        boutons.add(gui.button(t("bouton.supprimer-groupe", "<red>Supprimer le groupe"), null,
                p -> gui.confirm(p, t("groupe.supprimer-titre", "<red><bold>Supprimer le groupe"),
                        t("groupe.supprimer-texte", "<white>Le groupe est supprimé ; ses claims restent à toi."),
                        q -> {
                            plugin.supprimerGroupe(q.getUniqueId(), groupe);
                            listeGroupes(q);
                        }, q -> menuGroupe(q, groupe))));
        boutons.add(retour(this::listeGroupes));
        gui.open(joueur, t("groupe.titre", "<aqua><bold>Groupe <nom>", "nom", g.nom), corps, List.of(), boutons, null, 2);
        lang.saveIfNeeded();
    }

    private void membresGroupe(Player joueur, String groupe) {
        List<Claim> claims = plugin.claimsDuGroupe(joueur, groupe);
        List<UUID> tous = new ArrayList<>();
        for (Claim c : claims) {
            for (UUID m : membres(c)) {
                if (!tous.contains(m)) {
                    tous.add(m);
                }
            }
        }
        membres(joueur, claims.stream().map(Claim::getId).toList(), groupe, tous, p -> menuGroupe(p, groupe));
    }

    private void renommerGroupe(Player joueur, String groupe) {
        KSClaim.Groupe g = plugin.joueur(joueur.getUniqueId()).groupes.get(groupe);
        if (g == null) {
            listeGroupes(joueur);
            return;
        }
        ActionButton valider = gui.form(t("bouton.enregistrer", "<green>Enregistrer"), null, (p, vue) -> {
            String nom = vue.getText("nom") == null ? "" : vue.getText("nom").trim();
            if (!nom.isEmpty()) {
                plugin.renommerGroupe(p.getUniqueId(), groupe, nom);
            }
            menuGroupe(p, groupe);
        });
        gui.open(joueur, t("groupe.renommer-titre", "<aqua><bold>Renommer le groupe"), List.of(),
                List.of(gui.text("nom", t("champ.nom-groupe", "Nom du groupe"), g.nom, 24)),
                List.of(valider, retour(p -> menuGroupe(p, groupe))), null, 1);
        lang.saveIfNeeded();
    }

    /** Ajouter (claims hors de ce groupe) ou retirer (claims du groupe) : un bouton par claim. */
    private void rangerClaim(Player joueur, String groupe, boolean ajouter) {
        List<ActionButton> boutons = new ArrayList<>();
        for (Claim c : KSClaim.claimsDe(joueur)) {
            boolean dedans = groupe.equals(plugin.groupeDuClaim(joueur.getUniqueId(), c.getId()));
            if (dedans != ajouter) {
                int id = c.getId();
                boutons.add(gui.button(Component.text(c.getName()), Component.text(position(c)), p -> {
                    plugin.ranger(p.getUniqueId(), id, ajouter ? groupe : null);
                    menuGroupe(p, groupe);
                }));
            }
        }
        boutons.add(retour(p -> menuGroupe(p, groupe)));
        List<Component> corps = List.of(ajouter
                ? t("ranger.ajouter", "<gray>Claim à ranger dans ce groupe (il quitte son ancien groupe).")
                : t("ranger.retirer", "<gray>Claim à sortir de ce groupe."));
        gui.open(joueur, ajouter ? t("ranger.titre-ajouter", "<aqua><bold>Ajouter un claim")
                : t("ranger.titre-retirer", "<aqua><bold>Retirer un claim"), corps, List.of(), boutons, null, 2);
        lang.saveIfNeeded();
    }

    private void choisirGroupe(Player joueur, int id) {
        Map<String, KSClaim.Groupe> groupes = plugin.joueur(joueur.getUniqueId()).groupes;
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("bouton.aucun-groupe", "<gray>Aucun groupe"), null, p -> {
            plugin.ranger(p.getUniqueId(), id, null);
            menuClaim(p, id);
        }));
        groupes.forEach((g, groupe) -> boutons.add(gui.button(Component.text(groupe.nom), null, p -> {
            plugin.ranger(p.getUniqueId(), id, g);
            menuClaim(p, id);
        })));
        boutons.add(gui.button(t("bouton.creer-groupe", "<green>Créer un groupe"), null, this::creerGroupe));
        boutons.add(retour(p -> menuClaim(p, id)));
        gui.open(joueur, t("choisir.titre", "<aqua><bold>Groupe du claim"), List.of(), List.of(), boutons, null, 2);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ vente et achat

    private void vendre(Player joueur, int id) {
        long max;
        try {
            max = Math.max(1, (long) Double.parseDouble(KSClaim.scs().getSettings().getSetting("max-sell-price")));
        } catch (RuntimeException e) {
            max = 1_000_000_000L;
        }
        long plafond = max;
        ActionButton valider = gui.form(t("bouton.mettre-en-vente", "<gold>Mettre en vente"), null, (p, vue) -> {
            long prix;
            try {
                prix = Long.parseLong((vue.getText("prix") == null ? "" : vue.getText("prix")).replaceAll("\\s", ""));
            } catch (NumberFormatException e) {
                prix = -1;
            }
            Claim c = claim(p, id);
            if (c == null) {
                ouvrir(p);
                return;
            }
            if (prix < 1 || prix > plafond) {
                message(p, t("vente.invalide", "<red>Prix invalide (1 à <max>).", "max", nombre(plafond)),
                        j -> vendre(j, id));
                return;
            }
            long valeur = prix;
            plugin.async(() -> KSClaim.api().addClaimSale(c, valeur), () -> menuClaim(p, id));
        });
        gui.open(joueur, t("vente.titre", "<gold><bold>Vendre ce claim"),
                List.of(t("vente.aide", "<gray>Un joueur qui se tient dans le claim peut l'acheter avec /ksclaim. "
                        + "S'il a déjà <gratuits> claims ou plus, le prix doit être au moins celui de son prochain claim.",
                        "gratuits", plugin.claimsGratuits())),
                List.of(gui.text("prix", t("champ.prix", "Prix (points)"), "", 12)),
                List.of(valider, retour(p -> menuClaim(p, id))), null, 1);
        lang.saveIfNeeded();
    }

    private void retirerVente(Player joueur, int id) {
        Claim c = claim(joueur, id);
        if (c == null) {
            ouvrir(joueur);
            return;
        }
        plugin.async(() -> KSClaim.api().removeClaimSale(c), () -> menuClaim(joueur, id));
    }

    /** Claim à vendre d'un autre joueur, sous les pieds de l'acheteur. */
    private void proposerAchat(Player joueur, Claim claim) {
        UUID uuid = joueur.getUniqueId();
        long prix = claim.getPrice();
        int possedes = KSClaim.nombreDeClaims(uuid);
        if (possedes >= plugin.claimsMax()) {
            message(joueur, t("claimer.max", "<red>Tu as déjà <max> claims (le maximum).", "max", plugin.claimsMax()),
                    this::ouvrir);
            return;
        }
        long minimum = possedes >= plugin.claimsGratuits() ? plugin.prix(possedes + 1) : 0;
        if (prix < minimum) {
            message(joueur, t("achat.minimum", "<red>Achat impossible : ce claim est vendu <prix>, moins que ton "
                    + "prochain claim (<minimum>).", "prix", pts(prix), "minimum", pts(minimum)), this::ouvrir);
            return;
        }
        gui.confirm(joueur, t("achat.titre", "<gold><bold>Acheter ce claim"),
                t("achat.texte", "<white>Claim <nom> de <proprio> pour <yellow><prix></yellow>.", "nom", claim.getName(),
                        "proprio", claim.getOwner(), "prix", pts(prix)),
                p -> acheter(p, claim, prix), this::ouvrir);
        lang.saveIfNeeded();
    }

    private void acheter(Player joueur, Claim claim, long prix) {
        UUID acheteur = joueur.getUniqueId();
        UUID vendeur = claim.getUUID();
        int ancienId = claim.getId();
        if (!claim.getSale() || claim.getPrice() != prix || vendeur.equals(acheteur)) {
            message(joueur, t("achat.change", "<red>Ce claim n'est plus en vente à ce prix."), this::ouvrir);
            return;
        }
        if (!KSEconomy.debiter(acheteur, prix)) {
            message(joueur, t("claimer.solde", "<red>Solde insuffisant : ce claim coûte <prix>.", "prix", pts(prix)),
                    this::ouvrir);
            return;
        }
        boolean[] ok = {false};
        plugin.async(() -> {
            KSClaim.api().removeClaimSale(claim);
            ok[0] = KSClaim.api().setClaimOwner(claim, joueur.getName());
        }, () -> {
            if (ok[0]) {
                KSEconomy.crediter(vendeur, prix);
                plugin.ranger(vendeur, ancienId, null);
                Player enLigne = Bukkit.getPlayer(vendeur);
                if (enLigne != null) {
                    enLigne.sendMessage(t("achat.vendu", "<gold>Ton claim a été acheté par <acheteur> (<prix>).",
                            "acheteur", joueur.getName(), "prix", pts(prix)));
                }
                message(joueur, t("achat.fait", "<green>Claim acheté (<prix>).", "prix", pts(prix)), this::ouvrir);
            } else {
                KSEconomy.crediter(acheteur, prix);
                message(joueur, t("claimer.erreur", "<red>Erreur de SimpleClaimSystem : rien n'a été payé."),
                        this::ouvrir);
            }
        });
    }

    // ------------------------------------------------------------------ suppression

    private void supprimer(Player joueur, int id) {
        Claim claim = claim(joueur, id);
        if (claim == null) {
            ouvrir(joueur);
            return;
        }
        List<Long> payes = plugin.joueur(joueur.getUniqueId()).prixPayes;
        long rembourse = payes.isEmpty() ? 0 : payes.get(payes.size() - 1);
        gui.confirm(joueur, t("supprimer.titre", "<red><bold>Supprimer ce claim"),
                t("supprimer.texte", "<white>Le claim <nom> est supprimé (le terrain n'est plus protégé). "
                        + "Remboursement : <yellow><prix></yellow> (le dernier prix payé).", "nom", claim.getName(),
                        "prix", rembourse == 0 ? "rien" : pts(rembourse)),
                p -> {
                    Claim c = claim(p, id);
                    if (c == null) {
                        ouvrir(p);
                        return;
                    }
                    boolean[] ok = {false};
                    plugin.async(() -> ok[0] = KSClaim.api().unclaim(c), () -> {
                        if (ok[0]) {
                            plugin.ranger(p.getUniqueId(), id, null);
                            long montant = plugin.retirerDernierPrix(p.getUniqueId());
                            KSEconomy.crediter(p.getUniqueId(), montant);
                            message(p, t("supprimer.fait", "<green>Claim supprimé. Remboursé : <prix>.", "prix",
                                    montant == 0 ? "rien" : pts(montant)), this::ouvrir);
                        } else {
                            message(p, t("claimer.erreur-suppr", "<red>Erreur de SimpleClaimSystem."), this::ouvrir);
                        }
                    });
                }, p -> menuClaim(p, id));
        lang.saveIfNeeded();
    }
}
