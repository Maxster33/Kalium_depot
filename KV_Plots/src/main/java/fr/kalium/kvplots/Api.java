package fr.kalium.kvplots;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.kalium.kvplots.api.KanvasPlots;
import fr.kalium.kvplots.api.Taille;

/** Implémentation de l'API publiée pour KV_Menu et les autres plugins de Kanvas. */
final class Api implements KanvasPlots {

    private final KVPlots plugin;

    Api(KVPlots plugin) {
        this.plugin = plugin;
    }

    private static PlotInfo vue(Plot p) {
        return p == null ? null : new PlotInfo(p.id, p.taille, p.createur, List.copyOf(p.editeurs),
                p.etat == Plot.Etat.VALIDE, p.chantier != Plot.Chantier.AUCUN, p.points(), p.votes.size(), p.moyenne(),
                p.titre, p.description);
    }

    private Plot trouver(int id) throws Refus {
        Plot p = plugin.plots().parId(id);
        if (p == null) throw new Refus("Ce plot n'existe plus.");
        return p;
    }

    @Override
    public World monde() {
        return plugin.monde();
    }

    @Override
    public List<PlotInfo> tousLesPlots() {
        return plugin.plots().tous().stream().sorted(java.util.Comparator.comparingInt(p -> p.id)).map(Api::vue).toList();
    }

    @Override
    public List<PlotInfo> plotsDuCreateur(UUID joueur) {
        return plugin.plots().duCreateur(joueur).stream().map(Api::vue).toList();
    }

    @Override
    public PlotInfo hasardANoter(UUID joueur) {
        return vue(plugin.hasardANoter(joueur));
    }

    @Override
    public void visiter(Player joueur, int id) throws Refus {
        Plot p = trouver(id);
        if (p.chantier == Plot.Chantier.SUPPRESSION) throw new Refus("Ce plot est en cours de suppression.");
        plugin.teleporter(joueur, p);
    }

    @Override
    public void definirLore(Player joueur, int id, String titre, String description) throws Refus {
        plugin.definirLore(joueur, trouver(id), titre, description);
    }

    @Override
    public List<PlotInfo> plotsDe(UUID joueur) {
        return plugin.plots().duJoueur(joueur).stream().map(Api::vue).toList();
    }

    @Override
    public PlotInfo plotEn(Location lieu) {
        if (lieu == null || !plugin.monde().equals(lieu.getWorld())) return null;
        return vue(plugin.plotEn(lieu.getBlockX(), lieu.getBlockZ()));
    }

    @Override
    public PlotInfo plot(int id) {
        return vue(plugin.plots().parId(id));
    }

    @Override
    public int occupes(UUID joueur, Taille taille) {
        return plugin.plots().compter(joueur, taille);
    }

    @Override
    public int places(UUID joueur, Taille taille) {
        return plugin.places(joueur, taille);
    }

    @Override
    public PlotInfo reserver(Player joueur, Taille taille) throws Refus {
        return vue(plugin.reserver(joueur, taille));
    }

    @Override
    public void teleporter(Player joueur, int id) throws Refus {
        Plot p = trouver(id);
        if (!p.peutConstruire(joueur.getUniqueId())) throw new Refus("Ce n'est pas l'un de tes plots.");
        plugin.teleporter(joueur, p);
    }

    @Override
    public void remettreAZero(Player joueur, int id) throws Refus {
        plugin.remettreAZero(joueur, trouver(id));
    }

    @Override
    public void supprimer(Player joueur, int id) throws Refus {
        plugin.supprimer(joueur, trouver(id));
    }

    @Override
    public void valider(Player joueur, int id) throws Refus {
        plugin.valider(joueur, trouver(id));
    }

    @Override
    public void rouvrir(Player joueur, int id) throws Refus {
        plugin.rouvrir(joueur, trouver(id));
    }

    @Override
    public boolean peutVoter(UUID joueur, int id) {
        Plot p = plugin.plots().parId(id);
        return p != null && p.votable(joueur);
    }

    @Override
    public int note(UUID joueur, int id) {
        Plot p = plugin.plots().parId(id);
        Plot.Vote v = p == null ? null : p.votes.get(joueur);
        return v == null ? 0 : v.note();
    }

    @Override
    public void voter(Player joueur, int id, int note) throws Refus {
        Plot p = trouver(id);
        plugin.voter(joueur, p, note);
        plugin.modeVote().verifier(joueur);
    }

    @Override
    public boolean enVote(Player joueur) {
        return plugin.modeVote().enVote(joueur);
    }

    @Override
    public void ajouterEditeur(Player createur, int id, UUID editeur) throws Refus {
        plugin.ajouterEditeur(createur, trouver(id), editeur);
    }

    @Override
    public void retirerEditeur(Player createur, int id, UUID editeur) throws Refus {
        plugin.retirerEditeur(createur, trouver(id), editeur);
    }
}
