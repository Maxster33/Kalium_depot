package fr.kalium.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.3.0 (LeKiwi06, 09/10/2026 : fiche d'un joueur : économie, maisons et téléportation, jetons et récompenses) : ce que
 * les autres plugins savent d'un joueur, en ligne ou hors ligne, en lecture seule. Tous facultatifs : un plugin absent
 * ou trop ancien (KS_Economy 1.4.1, KS_Teleport 1.0.1, KS_CoffreMort 1.0.2, KS_RewardsGUI 1.4.1 nécessaires) donne null
 * ou moins de détail, et la fiche le dit.
 * Chaque plugin est lu dans sa propre classe, chargée seulement s'il est là.
 */
final class Infos {

    /** Économie d'un joueur : lignes déjà écrites (solde, magasin, ventes), puis dernières ventes et derniers achats. */
    record Economie(List<String> resume, List<String> ventes, List<String> achats) {
    }

    /** Un coffre de mort actif : où, et ce qu'il en reste à dire. */
    record CoffreMort(Location position, String texte) {
    }

    private Infos() {
    }

    private static boolean present(String plugin) {
        return Bukkit.getPluginManager().isPluginEnabled(plugin);
    }

    private static void illisible(String plugin, Throwable e) {
        Bukkit.getLogger().warning("[KS_AntiCheat] " + plugin + " absent ou trop ancien pour la fiche d'un joueur : " + e);
    }

    // ------------------------------------------------------------------ textes communs

    /** « 1 point », « 1 234 points » (comme KS_Economy). */
    static String points(long valeur) {
        String chiffres = String.valueOf(Math.abs(valeur));
        StringBuilder sortie = new StringBuilder(valeur < 0 ? "-" : "");
        for (int i = 0; i < chiffres.length(); i++) {
            if (i > 0 && (chiffres.length() - i) % 3 == 0) {
                sortie.append(' ');
            }
            sortie.append(chiffres.charAt(i));
        }
        return sortie + (Math.abs(valeur) > 1 ? " points" : " point");
    }

    /** « Overworld, x 120 y 64 z -40 ». */
    static String position(Location lieu) {
        if (lieu == null || !lieu.isWorldLoaded()) {
            return "?";
        }
        String monde = switch (lieu.getWorld().getEnvironment()) {
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> "Overworld";
        };
        return monde + ", x " + lieu.getBlockX() + " y " + lieu.getBlockY() + " z " + lieu.getBlockZ();
    }

    // ------------------------------------------------------------------ économie (KS_Economy)

    static boolean economieDisponible() {
        return present("KS_Economy");
    }

    /** Null si KS_Economy est absent ou trop ancien. */
    static Economie economie(UUID joueur, int lignes) {
        if (!present("KS_Economy")) {
            return null;
        }
        try {
            return Eco.lire(joueur, lignes);
        } catch (LinkageError | RuntimeException e) {
            illisible("KS_Economy", e);
            return null;
        }
    }

    private static final class Eco {
        static Economie lire(UUID joueur, int lignes) {
            fr.kalium.economy.api.ResumeJoueur r = fr.kalium.economy.KSEconomy.resume(joueur, lignes);
            List<String> resume = new ArrayList<>();
            resume.add("Solde : " + points(r.solde()) + (r.soldeMasque() ? " (masqué aux autres joueurs)" : ""));
            if (r.magasin() == null) {
                resume.add("Magasin : aucun" + (r.boutiques() > 0 ? " ; " + r.boutiques() + " boutique(s)" : ""));
            } else {
                resume.add("Magasin : « " + r.magasin() + " » (" + position(r.positionMagasin()) + "), " + r.boutiques()
                        + " boutique(s), " + points(r.pointsEnAttente()) + " en attente dans les boutiques");
            }
            resume.add("Ventes de ses boutiques : " + r.ventes() + " (" + r.lots7j() + " lot(s) ces 7 derniers jours), "
                    + points(r.pointsGagnes()) + " gagnés");
            List<String> ventes = new ArrayList<>();
            for (fr.kalium.economy.api.ResumeJoueur.Vente v : r.dernieresVentes()) {
                ventes.add(vente(v) + " à " + v.acheteur() + prix(v));
            }
            List<String> achats = new ArrayList<>();
            for (fr.kalium.economy.api.ResumeJoueur.Vente v : r.derniersAchats()) {
                achats.add(vente(v) + " chez " + v.vendeur() + prix(v));
            }
            return new Economie(resume, ventes, achats);
        }

        private static String vente(fr.kalium.economy.api.ResumeJoueur.Vente v) {
            return Alertes.DATE.format(Instant.ofEpochMilli(v.date())) + " - " + v.lots() + " lot(s) de "
                    + (v.boutique() == null ? "boutique supprimée" : "« " + v.boutique() + " »");
        }

        private static String prix(fr.kalium.economy.api.ResumeJoueur.Vente v) {
            return " (" + (v.points() > 0 || v.objets() == 0 ? points(v.points()) : v.objets() + " objet(s)") + ")";
        }
    }

    // ------------------------------------------------------------------ maisons (lit, KS_Teleport)

    /**
     * Destinations du joueur, dans l'ordre : « Lit » (point de réapparition enregistré, sans vérifier qu'il existe
     * encore), puis celles de KS_Teleport (maison du spawn, emplacements) s'il est là.
     */
    static Map<String, Location> maisons(OfflinePlayer joueur) {
        Map<String, Location> r = new LinkedHashMap<>();
        try {
            Location lit = joueur.getRespawnLocation(false);
            if (lit != null && lit.isWorldLoaded()) {
                r.put("Lit", lit);
            }
        } catch (RuntimeException e) {
            illisible("Lit de " + joueur.getName(), e);
        }
        if (present("KS_Teleport")) {
            try {
                r.putAll(Tp.lire(joueur.getUniqueId()));
            } catch (LinkageError | RuntimeException e) {
                illisible("KS_Teleport", e);
            }
        }
        return r;
    }

    private static final class Tp {
        static Map<String, Location> lire(UUID joueur) {
            return fr.kalium.teleport.KSTeleport.destinations(joueur);
        }
    }

    // ------------------------------------------------------------------ jetons et badges (KS_Jetons)

    /** Au moins un des plugins de l'écran « Jetons et récompenses » est là. */
    static boolean jetonsDisponibles() {
        return present("KS_Jetons") || present("KS_RewardsGUI") || present("KS_CoffreMort");
    }

    /** Jetons de l'inventaire spécial puis badges portés, une ligne chacun ; null si KS_Jetons est absent. */
    static List<String> jetons(UUID joueur) {
        if (!present("KS_Jetons")) {
            return null;
        }
        try {
            return Jetons.lire(joueur);
        } catch (LinkageError | RuntimeException e) {
            illisible("KS_Jetons", e);
            return null;
        }
    }

    private static final class Jetons {
        static List<String> lire(UUID joueur) {
            List<String> r = new ArrayList<>();
            for (fr.kalium.jetons.KSJetons.Type type : fr.kalium.jetons.KSJetons.Type.values()) {
                r.add(type.nom() + " : " + fr.kalium.jetons.KSJetons.nombre(joueur, type));
            }
            for (fr.kalium.jetons.KSJetons.Badge badge : fr.kalium.jetons.KSJetons.Badge.values()) {
                int niveau = fr.kalium.jetons.KSJetons.niveauBadge(joueur, badge);
                r.add(badge.nom() + " : " + (niveau == 0 ? "aucun" : "niveau " + niveau));
            }
            return r;
        }
    }

    // ------------------------------------------------------------------ récompenses (KS_RewardsGUI)

    /**
     * Récompenses en attente dans /rewards, de la plus ancienne à la plus récente (liste vide : aucune) ; null si
     * KS_RewardsGUI est absent. Avant KS_RewardsGUI 1.4.1 : seulement la date de la plus ancienne.
     */
    static List<String> recompenses(UUID joueur) {
        if (!present("KS_RewardsGUI")) {
            return null;
        }
        try {
            return Rewards.lire(joueur);
        } catch (LinkageError e) {
            try {
                return Rewards.lireAncien(joueur);
            } catch (LinkageError | RuntimeException e2) {
                illisible("KS_RewardsGUI", e2);
                return null;
            }
        } catch (RuntimeException e) {
            illisible("KS_RewardsGUI", e);
            return null;
        }
    }

    private static final class Rewards {
        static List<String> lire(UUID joueur) {
            List<String> r = new ArrayList<>();
            if (!(Bukkit.getPluginManager().getPlugin("KS_RewardsGUI") instanceof fr.kalium.rewardsgui.KSRewardsGUI p)) {
                return r;
            }
            for (fr.kalium.rewardsgui.KSRewardsGUI.EnAttente a : p.recompensesEnAttente(joueur)) {
                r.add(Alertes.DATE.format(Instant.ofEpochMilli(a.date())) + " - " + a.origine() + " : " + a.raison() + " ("
                        + a.elements() + " élément(s)" + (a.locale() ? ", ses propres objets" : "") + ")");
            }
            return r;
        }

        static List<String> lireAncien(UUID joueur) {
            List<String> r = new ArrayList<>();
            if (Bukkit.getPluginManager().getPlugin("KS_RewardsGUI") instanceof fr.kalium.rewardsgui.KSRewardsGUI p) {
                Long date = p.plusAnciennesEnAttente().get(joueur);
                if (date != null) {
                    r.add("Au moins une, la plus ancienne depuis le " + Alertes.DATE.format(Instant.ofEpochMilli(date))
                            + " (détail : KS_RewardsGUI 1.4.1 nécessaire)");
                }
            }
            return r;
        }
    }

    // ------------------------------------------------------------------ coffres de mort (KS_CoffreMort)

    /** Coffres de mort actifs, du plus ancien au plus récent ; null si KS_CoffreMort est absent ou trop ancien. */
    static List<CoffreMort> coffresDeMort(UUID joueur) {
        if (!present("KS_CoffreMort")) {
            return null;
        }
        try {
            return Mort.lire(joueur);
        } catch (LinkageError | RuntimeException e) {
            illisible("KS_CoffreMort", e);
            return null;
        }
    }

    private static final class Mort {
        static List<CoffreMort> lire(UUID joueur) {
            List<CoffreMort> r = new ArrayList<>();
            for (fr.kalium.coffremort.KSCoffreMort.CoffreActif c : fr.kalium.coffremort.KSCoffreMort.coffresActifs(joueur)) {
                long minutes = Math.max(0, (c.expiration() - System.currentTimeMillis() + 59_999) / 60_000);
                r.add(new CoffreMort(c.position(), position(c.position()) + " - " + c.piles() + " pile(s) d'objets, encore "
                        + minutes + " min"));
            }
            return r;
        }
    }
}
