package fr.kalium.contrats;

import fr.kalium.contrats.Contrat.Bilan;
import fr.kalium.contrats.Contrat.Demande;
import fr.kalium.contrats.Contrat.Engagement;
import fr.kalium.contrats.Contrat.Etat;
import fr.kalium.contrats.Contrat.Fin;
import fr.kalium.contrats.Contrat.Termes;
import fr.kalium.contrats.Contrat.TypeDemande;
import fr.kalium.contrats.Contrat.Version;
import fr.kalium.economy.KSEconomy;
import fr.kalium.economy.api.ContratsEchange;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les contrats et leurs règles (LeKiwi06, 27/10/2026) :
 * - une demande (nouveau contrat, modification, validation anticipée, annulation) n'est effective que quand tous les
 *   autres participants l'ont acceptée ; un seul refus la fait tomber pour tout le monde ;
 * - garantie : un montant par partie, divisé entre ses joueurs, retiré à chacun quand il accepte, rendu à la fin ;
 * - fin d'une période (récurrent) ou expiration : validée d'office si tout est livré par /echange et sans service,
 *   sinon chacun valide ou conteste (7 jours sans réponse = validé) ; contester ou signaler : litige, tranché par un
 *   admin qui répartit librement les garanties.
 * Enregistré dans plugins/KS_Contrats/contrats.yml ; implémente aussi les livraisons par /echange (KS_Economy).
 */
final class Registre implements ContratsEchange {

    /** Joueurs au plus par partie, contrats ouverts (actifs et propositions) au plus par joueur. */
    static final int PAR_PARTIE = 10, OUVERTS_MAX = 30;

    /** Réglages personnels d'un joueur pour un contrat (LeKiwi06 : « chaque joueur peut avoir son propre nom et logo »). */
    static final class Perso {
        String nom;
        ItemStack logo;
        boolean favori;
    }

    private final KSContrats plugin;
    private final Lang lang;
    private final File fichier;
    final Map<String, Contrat> contrats = new LinkedHashMap<>();
    /** Compte -> contrat -> réglages personnels. */
    private final Map<UUID, Map<String, Perso>> persos = new HashMap<>();
    /** Compte -> messages à lui montrer à sa prochaine connexion (MiniMessage). */
    private final Map<UUID, List<String>> messages = new HashMap<>();
    private int prochain;
    private boolean aSauver;

