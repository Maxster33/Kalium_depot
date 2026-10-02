package fr.kalium.economy;

import fr.kalium.economy.Magasins.Boutique;
import fr.kalium.economy.Magasins.Magasin;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 1.1.0 - boutiques (cahier des charges, catégorie 2, « Magasins » révisés le 02/10/2026) : création en posant un
 * panneau sur un contenant d'un claim de son magasin, achat en cliquant sur le panneau (sur place ou depuis le
 * catalogue), protections du contenant et du panneau.
 */
final class Boutiques implements Listener {

    /** Quantité maximale d'un lot (36 piles de 64 : un inventaire plein). */
    static final int QUANTITE_MAX = 2304;
    private static final long POINTS_MAX = 1_000_000_000L;

    /** Boutique en cours de création par un joueur. */
    private static final class Creation {
        Block panneau;
        ItemStack objet;
        int quantite;
        ItemStack prixObjet;
        int prixQuantite;
        long prixPoints;
        boolean prixEnPoints;
    }

    /** Étape de la création : choix de l'objet vendu ou de l'objet du prix. */
    private enum Etape { VENTE, PRIX }

    /** Coffre de choix d'un objet dans l'inventaire. */
    private record Selection(UUID joueur, Etape etape) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final KSEconomy plugin;
    private final Magasins magasins;
    private final Lang lang;
    private final Gui gui;
    private final Map<UUID, Creation> creations = new HashMap<>();
    /** Id du jeu -> nom français (noms_objets.txt) ; nom simplifié -> id. */
    private final Map<String, String> noms = new LinkedHashMap<>();

