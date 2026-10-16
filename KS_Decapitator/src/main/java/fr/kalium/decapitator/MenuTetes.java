package fr.kalium.decapitator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * /tetes : toutes les têtes, 45 par page. Rien ne peut être déposé dans le menu.
 *
 * 1.2.0 (LeKiwi06, 11/10/2026) : ouvert à tous les joueurs, pour savoir quelles têtes ils ont déjà obtenues (voir
 * Collection). Une tête déjà eue est montrée telle quelle, avec « Obtenue » ; une tête jamais eue par un bloc de barrière
 * à son nom, avec « Pas encore obtenue ». Le titre donne le compte du joueur.
 * Opérateurs (ks.decapitator.tetes, choix de LeKiwi06 du 30/09/2026 pour valider les textures en jeu) : toutes les
 * têtes sont montrées ; clic sur une tête = une tête dans l'inventaire (le reste tombe au sol s'il est plein).
 * 1.3.0 (LeKiwi06, 16/10/2026) : têtes de joueurs (si actives, voir TetesDeJoueurs). Dès sa première tête de joueur
 * obtenue, le joueur a des pages de plus, après celles des mobs : tous les joueurs du serveur, relus à chaque ouverture
 * (un nouveau joueur y apparaît aussitôt). Tête obtenue = la tête du joueur ; manquante = une étoile du Nether à son
 * nom. Les opérateurs voient ces pages en permanence.
 */
final class MenuTetes implements Listener {

    private static final String PERMISSION = "ks.decapitator.tetes";
    private static final int PAR_PAGE = 45;
    private static final int PRECEDENTE = 45;
    private static final int SUIVANTE = 53;

    /** Page affichée, gardée par l'inventaire du menu ; joueurs : ceux de la page (vide sur une page de mobs). */
    private record Page(int numero, boolean pageJoueurs, List<UUID> joueurs) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    static void ouvrir(Player joueur, int numero) {
        List<String> ids = KSDecapitator.ids();
        KSDecapitator.collection().regarder(joueur);
        Set<String> obtenues = KSDecapitator.collection().de(joueur.getUniqueId());
        boolean toutes = joueur.hasPermission(PERMISSION);
        long compte = ids.stream().filter(obtenues::contains).count();
        int pagesMobs = Math.max(1, (ids.size() + PAR_PAGE - 1) / PAR_PAGE);
        TetesDeJoueurs tetesDeJoueurs = KSDecapitator.tetesDeJoueurs();
        boolean voitJoueurs = tetesDeJoueurs != null
                && (toutes || obtenues.stream().anyMatch(id -> id.startsWith(TetesDeJoueurs.PREFIXE)));
        List<OfflinePlayer> joueurs = voitJoueurs ? tetesDeJoueurs.joueurs() : List.of();
        int pages = pagesMobs + (joueurs.size() + PAR_PAGE - 1) / PAR_PAGE;
        int page = Math.max(0, Math.min(numero, pages - 1));
        Inventory menu;
        if (page < pagesMobs) {
            menu = Bukkit.createInventory(new Page(page, false, List.of()), 54,
                    Component.text("Têtes " + compte + "/" + ids.size() + " - page " + (page + 1) + "/" + pages));
            for (int i = 0; i < PAR_PAGE && page * PAR_PAGE + i < ids.size(); i++) {
                String id = ids.get(page * PAR_PAGE + i);
                boolean obtenue = obtenues.contains(id);
                menu.setItem(i, obtenue || toutes ? tete(id, obtenue) : manquante(id));
            }
        } else {
            long trouves = joueurs.stream().filter(j -> obtenues.contains(TetesDeJoueurs.PREFIXE + j.getUniqueId())).count();
            int debut = (page - pagesMobs) * PAR_PAGE;
            List<OfflinePlayer> surPage = joueurs.subList(debut, Math.min(debut + PAR_PAGE, joueurs.size()));
            menu = Bukkit.createInventory(new Page(page, true, surPage.stream().map(OfflinePlayer::getUniqueId).toList()), 54,
                    Component.text("Joueurs " + trouves + "/" + joueurs.size() + " - page " + (page + 1) + "/" + pages));
            for (int i = 0; i < surPage.size(); i++) {
                OfflinePlayer cible = surPage.get(i);
                boolean obtenue = obtenues.contains(TetesDeJoueurs.PREFIXE + cible.getUniqueId());
                menu.setItem(i, obtenue || toutes ? avecEtat(tetesDeJoueurs.tete(cible), obtenue) : joueurManquant(cible));
            }
        }
        if (page > 0) {
            menu.setItem(PRECEDENTE, bouton("Page précédente"));
        }
        if (page < pages - 1) {
            menu.setItem(SUIVANTE, bouton("Page suivante"));
        }
        joueur.openInventory(menu);
    }

    private static Component etat(boolean obtenue) {
        return Component.text(obtenue ? "Obtenue" : "Pas encore obtenue", obtenue ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false);
    }

    /** La tête, avec son état en dernière ligne de description. */
    private static ItemStack tete(String id, boolean obtenue) {
        return avecEtat(KSDecapitator.creerTete(id), obtenue);
    }

    private static ItemStack avecEtat(ItemStack tete, boolean obtenue) {
        ItemMeta meta = tete.getItemMeta();
        List<Component> description = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        description.add(etat(obtenue));
        meta.lore(description);
        tete.setItemMeta(meta);
        return tete;
    }

    /** Tête jamais eue : un bloc de barrière à son nom (vitre grise avant la 1.3.0, trop peu visible selon LeKiwi06). */
    private static ItemStack manquante(String id) {
        ItemStack vitre = new ItemStack(Material.BARRIER);
        ItemMeta meta = vitre.getItemMeta();
        meta.displayName(Component.text(KSDecapitator.nom(id), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(etat(false)));
        vitre.setItemMeta(meta);
        return vitre;
    }

    /** Tête de joueur jamais eue : une étoile du Nether à son nom. */
    private static ItemStack joueurManquant(OfflinePlayer joueur) {
        ItemStack etoile = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = etoile.getItemMeta();
        meta.displayName(Component.text("Tête de " + joueur.getName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(etat(false)));
        etoile.setItemMeta(meta);
        return etoile;
    }

    private static ItemStack bouton(String texte) {
        ItemStack fleche = new ItemStack(Material.ARROW);
        ItemMeta meta = fleche.getItemMeta();
        meta.displayName(Component.text(texte, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        fleche.setItemMeta(meta);
        return fleche;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Page page)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player joueur) || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int case_ = event.getRawSlot();
        List<String> ids = KSDecapitator.ids();
        int numero = page.numero() * PAR_PAGE + case_;
        if (case_ == PRECEDENTE && page.numero() > 0) {
            ouvrir(joueur, page.numero() - 1);
        } else if (case_ == SUIVANTE) {
            ouvrir(joueur, page.numero() + 1);
        } else if (case_ < PAR_PAGE && joueur.hasPermission(PERMISSION)) {
            ItemStack tete = null;
            if (page.pageJoueurs()) {
                if (case_ < page.joueurs().size() && KSDecapitator.tetesDeJoueurs() != null) {
                    tete = KSDecapitator.tetesDeJoueurs().tete(Bukkit.getOfflinePlayer(page.joueurs().get(case_)));
                }
            } else if (numero < ids.size()) {
                tete = KSDecapitator.creerTete(ids.get(numero));
            }
            if (tete != null) {
                for (ItemStack reste : joueur.getInventory().addItem(tete).values()) {
                    joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Page) {
            event.setCancelled(true);
        }
    }
}
