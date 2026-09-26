package fr.kalium.kvplots.api;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Accès aux plots de Kanvas pour les autres plugins (KV_Menu...). Publié par KV_Plots dans le registre de services de
 * Paper :
 * <pre>
 * KanvasPlots plots = getServer().getServicesManager().load(KanvasPlots.class);
 * </pre>
 * Les actions vérifient elles-mêmes les droits du joueur et lèvent {@link Refus} avec le message à lui afficher.
 */
public interface KanvasPlots {

    /** Vue d'un plot (copie : ne change pas si le plot change ensuite). */
    record PlotInfo(int id, Taille taille, UUID createur, List<UUID> editeurs, boolean valide, boolean enPreparation,
                    int points, int votes, double moyenne, String titre, String description, int concours) {}

    /** Phases d'un concours de build. */
    enum PhaseConcours { EN_COURS, VOTES, TERMINE, ANNULE }

    /** Concours : fin = fin de la construction ; finVotes = fin des votes (0 avant la phase de votes). */
    record ConcoursInfo(int id, String theme, Taille taille, PhaseConcours phase, long debut, long fin, long dureeVotes,
                        long finVotes, int participants) {}

    /** Signalement d'un plot ; classé = traité par le staff (action faite). */
    record SignalementInfo(int id, int plot, UUID auteur, List<String> raisons, String autre, long date, boolean classe,
                           UUID traitePar, String action) {}

    /** Longueurs maximales (caractères visibles ; codes couleur « & » non comptés). */
    int TITRE_MAX = 32, DESCRIPTION_MAX = 200;

    /** Action refusée ; le message est prêt à être montré au joueur. */
    final class Refus extends Exception {
        public Refus(String message) {
            super(message, null, false, false);
        }
    }

    /** Monde des plots. */
    World monde();

    /** Tous les plots réservés, triés par numéro. */
    List<PlotInfo> tousLesPlots();

    /** Plots créés par ce joueur, triés par numéro. */
    List<PlotInfo> plotsDuCreateur(UUID joueur);

    /** Plot validé au hasard que le joueur peut noter et n'a pas encore noté, ou null. */
    PlotInfo hasardANoter(UUID joueur);

    /** Téléporte le joueur au bord de n'importe quel plot (visite). */
    void visiter(Player joueur, int id) throws Refus;

    /** Titre et description (vides = retirés) ; créateur seulement, à tout moment. */
    void definirLore(Player joueur, int id, String titre, String description) throws Refus;

    /** Plots dont le joueur est créateur ou éditeur, triés par numéro. */
    List<PlotInfo> plotsDe(UUID joueur);

    /** Plot à cet endroit (routes intérieures d'un grand plot comprises), ou null. */
    PlotInfo plotEn(Location lieu);

    PlotInfo plot(int id);

    /** Plots de cette taille que le joueur a créés. */
    int occupes(UUID joueur, Taille taille);

    /** Plots de cette taille qu'un joueur peut avoir en même temps. */
    int places(UUID joueur, Taille taille);

    /**
     * Réserve un plot : le plot libre où se tient le joueur, sinon le plus proche du centre ; téléporte le joueur
     * (après la préparation pour un grand plot).
     */
    PlotInfo reserver(Player joueur, Taille taille) throws Refus;

    /** Téléporte le joueur au bord de l'un de ses plots (créateur ou éditeur). */
    void teleporter(Player joueur, int id) throws Refus;

    /**
     * Remet le terrain du plot à l'état vierge (le plot reste réservé). Créateur, sauf plot validé ; staff
     * (kvplots.admin) toujours. Travaux étalés sur plusieurs ticks.
     */
    void remettreAZero(Player joueur, int id) throws Refus;

    /** Efface le plot et libère la place (un grand plot redevient 4 plots moyens). Mêmes droits. */
    void supprimer(Player joueur, int id) throws Refus;

    /** Fige le plot (créateur seulement) : il devient votable et libère sa place. */
    void valider(Player joueur, int id) throws Refus;

    /** Rouvre un plot validé (créateur seulement, avec une place libre de la même taille) ; les votes sont gardés. */
    void rouvrir(Player joueur, int id) throws Refus;

