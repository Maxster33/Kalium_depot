package fr.kalium.votes;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

/**
 * Menu /vote (coffre de 3 rangées) : en haut « Ma série » et « Top du mois », au milieu un objet par site (clic = lien
 * dans le tchat), en bas « Fermer ». Noms courts : aucun texte ne défile.
 */
final class Menu {

    private static final Object MARQUE = "ks_votes";

    private final KSVotes plugin;

    Menu(KSVotes plugin) {
        this.plugin = plugin;
    }

    private Lang lang() {
        return plugin.lang;
    }

    void ouvrir(Player p) {
        Contenant c = Contenant.pour(p, 3, lang().c("menu.titre", "<dark_green>Voter pour Kixster"), MARQUE);
        remplir(c, p);
        c.ouvrir(p);
        plugin.verifierApiMenu(p);
        lang().saveIfNeeded();
    }

    /** Remplit à nouveau le menu s'il est ouvert (après un vote reçu). */
    void rafraichir(Player p) {
        Contenant c = Contenant.ouvert(p, MARQUE);
        if (c != null) {
            c.vider();
            remplir(c, p);
        }
    }

    private void remplir(Contenant c, Player p) {
        KSVotes.Votant v = plugin.votants.get(p.getUniqueId());
        LocalDate jour = plugin.aujourdhui();
        int prochaine = plugin.serieAuProchainVote(v);
        String valeur = KSVotes.format(plugin.valeur(prochaine));
        boolean bedrock = p.getName().startsWith(plugin.prefixe);
        String pseudo = bedrock ? p.getName().substring(plugin.prefixe.length()) : p.getName();

        // Sites avec un lien, centrés sur la rangée du milieu.
        List<KSVotes.Site> visibles = new ArrayList<>();
        for (KSVotes.Site s : plugin.sites.values()) {
            if (!s.lien().isBlank()) {
                visibles.add(s);
            }
        }
        int debut = 9 + (9 - Math.min(9, visibles.size())) / 2;
        for (int i = 0; i < visibles.size() && i < 9; i++) {
            KSVotes.Site s = visibles.get(i);
            boolean fait = v != null && jour.equals(v.sites.get(s.id()));
            List<Component> l = new ArrayList<>();
            if (fait) {
                l.add(lang().c("menu.site.fait", "<gray>Déjà voté aujourd'hui"));
                l.add(lang().c("menu.site.prochain", "<gray>Prochain vote dans <yellow><temps>", "temps",
                        duree(plugin.avantMinuit())));
            } else {
                l.add(lang().c("menu.site.disponible", "<green>Disponible"));
                l.add(lang().c("menu.site.coffre", "<gray>Coffre de valeur <gold><valeur>", "valeur", valeur));
            }
            l.add(lang().c("menu.site.pseudo", "<gray>Pseudo à taper : <white><pseudo>", "pseudo", pseudo));
            l.add(lang().c("menu.site.clic", "<yellow>Clic : lien du site"));
            c.poser(debut + i, Contenant.objet(fait ? Material.GRAY_DYE : Material.EMERALD,
                    lang().c("menu.site.nom", "<green><site>", "site", s.nom()), l), j -> lien(j, s));
        }
        if (visibles.isEmpty()) {
            c.poser(13, Contenant.objet(Material.BARRIER, lang().c("menu.aucun", "<red>Bientôt disponible"),
                    Contenant.lignes(lang().c("menu.aucun-texte", "<gray>Les sites de vote arrivent très bientôt."))),
                    null);
        }

        // Ma série.
        int serie = plugin.serieActuelle(v);
        List<Component> s = new ArrayList<>();
        s.add(lang().c("menu.serie.jours", "<gray>Série : <yellow><jours> jour(s)", "jours", serie));
        s.add(lang().c("menu.serie.coffre", "<gray>Prochain coffre : <gold><valeur>", "valeur", valeur));
        Map.Entry<Integer, Double> palier = plugin.prochainPalier(serie);
        if (palier == null) {
            s.add(lang().c("menu.serie.max", "<gray>Palier maximal atteint"));
        } else {
            s.add(lang().c("menu.serie.palier", "<gray>Coffres de <gold><valeur></gold> dès <jours> jours",
                    "valeur", KSVotes.format(palier.getValue()), "jours", palier.getKey()));
        }
        long[] duMois = plugin.mois.getOrDefault(plugin.moisCle(YearMonth.from(jour)), Map.of()).get(p.getUniqueId());
        s.add(lang().c("menu.serie.mois", "<gray>Votes ce mois : <yellow><n>", "n", duMois == null ? 0 : duMois[0]));
        if (serie > 0 && (v == null || !jour.equals(v.dernierJour))) {
            s.addAll(Contenant.lignes(lang().c("menu.serie.garder", "<red>Vote aujourd'hui pour garder ta série.")));
        }
        s.addAll(Contenant.lignes(lang().c("menu.serie.regle",
                "<dark_gray>Un jour sans vote remet la série à zéro.")));
        if (bedrock) {
            s.addAll(Contenant.lignes(lang().c("menu.serie.bedrock",
                    "<gray>Sur les sites, tape ton pseudo sans le point.")));
        }
        c.poser(3, Contenant.objet(Material.CLOCK, lang().c("menu.serie.nom", "<gold>Ma série"), s), null);

        // Top du mois.
        String ym = plugin.moisCle(YearMonth.from(jour));
        List<Component> t = new ArrayList<>();
        List<Map.Entry<UUID, long[]>> top = plugin.classement(ym, 10);
        if (top.isEmpty()) {
            t.add(lang().c("menu.top.vide", "<gray>Aucun vote ce mois-ci"));
        }
        for (int i = 0; i < top.size(); i++) {
            t.add(lang().c("menu.top.ligne", "<yellow><place>. <white><joueur> <gray>: <votes>", "place", i + 1,
                    "joueur", plugin.nom(top.get(i).getKey()), "votes", top.get(i).getValue()[0]));
        }
        if (!plugin.topCoffres.isEmpty()) {
            List<String> valeurs = new ArrayList<>();
            for (double d : plugin.topCoffres) {
                valeurs.add(KSVotes.format(d));
            }
            t.addAll(Contenant.lignes(lang().c("menu.top.coffres",
                    "<gray>Fin du mois : coffres de <valeurs> aux <n> premiers.", "valeurs", String.join(", ", valeurs),
                    "n", valeurs.size())));
        }
        c.poser(5, Contenant.objet(Material.GOLD_INGOT, lang().c("menu.top.nom", "<gold>Top du mois"), t), null);

        c.barre();
        c.poser(c.bas(4), Contenant.objet(Material.BARRIER, lang().c("menu.fermer", "<red>Fermer"), List.of()),
                Contenant::fermer);
    }

    /** Lien du site dans le tchat : cliquable en Java, lisible en Bedrock. */
    private void lien(Player p, KSVotes.Site s) {
        Contenant.fermer(p);
        p.sendMessage(lang().c("lien", "<gray>Vote sur <green><site></green> : <aqua><u><lien></u>", "site", s.nom(),
                "lien", s.lien()).clickEvent(ClickEvent.openUrl(s.lien())));
        lang().saveIfNeeded();
    }

    private String duree(Duration d) {
        long minutes = Math.max(1, (d.toSeconds() + 59) / 60);
        return minutes >= 60 ? (minutes / 60) + " h " + String.format("%02d", minutes % 60) : minutes + " min";
    }
}
