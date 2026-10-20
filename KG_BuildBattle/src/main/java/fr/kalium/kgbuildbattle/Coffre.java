package fr.kalium.kgbuildbattle;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.kalium.menu.api.Contenant;
import net.kyori.adventure.text.Component;

/**
 * 0.6.0 - réglages d'une partie privée en coffre (demande de LeKiwi06, 20/10/2026 : menus de Kal-Games en coffre, Échap
 * ramène à la page précédente). Tempo et taille des équipes : un clic passe au choix suivant (clic droit : précédent) ;
 * thèmes écrits : oui / non. Les derniers réglages de chaque joueur sont retenus (plugins/KG_BuildBattle/reglages.yml)
 * pour la création suivante et « Recréer ma dernière partie ».
 */
final class Coffre {

    private enum Ecran { REGLAGES }

    /** Réglages en cours d'une partie privée. */
    static final class Reglages {
        Tempo tempo = Tempo.NORMAL;
        int taille = 1;
        boolean ecrits;
        Consumer<Player> retour;
    }

    private static final String[] TAILLES = {"Solo", "Duo", "Trio", "Squad"};

    private final KGBuildBattle plugin;
    private final Parties parties;
    private final Menus menus;
    private final Map<UUID, Reglages> brouillons = new HashMap<>();
    private final File fichier;
    private final YamlConfiguration retenus;

    Coffre(KGBuildBattle plugin, Parties parties, Menus menus) {
        this.plugin = plugin;
        this.parties = parties;
        this.menus = menus;
        this.fichier = new File(plugin.getDataFolder(), "reglages.yml");
        this.retenus = YamlConfiguration.loadConfiguration(fichier);
    }

    private Component t(String cle, String def, Object... paires) {
        return menus.t(cle, def, paires);
    }

    private fr.kalium.kgmenu.KgMenu kgMenu() {
        return (fr.kalium.kgmenu.KgMenu) plugin.getServer().getPluginManager().getPlugin("KG_Menu");
    }

    /** Le joueur veut-il les menus en coffres (réglage « Paramètres › Affichage » de KG_Menu 1.2.0) ? */
    boolean enCoffre(Player joueur) {
        try {
            return kgMenu().coffres(joueur);
        } catch (LinkageError | RuntimeException e) {
            return false;
        }
    }

    Reglages depart(Player joueur) {
        Reglages r = new Reglages();
        String base = joueur.getUniqueId() + ".";
        r.tempo = Tempo.lire(retenus.getString(base + "tempo", Tempo.NORMAL.name()));
        r.taille = Math.max(1, Math.min(4, retenus.getInt(base + "taille", 1)));
        r.ecrits = retenus.getBoolean(base + "ecrits", false);
        return r;
    }

    long derniere(Player joueur) {
        return retenus.getLong(joueur.getUniqueId() + ".quand", 0L);
    }

    /** Crée la partie privée avec ces réglages et envoie l'hôte sur Kanvas (réglages retenus). */
    void lancer(Player joueur, Reglages r) {
        String base = joueur.getUniqueId() + ".";
        retenus.set(base + "tempo", r.tempo.name());
        retenus.set(base + "taille", r.taille);
        retenus.set(base + "ecrits", r.ecrits);
        retenus.set(base + "quand", System.currentTimeMillis());
        try {
            plugin.getDataFolder().mkdirs();
            retenus.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().warning("reglages.yml non enregistré : " + e.getMessage());
        }
        Partie partie = parties.creer(joueur, r.tempo, r.taille, r.ecrits);
        joueur.sendMessage(t("menu.cree", "<green>Partie privée créée : code <white><code></white>.", "code", partie.code));
        parties.envoyerPrive(joueur, partie);
    }

    /** « Créer une partie » &gt; Build Battle : réglages en coffre. */
    void creer(Player joueur, Consumer<Player> retour) {
        Reglages r = depart(joueur);
        r.retour = retour;
        brouillons.put(joueur.getUniqueId(), r);
        Contenant.apres(() -> reglages(joueur));
    }

