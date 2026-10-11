package fr.kalium.economy;

import fr.kalium.menu.api.Contenant;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 1.7.0 (LeKiwi06, 11/10/2026) : « j'aimerai que le wandering trader propose aussi 10 échanges, les moins cher ont
 * echangeable jusqu'a 10 fois, puis plus ça vaut moins on peux échanger, un item comme le diamant serait échangeable 5
 * fois par exemple, un item comme le totem ne le serait qu'en 1 exemplaire : les règles de trades sont les mêmes que pour
 * les rachats de la semaine concernant les items tirés, leurs quantité, et leurs prix ». Ses réponses : le marchand
 * achète (le joueur donne l'objet et reçoit des émeraudes du score) ; menu en coffre ; un tirage par marchand, stock
 * commun à tous les joueurs.
 *
 * - Clic droit sur un marchand ambulant : menu de ses 10 rachats (tirés à son premier clic comme les rachats de la
 *   semaine : 10 familles au hasard, un objet par famille, prix du barème à plus ou moins 25 %, lot de la gamme du prix).
 * - Chaque rachat sert un nombre limité de fois, pour tous les joueurs ensemble, selon la gamme du prix à l'unité :
 *   10, 10, 5, 2, 1 fois (moins de 0,1 ; 0,1 à 1 ; 1 à 10 ; 10 à 100 ; 100 et plus ; config.yml : marchand.echanges).
 * - Les rachats et ce qu'il en reste sont gardés sur le marchand lui-même : ils disparaissent avec lui, et survivent à
 *   un redémarrage tant qu'il est là. Pas de quota du jour ici : le stock du marchand fait la limite.
 * - Un marchand qui porte l'étiquette ks_pnj (PNJ à échanges de KS_Villageois) n'est pas concerné.
 */
final class Marchand implements Listener {

    private static final String PNJ = "ks_pnj";
    private static final double DISTANCE_MAX = 8;
    private static final List<Integer> ECHANGES = List.of(10, 10, 5, 2, 1);
    /** Cases des 10 rachats : deux rangées de 5, centrées (comme les rachats de la semaine). */
    private static final int[] PLACES = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

    /** Un rachat du marchand et le nombre de fois qu'il peut encore servir. */
    private record Echange(Rachats.Offre offre, int reste) {
    }

    private final KSEconomy plugin;
    private final Lang lang;
    private final Rachats rachats;
    private final NamespacedKey cle;

    Marchand(KSEconomy plugin, Rachats rachats) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.rachats = rachats;
        this.cle = new NamespacedKey(plugin, "marchand");
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    /** Nombre de fois qu'un rachat peut servir, selon la gamme de son prix à l'unité. */
    private int fois(Rachats.Offre offre) {
        List<Integer> liste = plugin.getConfig().getIntegerList("marchand.echanges");
        if (liste.isEmpty()) {
            liste = ECHANGES;
        }
        return Math.max(1, liste.get(Math.min(Rachats.gamme(offre.unite()), liste.size() - 1)));
    }

    // ------------------------------------------------------------------ rachats gardés sur le marchand

    /** Les rachats de ce marchand, tirés à la première lecture. */
    private List<Echange> lire(WanderingTrader marchand) {
        List<Echange> echanges = new ArrayList<>();
        String texte = marchand.getPersistentDataContainer().get(cle, PersistentDataType.STRING);
        if (texte == null) {
            for (Rachats.Offre offre : rachats.tirage(Math.max(1, plugin.getConfig().getInt("marchand.objets", 10)))) {
                echanges.add(new Echange(offre, fois(offre)));
            }
            ecrire(marchand, echanges);
            StringBuilder journal = new StringBuilder("Marchand ambulant " + marchand.getUniqueId() + " :");
            for (Echange e : echanges) {
                journal.append(' ').append(e.offre().quantite()).append(" x ").append(e.offre().entree().id())
                        .append(" = ").append(e.offre().points()).append(" (").append(e.reste()).append(" fois) ;");
            }
            plugin.getLogger().info(journal.toString());
            return echanges;
        }
        for (String ligne : texte.split("\n")) {
            String[] c = ligne.split(";");
            try {
                Rachats.Offre offre = c.length == 5 ? rachats.offre(c[0], Double.parseDouble(c[1]), Integer.parseInt(c[2]),
                        Long.parseLong(c[3])) : null;
                if (offre != null) {
                    echanges.add(new Echange(offre, Integer.parseInt(c[4])));
                }
            } catch (NumberFormatException e) {
                // ligne illisible : rachat ignoré
            }
        }
        return echanges;
    }

    private void ecrire(WanderingTrader marchand, List<Echange> echanges) {
        StringBuilder texte = new StringBuilder();
        for (Echange e : echanges) {
            texte.append(texte.isEmpty() ? "" : "\n").append(e.offre().entree().id()).append(';').append(e.offre().unite())
                    .append(';').append(e.offre().quantite()).append(';').append(e.offre().points()).append(';')
                    .append(e.reste());
        }
        marchand.getPersistentDataContainer().set(cle, PersistentDataType.STRING, texte.toString());
    }

    /** Le marchand, s'il est toujours là et à portée du joueur. */
    private WanderingTrader present(Player joueur, UUID id) {
        Entity entite = plugin.getServer().getEntity(id);
        return entite instanceof WanderingTrader marchand && marchand.isValid()
                && marchand.getWorld().equals(joueur.getWorld())
                && marchand.getLocation().distance(joueur.getLocation()) <= DISTANCE_MAX ? marchand : null;
    }

    // ------------------------------------------------------------------ menu

    @EventHandler(priority = EventPriority.HIGH)
    public void onClic(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof WanderingTrader marchand) || marchand.getScoreboardTags().contains(PNJ)) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) {
            ouvrir(event.getPlayer(), marchand.getUniqueId());
            lang.saveIfNeeded();
        }
    }

    private void ouvrir(Player joueur, UUID id) {
        WanderingTrader marchand = present(joueur, id);
        if (marchand == null) {
            Contenant.fermer(joueur);
            plugin.menus().message(joueur, t("marchand.parti", "<red>Le marchand ambulant n'est plus là."), true);
            return;
        }
        List<Echange> echanges = lire(marchand);
        Contenant c = plugin.menus().menu(joueur, 6, t("marchand.titre", "<dark_gray>Marchand ambulant"), "marchand");
        c.poser(4, Menus.objet(Material.EMERALD, t("marchand.info", "<gold>Marchand ambulant"),
                t("marchand.intro", "<white>Il rachète ces objets. Chaque rachat ne sert qu'un nombre limité de fois, "
                        + "pour tous les joueurs ensemble."),
                t("eco.solde", "<white>Solde : <green><solde>", "solde",
                        KSEconomy.points(KSEconomy.solde(joueur.getUniqueId())))), null);
        for (int i = 0; i < echanges.size() && i < PLACES.length; i++) {
            Echange e = echanges.get(i);
            Rachats.Offre o = e.offre();
            int possede = Rachats.compter(joueur, o.entree());
            int numero = i;
            c.poser(PLACES[i], Contenant.objet(rachats.icone(o),
                    t("rachats.objet", "<yellow><quantite> x <objet>", "quantite", o.quantite(), "objet",
                            rachats.nom(o.entree())),
                    List.of(t("rachats.objet-prix", "<gray>Racheté <green><points></green> le lot", "points",
                                    KSEconomy.points(o.points())),
                            e.reste() > 0 ? t("marchand.reste", "<gray>Encore <white><n></white> fois", "n", e.reste())
                                    : t("marchand.epuise", "<red>Épuisé"),
                            t("rachats.bouton-info", "<gray>Tu en as <white><n></white> (<lots> lot(s))", "n", possede,
                                    "lots", possede / o.quantite()),
                            t("marchand.clic", "<dark_gray>Clic : vendre 1 lot"))), p -> vendre(p, id, numero, o));
        }
        plugin.menus().sortie(c, null);
        c.ouvrir(joueur);
    }

    private void vendre(Player joueur, UUID id, int numero, Rachats.Offre attendue) {
        WanderingTrader marchand = present(joueur, id);
        if (marchand == null) {
            Contenant.fermer(joueur);
            plugin.menus().message(joueur, t("marchand.parti", "<red>Le marchand ambulant n'est plus là."), true);
            lang.saveIfNeeded();
            return;
        }
        List<Echange> echanges = lire(marchand);
        Echange e = numero < echanges.size() ? echanges.get(numero) : null;
        if (e == null || !e.offre().entree().id().equals(attendue.entree().id())) {
            ouvrir(joueur, id);
            return;
        }
        Rachats.Offre o = e.offre();
        if (e.reste() <= 0) {
            ouvrir(joueur, id);
            plugin.menus().message(joueur, t("marchand.epuise-message", "<red>Le marchand n'en veut plus."), true);
        } else if (Rachats.compter(joueur, o.entree()) < o.quantite()) {
            plugin.menus().message(joueur, t("rachats.manque", "<red>Il te faut <quantite> x <objet> dans ton inventaire.",
                    "quantite", o.quantite(), "objet", rachats.nom(o.entree())), true);
        } else {
            Rachats.retirer(joueur, o.entree(), o.quantite());
            KSEconomy.crediter(joueur.getUniqueId(), o.points());
            echanges.set(numero, new Echange(o, e.reste() - 1));
            ecrire(marchand, echanges);
            plugin.getLogger().info(joueur.getName() + " vend " + o.quantite() + " x " + o.entree().id() + " pour "
                    + o.points() + " points (marchand ambulant).");
            ouvrir(joueur, id);
            plugin.menus().message(joueur, t("rachats.vendu", "<white>Vendu : <yellow><nombre> x <objet></yellow> pour "
                    + "<green><points></green>.", "nombre", o.quantite(), "objet", rachats.nom(o.entree()), "points",
                    KSEconomy.points(o.points())), false);
        }
        lang.saveIfNeeded();
    }
}