    Registre(KSContrats plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.fichier = new File(plugin.getDataFolder(), "contrats.yml");
        charger();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    // ------------------------------------------------------------------ fichier

    private void charger() {
        if (!fichier.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(fichier);
        prochain = y.getInt("prochain");
        ConfigurationSection cs = y.getConfigurationSection("contrats");
        if (cs != null) {
            for (String id : cs.getKeys(false)) {
                try {
                    contrats.put(id, Contrat.lire(id, cs.getConfigurationSection(id)));
                } catch (RuntimeException e) {
                    plugin.getLogger().warning("Contrat " + id + " illisible : " + e);
                }
            }
        }
        ConfigurationSection ps = y.getConfigurationSection("persos");
        if (ps != null) {
            for (String compte : ps.getKeys(false)) {
                ConfigurationSection pc = ps.getConfigurationSection(compte);
                Map<String, Perso> m = new HashMap<>();
                for (String id : pc.getKeys(false)) {
                    Perso p = new Perso();
                    p.nom = pc.getString(id + ".nom");
                    p.logo = Contrat.texteVersObjet(pc.getString(id + ".logo"));
                    p.favori = pc.getBoolean(id + ".favori");
                    m.put(id, p);
                }
                persos.put(UUID.fromString(compte), m);
            }
        }
        ConfigurationSection ms = y.getConfigurationSection("messages");
        if (ms != null) {
            ms.getKeys(false).forEach(k -> messages.put(UUID.fromString(k), new ArrayList<>(ms.getStringList(k))));
        }
    }

    void sauverSiBesoin() {
        if (aSauver) {
            sauver();
        }
    }

    void sauver() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("prochain", prochain);
        ConfigurationSection cs = y.createSection("contrats");
        contrats.forEach((id, c) -> c.ecrire(cs.createSection(id)));
        ConfigurationSection ps = y.createSection("persos");
        persos.forEach((compte, m) -> m.forEach((id, p) -> {
            String base = compte + "." + id;
            ps.set(base + ".nom", p.nom);
            ps.set(base + ".logo", p.logo == null ? null : Contrat.objetVersTexte(p.logo));
            ps.set(base + ".favori", p.favori);
        }));
        ConfigurationSection ms = y.createSection("messages");
        messages.forEach((compte, liste) -> {
            if (!liste.isEmpty()) {
                ms.set(compte.toString(), liste);
            }
        });
        try {
            plugin.getDataFolder().mkdirs();
            y.save(fichier);
            aSauver = false;
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer contrats.yml : " + e.getMessage());
        }
    }

    void modifie() {
        aSauver = true;
    }

    // ------------------------------------------------------------------ comptes et messages

    static UUID compte(Player joueur) {
        return CompteTest.de(joueur.getUniqueId());
    }

    /** Le joueur en ligne qui joue ce compte en ce moment (null : hors ligne, ou dans l'autre monde). */
    static Player enLigne(UUID compte) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (compte(p).equals(compte)) {
                return p;
            }
        }
        return null;
    }

    /** Message tout de suite s'il est là, sinon à sa prochaine connexion. */
    void prevenir(UUID compte, Component texte) {
        Player p = enLigne(compte);
        if (p != null) {
            p.sendMessage(texte);
        } else {
            messages.computeIfAbsent(compte, u -> new ArrayList<>()).add(MiniMessage.miniMessage().serialize(texte));
            modifie();
        }
    }

    void prevenirTous(Contrat c, Component texte, UUID sauf) {
        for (UUID u : c.participants()) {
            if (!u.equals(sauf)) {
                prevenir(u, texte);
            }
        }
    }

    void prevenirAdmins(Component texte) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("kscontrats.admin")) {
                p.sendMessage(texte);
            }
        }
    }

    /** Messages en attente du compte que ce joueur joue maintenant. */
    void distribuer(Player joueur) {
        List<String> liste = messages.remove(compte(joueur));
        if (liste != null) {
            liste.forEach(m -> joueur.sendMessage(MiniMessage.miniMessage().deserialize(m)));
            modifie();
        }
    }

    /** Ce contrat attend-il une réponse de ce compte (demande à accepter, période à valider) ? */
    boolean attend(Contrat c, UUID compte) {
        if (!c.participe(compte)) {
            return false;
        }
        if (c.demande != null && (c.etat == Etat.PROPOSITION || c.etat == Etat.ACTIF)
                && !c.demande.acceptes.contains(compte)) {
            return true;
        }
        return (c.etat == Etat.ACTIF || c.etat == Etat.ECHU) && c.bilans.stream().anyMatch(b -> !b.valides.contains(compte));
    }

    int attention(UUID compte) {
        return (int) contrats.values().stream().filter(c -> attend(c, compte)).count();
    }

    int litiges() {
        return (int) contrats.values().stream().filter(c -> c.etat == Etat.LITIGE).count();
    }

    // ------------------------------------------------------------------ réglages personnels

    Perso perso(UUID compte, Contrat c) {
        return persos.computeIfAbsent(compte, u -> new HashMap<>()).computeIfAbsent(c.id, id -> new Perso());
    }

    Perso persoSiExiste(UUID compte, Contrat c) {
        Map<String, Perso> m = persos.get(compte);
        return m == null ? null : m.get(c.id);
    }

    /** Nom vu par ce compte : le sien, sinon « Contrat avec ... ». */
    String nomPour(UUID compte, Contrat c) {
        Perso p = persoSiExiste(compte, c);
        if (p != null && p.nom != null) {
            return p.nom;
        }
        List<UUID> autres = c.dansA(compte) ? c.partieB : c.partieA;
        if (!c.participe(compte)) {
            return c.nomsDe(c.partieA) + " / " + c.nomsDe(c.partieB);
        }
        String premier = autres.isEmpty() ? "?" : c.nom(autres.get(0));
        return autres.size() > 1 ? "Contrat avec " + premier + " et " + (autres.size() - 1) + " autre"
                + (autres.size() > 2 ? "s" : "") : "Contrat avec " + premier;
    }

    boolean favori(UUID compte, Contrat c) {
        Perso p = persoSiExiste(compte, c);
        return p != null && p.favori;
    }

    /** Contrats de ce compte dans ces états : favoris d'abord, puis du plus récent au plus ancien. */
    List<Contrat> de(UUID compte, Etat... etats) {
        List<Etat> voulus = List.of(etats);
        List<Contrat> liste = new ArrayList<>();
        for (Contrat c : contrats.values()) {
            if (c.participe(compte) && voulus.contains(c.etat)) {
                liste.add(c);
            }
        }
        liste.sort(Comparator.comparing((Contrat c) -> !favori(compte, c)).thenComparing(c -> -c.cree));
        return liste;
    }

    // ------------------------------------------------------------------ garanties

    private boolean prendre(Contrat c, UUID compte, long points) {
        if (points <= 0) {
            return true;
        }
        if (!KSEconomy.debiterCompte(compte, points)) {
            return false;
        }
        c.sequestre.merge(compte, points, Long::sum);
        modifie();
        return true;
    }

    private void rendre(Contrat c, UUID compte, long points) {
        long tenu = c.sequestre.getOrDefault(compte, 0L);
        long r = Math.min(points, tenu);
        if (r <= 0) {
            return;
        }
        KSEconomy.crediterCompte(compte, r);
        if (tenu - r == 0) {
            c.sequestre.remove(compte);
        } else {
            c.sequestre.put(compte, tenu - r);
        }
        modifie();
    }

    private void toutRendre(Contrat c) {
        for (UUID u : new ArrayList<>(c.sequestre.keySet())) {
            rendre(c, u, c.sequestre.get(u));
        }
    }

    /** Une demande tombe : les suppléments de garantie d'une modification sont rendus. */
    private void rendreSupplements(Contrat c) {
        if (c.demande != null) {
            c.demande.supplements.forEach((u, p) -> rendre(c, u, p));
        }
    }

    // ------------------------------------------------------------------ demandes

    private int ouverts(UUID compte) {
        return (int) contrats.values().stream().filter(c -> c.participe(compte) && c.ouvert()).count();
    }

    /** Vérifications communes d'une proposition ; null si tout va bien. */
    private Component verifier(Termes t) {
        if (t.a.vide()) {
            return t("refus.engagement-a", "<red>Il manque l'engagement de la partie A.");
        }
        if (t.b.vide()) {
            return t("refus.engagement-b", "<red>Il manque l'engagement de la partie B.");
        }
        return null;
    }

    /** Nouveau contrat : partie A = l'auteur et son équipe, partie B = les autres. Null si envoyé, sinon le refus. */
    Component proposer(UUID auteur, String nomAuteur, Brouillon b) {
        if (b.autres.isEmpty()) {
            return t("refus.autres", "<red>Choisis au moins un joueur pour l'autre partie.");
        }
        Component refus = verifier(b.termes);
        if (refus != null) {
            return refus;
        }
        if (b.equipe.size() + 1 > PAR_PARTIE || b.autres.size() > PAR_PARTIE) {
            return t("refus.trop", "<red>10 joueurs au plus par partie.");
        }
        if (ouverts(auteur) >= OUVERTS_MAX) {
            return t("refus.trop-contrats", "<red>Tu as déjà 30 contrats en cours (actifs ou proposés).");
        }
        Contrat c = new Contrat();
        c.id = "c" + (prochain + 1);
        c.cree = System.currentTimeMillis();
        c.partieA.add(auteur);
        c.partieA.addAll(b.equipe);
        c.partieB.addAll(b.autres);
        c.noms.put(auteur, nomAuteur);
        for (UUID u : c.participants()) {
            if (!u.equals(auteur)) {
                c.noms.put(u, b.noms.getOrDefault(u, "?"));
            }
        }
        Demande d = new Demande();
        d.type = TypeDemande.NOUVEAU;
        d.auteur = auteur;
        d.date = c.cree;
        d.termes = b.termes.copie();
        d.acceptes.add(auteur);
        c.demande = d;
        long part = c.part(auteur, d.termes.garantie);
        if (!prendre(c, auteur, part)) {
            return t("refus.solde", "<red>Solde insuffisant pour ta part de la garantie (<part>).", "part",
                    Textes.points(part));
        }
        prochain++;
        contrats.put(c.id, c);
        c.noter("Proposé par " + nomAuteur + (part > 0 ? " (garantie : " + Textes.points(part) + " retenus)" : ""));
        prevenirTous(c, t("message.propose", "<yellow><joueur> te propose un contrat. <gray>(/contrat, Propositions)",
                "joueur", nomAuteur), auteur);
        modifie();
        return null;
    }

    /** Demande sur un contrat actif (modification, validation anticipée, annulation). */
    Component demander(Contrat c, UUID auteur, TypeDemande type, Termes termes) {
        if (c.etat != Etat.ACTIF) {
            return t("refus.pas-actif", "<red>Ce contrat n'est plus actif.");
        }
        if (c.demande != null) {
            return t("refus.demande-en-cours", "<red>Une demande attend déjà une réponse sur ce contrat.");
        }
        Demande d = new Demande();
        d.type = type;
        d.auteur = auteur;
        d.date = System.currentTimeMillis();
        d.acceptes.add(auteur);
        if (type == TypeDemande.MODIFICATION) {
            Component refus = verifier(termes);
            if (refus != null) {
                return refus;
            }
            d.termes = termes.copie();
            long manque = c.part(auteur, d.termes.garantie) - c.sequestre.getOrDefault(auteur, 0L);
            if (manque > 0) {
                if (!prendre(c, auteur, manque)) {
                    return t("refus.solde-plus", "<red>Solde insuffisant pour ta part de la nouvelle garantie "
                            + "(<manque> de plus).", "manque", Textes.points(manque));
                }
                d.supplements.put(auteur, manque);
            }
        }
        c.demande = d;
        String nom = c.nom(auteur);
        c.noter(switch (type) {
            case MODIFICATION -> "Modification proposée par " + nom;
            case VALIDATION -> "Validation anticipée demandée par " + nom;
            default -> "Annulation demandée par " + nom;
        });
        prevenirTous(c, switch (type) {
            case MODIFICATION -> t("message.modif", "<yellow><joueur> propose une modification de <contrat>. "
                    + "<gray>(/contrat, Propositions)", "joueur", nom, "contrat", c.id);
            case VALIDATION -> t("message.validation", "<yellow><joueur> demande la validation anticipée de "
                    + "<contrat>. <gray>(/contrat, Propositions)", "joueur", nom, "contrat", c.id);
            default -> t("message.annulation", "<yellow><joueur> demande l'annulation de <contrat>. "
                    + "<gray>(/contrat, Propositions)", "joueur", nom, "contrat", c.id);
        }, auteur);
        modifie();
        return null;
    }

    Component accepter(Contrat c, UUID compte) {
        Demande d = c.demande;
        if (d == null || !c.participe(compte) || (c.etat != Etat.PROPOSITION && c.etat != Etat.ACTIF)) {
            return t("refus.plus-de-demande", "<red>Cette demande n'attend plus de réponse.");
        }
        if (d.acceptes.contains(compte)) {
            return t("refus.deja", "<red>Tu as déjà accepté ; en attente des autres joueurs.");
        }
        if (d.type == TypeDemande.NOUVEAU) {
            long part = c.part(compte, d.termes.garantie);
            if (!prendre(c, compte, part)) {
                return t("refus.solde", "<red>Solde insuffisant pour ta part de la garantie (<part>).", "part",
                        Textes.points(part));
            }
        } else if (d.type == TypeDemande.MODIFICATION) {
            long manque = c.part(compte, d.termes.garantie) - c.sequestre.getOrDefault(compte, 0L);
            if (manque > 0) {
                if (!prendre(c, compte, manque)) {
                    return t("refus.solde-plus", "<red>Solde insuffisant pour ta part de la nouvelle garantie "
                            + "(<manque> de plus).", "manque", Textes.points(manque));
                }
                d.supplements.put(compte, manque);
            }
        }
        d.acceptes.add(compte);
        c.noter("Accepté par " + c.nom(compte));
        if (d.acceptes.containsAll(c.participants())) {
            effectuer(c);
        }
        modifie();
        return null;
    }

    /** Tous ont accepté : la demande prend effet. */
    private void effectuer(Contrat c) {
        Demande d = c.demande;
        long maintenant = System.currentTimeMillis();
        switch (d.type) {
            case NOUVEAU -> {
                c.etat = Etat.ACTIF;
                c.termes = d.termes;
                c.demande = null;
                c.debutPeriode = maintenant;
                c.echeance = c.termes.duree > 0 ? maintenant + c.termes.duree : 0;
                c.remettreLivraisons();
                ajouterVersion(c, d);
                c.noter("Contrat actif (version 1)");
                prevenirTous(c, t("message.actif", "<green>Le contrat <contrat> est actif : tout le monde a accepté.",
                        "contrat", c.id), null);
            }
            case MODIFICATION -> {
                Termes ancien = c.termes;
                c.termes = d.termes;
                c.demande = null;
                for (UUID u : c.participants()) {
                    long trop = c.sequestre.getOrDefault(u, 0L) - c.part(u, c.termes.garantie);
                    if (trop > 0) {
                        rendre(c, u, trop);
                    }
                }
                c.echeance = c.termes.duree > 0 ? maintenant + c.termes.duree : 0;
                if (ancien.periode != c.termes.periode) {
                    c.debutPeriode = maintenant;
                    c.remettreLivraisons();
                } else {
                    c.reporterLivraisons(ancien);
                }
                ajouterVersion(c, d);
                c.noter("Modification acceptée par tous (version " + c.versions.size() + ")");
                prevenirTous(c, t("message.modif-faite", "<green>La modification de <contrat> est acceptée par tous.",
                        "contrat", c.id), null);
            }
            case VALIDATION -> {
                c.demande = null;
                c.noter("Validation anticipée acceptée par tous");
                terminer(c, Fin.VALIDE);
                prevenirTous(c, t("message.valide", "<green>Le contrat <contrat> est validé : garanties rendues.",
                        "contrat", c.id), null);
            }
            case ANNULATION -> {
                c.demande = null;
                c.noter("Annulation acceptée par tous");
                terminer(c, Fin.ANNULE);
                prevenirTous(c, t("message.annule", "<yellow>Le contrat <contrat> est annulé : garanties rendues.",
                        "contrat", c.id), null);
            }
        }
    }

    private void ajouterVersion(Contrat c, Demande d) {
        Version v = new Version();
        v.numero = c.versions.size() + 1;
        v.date = System.currentTimeMillis();
        v.termes = c.termes.copie();
        d.acceptes.forEach(u -> v.signataires.add(c.nom(u)));
        c.versions.add(v);
    }

    Component refuser(Contrat c, UUID compte) {
        Demande d = c.demande;
        if (d == null || !c.participe(compte)) {
            return t("refus.plus-de-demande", "<red>Cette demande n'attend plus de réponse.");
        }
        boolean auteur = d.auteur.equals(compte);
        String nom = c.nom(compte);
        if (d.type == TypeDemande.NOUVEAU) {
            toutRendre(c);
            c.demande = null;
            c.noter(auteur ? "Proposition retirée par " + nom : "Refusé par " + nom);
            terminer(c, auteur ? Fin.RETIRE : Fin.REFUSE);
        } else {
            rendreSupplements(c);
            c.demande = null;
            c.noter((auteur ? "Demande retirée par " : "Demande refusée par ") + nom);
        }
        prevenirTous(c, auteur
                ? t("message.retire", "<gray><joueur> a retiré sa demande sur <contrat>.", "joueur", nom, "contrat", c.id)
                : t("message.refuse", "<red><joueur> a refusé la demande sur <contrat> : elle tombe pour tout le monde.",
                "joueur", nom, "contrat", c.id), compte);
        modifie();
        return null;
    }

    /** « Proposer une modification » d'une proposition ou d'une modification reçue : elle remplace la demande. */
    Component contreProposer(Contrat c, UUID compte, Termes termes) {
        Demande d = c.demande;
        if (d == null || !c.participe(compte) || d.auteur.equals(compte)
                || (d.type != TypeDemande.NOUVEAU && d.type != TypeDemande.MODIFICATION)) {
            return t("refus.plus-de-demande", "<red>Cette demande n'attend plus de réponse.");
        }
        Component refus = verifier(termes);
        if (refus != null) {
            return refus;
        }
        Demande n = new Demande();
        n.type = d.type;
        n.auteur = compte;
        n.date = System.currentTimeMillis();
        n.termes = termes.copie();
        n.acceptes.add(compte);
        long tenuSansSupplement = c.sequestre.getOrDefault(compte, 0L) - d.supplements.getOrDefault(compte, 0L);
        long besoin = d.type == TypeDemande.NOUVEAU ? c.part(compte, n.termes.garantie)
                : c.part(compte, n.termes.garantie) - tenuSansSupplement;
        long disponible = KSEconomy.soldeCompte(compte) + (d.type == TypeDemande.NOUVEAU
                ? c.sequestre.getOrDefault(compte, 0L) : d.supplements.getOrDefault(compte, 0L));
        if (besoin > disponible) {
            return t("refus.solde", "<red>Solde insuffisant pour ta part de la garantie (<part>).", "part",
                    Textes.points(besoin));
        }
        if (d.type == TypeDemande.NOUVEAU) {
            // Les acceptations portaient sur l'ancienne version : chacun récupère sa part.
            toutRendre(c);
        } else {
            rendreSupplements(c);
        }
        c.demande = n;
        if (besoin > 0) {
            prendre(c, compte, besoin);
            if (d.type == TypeDemande.MODIFICATION) {
                n.supplements.put(compte, besoin);
            }
        }
        String nom = c.nom(compte);
        c.noter("Contre-proposition de " + nom);
        prevenirTous(c, t("message.contre", "<yellow><joueur> propose une autre version de <contrat>. "
                + "<gray>(/contrat, Propositions)", "joueur", nom, "contrat", c.id), compte);
        modifie();
        return null;
    }

    // ------------------------------------------------------------------ fin, litiges

    private void terminer(Contrat c, Fin fin) {
        if (fin != Fin.TRANCHE) {
            toutRendre(c);
        }
        rendreSupplements(c);
        c.demande = null;
        c.bilans.clear();
        c.etat = Etat.TERMINE;
        c.fin = fin;
        c.dateFin = System.currentTimeMillis();
        modifie();
    }

    /** Signaler un non-respect, ou contester une période : litige, garanties bloquées, admins prévenus. */
    Component signaler(Contrat c, UUID compte, String texte) {
        if (!c.participe(compte) || (c.etat != Etat.ACTIF && c.etat != Etat.ECHU)) {
            return t("refus.pas-actif", "<red>Ce contrat n'est plus actif.");
        }
        rendreSupplements(c);
        c.demande = null;
        c.etat = Etat.LITIGE;
        String nom = c.nom(compte);
        c.noter("Non-respect signalé par " + nom + " : " + texte);
        prevenirTous(c, t("message.litige", "<red><joueur> a signalé un non-respect de <contrat> : il est en litige "
                + "jusqu'à la décision d'un admin.", "joueur", nom, "contrat", c.id), compte);
        prevenirAdmins(t("message.litige-admin", "<red>[Contrats] Litige sur <contrat>, signalé par <joueur>. "
                + "<gray>(/contrat admin)", "joueur", nom, "contrat", c.id));
        modifie();
        return null;
    }

    /** Décision d'un admin : le total en séquestre réparti librement entre les participants. */
    Component trancher(Contrat c, Map<UUID, Long> repartition, String admin) {
        if (c.etat != Etat.LITIGE) {
            return t("refus.pas-litige", "<red>Ce contrat n'est pas en litige.");
        }
        long total = repartition.values().stream().mapToLong(Long::longValue).sum();
        if (total != c.totalSequestre()) {
            return t("refus.total", "<red>Le total (<total>) doit être égal aux garanties en séquestre (<sequestre>).",
                    "total", Textes.points(total), "sequestre", Textes.points(c.totalSequestre()));
        }
        List<String> details = new ArrayList<>();
        repartition.forEach((u, p) -> {
            if (p > 0) {
                KSEconomy.crediterCompte(u, p);
            }
            details.add(c.nom(u) + " " + Textes.points(p));
        });
        c.sequestre.clear();
        c.noter("Tranché par " + admin + " : " + String.join(", ", details));
        terminer(c, Fin.TRANCHE);
        for (UUID u : c.participants()) {
            prevenir(u, t("message.tranche", "<yellow>Litige sur <contrat> tranché par un admin : tu reçois <points>.",
                    "contrat", c.id, "points", Textes.points(repartition.getOrDefault(u, 0L))));
        }
        modifie();
        return null;
    }

    Component validerBilan(Contrat c, Bilan b, UUID compte) {
        if (!c.bilans.contains(b) || !c.participe(compte) || (c.etat != Etat.ACTIF && c.etat != Etat.ECHU)) {
            return t("refus.plus-de-bilan", "<red>Cette période n'attend plus de réponse.");
        }
        b.valides.add(compte);
        c.noter("Période du " + Textes.date(b.debut) + " validée par " + c.nom(compte));
        if (b.valides.containsAll(c.participants())) {
            fermerBilan(c, b, "validée par tous");
        }
        modifie();
        return null;
    }

    private void fermerBilan(Contrat c, Bilan b, String comment) {
        c.bilans.remove(b);
        c.noter((b.dernier ? "Fin du contrat " : "Période du " + Textes.date(b.debut) + " ") + comment);
        if (b.dernier) {
            terminer(c, Fin.VALIDE);
            prevenirTous(c, t("message.fin-validee", "<green>Le contrat <contrat> est terminé et validé : garanties "
                    + "rendues.", "contrat", c.id), null);
        }
        modifie();
    }

    // ------------------------------------------------------------------ temps

    /** Toutes les 30 s : périodes, expirations, bilans sans réponse depuis 7 jours. */
    void tic() {
        long maintenant = System.currentTimeMillis();
        for (Contrat c : new ArrayList<>(contrats.values())) {
            if (c.etat == Etat.ACTIF && c.termes != null) {
                if (c.termes.periode != null) {
                    long suivante = c.termes.periode.suivante(c.debutPeriode);
                    while (suivante <= maintenant && (c.echeance == 0 || suivante < c.echeance)) {
                        clorePeriode(c, c.debutPeriode, suivante, false);
                        c.debutPeriode = suivante;
                        c.remettreLivraisons();
                        suivante = c.termes.periode.suivante(c.debutPeriode);
                    }
                }
                if (c.etat == Etat.ACTIF && c.echeance > 0 && maintenant >= c.echeance) {
                    clorePeriode(c, c.debutPeriode, c.echeance, true);
                }
            }
            if (c.etat == Etat.ACTIF || c.etat == Etat.ECHU) {
                for (Bilan b : new ArrayList<>(c.bilans)) {
                    if (maintenant - b.fin >= Contrat.DELAI_BILAN && c.ouvert()) {
                        fermerBilan(c, b, "validée sans réponse de tous au bout de 7 jours");
                    }
                }
            }
        }
    }

    private void clorePeriode(Contrat c, long debut, long fin, boolean dernier) {
        Bilan b = new Bilan();
        b.debut = debut;
        b.fin = fin;
        b.dernier = dernier;
        b.livresA = new ArrayList<>(c.livresA);
        b.livresB = new ArrayList<>(c.livresB);
        b.pointsA = c.pointsLivresA;
        b.pointsB = c.pointsLivresB;
        if (dernier) {
            rendreSupplements(c);
            if (c.demande != null) {
                c.noter("Demande en cours abandonnée : le contrat est arrivé à son terme");
            }
            c.demande = null;
            c.etat = Etat.ECHU;
        }
        if (Contrat.toutLivre(c.termes, b.livresA, b.livresB, b.pointsA, b.pointsB)) {
            c.bilans.add(b);
            fermerBilan(c, b, "validée d'office (tout a été livré)");
            if (!dernier) {
                prevenirTous(c, t("message.periode-ok", "<green>Contrat <contrat> : période terminée, tout a été livré.",
                        "contrat", c.id), null);
            }
        } else {
            c.bilans.add(b);
            c.noter(dernier ? "Contrat arrivé à son terme : à valider ou contester"
                    : "Période du " + Textes.date(debut) + " terminée : à valider ou contester");
            prevenirTous(c, dernier
                    ? t("message.echu", "<yellow>Le contrat <contrat> est arrivé à son terme : valide-le ou conteste-le "
                    + "dans /contrat, Propositions (7 jours sans réponse : validé).", "contrat", c.id)
                    : t("message.periode", "<yellow>Contrat <contrat> : période terminée, valide-la ou conteste-la dans "
                    + "/contrat, Propositions (7 jours sans réponse : validée).", "contrat", c.id), null);
        }
        modifie();
    }

    // ------------------------------------------------------------------ /echange (KS_Economy)

    /** Contrat livrable entre ces deux comptes : actif, chacun d'un côté. */
    private Contrat livrable(String id, UUID donneur, UUID receveur) {
        Contrat c = contrats.get(id);
        if (c == null || c.etat != Etat.ACTIF || c.termes == null) {
            return null;
        }
        boolean opposes = (c.dansA(donneur) && c.partieB.contains(receveur))
                || (c.partieB.contains(donneur) && c.dansA(receveur));
        return opposes ? c : null;
    }

    @Override
    public List<String> communs(Player a, Player b) {
        List<String> ids = new ArrayList<>();
        UUID ca = compte(a);
        for (Contrat c : de(ca, Etat.ACTIF)) {
            if (livrable(c.id, ca, compte(b)) != null) {
                ids.add(c.id);
            }
        }
        return ids;
    }

    @Override
    public ItemStack icone(String id, Player pour) {
        Contrat c = contrats.get(id);
        return c == null || c.etat != Etat.ACTIF ? null : plugin.menus().icone(pour, c);
    }

    @Override
    public Tri trier(String id, Player donneur, Player receveur, List<ItemStack> objets) {
        UUID cd = compte(donneur);
        Contrat c = livrable(id, cd, compte(receveur));
        if (c == null) {
            return null;
        }
        boolean a = c.dansA(cd);
        Engagement e = c.termes.de(a);
        List<Integer> reste = new ArrayList<>();
        for (int i = 0; i < e.lignes.size(); i++) {
            reste.add(Math.max(0, e.lignes.get(i).quantite - c.livres(a).get(i)));
        }
        List<ItemStack> acceptes = new ArrayList<>();
        List<ItemStack> refuses = new ArrayList<>();
        for (ItemStack objet : objets) {
            int quantite = objet.getAmount();
            for (int i = 0; i < e.lignes.size() && quantite > 0; i++) {
                if (reste.get(i) > 0 && e.lignes.get(i).correspond(objet)) {
                    int pris = Math.min(quantite, reste.get(i));
                    reste.set(i, reste.get(i) - pris);
                    quantite -= pris;
                    ItemStack accepte = objet.clone();
                    accepte.setAmount(pris);
                    acceptes.add(accepte);
                }
            }
            if (quantite > 0) {
                ItemStack refuse = objet.clone();
                refuse.setAmount(quantite);
                refuses.add(refuse);
            }
        }
        return new Tri(acceptes, refuses);
    }

    @Override
    public long pointsMax(String id, Player donneur, Player receveur) {
        UUID cd = compte(donneur);
        Contrat c = livrable(id, cd, compte(receveur));
        if (c == null) {
            return -1;
        }
        boolean a = c.dansA(cd);
        return Math.max(0, c.termes.de(a).points - (a ? c.pointsLivresA : c.pointsLivresB));
    }

    @Override
    public void livrer(String id, Player donneur, Player receveur, List<ItemStack> objets, long points) {
        UUID cd = compte(donneur);
        Contrat c = livrable(id, cd, compte(receveur));
        if (c == null || (objets.isEmpty() && points <= 0)) {
            return;
        }
        boolean a = c.dansA(cd);
        Engagement e = c.termes.de(a);
        List<Integer> livres = c.livres(a);
        List<String> details = new ArrayList<>();
        for (ItemStack objet : objets) {
            int quantite = objet.getAmount();
            for (int i = 0; i < e.lignes.size() && quantite > 0; i++) {
                int reste = e.lignes.get(i).quantite - livres.get(i);
                if (reste > 0 && e.lignes.get(i).correspond(objet)) {
                    int pris = Math.min(quantite, reste);
                    livres.set(i, livres.get(i) + pris);
                    quantite -= pris;
                    details.add(pris + " " + Textes.nomTexte(objet) + " (" + livres.get(i) + "/"
                            + e.lignes.get(i).quantite + ")");
                }
            }
        }
        long pts = Math.min(points, e.points - (a ? c.pointsLivresA : c.pointsLivresB));
        if (pts > 0) {
            if (a) {
                c.pointsLivresA += pts;
            } else {
                c.pointsLivresB += pts;
            }
            details.add(Textes.points(pts) + " (" + Textes.nombre(a ? c.pointsLivresA : c.pointsLivresB) + "/"
                    + Textes.nombre(e.points) + ")");
        }
        if (details.isEmpty()) {
            return;
        }
        String texte = String.join(", ", details);
        c.noter("Livré par " + c.nom(cd) + " à " + c.nom(compte(receveur)) + " (/echange) : " + texte);
        Component message = t("message.livre", "<green>Contrat <contrat> : livraison comptée : <details>.",
                "contrat", c.id, "details", texte);
        donneur.sendMessage(message);
        receveur.sendMessage(message);
        modifie();
    }
}