    private boolean droit(Player joueur) {
        try {
            return kgMenu().clic(joueur).isRightClick();
        } catch (LinkageError | RuntimeException e) {
            return false;
        }
    }

    private void reglages(Player joueur) {
        Reglages r = brouillons.get(joueur.getUniqueId());
        if (r == null) {
            return;
        }
        Contenant menu = Contenant.pour(joueur, 4, t("coffre.titre", "<dark_gray>Nouvelle partie Build Battle"), Ecran.REGLAGES);
        menu.poser(4, Contenant.objet(Material.BRICKS, t("menu.bouton", "<gold><bold>Build Battle"),
                Contenant.lignes(t("coffre.info", "<gray>Construis sur un thème avec ton équipe, puis vote. Jusqu'à <max> "
                        + "équipes ; tu lanceras la partie depuis la salle d'attente sur Kanvas.", "max", parties.equipesMax()))), null);
        List<Component> choix = List.of(t("coffre.choix-clic", "<yellow>Clic gauche : suivant, clic droit : précédent"));
        menu.poser(20, Contenant.objet(new ItemStack(Material.CLOCK, Math.max(1, r.tempo.minutes)),
                t("coffre.tempo", "<white>Tempo : <aqua><tempo>", "tempo", r.tempo.libelle()), choix), p -> {
            Tempo[] tempos = Tempo.values();
            r.tempo = tempos[Math.floorMod(r.tempo.ordinal() + (droit(p) ? -1 : 1), tempos.length)];
            reglages(p);
        });
        menu.poser(22, Contenant.objet(new ItemStack(Material.PLAYER_HEAD, r.taille),
                t("coffre.taille", "<white>Équipes : <aqua><taille>", "taille", TAILLES[r.taille - 1]), choix), p -> {
            r.taille = Math.floorMod(r.taille - 1 + (droit(p) ? -1 : 1), 4) + 1;
            reglages(p);
        });
        List<Component> lignesEcrits = new java.util.ArrayList<>(Contenant.lignes(
                t("coffre.ecrits-info", "<gray>Chaque joueur écrit un thème, puis tout le monde vote parmi les propositions.")));
        lignesEcrits.add(t("coffre.bascule", "<yellow>Clic : changer"));
        menu.poser(24, Contenant.objet(new ItemStack(r.ecrits ? Material.LIME_DYE : Material.GRAY_DYE),
                r.ecrits ? t("coffre.ecrits-oui", "<white>Thèmes écrits : <green>oui") : t("coffre.ecrits-non", "<white>Thèmes écrits : <red>non"),
                lignesEcrits), p -> {
            r.ecrits = !r.ecrits;
            reglages(p);
        });
        menu.barre();
        menu.poser(menu.bas(4), Contenant.objet(Material.LIME_CONCRETE, t("coffre.creer", "<green><bold>Créer la partie"),
                Contenant.lignes(t("coffre.creer-info", "<gray>Tu en es l'hôte ; tu pars sur Kanvas."))), p -> {
            Reglages actuel = brouillons.remove(p.getUniqueId());
            if (actuel == null) {
                return;
            }
            Contenant.fermer(p);
            Contenant.apres(() -> lancer(p, actuel));
        });
        Consumer<Player> retour = p -> {
            brouillons.remove(p.getUniqueId());
            if (r.retour != null) {
                r.retour.accept(p);
            } else {
                Contenant.fermer(p);
            }
        };
        menu.poser(menu.bas(5), Contenant.objet(Material.OAK_DOOR, t("coffre.retour", "<white>Retour"), List.of()), retour);
        menu.ouvrir(joueur);
        try {
            kgMenu().echap(joueur, menu, retour); // Échap = « Retour »
        } catch (LinkageError | RuntimeException e) {
            // KG_Menu plus ancien que la 1.2.0 : Échap ferme le coffre.
        }
    }
}