    /** Le joueur peut-il noter ce plot (validé, et ni créateur ni éditeur, même ancien) ? */
    boolean peutVoter(UUID joueur, int id);

    /** Note donnée par le joueur à ce plot (1 à 5), ou 0 s'il ne l'a pas noté. */
    int note(UUID joueur, int id);

    /** Vote de 1 à 5 ; remplace l'ancien vote du joueur sur ce plot. */
    void voter(Player joueur, int id, int note) throws Refus;

    /** Le joueur est-il en train de noter un plot (inventaire remplacé par les terracottas) ? */
    boolean enVote(Player joueur);

    // --- Concours de build ---

    /** Concours en cours ou en votes, ou null. */
    ConcoursInfo concoursActuel();

    /** Concours terminés, du plus récent au plus ancien. */
    List<ConcoursInfo> anciensConcours();

    ConcoursInfo concours(int id);

    /** Plots d'un concours ; classés (points puis moyenne) une fois les votes ouverts, sinon par numéro. */
    List<PlotInfo> plotsDuConcours(int id);

    /** Plot du joueur (créateur) dans le concours actif, ou null. */
    PlotInfo participation(UUID joueur);

    /** Participer : un plot en plus (hors limites) de la taille du concours, téléportation. */
    PlotInfo participer(Player joueur) throws Refus;

    /** Annuler sa participation pendant le concours : le plot est supprimé. */
    void annulerParticipation(Player joueur) throws Refus;

    /** « 2 j 3 h 10 min ». */
    String duree(long millisecondes);

    /** Staff : lance un concours (durées en millisecondes). */
    ConcoursInfo lancerConcours(Player staff, String theme, Taille taille, long duree, long dureeVotes) throws Refus;

    /** Staff : thème (vide = inchangé), fin dans « duree » (pendant la construction), durée des votes (ou fin des votes
     *  pendant les votes) ; une durée de 0 = inchangée. */
    void modifierConcours(Player staff, String theme, long duree, long dureeVotes) throws Refus;

    /** Staff : fin de la construction maintenant (les votes s'ouvrent). */
    void terminerConcours(Player staff) throws Refus;

    /** Staff : fin des votes maintenant (classement). */
    void cloreVotes(Player staff) throws Refus;

    /** Staff : annule le concours actif (ses plots sont supprimés). */
    void annulerConcours(Player staff) throws Refus;

    /** Staff : exclut un plot du concours actif (il est supprimé). */
    void exclureDuConcours(Player staff, int id) throws Refus;

    // --- Signalements ---

    /** Raisons à cocher (plus une case « Autre » avec texte libre). */
    List<String> raisonsSignalement();

    /** Le joueur peut-il signaler ce plot (ni créateur ni éditeur, même ancien) ? */
    boolean peutSignaler(UUID joueur, int id);

    /** Signale un plot (un seul signalement non traité par joueur et par plot) ; prévient le staff connecté. */
    SignalementInfo signaler(Player joueur, int id, List<String> raisons, String autre) throws Refus;

    /** Objet de signalement (poudre de blaze) donné en mode vote. */
    boolean estObjetSignalement(ItemStack item);

    /** Plot que le joueur est en train de noter (0 = pas en mode vote). */
    int plotEnVote(Player joueur);

    /** Signalements non classés (false) ou classés (true), du plus récent au plus ancien. */
    List<SignalementInfo> signalements(boolean classes);

    SignalementInfo signalement(int id);

    /** Staff : classe le signalement, avec l'action faite (texte libre, ex. « aucune », « plot dévalidé »). */
    void classer(Player staff, int signalement, String action) throws Refus;

    /** Staff : dévalide un plot (il repasse en travaux, votes gardés). */
    void devalider(Player staff, int id) throws Refus;

    /** Ajoute un éditeur (seul le créateur du plot peut le faire). */
    void ajouterEditeur(Player createur, int id, UUID editeur) throws Refus;

    /** Retire un éditeur (seul le créateur du plot peut le faire). */
    void retirerEditeur(Player createur, int id, UUID editeur) throws Refus;
}
