package fr.kalium.enclume;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import fr.kalium.rewardsgui.KSRewardsGUI;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

/**
 * 1.4.0 (LeKiwi06, 25/10/2026) : enclume en coffre. Demande : « les joueurs bedrock m'ont dit que l'enclume n'arrive pas
 * à dépasser la limite vanilla de prix d'xp, ça ne propose pas l'item dans la case de résultat » ; « un joueur bedrock ne
 * peux pas mettre de livre affilage sur son épée, car leur jeu ne reconnais pas ce livre comme un livre enchanté
 * valide » ; proposition : l'interface de fusion des coffres de récompense « s'ouvre quand on sneak+click, si on essaie
 * de mettre 2 coffres de récompenses elle fonctionne comme d'habitude, mais elle peut également marcher comme une
 * enclume normale » ; « il faut par contre veiller a ce que l'enclume s'use parfois ». Ses choix : usure de 12 % à chaque
 * validation, fusion comprise ; objets posés dans les cases, coffres de récompense compris ; menu dans KS_Enclume.
 *
 * - Accroupi + clic droit sur une enclume (neuve, ébréchée ou endommagée) : ce menu (coffre de 3 rangées) s'ouvre. Le
 *   clic sans s'accroupir ouvre toujours l'enclume du jeu (renommage, fioles de KS_FioleExp).
 * - Un clic sur un objet de son inventaire le pose (toute la pile) dans la première case libre ; un clic sur un objet
 *   posé le reprend. Rien ne passe par le curseur. Fermeture (déconnexion comprise) : les objets posés sont rendus
 *   (ceux qui ne tiennent plus dans l'inventaire tombent aux pieds du joueur).
 * - Le résultat est calculé par le serveur, avec l'enclume du jeu (règles vanilla, réduction de la 1.2.0 au-delà de
 *   50 niveaux, aucun plafond) : le jeu du joueur n'y est pour rien, Java comme Bedrock. Aperçu dans la case de droite,
 *   coût en niveaux sur « Valider ».
 * - Deux coffres de récompense non ouverts : fusion de KS_RewardsGUI (coût en points d'expérience, comme avant).
 * - Refusés : jetons et badges de KS_Jetons (fusion des badges dans l'enclume du jeu, faite par KS_Jetons à la prise).
 * - Chaque validation (hors créatif) : 12 % de chances que l'enclume s'abîme, comme dans le jeu (neuve -> ébréchée ->
 *   endommagée -> détruite ; le menu se ferme alors).
 */
final class EnclumeCoffre implements Listener {

    private static final int AIDE = 4;
    private static final int ENTREE_1 = 11;
    private static final int ENTREE_2 = 13;
    private static final int APERCU = 15;
    private static final int VALIDER = 22;
    /** Chance d'usure à chaque validation (celle du jeu, AnvilMenu.onTake). */
    private static final float USURE = 0.12f;
    /** Distance au plus entre le joueur et l'enclume pour valider (carré, en blocs). */
    private static final double PORTEE_CARRE = 8 * 8;

    /** Menu d'un joueur : l'enclume cliquée ; les objets posés sont dans les cases ENTREE_1 et ENTREE_2. */
    private static final class Fenetre implements InventoryHolder {
        final Block enclume;
        Inventory inventaire;

        Fenetre(Block enclume) {
            this.enclume = enclume;
        }

        @Override
        public Inventory getInventory() {
            return inventaire;
        }
    }

    /**
     * Ce que donnent les deux objets posés : un refus, une réparation ou un enchantement (niveaux, unités de la 2e case
     * consommées ; 0 : toute la case) ou une fusion de coffres (points d'expérience).
     */
    private record Calcul(ItemStack resultat, int niveaux, int unites, int points, Component refus) {

        static Calcul refuse(Component refus) {
            return new Calcul(null, 0, 0, 0, refus);
        }

        boolean fusion() {
            return points > 0;
        }
    }

    private final KSEnclume plugin;
    private boolean erreurSignalee;

