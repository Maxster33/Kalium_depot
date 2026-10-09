package fr.kalium.kvbuildbattle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import fr.kalium.menu.api.Gui;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

/**
 * Une partie de Build Battle, sur une colonne de l'arène (cahier des charges : KV_BuildBattle/CAHIER_DES_CHARGES.md).
 * Phases : salle d'attente -> thème (5 thèmes au hasard à voter, ou thèmes écrits puis votés) -> construction
 * chronométrée (une boîte par équipe) -> vote de chaque terrain, l'un après l'autre -> résultats -> retour sur
 * kal-games, zones vidées. Avance d'une seconde à chaque appel de {@link #seconde()}.
 */
final class Partie {

    enum Type { PUBLIC, PRIVE }

    enum Phase { ATTENTE, THEME_ECRITURE, THEME_VOTE, CONSTRUCTION, VOTES, RESULTATS, FINIE }

    private static final String[] MODES = {"Solo", "Duo", "Trio", "Squad"};

    private final KVBuildBattle plugin;
    private final Jeu jeu;
    final int colonne;
    final Type type;
    final String id, code;
    UUID hote;
    final int tailleEquipes, equipesMax, minutes;
    final boolean themesEcrits;
    final Set<UUID> joueurs = new LinkedHashSet<>();

    Phase phase = Phase.ATTENTE;
    /** Secondes restantes de la phase (-1 = pas de compte à rebours) et durée totale de la phase. */
    private int compteur = -1, duree = 1;
    /** Équipes, dans l'ordre des boîtes de la colonne (rang 0, 1...). */
    final List<List<UUID>> equipes = new ArrayList<>();
    private final List<String> choix = new ArrayList<>();
    private final Map<String, UUID> auteurs = new HashMap<>();
    private final Map<UUID, String> propositions = new LinkedHashMap<>();
    private final Map<UUID, Integer> votesTheme = new HashMap<>();
    private final Set<String> signales = new HashSet<>();
    String theme;
    /** Équipe dont le terrain est noté (phase VOTES), ou gagnante (RESULTATS). */
    int terrain = -1;
    private final Map<Integer, Map<UUID, Integer>> notes = new HashMap<>();

    Partie(KVBuildBattle plugin, Jeu jeu, int colonne, Type type, String id, String code, UUID hote, int tailleEquipes,
           int equipesMax, int minutes, boolean themesEcrits) {
        this.plugin = plugin;
        this.jeu = jeu;
        this.colonne = colonne;
        this.type = type;
        this.id = id;
        this.code = code;
        this.hote = hote;
        this.tailleEquipes = tailleEquipes;
        this.equipesMax = equipesMax;
        this.minutes = minutes;
        this.themesEcrits = themesEcrits;
    }

    int places() {
        return tailleEquipes * equipesMax;
    }

    String mode() {
        return MODES[Math.max(1, Math.min(4, tailleEquipes)) - 1];
    }

    String description() {
        return (type == Type.PRIVE ? "partie privée " + code : "file publique " + mode()) + ", " + minutes + " min";
    }

    private int duree(String cle, int def) {
        return Math.max(1, plugin.getConfig().getInt("durees." + cle, def));
    }

    // --- Joueurs ---

