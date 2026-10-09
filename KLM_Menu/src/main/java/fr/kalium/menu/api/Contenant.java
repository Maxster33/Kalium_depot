package fr.kalium.menu.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 2.11.0 (LeKiwi06, 09/10/2026 : « j'aimerais rendre les interfaces plus jolies, et selon moi ça passe par davantage
 * d'interfaces de type contenant quand c'est possible au lieu des boutons ») : menu de type contenant, pour tous les
 * plugins (d'abord écrit dans KS_RewardsGUI 1.4.0 pour /rewards, modèle validé en jeu par LeKiwi06).
 *
 * Un coffre de 1 à 6 rangées ; chaque case porte un objet (son nom et ses lignes de description disent ce que fait le
 * clic) et, au besoin, une action. Rien ne se prend ni ne se dépose : KLM_Menu annule tous les clics d'un tel menu et
 * déclenche l'action de la case cliquée (avec un son). Le même menu peut être vidé et rempli à nouveau sans se refermer
 * (le curseur ne bouge pas) : {@link #ouvert}.
 *
 * Modèle d'un menu : 6 rangées ; les objets cliquables en haut (45 par page) ; en bas, une barre d'actions sur fond de
 * vitres ({@link #barre}) : page précédente à gauche, page suivante à droite, action principale au centre, « Fermer »
 * ou « Retour » à côté. Les messages courts vont au-dessus de la barre d'objets du joueur ({@link #message}), pas dans
 * le tchat. Une saisie (texte, nombre) reste une fenêtre de Gui, ouverte depuis une case.
 *
 * Une ligne de description ne passe pas à la ligne toute seule : la couper à environ 35 caractères ({@link #lignes}).
 *
 * Pendant un clic, le jeu interdit d'ouvrir ou de fermer une fenêtre : {@link #ouvrir}, {@link #fermer} et
 * {@link #saisie} attendent d'eux-mêmes la fin du clic ; pour toute autre fenêtre (un vrai coffre, par exemple),
 * passer par {@link #apres}.
 */
public final class Contenant implements InventoryHolder {

    /** Cases par page au-dessus de la barre d'actions d'un menu de 6 rangées. */
    public static final int PAR_PAGE = 45;

    private final Inventory inventaire;
    private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
    private final Object marque;
    /** Vrai pendant l'action d'un clic (voir {@link #apres}). */
    private static boolean enClic;

    /**
     * @param rangees 1 à 6
     * @param marque  ce que le menu affiche (pour le reconnaître et le remplir à nouveau au lieu d'en ouvrir un autre)
     */
    public Contenant(int rangees, Component titre, Object marque) {
        this.inventaire = Bukkit.createInventory(this, Math.max(1, Math.min(6, rangees)) * 9, titre);
        this.marque = marque;
    }

    @Override
    public Inventory getInventory() {
        return inventaire;
    }

    /** Le menu de cette marque déjà ouvert par le joueur, ou null. */
    public static Contenant ouvert(Player joueur, Object marque) {
        return joueur.getOpenInventory().getTopInventory().getHolder() instanceof Contenant c && marque.equals(c.marque)
                ? c : null;
    }

    /** Le menu de cette marque déjà ouvert par le joueur, vidé ; sinon un nouveau menu de 6 rangées. */
    public static Contenant pour(Player joueur, Component titre, Object marque) {
        return pour(joueur, 6, titre, marque);
    }

    /** Comme {@link #pour(Player, Component, Object)}, pour un menu de 1 à 6 rangées (petit menu, confirmation). */
    public static Contenant pour(Player joueur, int rangees, Component titre, Object marque) {
        Contenant ouvert = ouvert(joueur, marque);
        if (ouvert == null || ouvert.taille() != Math.max(1, Math.min(6, rangees)) * 9) {
            return new Contenant(rangees, titre, marque);
        }
        ouvert.vider();
        return ouvert;
    }

    public void vider() {
        inventaire.clear();
        actions.clear();
    }

    /** Pose un objet dans une case, avec l'action de son clic (null : case sans action). */
    public void poser(int place, ItemStack objet, Consumer<Player> action) {
        inventaire.setItem(place, objet);
        if (action == null) {
            actions.remove(place);
        } else {
            actions.put(place, action);
        }
    }

    /** Barre d'actions : la dernière rangée remplie de vitres grises (à faire avant d'y poser les actions). */
    public void barre() {
        for (int place = inventaire.getSize() - 9; place < inventaire.getSize(); place++) {
            poser(place, decor(), null);
        }
    }

    public int taille() {
        return inventaire.getSize();
    }

    /** Case de la barre d'actions (dernière rangée) : colonne 0 à 8. Modèle : 0 et 8 pages, 3 aide, 4 action, 5 sortie. */
    public int bas(int colonne) {
        return inventaire.getSize() - 9 + colonne;
    }

    public void ouvrir(Player joueur) {
        if (joueur.getOpenInventory().getTopInventory() != inventaire) {
            apres(() -> {
                if (joueur.isOnline()) {
                    joueur.openInventory(inventaire);
                }
            });
        }
    }

    /** Ferme la fenêtre du joueur (après le clic en cours). */
    public static void fermer(Player joueur) {
        apres(joueur::closeInventory);
    }

    /** Exécute tout de suite, ou juste après le clic en cours (ouvrir ou fermer une fenêtre pendant un clic est interdit). */
    public static void apres(Runnable suite) {
        if (enClic) {
            Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(Contenant.class), suite);
        } else {
            suite.run();
        }
    }

    /**
     * Ouvre une fenêtre de saisie (Gui : texte, nombre, long texte) depuis un menu : le menu est refermé d'abord, et un
     * joueur Bedrock attend un quart de seconde (son jeu n'affiche pas un formulaire pendant qu'un coffre se referme).
     */
    public static void saisie(Player joueur, Runnable fenetre) {
        if (!(joueur.getOpenInventory().getTopInventory().getHolder() instanceof Contenant)) {
            apres(fenetre);
            return;
        }
        apres(() -> {
            joueur.closeInventory();
            if (BedrockColors.isBedrock(joueur)) {
                Bukkit.getScheduler().runTaskLater(JavaPlugin.getProvidingPlugin(Contenant.class), () -> {
                    if (joueur.isOnline()) {
                        fenetre.run();
                    }
                }, 5L);
            } else {
                fenetre.run();
            }
        });
    }

    /** Un objet de menu : nom et lignes sans italique, sans les attributs du jeu (dégâts, etc.). */
    public static ItemStack objet(Material materiau, Component nom, List<Component> lignes) {
        return objet(new ItemStack(materiau), nom, lignes);
    }

    /** Habille un objet existant (tête de joueur, objet vendu...) en objet de menu ; ses autres données sont gardées. */
    public static ItemStack objet(ItemStack modele, Component nom, List<Component> lignes) {
        ItemStack objet = modele.clone();
        ItemMeta meta = objet.getItemMeta();
        if (meta == null) {
            return objet;
        }
        if (nom != null) {
            meta.displayName(nom.decoration(TextDecoration.ITALIC, false));
        }
        List<Component> lore = new ArrayList<>();
        lignes.forEach(l -> lore.add(l.decoration(TextDecoration.ITALIC, false)));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        objet.setItemMeta(meta);
        return objet;
    }

    /** Case de décor : vitre grise sans nom. */
    public static ItemStack decor() {
        return objet(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
    }

    /**
     * Coupe un texte en lignes d'environ 35 caractères (aux espaces), toutes de la même couleur : la description d'un
     * objet ne passe pas à la ligne toute seule.
     */
    public static List<Component> lignes(String texte, net.kyori.adventure.text.format.TextColor couleur) {
        List<Component> lignes = new ArrayList<>();
        StringBuilder ligne = new StringBuilder();
        for (String mot : texte.trim().split("\\s+")) {
            if (ligne.length() > 0 && ligne.length() + 1 + mot.length() > 35) {
                lignes.add(Component.text(ligne.toString(), couleur));
                ligne.setLength(0);
            }
            if (ligne.length() > 0) {
                ligne.append(' ');
            }
            ligne.append(mot);
        }
        if (ligne.length() > 0) {
            lignes.add(Component.text(ligne.toString(), couleur));
        }
        return lignes;
    }

    /**
     * Coupe un texte déjà mis en forme (lang.yml) en lignes d'environ 35 caractères : sa première couleur est gardée
     * pour toutes les lignes (gris s'il n'en a pas), les autres mises en forme sont perdues. Un texte qui tient sur
     * une ligne est gardé tel quel.
     */
    public static List<Component> lignes(Component texte) {
        if (PlainTextComponentSerializer.plainText().serialize(texte).length() <= 35) {
            return List.of(texte);
        }
        net.kyori.adventure.text.format.TextColor couleur = null;
        Component c = texte;
        while (couleur == null && c != null) {
            couleur = c.color();
            c = c.children().isEmpty() ? null : c.children().get(0);
        }
        return lignes(PlainTextComponentSerializer.plainText().serialize(texte),
                couleur == null ? NamedTextColor.GRAY : couleur);
    }

    /** Message court au-dessus de la barre d'objets du joueur ; refus = son grave, sinon son de ramassage. */
    public static void message(Player joueur, Component texte, boolean refus) {
        joueur.sendActionBar(texte);
        joueur.playSound(joueur.getLocation(), refus ? Sound.BLOCK_NOTE_BLOCK_BASS : Sound.ENTITY_ITEM_PICKUP, 0.6f,
                refus ? 0.8f : 1f);
    }

    /** Écoute commune à tous les menus de ce type : enregistrée une seule fois, par KLM_Menu. */
    public static final class Ecoute implements Listener {

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getView().getTopInventory().getHolder() instanceof Contenant menu)) {
                return;
            }
            event.setCancelled(true);
            if (event.getClickedInventory() != menu.inventaire || !(event.getWhoClicked() instanceof Player joueur)) {
                return;
            }
            Consumer<Player> action = menu.actions.get(event.getSlot());
            if (action != null) {
                joueur.playSound(joueur.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                enClic = true;
                try {
                    action.accept(joueur);
                } finally {
                    enClic = false;
                }
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getView().getTopInventory().getHolder() instanceof Contenant) {
                event.setCancelled(true);
            }
        }
    }
}