    Boutiques(KSEconomy plugin, Magasins magasins) {
        this.plugin = plugin;
        this.magasins = magasins;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
        chargerNoms();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private void message(Player joueur, Component texte) {
        gui.notice(joueur, t("boutique.titre", "<gold><bold>Boutique"), texte, null);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ noms des objets

    private void chargerNoms() {
        try (InputStream in = plugin.getResource("noms_objets.txt")) {
            if (in == null) {
                return;
            }
            BufferedReader lecteur = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                int sep = ligne.indexOf(';');
                if (ligne.startsWith("#") || sep < 0) {
                    continue;
                }
                Material m = Material.matchMaterial(ligne.substring(0, sep));
                if (m != null && m.isItem() && !m.isAir()) {
                    noms.put(m.getKey().getKey(), ligne.substring(sep + 1));
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("noms_objets.txt illisible : " + e.getMessage());
        }
    }

    private static String simplifier(String texte) {
        String sansAccents = Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase(Locale.ROOT).replace('_', ' ').replaceAll("\\s+", " ").trim();
    }

    /** Objets dont le nom français ou l'id correspond (exact d'abord, puis « contient »), 20 au plus. */
    private List<Material> rechercher(String texte) {
        String cherche = simplifier(texte);
        List<Material> exacts = new ArrayList<>();
        List<Material> proches = new ArrayList<>();
        for (Map.Entry<String, String> e : noms.entrySet()) {
            String id = simplifier(e.getKey());
            String nom = simplifier(e.getValue());
            Material m = Material.matchMaterial(e.getKey());
            if (m == null) {
                continue;
            }
            if (id.equals(cherche) || nom.equals(cherche)) {
                exacts.add(m);
            } else if (id.contains(cherche) || nom.contains(cherche)) {
                proches.add(m);
            }
        }
        if (!exacts.isEmpty()) {
            return exacts;
        }
        return proches.subList(0, Math.min(20, proches.size()));
    }

    /** Nom d'un objet : nom donné (objets custom) ou nom du jeu, traduit par le client. */
    static Component nomObjet(ItemStack objet) {
        ItemMeta meta = objet.hasItemMeta() ? objet.getItemMeta() : null;
        if (meta != null && meta.hasDisplayName()) {
            return meta.displayName();
        }
        return Component.translatable(objet.translationKey());
    }

    static Component lot(int quantite, ItemStack objet) {
        return Component.text(quantite + " x ").append(nomObjet(objet));
    }

    Component prix(Boutique b) {
        return b.enPoints() ? Component.text(KSEconomy.points(b.prixPoints)) : lot(b.prixQuantite, b.prixObjet);
    }

    // ------------------------------------------------------------------ création

    /** Panneau posé sur un contenant : boutique possible si le chunk est un claim de son magasin. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpenSign(PlayerOpenSignEvent event) {
        Block panneau = event.getSign().getBlock();
        if (magasins.boutiqueDuPanneau(panneau) != null) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() != PlayerOpenSignEvent.Cause.PLACE || Magasins.contenantDu(panneau) == null) {
            return;
        }
        Player joueur = event.getPlayer();
        Magasin magasin = magasins.magasinDuChunk(panneau.getChunk());
        if (magasin == null || !magasin.proprio.equals(joueur.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> proposer(joueur, panneau));
    }

    private void proposer(Player joueur, Block panneau) {
        Creation c = new Creation();
        c.panneau = panneau;
        creations.put(joueur.getUniqueId(), c);
        List<ActionButton> boutons = List.of(
                gui.button(t("boutique.bouton-creer", "<green>Créer une boutique"), null, p -> choisirObjet(p, Etape.VENTE)),
                gui.button(t("boutique.bouton-fermer", "<gray>Fermer"), null, p -> creations.remove(p.getUniqueId())));
        gui.open(joueur, t("boutique.titre", "<gold><bold>Boutique"),
                List.of(t("boutique.proposer", "<white>Faire de ce panneau une boutique ? <gray>Le contenu du contenant "
                        + "sera son stock ; les paiements en objets y arriveront.")),
                List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    private Creation creation(Player joueur) {
        return creations.get(joueur.getUniqueId());
    }

    private void choisirObjet(Player joueur, Etape etape) {
        if (creation(joueur) == null) {
            return;
        }
        List<ActionButton> boutons = List.of(
                gui.button(t("boutique.ecrire", "<white>Écrire le nom"), null, p -> ecrireNom(p, etape)),
                gui.button(t("boutique.inventaire", "<white>Choisir dans l'inventaire"), null,
                        p -> choisirDansInventaire(p, etape)),
                gui.button(t("boutique.annuler", "<red>Annuler"), null, p -> creations.remove(p.getUniqueId())));
        gui.open(joueur, etape == Etape.VENTE ? t("boutique.titre-vente", "<gold><bold>Objet à vendre")
                        : t("boutique.titre-prix", "<gold><bold>Objet demandé en paiement"),
                List.of(t("boutique.choix-aide", "<gray>Écris le nom français ou l'id du jeu (« diamant », « diamond »), "
                        + "ou choisis un objet de ton inventaire (objets spéciaux compris).")),
                List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void ecrireNom(Player joueur, Etape etape) {
        ActionButton chercher = gui.form(t("boutique.chercher", "<green>Chercher"), null, (p, vue) -> {
            String texte = vue.getText("nom") == null ? "" : vue.getText("nom");
            List<Material> trouves = texte.isBlank() ? List.of() : rechercher(texte);
            if (trouves.isEmpty()) {
                gui.notice(p, t("boutique.titre", "<gold><bold>Boutique"),
                        t("boutique.introuvable", "<red>Aucun objet ne correspond à « <texte> ».", "texte", texte),
                        q -> ecrireNom(q, etape));
            } else if (trouves.size() == 1) {
                choisi(p, etape, new ItemStack(trouves.get(0)));
            } else {
                List<ActionButton> boutons = new ArrayList<>();
                for (Material m : trouves) {
                    boutons.add(gui.button(Component.translatable(m.translationKey()), null,
                            q -> choisi(q, etape, new ItemStack(m))));
                }
                boutons.add(gui.button(t("boutique.autre-nom", "<gray>Autre nom"), null, q -> ecrireNom(q, etape)));
                gui.open(p, t("boutique.resultats", "<gold><bold>Quel objet ?"), List.of(), List.of(), boutons,
                        gui.close(), 2);
            }
            lang.saveIfNeeded();
        });
        gui.open(joueur, t("boutique.titre-nom", "<gold><bold>Nom de l'objet"), List.of(),
                List.of(gui.text("nom", t("boutique.champ-nom", "Nom ou id"), "", 64)),
                List.of(chercher, gui.button(t("boutique.retour", "<gray>Retour"), null, p -> choisirObjet(p, etape))),
                gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void choisirDansInventaire(Player joueur, Etape etape) {
        Inventory choix = Bukkit.createInventory(new Selection(joueur.getUniqueId(), etape), 36,
                t("boutique.titre-inventaire", "Clique sur l'objet"));
        ItemStack[] contenu = joueur.getInventory().getStorageContents();
        for (int i = 0; i < contenu.length && i < 36; i++) {
            if (contenu[i] != null && !contenu[i].getType().isAir()) {
                choix.setItem(i, contenu[i].clone());
            }
        }
        joueur.openInventory(choix);
    }

    @EventHandler
    public void onClickSelection(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Selection selection)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory() || event.getCurrentItem() == null
                || event.getCurrentItem().getType().isAir() || !(event.getWhoClicked() instanceof Player joueur)) {
            return;
        }
        ItemStack objet = event.getCurrentItem().clone();
        objet.setAmount(1);
        Bukkit.getScheduler().runTask(plugin, () -> {
            joueur.closeInventory();
            choisi(joueur, selection.etape(), objet);
        });
    }

    @EventHandler
    public void onDragSelection(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Selection) {
            event.setCancelled(true);
        }
    }

    private void choisi(Player joueur, Etape etape, ItemStack objet) {
        Creation c = creation(joueur);
        if (c == null) {
            return;
        }
        if (etape == Etape.VENTE) {
            c.objet = objet;
        } else {
            c.prixObjet = objet;
        }
        quantite(joueur, etape);
    }

    private void quantite(Player joueur, Etape etape) {
        Creation c = creation(joueur);
        if (c == null) {
            return;
        }
        ItemStack objet = etape == Etape.VENTE ? c.objet : c.prixObjet;
        ActionButton valider = gui.form(t("boutique.valider", "<green>Valider"), null, (p, vue) -> {
            int n = entier(vue.getText("quantite"));
            if (n < 1 || n > QUANTITE_MAX) {
                gui.notice(p, t("boutique.titre", "<gold><bold>Boutique"),
                        t("boutique.quantite-invalide", "<red>Quantité invalide (1 à <max>).", "max", QUANTITE_MAX),
                        q -> quantite(q, etape));
                return;
            }
            Creation cc = creation(p);
            if (cc == null) {
                return;
            }
            if (etape == Etape.VENTE) {
                cc.quantite = n;
                choisirPrix(p);
            } else {
                cc.prixQuantite = n;
                cc.prixEnPoints = false;
                recapitulatif(p);
            }
        });
        gui.open(joueur, t("boutique.titre-quantite", "<gold><bold>Quantité"),
                List.of(t("boutique.quantite-objet", "<white>Objet : ").append(nomObjet(objet))),
                List.of(gui.text("quantite", t("boutique.champ-quantite", "Quantité par lot"), "1", 5)),
                List.of(valider), gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void choisirPrix(Player joueur) {
        List<ActionButton> boutons = List.of(
                gui.button(t("boutique.monnaie", "<green>Monnaie (points)"), null, this::prixEnPoints),
                gui.button(t("boutique.objet", "<white>Objet"), null, p -> choisirObjet(p, Etape.PRIX)),
                gui.button(t("boutique.annuler", "<red>Annuler"), null, p -> creations.remove(p.getUniqueId())));
        gui.open(joueur, t("boutique.titre-prix-choix", "<gold><bold>Prix d'un lot"),
                List.of(t("boutique.prix-aide", "<gray>Monnaie : les points payés attendent dans la boutique, tu les "
                        + "récupères depuis ton magasin. Objet : il arrive dans le contenant.")),
                List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void prixEnPoints(Player joueur) {
        ActionButton valider = gui.form(t("boutique.valider", "<green>Valider"), null, (p, vue) -> {
            long n = entierLong(vue.getText("points"));
            Creation c = creation(p);
            if (c == null) {
                return;
            }
            if (n < 1 || n > POINTS_MAX) {
                gui.notice(p, t("boutique.titre", "<gold><bold>Boutique"),
                        t("boutique.points-invalides", "<red>Prix invalide."), this::prixEnPoints);
                return;
            }
            c.prixPoints = n;
            c.prixEnPoints = true;
            c.prixObjet = null;
            recapitulatif(p);
        });
        gui.open(joueur, t("boutique.titre-points", "<gold><bold>Prix en points"), List.of(),
                List.of(gui.text("points", t("boutique.champ-points", "Points par lot"), "", 12)),
                List.of(valider), gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void recapitulatif(Player joueur) {
        Creation c = creation(joueur);
        if (c == null) {
            return;
        }
        Component prix = c.prixEnPoints ? Component.text(KSEconomy.points(c.prixPoints)) : lot(c.prixQuantite, c.prixObjet);
        gui.confirm(joueur, t("boutique.titre-recap", "<gold><bold>Nouvelle boutique"),
                t("boutique.recap", "<white>Vend <lot> contre <prix>.", "lot", lot(c.quantite, c.objet), "prix", prix),
                this::creer, p -> creations.remove(p.getUniqueId()));
        lang.saveIfNeeded();
    }

    private void creer(Player joueur) {
        Creation c = creations.remove(joueur.getUniqueId());
        if (c == null || c.objet == null) {
            return;
        }
        Block panneau = c.panneau;
        Magasin magasin = magasins.magasinDuChunk(panneau.getChunk());
        if (!(panneau.getState() instanceof Sign) || Magasins.contenantDu(panneau) == null || magasin == null
                || !magasin.proprio.equals(joueur.getUniqueId()) || magasins.boutiqueDuPanneau(panneau) != null) {
            message(joueur, t("boutique.plus-possible", "<red>Ce panneau ne peut plus devenir une boutique."));
            return;
        }
        if (magasins.nombreDeBoutiques(joueur.getUniqueId()) >= magasins.boutiquesMax(magasin)) {
            message(joueur, t("boutique.limite", "<red>Ton magasin a déjà <max> boutiques. /magasin agrandir pour en "
                    + "ajouter.", "max", magasins.boutiquesMax(magasin)));
            return;
        }
        Boutique b = new Boutique();
        b.id = magasins.nouvelId();
        b.proprio = joueur.getUniqueId();
        b.panneau = panneau.getLocation();
        b.objet = c.objet;
        b.quantite = c.quantite;
        if (c.prixEnPoints) {
            b.prixPoints = c.prixPoints;
        } else {
            b.prixObjet = c.prixObjet;
            b.prixQuantite = c.prixQuantite;
        }
        magasins.boutiques.put(b.id, b);
        magasins.sauver();
        ecrirePanneau(b);
        message(joueur, t("boutique.creee", "<green>Boutique créée."));
    }

    /** Panneau d'une boutique : « [Troc] », le lot vendu, « contre », le prix ; ciré (non modifiable). */
    void ecrirePanneau(Boutique b) {
        if (!(b.panneau.getBlock().getState() instanceof Sign panneau)) {
            return;
        }
        SignSide face = panneau.getSide(Side.FRONT);
        face.line(0, Component.text("[Troc]", NamedTextColor.DARK_BLUE).decoration(TextDecoration.BOLD, true));
        face.line(1, lot(b.quantite, b.objet));
        face.line(2, Component.text("contre", NamedTextColor.DARK_GRAY));
        face.line(3, prix(b));
        panneau.setWaxed(true);
        panneau.update();
    }

    // ------------------------------------------------------------------ achat

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }
        Boutique b = magasins.boutiqueDuPanneau(event.getClickedBlock());
        if (b == null) {
            return;
        }
        event.setCancelled(true);
        Player joueur = event.getPlayer();
        if (b.proprio.equals(joueur.getUniqueId())) {
            plugin.menuMagasin().gererBoutique(joueur, b);
        } else {
            ouvrirAchat(joueur, b);
        }
    }

    /** Stock du contenant en lots, -1 si le contenant n'existe plus. */
    int lotsEnStock(Boutique b) {
        Inventory inv = Magasins.inventaire(Magasins.contenantDu(b.panneau.getBlock()));
        return inv == null ? -1 : compter(inv.getContents(), b.objet) / b.quantite;
    }

    static int compter(ItemStack[] contenu, ItemStack modele) {
        int n = 0;
        for (ItemStack objet : contenu) {
            if (objet != null && objet.isSimilar(modele)) {
                n += objet.getAmount();
            }
        }
        return n;
    }

    /** Menu d'achat (sur place ou depuis le catalogue). */
    void ouvrirAchat(Player joueur, Boutique b) {
        if (magasins.verrouillee(b)) {
            message(joueur, t("boutique.verrouillee", "<red>Le propriétaire gère cette boutique : réessaie dans un instant."));
            return;
        }
        int stock = lotsEnStock(b);
        List<Component> corps = new ArrayList<>();
        corps.add(t("achat.vend", "<white>Vend : ").append(lot(b.quantite, b.objet)));
        corps.add(t("achat.contre", "<white>Contre : ").append(prix(b)));
        corps.add(stock < 0 ? t("achat.ferme", "<red>Boutique fermée (contenant disparu).")
                : t("achat.stock", "<white>Stock : <lots> lot(s)", "lots", stock));
        ActionButton acheter = gui.form(t("achat.bouton", "<green>Acheter"), null,
                (p, vue) -> acheter(p, b, entier(vue.getText("lots"))));
        gui.open(joueur, t("achat.titre", "<gold><bold>Boutique de <proprio>", "proprio", nomJoueur(b.proprio)), corps,
                List.of(gui.text("lots", t("achat.champ-lots", "Nombre de lots"), "1", 4)), List.of(acheter), gui.close(),
                1);
        lang.saveIfNeeded();
    }

    static String nomJoueur(UUID joueur) {
        String nom = Bukkit.getOfflinePlayer(joueur).getName();
        return nom == null ? "?" : nom;
    }

    private void acheter(Player joueur, Boutique b, int lots) {
        if (!magasins.boutiques.containsKey(b.id)) {
            message(joueur, t("achat.disparue", "<red>Cette boutique n'existe plus."));
            return;
        }
        if (magasins.verrouillee(b)) {
            message(joueur, t("boutique.verrouillee", "<red>Le propriétaire gère cette boutique : réessaie dans un instant."));
            return;
        }
        Inventory stock = Magasins.inventaire(Magasins.contenantDu(b.panneau.getBlock()));
        if (lots < 1 || lots > 1000) {
            message(joueur, t("achat.lots-invalides", "<red>Nombre de lots invalide."));
            return;
        }
        if (stock == null) {
            message(joueur, t("achat.ferme", "<red>Boutique fermée (contenant disparu)."));
            return;
        }
        long nbObjets = (long) b.quantite * lots;
        if (compter(stock.getContents(), b.objet) < nbObjets) {
            message(joueur, t("achat.rupture", "<red>Pas assez de stock pour <lots> lot(s).", "lots", lots));
            return;
        }
        long points = b.prixPoints * lots;
        long nbPaiement = (long) b.prixQuantite * lots;
        if (b.enPoints() ? KSEconomy.solde(joueur.getUniqueId()) < points
                : compter(joueur.getInventory().getStorageContents(), b.prixObjet) < nbPaiement) {
            message(joueur, t("achat.paiement", "<red>Tu n'as pas de quoi payer <lots> lot(s).", "lots", lots));
            return;
        }
        List<ItemStack> achetes = piles(b.objet, nbObjets);
        List<ItemStack> paiement = b.enPoints() ? List.of() : piles(b.prixObjet, nbPaiement);
        // Places : l'acheteur reçoit les objets (après avoir donné son paiement), le contenant reçoit le paiement.
        Inventory essaiJoueur = copie(joueur.getInventory().getStorageContents());
        paiement.forEach(p -> essaiJoueur.removeItem(p.clone()));
        if (!rentre(essaiJoueur, achetes)) {
            message(joueur, t("achat.plein", "<red>Pas assez de place dans ton inventaire."));
            return;
        }
        Inventory essaiStock = copie(stock.getContents());
        achetes.forEach(a -> essaiStock.removeItem(a.clone()));
        if (!rentre(essaiStock, paiement)) {
            message(joueur, t("achat.stock-plein", "<red>Le contenant de la boutique est plein : impossible de payer."));
            return;
        }
        if (b.enPoints()) {
            if (!KSEconomy.debiter(joueur.getUniqueId(), points)) {
                message(joueur, t("achat.paiement", "<red>Tu n'as pas de quoi payer <lots> lot(s).", "lots", lots));
                return;
            }
            b.pointsEnAttente += points;
        } else {
            paiement.forEach(p -> joueur.getInventory().removeItem(p.clone()));
        }
        achetes.forEach(a -> stock.removeItem(a.clone()));
        paiement.forEach(p -> stock.addItem(p.clone()));
        achetes.forEach(a -> joueur.getInventory().addItem(a.clone()));
        magasins.sauver();
        plugin.getLogger().info("Boutique " + b.id + " : " + joueur.getName() + " achète " + lots + " lot(s) à "
                + nomJoueur(b.proprio));
        message(joueur, t("achat.fait", "<green>Acheté : <lots> lot(s).", "lots", lots));
    }

    /** Piles d'un modèle pour une quantité totale. */
    static List<ItemStack> piles(ItemStack modele, long total) {
        List<ItemStack> liste = new ArrayList<>();
        long reste = total;
        while (reste > 0) {
            ItemStack pile = modele.clone();
            int n = (int) Math.min(reste, modele.getMaxStackSize());
            pile.setAmount(n);
            liste.add(pile);
            reste -= n;
        }
        return liste;
    }

    private static Inventory copie(ItemStack[] contenu) {
        int taille = Math.max(9, ((contenu.length + 8) / 9) * 9);
        Inventory essai = Bukkit.createInventory(null, Math.min(54, taille));
        for (int i = 0; i < contenu.length && i < essai.getSize(); i++) {
            essai.setItem(i, contenu[i] == null ? null : contenu[i].clone());
        }
        return essai;
    }

    private static boolean rentre(Inventory essai, List<ItemStack> objets) {
        for (ItemStack objet : objets) {
            if (!essai.addItem(objet.clone()).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static int entier(String texte) {
        long n = entierLong(texte);
        return n > Integer.MAX_VALUE ? -1 : (int) n;
    }

    static long entierLong(String texte) {
        try {
            return Long.parseLong(texte == null ? "" : texte.replaceAll("\\s", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ------------------------------------------------------------------ protections

    /** Casser un panneau ou un contenant de boutique : le propriétaire seulement ; la boutique est supprimée. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block bloc = event.getBlock();
        List<Boutique> touchees = new ArrayList<>();
        Boutique duPanneau = magasins.boutiqueDuPanneau(bloc);
        if (duPanneau != null) {
            touchees.add(duPanneau);
        } else if (Magasins.contenantAccepte(bloc.getType())) {
            touchees.addAll(magasins.boutiquesDuContenant(bloc));
        }
        if (touchees.isEmpty()) {
            return;
        }
        Player joueur = event.getPlayer();
        for (Boutique b : touchees) {
            if (!b.proprio.equals(joueur.getUniqueId()) || magasins.verrouillee(b)) {
                event.setCancelled(true);
                joueur.sendMessage(t("boutique.protegee", "<red>Cette boutique appartient à <proprio>.", "proprio",
                        nomJoueur(b.proprio)));
                return;
            }
        }
        for (Boutique b : touchees) {
            supprimer(b, joueur);
        }
    }

    /** Supprime une boutique (les points en attente sont rendus au propriétaire). */
    void supprimer(Boutique b, Player auteur) {
        magasins.boutiques.remove(b.id);
        if (b.pointsEnAttente > 0) {
            KSEconomy.crediter(b.proprio, b.pointsEnAttente);
        }
        magasins.sauver();
        if (b.panneau.getBlock().getState() instanceof Sign panneau) {
            panneau.setWaxed(false);
            panneau.update();
        }
        if (auteur != null) {
            auteur.sendMessage(t("boutique.supprimee", "<yellow>Boutique supprimée<points>.", "points",
                    b.pointsEnAttente > 0 ? " (" + KSEconomy.points(b.pointsEnAttente) + " rendus)" : ""));
        }
    }

    private boolean estBoutique(Block bloc) {
        return magasins.boutiqueDuPanneau(bloc) != null
                || (Magasins.contenantAccepte(bloc.getType()) && !magasins.boutiquesDuContenant(bloc).isEmpty());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::estBoutique);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::estBoutique);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::estBoutique)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::estBoutique)) {
            event.setCancelled(true);
        }
    }

    /** Entonnoirs : rien n'entre ni ne sort du contenant d'une boutique. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        if (contenantDeBoutique(event.getSource()) || contenantDeBoutique(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    private boolean contenantDeBoutique(Inventory inv) {
        if (inv.getLocation() == null) {
            return false;
        }
        Block bloc = inv.getLocation().getBlock();
        return Magasins.contenantAccepte(bloc.getType()) && !magasins.boutiquesDuContenant(bloc).isEmpty();
    }

    /** Contenant verrouillé (gestion à distance en cours) : personne ne l'ouvre sur place. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getLocation() == null) {
            return;
        }
        Block bloc = inv.getLocation().getBlock();
        if (!Magasins.contenantAccepte(bloc.getType())) {
            return;
        }
        for (Boutique b : magasins.boutiquesDuContenant(bloc)) {
            if (magasins.verrouillee(b)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(t("boutique.verrouillee",
                        "<red>Le propriétaire gère cette boutique : réessaie dans un instant."));
                return;
            }
        }
    }
}