    private List<Player> enLigne() {
        List<Player> l = new ArrayList<>();
        for (UUID u : joueurs) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) l.add(p);
        }
        return l;
    }

    private void annoncer(String message) {
        for (Player p : enLigne()) p.sendMessage(message);
    }

    private void titre(String haut, String bas) {
        Title t = Title.title(Component.text(haut, NamedTextColor.GOLD), Component.text(bas, NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500)));
        for (Player p : enLigne()) p.showTitle(t);
    }

    int equipeDe(UUID joueur) {
        for (int i = 0; i < equipes.size(); i++) if (equipes.get(i).contains(joueur)) return i;
        return -1;
    }

    private static String noms(List<UUID> equipe) {
        List<String> n = new ArrayList<>();
        for (UUID u : equipe) {
            String nom = Bukkit.getOfflinePlayer(u).getName();
            n.add(nom == null ? "?" : nom);
        }
        return n.isEmpty() ? "(équipe partie)" : String.join(", ", n);
    }

    /** Arrivée en salle d'attente : objets de la salle ; mode aventure, invincible (0.3.2, LeKiwi06). */
    void accueillir(Player joueur) {
        joueurs.add(joueur.getUniqueId());
        joueur.setGameMode(GameMode.ADVENTURE);
        joueur.setFlying(false);
        joueur.setAllowFlight(false);
        joueur.setFoodLevel(20);
        joueur.getInventory().clear();
        if (type == Type.PRIVE && joueur.getUniqueId().equals(hote)) joueur.getInventory().setItem(4, jeu.objets().lancer());
        for (Player p : enLigne()) {
            if (p.equals(joueur)) {
                p.sendMessage("§6Build Battle §7- " + description() + " : salle d'attente (" + joueurs.size() + "/"
                        + places() + " joueurs)." + (type == Type.PRIVE && p.getUniqueId().equals(hote)
                        ? " §aTu es l'hôte : lance la partie avec l'émeraude." : ""));
            } else {
                p.sendMessage("§7" + joueur.getName() + " a rejoint la salle d'attente (" + joueurs.size() + "/" + places() + ").");
            }
        }
        rafraichirBarre();
    }

    /** Départ d'un joueur (déconnexion, changement de monde, fin) ; la partie se termine si plus personne n'est là. */
    void retirer(Player joueur) {
        UUID u = joueur.getUniqueId();
        if (!joueurs.remove(u)) return;
        propositions.remove(u);
        votesTheme.remove(u);
        for (List<UUID> e : equipes) e.remove(u);
        if (phase == Phase.FINIE) return;
        if (joueurs.isEmpty()) {
            terminer();
            return;
        }
        annoncer("§7" + joueur.getName() + " a quitté la partie.");
        if (u.equals(hote)) {
            hote = joueurs.iterator().next();
            Player nouvel = Bukkit.getPlayer(hote);
            if (nouvel != null && type == Type.PRIVE && phase == Phase.ATTENTE) {
                nouvel.getInventory().setItem(4, jeu.objets().lancer());
                nouvel.sendMessage("§aTu es maintenant l'hôte : lance la partie avec l'émeraude.");
            }
        }
    }

    // --- Horloge ---

    void seconde() {
        switch (phase) {
            case ATTENTE -> attente();
            case FINIE -> { }
            default -> {
                compteur--;
                if (compteur <= 0) finDePhase();
            }
        }
        rafraichirBarre();
    }

    private void attente() {
        if (type == Type.PRIVE) return; // lancée par l'hôte
        if (joueurs.size() >= places()) {
            demarrer();
            return;
        }
        boolean assez = joueurs.size() > tailleEquipes; // de quoi faire 2 équipes
        if (!assez) {
            if (compteur >= 0) annoncer("§cPas assez de joueurs : compte à rebours annulé.");
            compteur = -1;
            return;
        }
        if (compteur < 0) {
            compteur = duree("depart", 30);
            duree = compteur;
            annoncer("§eLa partie commence dans " + compteur + " s.");
            return;
        }
        compteur--;
        if (compteur <= 0) demarrer();
        else if (compteur <= 5 || compteur == 10) annoncer("§eDépart dans " + compteur + " s.");
    }

    private void finDePhase() {
        switch (phase) {
            case THEME_ECRITURE -> finEcriture();
            case THEME_VOTE -> finVoteTheme();
            case CONSTRUCTION -> finConstruction();
            case VOTES -> terrainSuivant();
            case RESULTATS -> terminer();
            default -> { }
        }
    }

    private void rafraichirBarre() {
        String texte;
        switch (phase) {
            case ATTENTE -> texte = "Salle d'attente · " + description() + " · " + joueurs.size() + "/" + places()
                    + (compteur >= 0 ? " · départ dans " + compteur + " s" : type == Type.PRIVE ? " · en attente de l'hôte"
                    : " · en attente de joueurs");
            case THEME_ECRITURE -> texte = "Propose un thème · " + temps(compteur);
            case THEME_VOTE -> texte = "Vote pour le thème · " + temps(compteur);
            case CONSTRUCTION -> texte = "Thème : " + theme + " · " + temps(compteur);
            case VOTES -> texte = "Construction de " + noms(equipes.get(terrain)) + " · " + temps(compteur);
            case RESULTATS -> texte = "Résultats · retour dans " + compteur + " s";
            default -> texte = "";
        }
        // 0.3.2 : barre d'action, comme les autres jeux (LeKiwi06), renvoyée chaque seconde.
        Component ligne = Component.text(texte, NamedTextColor.GOLD);
        for (Player p : enLigne()) p.sendActionBar(ligne);
    }

    private static String temps(int s) {
        return String.format(Locale.ROOT, "%d:%02d", Math.max(0, s) / 60, Math.max(0, s) % 60);
    }

    /** Raccourcit la phase à 3 s (tout le monde a voté / proposé). */
    private void raccourcir() {
        if (compteur > 3) {
            compteur = 3;
            annoncer("§7Tout le monde a répondu : suite dans 3 s.");
        }
    }

    // --- Départ ---

    /** Lancement par l'hôte d'une partie privée. */
    void lancerParHote(Player joueur) {
        if (phase != Phase.ATTENTE) return;
        if (!joueur.getUniqueId().equals(hote)) {
            joueur.sendMessage("§cSeul l'hôte peut lancer la partie.");
            return;
        }
        if (joueurs.size() <= tailleEquipes) {
            joueur.sendMessage("§cIl faut au moins 2 équipes (" + (tailleEquipes + 1) + " joueurs).");
            return;
        }
        demarrer();
    }

    private void demarrer() {
        if (!plugin.arene().boitesCollees()) {
            annoncer("§cL'arène du Build Battle n'est pas encore générée : impossible de lancer la partie.");
            compteur = -1;
            return;
        }
        if (type == Type.PRIVE) jeu.publierFermee(id);
        List<UUID> ordre = new ArrayList<>(joueurs);
        Collections.shuffle(ordre);
        int nb = Math.min(equipesMax, (ordre.size() + tailleEquipes - 1) / tailleEquipes);
        for (int i = 0; i < nb; i++) equipes.add(new ArrayList<>());
        for (int i = 0; i < ordre.size(); i++) equipes.get(Math.min(nb - 1, i / tailleEquipes)).add(ordre.get(i));
        for (Player p : enLigne()) {
            p.getInventory().clear();
            if (tailleEquipes > 1) {
                List<UUID> e = equipes.get(equipeDe(p.getUniqueId()));
                p.sendMessage("§6Ton équipe : §f" + noms(e));
            }
        }
        annoncer("§6La partie commence ! §7" + equipes.size() + " équipes.");
        if (themesEcrits) {
            phase = Phase.THEME_ECRITURE;
            compteur = duree = duree("ecriture-theme", 60);
            for (Player p : enLigne()) {
                p.getInventory().setItem(4, jeu.objets().theme(true));
                ouvrirEcriture(p);
            }
        } else {
            choix.addAll(jeu.themesAuHasard(5));
            ouvrirVoteTheme();
        }
    }

    // --- Thème ---

    void ouvrirEcriture(Player joueur) {
        if (phase != Phase.THEME_ECRITURE) return;
        Gui g = jeu.gui();
        g.open(joueur, Component.text("Propose un thème", NamedTextColor.GOLD),
                List.of(Component.text("Écris un thème (64 caractères au plus). Tout le monde votera ensuite parmi les "
                        + "propositions.", NamedTextColor.GRAY)),
                List.of(g.text("theme", Component.text("Ton thème"), propositions.getOrDefault(joueur.getUniqueId(), ""), 64)),
                List.of(g.form(Component.text("Proposer", NamedTextColor.GREEN), null,
                        (p, vue) -> proposer(p, vue.getText("theme")))),
                null, 1);
    }

    private void proposer(Player joueur, String texte) {
        if (phase != Phase.THEME_ECRITURE || !joueurs.contains(joueur.getUniqueId())) return;
        String t = texte == null ? "" : texte.replaceAll("[§&][0-9a-fk-orA-FK-OR]", "").replaceAll("\\s+", " ").trim();
        if (t.length() > 64) t = t.substring(0, 64);
        if (t.isEmpty()) {
            joueur.sendMessage("§cÉcris un thème.");
            return;
        }
        propositions.put(joueur.getUniqueId(), t);
        joueur.sendMessage("§aThème proposé : §f" + t);
        if (propositions.keySet().containsAll(enLigneIds())) raccourcir();
    }

    private Set<UUID> enLigneIds() {
        Set<UUID> s = new HashSet<>();
        for (Player p : enLigne()) s.add(p.getUniqueId());
        return s;
    }

    private void finEcriture() {
        Map<String, String> distincts = new LinkedHashMap<>();
        for (Map.Entry<UUID, String> e : propositions.entrySet()) {
            String cle = e.getValue().toLowerCase(Locale.ROOT);
            if (!distincts.containsKey(cle)) {
                distincts.put(cle, e.getValue());
                auteurs.put(e.getValue(), e.getKey());
            }
        }
        choix.addAll(distincts.values());
        if (choix.isEmpty()) {
            annoncer("§7Aucun thème proposé : 5 thèmes tirés au hasard.");
            choix.addAll(jeu.themesAuHasard(5));
        } else if (choix.size() == 1) {
            theme = choix.get(0);
            construction();
            return;
        }
        ouvrirVoteTheme();
    }

    private void ouvrirVoteTheme() {
        phase = Phase.THEME_VOTE;
        compteur = duree = duree("vote-theme", 30);
        for (Player p : enLigne()) {
            p.getInventory().clear();
            p.getInventory().setItem(4, jeu.objets().theme(false));
            if (themesEcrits) p.getInventory().setItem(8, jeu.objets().signaler());
            ouvrirVoteTheme(p);
        }
    }

    void ouvrirVoteTheme(Player joueur) {
        if (phase == Phase.THEME_ECRITURE) {
            ouvrirEcriture(joueur);
            return;
        }
        if (phase != Phase.THEME_VOTE) return;
        Gui g = jeu.gui();
        List<ActionButton> boutons = new ArrayList<>();
        Integer actuel = votesTheme.get(joueur.getUniqueId());
        for (int i = 0; i < choix.size(); i++) {
            int n = i;
            boutons.add(g.button(Component.text((actuel != null && actuel == i ? "» " : "") + choix.get(i),
                    actuel != null && actuel == i ? NamedTextColor.GREEN : NamedTextColor.YELLOW), null, p -> voterTheme(p, n)));
        }
        g.open(joueur, Component.text("Vote pour le thème", NamedTextColor.GOLD),
                List.of(Component.text("Le thème le plus choisi l'emporte (égalité : tirage au sort).", NamedTextColor.GRAY)),
                List.of(), boutons, null, 1);
    }

    private void voterTheme(Player joueur, int n) {
        if (phase != Phase.THEME_VOTE || !joueurs.contains(joueur.getUniqueId()) || n < 0 || n >= choix.size()) return;
        votesTheme.put(joueur.getUniqueId(), n);
        joueur.sendMessage("§aTu as voté pour : §f" + choix.get(n));
        if (votesTheme.keySet().containsAll(enLigneIds())) raccourcir();
    }

    void ouvrirSignalement(Player joueur) {
        if (phase != Phase.THEME_VOTE || !themesEcrits) return;
        Gui g = jeu.gui();
        List<ActionButton> boutons = new ArrayList<>();
        for (String t : choix) {
            boutons.add(g.button(Component.text("Signaler : " + t, NamedTextColor.RED), null, p -> {
                String cle = p.getUniqueId() + "|" + t;
                if (!signales.add(cle)) {
                    p.sendMessage("§7Tu as déjà signalé ce thème.");
                    return;
                }
                jeu.signalerTheme(p, this, t, auteurs.get(t));
                p.sendMessage("§aThème signalé au staff. Merci !");
            }));
        }
        g.open(joueur, Component.text("Signaler un thème", NamedTextColor.RED),
                List.of(Component.text("Choisis le thème inapproprié à signaler au staff.", NamedTextColor.GRAY)),
                List.of(), boutons, null, 1);
    }

    private void finVoteTheme() {
        int[] compte = new int[choix.size()];
        for (int v : votesTheme.values()) if (v >= 0 && v < compte.length) compte[v]++;
        int max = 0;
        for (int c : compte) max = Math.max(max, c);
        List<Integer> meilleurs = new ArrayList<>();
        for (int i = 0; i < compte.length; i++) if (compte[i] == max) meilleurs.add(i);
        theme = choix.get(meilleurs.get(ThreadLocalRandom.current().nextInt(meilleurs.size())));
        construction();
    }

    // --- Construction ---

    private void construction() {
        phase = Phase.CONSTRUCTION;
        compteur = duree = minutes * 60;
        for (int i = 0; i < equipes.size(); i++) {
            plugin.arene().membres(colonne, i, equipes.get(i));
            Location l = plugin.arene().apparitionBoite(colonne, i);
            for (UUID u : equipes.get(i)) {
                Player p = Bukkit.getPlayer(u);
                if (p == null) continue;
                p.getInventory().clear();
                p.closeInventory();
                if (l != null) p.teleport(l);
                p.setGameMode(GameMode.CREATIVE);
                p.setAllowFlight(true);
            }
        }
        titre("Thème : " + theme, "Vous avez " + minutes + " min !");
        annoncer("§6Thème : §f" + theme + "§6. Construisez dans votre zone (" + minutes + " min).");
    }

    private void finConstruction() {
        for (int i = 0; i < equipes.size(); i++) plugin.arene().membres(colonne, i, List.of());
        annoncer("§6Temps écoulé ! §7Place aux votes.");
        phase = Phase.VOTES;
        terrain = -1;
        terrainSuivant();
    }

    // --- Votes ---

    private void terrainSuivant() {
        terrain++;
        if (terrain >= equipes.size()) {
            resultats();
            return;
        }
        compteur = duree = duree("vote-terrain", 30);
        notes.put(terrain, new HashMap<>());
        Location l = plugin.arene().apparitionBoite(colonne, terrain);
        List<UUID> equipe = equipes.get(terrain);
        titre(noms(equipe), "Note cette construction !");
        for (Player p : enLigne()) {
            p.closeInventory();
            if (l != null) p.teleport(l);
            p.getInventory().clear();
            if (equipe.contains(p.getUniqueId())) {
                p.sendMessage("§eC'est votre construction : vous ne votez pas pour elle.");
            } else {
                for (int n = 1; n <= 5; n++) p.getInventory().setItem(1 + n, jeu.objets().note(n));
            }
        }
    }

    void noter(Player joueur, int note) {
        if (phase != Phase.VOTES || terrain < 0 || terrain >= equipes.size()) return;
        if (equipes.get(terrain).contains(joueur.getUniqueId())) return;
        notes.get(terrain).put(joueur.getUniqueId(), note);
        joueur.sendMessage("§aTu as donné " + note + "/5. §7(Tu peux changer ta note jusqu'au terrain suivant.)");
        Set<UUID> attendus = enLigneIds();
        attendus.removeAll(equipes.get(terrain));
        if (notes.get(terrain).keySet().containsAll(attendus)) raccourcir();
    }

    // --- Résultats ---

    private record Score(int equipe, int points, int votes) {
        double moyenne() {
            return votes == 0 ? 0 : points / (double) votes;
        }
    }

    private void resultats() {
        phase = Phase.RESULTATS;
        compteur = duree = duree("resultats", 15);
        List<Score> scores = new ArrayList<>();
        for (int i = 0; i < equipes.size(); i++) {
            int total = 0, nb = 0;
            for (int n : notes.getOrDefault(i, Map.of()).values()) {
                total += n;
                nb++;
            }
            scores.add(new Score(i, total, nb));
        }
        scores.sort((a, b) -> a.points() != b.points() ? Integer.compare(b.points(), a.points())
                : Double.compare(b.moyenne(), a.moyenne()));
        terrain = scores.get(0).equipe();
        annoncer("§6§l--- Résultats · " + theme + " ---");
        for (int r = 0; r < scores.size(); r++) {
            Score s = scores.get(r);
            annoncer("§e" + (r + 1) + ". §f" + noms(equipes.get(s.equipe())) + " §7- " + s.points() + " points ("
                    + String.format(Locale.ROOT, "%.1f", s.moyenne()) + "/5, " + s.votes() + " vote" + (s.votes() > 1 ? "s" : "") + ")");
        }
        titre("Victoire : " + noms(equipes.get(terrain)), scores.get(0).points() + " points");
        // 0.4.0 : objet « Rejouer » au retour sur kal-games (réglages déposés maintenant, avant le renvoi).
        jeu.publierRejouer(this);
        Location l = plugin.arene().apparitionBoite(colonne, terrain);
        for (Player p : enLigne()) {
            p.getInventory().clear();
            if (l != null) p.teleport(l);
        }
    }

    /** Fin (normale ou partie vide) : retour sur kal-games, zones vidées, colonne libérée. */
    void terminer() {
        if (phase == Phase.FINIE) return;
        boolean jouee = phase != Phase.ATTENTE;
        phase = Phase.FINIE;
        for (Player p : enLigne()) {
            jeu.sortir(p);
            jeu.renvoyer(p);
        }
        joueurs.clear();
        if (!jouee) {
            jeu.liberer(this);
            return;
        }
        for (int i = 0; i < equipes.size(); i++) {
            plugin.arene().membres(colonne, i, List.of());
            plugin.arene().viderZone(colonne, i, null);
        }
        plugin.travaux().apres(() -> jeu.liberer(this));
    }

    /** Boîte où le joueur doit rester en ce moment (-1 = aucune contrainte). */
    int boiteImposee(UUID joueur) {
        return switch (phase) {
            case CONSTRUCTION -> equipeDe(joueur);
            case VOTES, RESULTATS -> terrain;
            default -> -1;
        };
    }
}