    EnclumeCoffre(KSEnclume plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ ouverture

    /** Après les protections (claims...) : un clic refusé par un autre plugin n'ouvre rien. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block bloc = event.getClickedBlock();
        Player joueur = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || bloc == null || !Tag.ANVIL.isTagged(bloc.getType())
                || !joueur.isSneaking()) {
            return;
        }
        // Ni l'objet en main (bloc posé...) ni l'enclume du jeu ; la main secondaire n'ouvre rien de plus.
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (joueur.isOnline() && Tag.ANVIL.isTagged(bloc.getType())) {
                ouvrir(joueur, bloc);
            }
        });
    }

    private void ouvrir(Player joueur, Block bloc) {
        Fenetre fenetre = new Fenetre(bloc);
        fenetre.inventaire = Bukkit.createInventory(fenetre, 27, Component.text("Enclume", NamedTextColor.DARK_GRAY));
        remplir(joueur, fenetre);
        joueur.openInventory(fenetre.inventaire);
    }

    // ------------------------------------------------------------------ calcul

    private static boolean vide(ItemStack objet) {
        return objet == null || objet.isEmpty();
    }

    private static boolean recompenses() {
        return Bukkit.getPluginManager().isPluginEnabled("KS_RewardsGUI");
    }

    private Calcul calculer(Player joueur, ItemStack a, ItemStack b) {
        if (vide(a) || vide(b)) {
            return null;
        }
        if (recompenses() && (Recompenses.estCoffre(a) || Recompenses.estCoffre(b))) {
            return Recompenses.calculer(a, b);
        }
        if (jetonOuBadge(a) || jetonOuBadge(b)) {
            return Calcul.refuse(rouge("Badges : enclume normale (clic)."));
        }
        return calculerEnclume(joueur, a, b);
    }

    /**
     * Jetons et badges de KS_Jetons : la fusion de deux badges est faite par KS_Jetons à la prise du résultat dans
     * l'enclume du jeu (coût et badges consommés) ; ici, elle serait faite comme une fusion vanilla, avec un mauvais coût.
     */
    private static boolean jetonOuBadge(ItemStack objet) {
        return objet.hasItemMeta() && objet.getItemMeta().getPersistentDataContainer().getKeys().stream()
                .anyMatch(cle -> cle.getNamespace().equals("ks_jetons"));
    }

    /**
     * Calcul par l'enclume du jeu, dans une fenêtre d'enclume jamais ouverte (MenuType.ANVIL) : poser les objets dans ses
     * cases lance le calcul du jeu et PrepareAnvilEvent (réduction de la 1.2.0, refus de KS_RewardsGUI, KS_Jetons,
     * KS_FioleExp...). Sans champ de nom, le jeu retirerait le nom d'un objet renommé (et compterait 1 niveau) : le nom
     * est retiré avant le calcul et remis sur le résultat.
     */
    private Calcul calculerEnclume(Player joueur, ItemStack a, ItemStack b) {
        ItemStack premier = a.clone();
        Component nom = premier.getData(DataComponentTypes.CUSTOM_NAME);
        if (nom != null) {
            premier.resetData(DataComponentTypes.CUSTOM_NAME);
        }
        ItemStack resultat;
        int niveaux;
        int unites;
        plugin.calculVirtuel = true;
        try {
            AnvilView vue = MenuType.ANVIL.create(joueur);
            vue.setMaximumRepairCost(Integer.MAX_VALUE);
            AnvilInventory cases = vue.getTopInventory();
            cases.setFirstItem(premier);
            cases.setSecondItem(b.clone());
            resultat = cases.getItem(2);
            niveaux = vue.getRepairCost();
            unites = vue.getRepairItemCountCost();
        } catch (RuntimeException | LinkageError e) {
            if (!erreurSignalee) {
                erreurSignalee = true;
                plugin.getLogger().warning("Enclume en coffre : calcul impossible (" + e + ")");
            }
            return Calcul.refuse(rouge("L'enclume en coffre est indisponible."));
        } finally {
            plugin.calculVirtuel = false;
        }
        if (vide(resultat)) {
            return Calcul.refuse(rouge("Ces objets ne se combinent pas."));
        }
        resultat = resultat.clone();
        if (nom != null) {
            resultat.setData(DataComponentTypes.CUSTOM_NAME, nom);
        }
        return new Calcul(resultat, Math.max(0, niveaux), Math.max(0, unites), 0, null);
    }

    private static boolean creatif(Player joueur) {
        return joueur.getGameMode() == GameMode.CREATIVE;
    }

    private static boolean assez(Player joueur, Calcul calcul) {
        if (creatif(joueur)) {
            return true;
        }
        return calcul.fusion() ? joueur.calculateTotalExperiencePoints() >= calcul.points()
                : joueur.getLevel() >= calcul.niveaux();
    }

    // ------------------------------------------------------------------ affichage

    private static Component texte(String texte, NamedTextColor couleur) {
        return Component.text(texte, couleur).decoration(TextDecoration.ITALIC, false);
    }

    private static Component rouge(String texte) {
        return texte(texte, NamedTextColor.RED);
    }

    private static Component gris(String texte) {
        return texte(texte, NamedTextColor.GRAY);
    }

    /** Un objet du menu : nom et lignes sans italique, sans les attributs du jeu. */
    private static ItemStack objet(Material materiau, Component nom, List<Component> lignes) {
        ItemStack objet = new ItemStack(materiau);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(nom.decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lignes.forEach(l -> lore.add(l.decoration(TextDecoration.ITALIC, false)));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        objet.setItemMeta(meta);
        return objet;
    }

    private void remplir(Player joueur, Fenetre fenetre) {
        Inventory menu = fenetre.inventaire;
        ItemStack decor = objet(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int place = 0; place < menu.getSize(); place++) {
            if (place != ENTREE_1 && place != ENTREE_2) {
                menu.setItem(place, decor);
            }
        }
        menu.setItem(AIDE, objet(Material.ANVIL, texte("Enclume", NamedTextColor.GOLD), List.of(
                gris("Clique sur 2 objets de ton"),
                gris("inventaire pour les poser :"),
                gris("équipement + livre, matériau"),
                gris("ou même objet ; ou 2 coffres"),
                gris("de récompense non ouverts."),
                gris("Clic sur un objet posé :"),
                gris("le reprendre."),
                texte("Renommer : clic sans", NamedTextColor.DARK_GRAY),
                texte("s'accroupir.", NamedTextColor.DARK_GRAY))));
        Calcul calcul = calculer(joueur, menu.getItem(ENTREE_1), menu.getItem(ENTREE_2));
        if (calcul == null) {
            menu.setItem(APERCU, objet(Material.LIGHT_GRAY_STAINED_GLASS_PANE, texte("Résultat", NamedTextColor.GRAY),
                    List.of(texte("Pose d'abord 2 objets.", NamedTextColor.DARK_GRAY))));
            menu.setItem(VALIDER, objet(Material.GRAY_DYE, texte("Valider", NamedTextColor.GRAY),
                    List.of(texte("Pose d'abord 2 objets.", NamedTextColor.DARK_GRAY))));
            return;
        }
        if (calcul.refus() != null) {
            menu.setItem(APERCU, objet(Material.BARRIER, rouge("Aucun résultat"), List.of(calcul.refus())));
            menu.setItem(VALIDER, objet(Material.GRAY_DYE, texte("Valider", NamedTextColor.GRAY), List.of(calcul.refus())));
            return;
        }
        menu.setItem(APERCU, calcul.resultat().clone());
        boolean assez = assez(joueur, calcul);
        String unite = calcul.fusion() ? " XP" : (calcul.niveaux() > 1 ? " niveaux" : " niveau");
        int cout = calcul.fusion() ? calcul.points() : calcul.niveaux();
        int a = calcul.fusion() ? joueur.calculateTotalExperiencePoints() : joueur.getLevel();
        List<Component> lignes = new ArrayList<>();
        lignes.add(gris(calcul.fusion() ? "Coût : " + cout + " points d'expérience" : "Coût : " + cout + unite));
        lignes.add(creatif(joueur) ? gris("Gratuit en créatif.")
                : assez ? gris("Tu en as " + a + ".") : rouge("Tu n'en as que " + a + "."));
        lignes.add(texte("L'enclume peut s'abîmer (12 %).", NamedTextColor.DARK_GRAY));
        menu.setItem(VALIDER, objet(assez ? Material.LIME_DYE : Material.RED_DYE,
                texte("Valider (" + cout + unite + ")", assez ? NamedTextColor.GREEN : NamedTextColor.RED), lignes));
    }

    private static void message(Player joueur, Component texte) {
        joueur.sendActionBar(texte);
        joueur.playSound(joueur.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
    }

    // ------------------------------------------------------------------ clics

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Fenetre fenetre)) {
            return;
        }
        // Rien ne passe par le curseur : tous les clics sont annulés, le plugin déplace lui-même les objets.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player joueur) || event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory() == fenetre.inventaire) {
            switch (event.getSlot()) {
                case ENTREE_1, ENTREE_2 -> reprendre(joueur, fenetre, event.getSlot());
                case VALIDER -> valider(joueur, fenetre);
                default -> {
                    return;
                }
            }
        } else if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            poser(joueur, fenetre, event.getSlot());
        } else {
            return;
        }
        if (joueur.getOpenInventory().getTopInventory() == fenetre.inventaire) {
            remplir(joueur, fenetre);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Fenetre) {
            event.setCancelled(true);
        }
    }

    /** Pose toute la pile de cette case de l'inventaire du joueur dans la première case libre du menu. */
    private void poser(Player joueur, Fenetre fenetre, int place) {
        ItemStack objet = joueur.getInventory().getItem(place);
        if (vide(objet)) {
            return;
        }
        int cible = vide(fenetre.inventaire.getItem(ENTREE_1)) ? ENTREE_1
                : vide(fenetre.inventaire.getItem(ENTREE_2)) ? ENTREE_2 : -1;
        if (cible < 0) {
            message(joueur, rouge("Les 2 cases sont prises : clique sur un objet posé pour le reprendre."));
            return;
        }
        fenetre.inventaire.setItem(cible, objet.clone());
        joueur.getInventory().setItem(place, null);
    }

    /** Rend l'objet posé dans cette case ; ce qui ne tient pas dans l'inventaire reste dans la case. */
    private void reprendre(Player joueur, Fenetre fenetre, int place) {
        ItemStack objet = fenetre.inventaire.getItem(place);
        if (vide(objet)) {
            return;
        }
        Map<Integer, ItemStack> reste = joueur.getInventory().addItem(objet.clone());
        fenetre.inventaire.setItem(place, reste.isEmpty() ? null : reste.values().iterator().next());
        if (!reste.isEmpty()) {
            message(joueur, rouge("Plus de place dans ton inventaire."));
        }
    }

    private boolean enclumeLa(Player joueur, Fenetre fenetre) {
        Location centre = fenetre.enclume.getLocation().add(0.5, 0.5, 0.5);
        return Tag.ANVIL.isTagged(fenetre.enclume.getType()) && joueur.getWorld().equals(centre.getWorld())
                && joueur.getLocation().distanceSquared(centre) <= PORTEE_CARRE;
    }

    private void valider(Player joueur, Fenetre fenetre) {
        ItemStack a = fenetre.inventaire.getItem(ENTREE_1);
        ItemStack b = fenetre.inventaire.getItem(ENTREE_2);
        // Recalculé au clic : les niveaux du joueur ou les coffres ont pu changer depuis l'affichage.
        Calcul calcul = calculer(joueur, a, b);
        if (calcul == null) {
            return;
        }
        if (calcul.refus() != null) {
            message(joueur, calcul.refus());
            return;
        }
        if (!enclumeLa(joueur, fenetre)) {
            message(joueur, rouge("L'enclume n'est plus à ta portée."));
            Bukkit.getScheduler().runTask(plugin, () -> joueur.closeInventory());
            return;
        }
        if (!assez(joueur, calcul)) {
            message(joueur, rouge(calcul.fusion() ? "Il te faut " + calcul.points() + " points d'expérience."
                    : "Il te faut " + calcul.niveaux() + " niveaux."));
            return;
        }
        if (calcul.fusion()) {
            ItemStack fusionne = Recompenses.fusionner(joueur, a, b);
            if (fusionne == null) {
                message(joueur, rouge("La fusion n'a pas pu se faire."));
                return;
            }
            if (!creatif(joueur)) {
                joueur.setExperienceLevelAndProgress(joueur.calculateTotalExperiencePoints() - calcul.points());
            }
            fenetre.inventaire.setItem(ENTREE_1, fusionne);
            fenetre.inventaire.setItem(ENTREE_2, null);
            joueur.sendActionBar(texte("Coffres fusionnés.", NamedTextColor.GREEN));
        } else {
            if (!creatif(joueur)) {
                joueur.giveExpLevels(-calcul.niveaux());
            }
            // Comme le jeu : la 2e case perd les unités de matériau utilisées, ou est vidée.
            ItemStack second = null;
            if (calcul.unites() > 0 && b.getAmount() > calcul.unites()) {
                second = b.clone();
                second.setAmount(b.getAmount() - calcul.unites());
            }
            fenetre.inventaire.setItem(ENTREE_1, calcul.resultat());
            fenetre.inventaire.setItem(ENTREE_2, second);
        }
        user(joueur, fenetre);
    }

    /** 12 % de chances (hors créatif) que l'enclume s'abîme ; détruite, le menu se ferme (objets rendus). */
    private void user(Player joueur, Fenetre fenetre) {
        Block bloc = fenetre.enclume;
        Location lieu = bloc.getLocation();
        if (creatif(joueur) || ThreadLocalRandom.current().nextFloat() >= USURE) {
            bloc.getWorld().playEffect(lieu, Effect.ANVIL_USE, 0);
            return;
        }
        // Pas de switch sur Material : le compilateur y ajoute une table de tous les matériaux du jeu (jar 7 fois plus gros).
        Material type = bloc.getType();
        Material suivant = type == Material.ANVIL ? Material.CHIPPED_ANVIL
                : type == Material.CHIPPED_ANVIL ? Material.DAMAGED_ANVIL : null;
        if (suivant == null) {
            bloc.setType(Material.AIR);
            bloc.getWorld().playEffect(lieu, Effect.ANVIL_BREAK, 0);
            message(joueur, rouge("L'enclume s'est brisée."));
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (joueur.getOpenInventory().getTopInventory() == fenetre.inventaire) {
                    joueur.closeInventory();
                }
            });
            return;
        }
        BlockData ancien = bloc.getBlockData();
        BlockData nouveau = suivant.createBlockData();
        if (ancien instanceof Directional avant && nouveau instanceof Directional apres) {
            apres.setFacing(avant.getFacing());
        }
        bloc.setBlockData(nouveau);
        bloc.getWorld().playEffect(lieu, Effect.ANVIL_USE, 0);
    }

    // ------------------------------------------------------------------ fermeture

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Fenetre fenetre && event.getPlayer() instanceof Player joueur) {
            rendre(joueur, fenetre);
        }
    }

    /** Rend les objets posés (ce qui ne tient pas dans l'inventaire tombe aux pieds du joueur). */
    private static void rendre(Player joueur, Fenetre fenetre) {
        for (int place : new int[] {ENTREE_1, ENTREE_2}) {
            ItemStack objet = fenetre.inventaire.getItem(place);
            fenetre.inventaire.setItem(place, null);
            if (!vide(objet)) {
                joueur.getInventory().addItem(objet).values()
                        .forEach(reste -> joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste));
            }
        }
    }

    /** Arrêt du plugin : les menus ouverts sont fermés, leurs objets rendus. */
    void fermerTout() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            if (joueur.getOpenInventory().getTopInventory().getHolder() instanceof Fenetre fenetre) {
                rendre(joueur, fenetre);
                joueur.closeInventory();
            }
        }
    }

    // ------------------------------------------------------------------ KS_RewardsGUI (facultatif)

    /** Appels à KS_RewardsGUI, seulement quand il est actif (la classe n'est chargée qu'au premier appel). */
    private static final class Recompenses {

        static boolean estCoffre(ItemStack objet) {
            return KSRewardsGUI.estCoffre(objet);
        }

        static Calcul calculer(ItemStack a, ItemStack b) {
            Component refus = KSRewardsGUI.refusFusion(a, b);
            if (refus != null) {
                return Calcul.refuse(refus);
            }
            return new Calcul(KSRewardsGUI.apercuFusion(a, b), 0, 0, KSRewardsGUI.coutFusion(a, b), null);
        }

        static ItemStack fusionner(Player joueur, ItemStack a, ItemStack b) {
            return KSRewardsGUI.fusionner(joueur, a, b);
        }
    }
}
