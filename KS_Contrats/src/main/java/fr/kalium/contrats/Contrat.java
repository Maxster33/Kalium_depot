package fr.kalium.contrats;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Un contrat entre deux parties (demande de LeKiwi06 du 27/10/2026 : « il sert à avoir une trace et qu'un admin puisse
 * trancher en cas de non-respect de l'engagement »).
 *
 * Partie A : celui qui a proposé et les joueurs qu'il a mis de son côté ; partie B : ceux avec qui il passe le
 * contrat. Les joueurs sont des comptes (compte de test dans le monde de test, voir CompteTest). Rien n'est jamais
 * effacé : versions acceptées, demandes, livraisons, signalements et décisions restent dans l'historique.
 */
final class Contrat {

    static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    static final long MINUTE = 60_000L, HEURE = 60 * MINUTE, JOUR = 24 * HEURE;
    /** Sans réponse à « Valider ou contester » au bout de 7 jours : compté comme validé (LeKiwi06, 27/10/2026). */
    static final long DELAI_BILAN = 7 * JOUR;

    enum Etat { PROPOSITION, ACTIF, ECHU, LITIGE, TERMINE }

    /** Raison de la fin d'un contrat terminé. */
    enum Fin { REFUSE, RETIRE, VALIDE, ANNULE, TRANCHE }

    enum TypeDemande { NOUVEAU, MODIFICATION, VALIDATION, ANNULATION }

    /** Récurrence (à compter de l'acceptation, LeKiwi06 du 27/10/2026). */
    enum Periode {
        JOUR, SEMAINE, DEUX_SEMAINES, MOIS;

        /** Début de la période suivante. */
        long suivante(long debut) {
            return switch (this) {
                case JOUR -> debut + Contrat.JOUR;
                case SEMAINE -> debut + 7 * Contrat.JOUR;
                case DEUX_SEMAINES -> debut + 14 * Contrat.JOUR;
                case MOIS -> Instant.ofEpochMilli(debut).atZone(PARIS).plusMonths(1).toInstant().toEpochMilli();
            };
        }
    }

    /** Une ligne d'un bien : un objet (ou n'importe quel objet de ce type) et sa quantité. */
    static final class Ligne {
        ItemStack modele;
        /** Vrai : objet choisi dans l'inventaire, à livrer à l'identique (usure non comptée) ; faux : nom écrit. */
        boolean exact;
        int quantite;

        Ligne copie() {
            Ligne l = new Ligne();
            l.modele = modele.clone();
            l.exact = exact;
            l.quantite = quantite;
            return l;
        }

        /** Cet objet compte-t-il pour cette ligne ? */
        boolean correspond(ItemStack objet) {
            if (objet == null || objet.getType() != modele.getType()) {
                return false;
            }
            return !exact || sansUsure(objet).isSimilar(sansUsure(modele));
        }

        boolean meme(Ligne autre) {
            return exact == autre.exact && sansUsure(modele).isSimilar(sansUsure(autre.modele));
        }

        private static ItemStack sansUsure(ItemStack objet) {
            ItemStack copie = objet.clone();
            copie.setAmount(1);
            ItemMeta meta = copie.getItemMeta();
            if (meta instanceof Damageable usure && usure.hasDamage()) {
                usure.setDamage(0);
                copie.setItemMeta(meta);
            }
            return copie;
        }
    }

    /** Ce qu'une partie s'engage à faire : un bien (objets, points) ou un service (texte). */
    static final class Engagement {
        boolean service;
        /** Service : le texte ; bien : la précision facultative. */
        String texte = "";
        List<Ligne> lignes = new ArrayList<>();
        long points;

        boolean vide() {
            return service ? texte.isBlank() : lignes.isEmpty() && points == 0;
        }

        Engagement copie() {
            Engagement e = new Engagement();
            e.service = service;
            e.texte = texte;
            lignes.forEach(l -> e.lignes.add(l.copie()));
            e.points = points;
            return e;
        }
    }

    /** Les termes d'une version du contrat. */
    static final class Termes {
        Engagement a = new Engagement();
        Engagement b = new Engagement();
        /** null : une fois. */
        Periode periode;
        /** Une fois : temps jusqu'à l'expiration ; récurrent : jusqu'à la fin ; 0 : aucune. Compté à l'acceptation. */
        long duree;
        /** Garantie par partie, divisée entre ses joueurs. */
        long garantie;

        Termes copie() {
            Termes t = new Termes();
            t.a = a.copie();
            t.b = b.copie();
            t.periode = periode;
            t.duree = duree;
            t.garantie = garantie;
            return t;
        }

        Engagement de(boolean partieA) {
            return partieA ? a : b;
        }
    }

    static final class Version {
        int numero;
        long date;
        Termes termes;
        List<String> signataires = new ArrayList<>();
    }

    /** Une demande qui attend l'accord de tous les autres participants. */
    static final class Demande {
        TypeDemande type;
        UUID auteur;
        long date;
        /** Nouveau contrat ou modification : les termes proposés. */
        Termes termes;
        Set<UUID> acceptes = new LinkedHashSet<>();
        /** Modification : points de garantie retirés en plus à chacun (rendus si la demande tombe). */
        Map<UUID, Long> supplements = new LinkedHashMap<>();
    }

    /** Une période (ou la fin du contrat) à valider ou contester. */
    static final class Bilan {
        long debut;
        long fin;
        /** Fin du contrat (expiration) : garanties rendues quand il est validé. */
        boolean dernier;
        List<Integer> livresA = new ArrayList<>();
        List<Integer> livresB = new ArrayList<>();
        long pointsA;
        long pointsB;
        Set<UUID> valides = new LinkedHashSet<>();
    }

    String id;
    long cree;
    Etat etat = Etat.PROPOSITION;
    Fin fin;
    long dateFin;
    List<UUID> partieA = new ArrayList<>();
    List<UUID> partieB = new ArrayList<>();
    Map<UUID, String> noms = new LinkedHashMap<>();
    /** Termes en vigueur (null tant que le contrat n'a pas été accepté). */
    Termes termes;
    List<Version> versions = new ArrayList<>();
    Demande demande;
    /** Points de garantie retenus pour chaque joueur. */
    Map<UUID, Long> sequestre = new LinkedHashMap<>();
    /** Expiration (une fois) ou fin (récurrent), 0 : aucune. */
    long echeance;
    /** Début de la période en cours (récurrent) ou de la version en vigueur (une fois). */
    long debutPeriode;
    /** Quantités livrées par chaque partie pour chaque ligne (période en cours), et points. */
    List<Integer> livresA = new ArrayList<>();
    List<Integer> livresB = new ArrayList<>();
    long pointsLivresA;
    long pointsLivresB;
    List<Bilan> bilans = new ArrayList<>();
    /** Historique en clair (date -> texte), jamais effacé. */
    List<Map.Entry<Long, String>> historique = new ArrayList<>();

    // ------------------------------------------------------------------ participants

    List<UUID> participants() {
        List<UUID> tous = new ArrayList<>(partieA);
        tous.addAll(partieB);
        return tous;
    }

    boolean participe(UUID compte) {
        return partieA.contains(compte) || partieB.contains(compte);
    }

    boolean dansA(UUID compte) {
        return partieA.contains(compte);
    }

    String nom(UUID compte) {
        return noms.getOrDefault(compte, "?");
    }

    String nomsDe(List<UUID> partie) {
        List<String> liste = new ArrayList<>();
        partie.forEach(u -> liste.add(nom(u)));
        return String.join(", ", liste);
    }

    /** Part de la garantie de ce joueur : divisée équitablement, le point de plus aux premiers de la partie. */
    long part(UUID compte, long garantie) {
        List<UUID> partie = dansA(compte) ? partieA : partieB;
        int i = partie.indexOf(compte);
        if (i < 0 || partie.isEmpty()) {
            return 0;
        }
        long n = partie.size();
        return garantie / n + (i < garantie % n ? 1 : 0);
    }

    long totalSequestre() {
        return sequestre.values().stream().mapToLong(Long::longValue).sum();
    }

    /** Termes affichés : en vigueur, sinon ceux de la proposition. */
    Termes termesVus() {
        return termes != null ? termes : demande != null ? demande.termes : new Termes();
    }

    boolean ouvert() {
        return etat != Etat.TERMINE;
    }

    void noter(String texte) {
        historique.add(Map.entry(System.currentTimeMillis(), texte));
    }

    /** Quantités livrées repartent à 0 (nouvelle période ou nouvelle version). */
    void remettreLivraisons() {
        livresA = zeros(termes.a.lignes.size());
        livresB = zeros(termes.b.lignes.size());
        pointsLivresA = 0;
        pointsLivresB = 0;
    }

    /** Nouvelle version : les livraisons d'une ligne restée identique sont gardées. */
    void reporterLivraisons(Termes ancien) {
        livresA = reporter(ancien.a, termes.a, livresA);
        livresB = reporter(ancien.b, termes.b, livresB);
        pointsLivresA = Math.min(pointsLivresA, termes.a.points);
        pointsLivresB = Math.min(pointsLivresB, termes.b.points);
    }

    private static List<Integer> reporter(Engagement ancien, Engagement nouveau, List<Integer> livres) {
        List<Integer> r = zeros(nouveau.lignes.size());
        for (int i = 0; i < nouveau.lignes.size(); i++) {
            for (int j = 0; j < ancien.lignes.size() && j < livres.size(); j++) {
                if (ancien.lignes.get(j).meme(nouveau.lignes.get(i))) {
                    r.set(i, Math.min(livres.get(j), nouveau.lignes.get(i).quantite));
                }
            }
        }
        return r;
    }

    static List<Integer> zeros(int n) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            l.add(0);
        }
        return l;
    }

    List<Integer> livres(boolean partieA) {
        ajuster();
        return partieA ? livresA : livresB;
    }

    /** Une quantité livrée par ligne (fichier modifié à la main, ancienne version) : listes complétées par des 0. */
    void ajuster() {
        if (termes == null) {
            return;
        }
        while (livresA.size() < termes.a.lignes.size()) {
            livresA.add(0);
        }
        while (livresB.size() < termes.b.lignes.size()) {
            livresB.add(0);
        }
    }

    /** Tout ce qui est dû en biens est livré, et aucun des deux engagements n'est un service. */
    static boolean toutLivre(Termes t, List<Integer> livresA, List<Integer> livresB, long pointsA, long pointsB) {
        return !t.a.service && !t.b.service && complet(t.a, livresA, pointsA) && complet(t.b, livresB, pointsB);
    }

    private static boolean complet(Engagement e, List<Integer> livres, long points) {
        for (int i = 0; i < e.lignes.size(); i++) {
            if (i >= livres.size() || livres.get(i) < e.lignes.get(i).quantite) {
                return false;
            }
        }
        return points >= e.points;
    }

    // ------------------------------------------------------------------ enregistrement

    void ecrire(ConfigurationSection s) {
        s.set("cree", cree);
        s.set("etat", etat.name());
        s.set("fin", fin == null ? null : fin.name());
        s.set("date-fin", dateFin);
        s.set("partie-a", uuids(partieA));
        s.set("partie-b", uuids(partieB));
        ConfigurationSection n = s.createSection("noms");
        noms.forEach((u, nom) -> n.set(u.toString(), nom));
        if (termes != null) {
            ecrire(s.createSection("termes"), termes);
        }
        ConfigurationSection vs = s.createSection("versions");
        for (Version v : versions) {
            ConfigurationSection c = vs.createSection(String.valueOf(v.numero));
            c.set("date", v.date);
            c.set("signataires", v.signataires);
            ecrire(c.createSection("termes"), v.termes);
        }
        if (demande != null) {
            ConfigurationSection d = s.createSection("demande");
            d.set("type", demande.type.name());
            d.set("auteur", demande.auteur.toString());
            d.set("date", demande.date);
            d.set("acceptes", uuids(new ArrayList<>(demande.acceptes)));
            ConfigurationSection sup = d.createSection("supplements");
            demande.supplements.forEach((u, p) -> sup.set(u.toString(), p));
            if (demande.termes != null) {
                ecrire(d.createSection("termes"), demande.termes);
            }
        }
        ConfigurationSection seq = s.createSection("sequestre");
        sequestre.forEach((u, p) -> seq.set(u.toString(), p));
        s.set("echeance", echeance);
        s.set("debut-periode", debutPeriode);
        s.set("livres-a", livresA);
        s.set("livres-b", livresB);
        s.set("points-livres-a", pointsLivresA);
        s.set("points-livres-b", pointsLivresB);
        ConfigurationSection bs = s.createSection("bilans");
        for (int i = 0; i < bilans.size(); i++) {
            Bilan b = bilans.get(i);
            ConfigurationSection c = bs.createSection(String.valueOf(i));
            c.set("debut", b.debut);
            c.set("fin", b.fin);
            c.set("dernier", b.dernier);
            c.set("livres-a", b.livresA);
            c.set("livres-b", b.livresB);
            c.set("points-a", b.pointsA);
            c.set("points-b", b.pointsB);
            c.set("valides", uuids(new ArrayList<>(b.valides)));
        }
        List<String> h = new ArrayList<>();
        historique.forEach(e -> h.add(e.getKey() + "|" + e.getValue()));
        s.set("historique", h);
    }

    static Contrat lire(String id, ConfigurationSection s) {
        Contrat c = new Contrat();
        c.id = id;
        c.cree = s.getLong("cree");
        c.etat = Etat.valueOf(s.getString("etat", "TERMINE"));
        c.fin = s.getString("fin") == null ? null : Fin.valueOf(s.getString("fin"));
        c.dateFin = s.getLong("date-fin");
        c.partieA = uuids(s.getStringList("partie-a"));
        c.partieB = uuids(s.getStringList("partie-b"));
        ConfigurationSection n = s.getConfigurationSection("noms");
        if (n != null) {
            n.getKeys(false).forEach(k -> c.noms.put(UUID.fromString(k), n.getString(k)));
        }
        if (s.isConfigurationSection("termes")) {
            c.termes = lireTermes(s.getConfigurationSection("termes"));
        }
        ConfigurationSection vs = s.getConfigurationSection("versions");
        if (vs != null) {
            for (String k : vs.getKeys(false)) {
                ConfigurationSection vc = vs.getConfigurationSection(k);
                Version v = new Version();
                v.numero = Integer.parseInt(k);
                v.date = vc.getLong("date");
                v.signataires = new ArrayList<>(vc.getStringList("signataires"));
                v.termes = lireTermes(vc.getConfigurationSection("termes"));
                c.versions.add(v);
            }
        }
        ConfigurationSection d = s.getConfigurationSection("demande");
        if (d != null) {
            Demande dem = new Demande();
            dem.type = TypeDemande.valueOf(d.getString("type"));
            dem.auteur = UUID.fromString(d.getString("auteur"));
            dem.date = d.getLong("date");
            dem.acceptes.addAll(uuids(d.getStringList("acceptes")));
            ConfigurationSection sup = d.getConfigurationSection("supplements");
            if (sup != null) {
                sup.getKeys(false).forEach(k -> dem.supplements.put(UUID.fromString(k), sup.getLong(k)));
            }
            if (d.isConfigurationSection("termes")) {
                dem.termes = lireTermes(d.getConfigurationSection("termes"));
            }
            c.demande = dem;
        }
        ConfigurationSection seq = s.getConfigurationSection("sequestre");
        if (seq != null) {
            seq.getKeys(false).forEach(k -> c.sequestre.put(UUID.fromString(k), seq.getLong(k)));
        }
        c.echeance = s.getLong("echeance");
        c.debutPeriode = s.getLong("debut-periode");
        c.livresA = new ArrayList<>(s.getIntegerList("livres-a"));
        c.livresB = new ArrayList<>(s.getIntegerList("livres-b"));
        c.pointsLivresA = s.getLong("points-livres-a");
        c.pointsLivresB = s.getLong("points-livres-b");
        ConfigurationSection bs = s.getConfigurationSection("bilans");
        if (bs != null) {
            for (String k : bs.getKeys(false)) {
                ConfigurationSection bc = bs.getConfigurationSection(k);
                Bilan b = new Bilan();
                b.debut = bc.getLong("debut");
                b.fin = bc.getLong("fin");
                b.dernier = bc.getBoolean("dernier");
                b.livresA = new ArrayList<>(bc.getIntegerList("livres-a"));
                b.livresB = new ArrayList<>(bc.getIntegerList("livres-b"));
                b.pointsA = bc.getLong("points-a");
                b.pointsB = bc.getLong("points-b");
                b.valides.addAll(uuids(bc.getStringList("valides")));
                c.bilans.add(b);
            }
        }
        for (String ligne : s.getStringList("historique")) {
            int sep = ligne.indexOf('|');
            if (sep > 0) {
                c.historique.add(Map.entry(Long.parseLong(ligne.substring(0, sep)), ligne.substring(sep + 1)));
            }
        }
        return c;
    }

    private static void ecrire(ConfigurationSection s, Termes t) {
        s.set("periode", t.periode == null ? null : t.periode.name());
        s.set("duree", t.duree);
        s.set("garantie", t.garantie);
        ecrire(s.createSection("a"), t.a);
        ecrire(s.createSection("b"), t.b);
    }

    private static void ecrire(ConfigurationSection s, Engagement e) {
        s.set("service", e.service);
        s.set("texte", e.texte);
        s.set("points", e.points);
        ConfigurationSection ls = s.createSection("lignes");
        for (int i = 0; i < e.lignes.size(); i++) {
            Ligne l = e.lignes.get(i);
            ConfigurationSection c = ls.createSection(String.valueOf(i));
            c.set("objet", objetVersTexte(l.modele));
            c.set("exact", l.exact);
            c.set("quantite", l.quantite);
        }
    }

    private static Termes lireTermes(ConfigurationSection s) {
        Termes t = new Termes();
        t.periode = s.getString("periode") == null ? null : Periode.valueOf(s.getString("periode"));
        t.duree = s.getLong("duree");
        t.garantie = s.getLong("garantie");
        t.a = lireEngagement(s.getConfigurationSection("a"));
        t.b = lireEngagement(s.getConfigurationSection("b"));
        return t;
    }

    private static Engagement lireEngagement(ConfigurationSection s) {
        Engagement e = new Engagement();
        if (s == null) {
            return e;
        }
        e.service = s.getBoolean("service");
        e.texte = s.getString("texte", "");
        e.points = s.getLong("points");
        ConfigurationSection ls = s.getConfigurationSection("lignes");
        if (ls != null) {
            for (String k : ls.getKeys(false)) {
                ConfigurationSection c = ls.getConfigurationSection(k);
                ItemStack objet = texteVersObjet(c.getString("objet"));
                if (objet == null) {
                    continue;
                }
                Ligne l = new Ligne();
                l.modele = objet;
                l.exact = c.getBoolean("exact");
                l.quantite = c.getInt("quantite");
                e.lignes.add(l);
            }
        }
        return e;
    }

    static String objetVersTexte(ItemStack objet) {
        ItemStack un = objet.clone();
        un.setAmount(1);
        return Base64.getEncoder().encodeToString(un.serializeAsBytes());
    }

    static ItemStack texteVersObjet(String texte) {
        if (texte == null || texte.isEmpty()) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(texte));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static List<String> uuids(List<UUID> liste) {
        List<String> r = new ArrayList<>();
        liste.forEach(u -> r.add(u.toString()));
        return r;
    }

    private static List<UUID> uuids(java.util.Collection<String> liste) {
        List<UUID> r = new ArrayList<>();
        for (String s : liste) {
            try {
                r.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // identifiant illisible : ignoré
            }
        }
        return r;
    }
}
