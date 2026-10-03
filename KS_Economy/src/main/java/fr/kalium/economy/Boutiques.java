package fr.kalium.economy;

import fr.kalium.economy.Magasins.Boutique;
import fr.kalium.economy.Magasins.Magasin;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.kalium.menu.api.Lisible;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
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
 *
 * 1.1.2 (retours de LeKiwi06, 03/10/2026) : noms français écrits par le plugin (panneau et menus) ; panneau en 4
 * lignes mesurées (nom de la boutique, lot, prix, état : stock, rupture, coffre plein, fermée) tenu à jour ; nom de
 * boutique choisi à la création et modifiable ; suppression depuis le menu : panneau retiré et rendu.
 *
 * 1.1.3 (LeKiwi06, 03/10/2026) : on n'achète pas dans sa propre boutique (stats des magasins) ; « Voir l'objet » montre
 * l'objet exact vendu (et celui demandé) dans un coffre en lecture seule ; « Signaler la boutique ».
 *
 * 1.2.0 (LeKiwi06, 03/10/2026) : une boutique (son contenant) n'est utilisée que par un acheteur à la fois (menu
 * d'achat, « Voir l'objet », achat ; libérée après l'achat, à la déconnexion ou après 30 s sans action) ; chaque vente
 * est enregistrée (statistiques, notification au propriétaire) ; bouton favori.
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

    /** 1.2.0 : acheteur qui utilise une boutique (clé du contenant) et fin de son utilisation. */
    private record Utilisation(UUID joueur, long fin) {
    }

    private static final long DUREE_UTILISATION_MS = 30_000;
    private final Map<String, Utilisation> utilisations = new HashMap<>();

    /**
     * 1.2.0 : réserve la boutique pour ce joueur (30 s, renouvelées à chaque action) ; faux si quelqu'un d'autre
     * l'utilise (message). Un joueur n'utilise qu'une boutique à la fois.
     */
    private boolean utiliser(Player joueur, Boutique b) {
        long maintenant = System.currentTimeMillis();
        utilisations.values().removeIf(u -> u.fin() < maintenant);
        String cle = Magasins.cleVerrou(b);
        Utilisation u = utilisations.get(cle);
        if (u != null && !u.joueur().equals(joueur.getUniqueId())) {
            message(joueur, t("achat.occupee", "<red>Quelqu'un utilise cette boutique en ce moment : réessaie dans un "
                    + "instant."));
            return false;
        }
        liberer(joueur.getUniqueId());
        utilisations.put(cle, new Utilisation(joueur.getUniqueId(), maintenant + DUREE_UTILISATION_MS));
        return true;
    }

    /** 1.2.0 : le joueur n'utilise plus de boutique. */
    void liberer(UUID joueur) {
        utilisations.values().removeIf(u -> u.joueur().equals(joueur));
    }

    @EventHandler
    public void onQuitUtilisation(org.bukkit.event.player.PlayerQuitEvent event) {
        liberer(event.getPlayer().getUniqueId());
    }

    /** 1.1.3 : coffre « Voir l'objet » (lecture seule) d'une boutique. */
    private record Vue(UUID joueur, String boutique) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

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
    /** Id du jeu -> nom français (noms_objets.txt, dans le jar depuis 1.1.2). */
    private static final Map<String, String> NOMS = new LinkedHashMap<>();
    /** Largeur d'une ligne de panneau (pixels de la police ; suspendu : 60) : au-delà, le jeu coupe au dernier mot. */
    private static final int LARGEUR_PANNEAU = 90, LARGEUR_SUSPENDU = 60;

    /** 1.1.2 : état d'une boutique (panneau, menus). */
    enum Etat { OUVERTE, RUPTURE, PLEIN, FERMEE, DISPARUE }

    Boutiques(KSEconomy plugin, Magasins magasins) {
        this.plugin = plugin;
        this.magasins = magasins;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
        chargerNoms();
        // Panneaux des chunks déjà chargés : nouvelle présentation (1.1.2) et état à jour.
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Boutique b : List.copyOf(magasins.boutiques.values())) {
                if (b.panneau.getWorld() != null
                        && b.panneau.getWorld().isChunkLoaded(b.panneau.getBlockX() >> 4, b.panneau.getBlockZ() >> 4)) {
                    ecrirePanneau(b);
                }
            }
        });
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
        NOMS.clear();
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
                    NOMS.put(m.getKey().getKey(), ligne.substring(sep + 1));
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
        for (Map.Entry<String, String> e : NOMS.entrySet()) {
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

    /**
     * Nom d'un objet : nom donné (objets custom), sinon nom français (1.1.2 : écrit par le plugin, quelle que soit la
     * langue du client ; avant, le nom anglais s'affichait sur les panneaux), sinon nom du jeu traduit par le client.
     */
    static Component nomObjet(ItemStack objet) {
        ItemMeta meta = objet.hasItemMeta() ? objet.getItemMeta() : null;
        if (meta != null && meta.hasDisplayName()) {
            return meta.displayName();
        }
        String francais = NOMS.get(objet.getType().getKey().getKey());
        return francais != null ? Component.text(francais) : Component.translatable(objet.translationKey());
    }

    /** Nom d'un objet en texte simple (panneaux, nom de boutique par défaut). */
    static String texteObjet(ItemStack objet) {
        Component nom = nomObjet(objet);
        if (nom instanceof net.kyori.adventure.text.TranslatableComponent) {
            return objet.getType().getKey().getKey().replace('_', ' ');
        }
        return PlainTextComponentSerializer.plainText().serialize(nom);
    }

    /** Nom d'une boutique : celui choisi par le propriétaire, sinon le nom de l'objet vendu. */
    static String nomBoutique(Boutique b) {
        return b.nom != null ? b.nom : texteObjet(b.objet);
    }

    /** Nom saisi : espaces réduits, 20 caractères au plus ; vide : null (nom de l'objet). */
    static String nettoyerNom(String texte) {
        String nom = texte == null ? "" : texte.replaceAll("\\s+", " ").trim();
        if (nom.length() > Magasins.NOM_MAX) {
            nom = nom.substring(0, Magasins.NOM_MAX).trim();
        }
        return nom.isEmpty() ? null : nom;
    }

    static String couper(String texte, int max) {
        return texte.length() > max ? texte.substring(0, max).trim() : texte;
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
        Magasin magasin = magasins.magasinDuChunk(panneau.getChunk());
        Component refus = magasin == null ? null : refusComplet(joueur, magasin);
        if (refus != null) {
            message(joueur, refus);
            return;
        }
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
                    boutons.add(gui.button(nomObjet(new ItemStack(m)), null,
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

    /** Récapitulatif et nom de la boutique (1.1.2 : nom choisi ici, l'objet vendu par défaut). */
    private void recapitulatif(Player joueur) {
        Creation c = creation(joueur);
        if (c == null) {
            return;
        }
        Component prix = c.prixEnPoints ? Component.text(KSEconomy.points(c.prixPoints)) : lot(c.prixQuantite, c.prixObjet);
        ActionButton creer = gui.form(t("boutique.bouton-creer-nom", "<green>Créer la boutique"), null,
                (p, vue) -> creer(p, vue.getText("nom")));
        gui.open(joueur, t("boutique.titre-recap", "<gold><bold>Nouvelle boutique"),
                List.of(t("boutique.recap", "<white>Vend <lot> contre <prix>.", "lot", lot(c.quantite, c.objet), "prix", prix)),
                List.of(gui.text("nom", t("boutique.champ-nom-boutique", "Nom de la boutique (20 caractères)"),
                        couper(texteObjet(c.objet), Magasins.NOM_MAX), Magasins.NOM_MAX)),
                List.of(creer, gui.button(t("boutique.annuler", "<red>Annuler"), null, p -> creations.remove(p.getUniqueId()))),
                gui.close(), 1);
        lang.saveIfNeeded();
    }

    /** Magasin complet (boutiques + places encore prises par des boutiques supprimées) : le refus, sinon null. */
    Component refusComplet(Player joueur, Magasin magasin) {
        UUID uuid = joueur.getUniqueId();
        int max = magasins.boutiquesMax(magasin);
        int attente = magasins.placesEnAttente(uuid);
        if (magasins.nombreDeBoutiques(uuid) + attente < max) {
            return null;
        }
        if (attente == 0) {
            return t("boutique.limite", "<red>Ton magasin a déjà <max> boutiques. /magasin agrandir pour en "
                    + "ajouter.", "max", max);
        }
        return t("boutique.limite-delai", "<red>Ton magasin est complet : <max> places, dont <attente> encore prise(s) "
                        + "par des boutiques supprimées. Prochaine place libre dans <delai>.", "max", max,
                "attente", attente, "delai", duree(magasins.avantLiberation(uuid)));
    }

    /** Durée lisible : « 2 h 15 min », « 40 min ». */
    static String duree(long ms) {
        long minutes = Math.max(1, (ms + 59_999) / 60_000);
        long h = minutes / 60;
        long min = minutes % 60;
        return h == 0 ? min + " min" : min == 0 ? h + " h" : h + " h " + min + " min";
    }

    private void creer(Player joueur, String nom) {
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
        Component refus = refusComplet(joueur, magasin);
        if (refus != null) {
            message(joueur, refus);
            return;
        }
        Boutique b = new Boutique();
        b.id = magasins.nouvelId();
        b.proprio = joueur.getUniqueId();
        b.panneau = panneau.getLocation();
        b.objet = c.objet;
        b.quantite = c.quantite;
        b.nom = nettoyerNom(nom);
        if (b.nom != null && b.nom.equals(texteObjet(b.objet))) {
            b.nom = null;
        }
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

    /**
     * Panneau d'une boutique (1.1.2) : nom (gras), lot vendu, « pour » + prix, état ; chaque ligne mesurée et coupée
     * avec « … » si elle dépasse (le jeu n'en affichait sinon que les premiers mots). Ciré (non modifiable).
     */
    void ecrirePanneau(Boutique b) {
        if (!(b.panneau.getBlock().getState() instanceof Sign panneau)) {
            return;
        }
        SignSide face = panneau.getSide(Side.FRONT);
        int largeur = panneau.getType().name().contains("HANGING") ? LARGEUR_SUSPENDU : LARGEUR_PANNEAU;
        face.line(0, ligne(largeur, nomBoutique(b), Style.style(NamedTextColor.DARK_BLUE, TextDecoration.BOLD)));
        face.line(1, ligne(largeur, b.quantite + " x " + texteObjet(b.objet), Style.empty()));
        face.line(2, ligne(largeur, "pour " + (b.enPoints() ? KSEconomy.points(b.prixPoints)
                : b.prixQuantite + " x " + texteObjet(b.prixObjet)), Style.style(NamedTextColor.DARK_GRAY)));
        int lots = lotsEnStock(b);
        face.line(3, switch (etat(b)) {
            case OUVERTE -> ligne(largeur, "Stock : " + lots + (lots > 1 ? " lots" : " lot"), Style.style(NamedTextColor.DARK_GREEN));
            case RUPTURE -> ligne(largeur, "Rupture de stock", Style.style(NamedTextColor.RED, TextDecoration.BOLD));
            case PLEIN -> ligne(largeur, "Coffre plein", Style.style(NamedTextColor.GOLD, TextDecoration.BOLD));
            case FERMEE -> ligne(largeur, "Fermée", Style.style(NamedTextColor.RED, TextDecoration.BOLD));
            case DISPARUE -> ligne(largeur, "Hors service", Style.style(NamedTextColor.DARK_RED));
        });
        panneau.setWaxed(true);
        panneau.update();
    }

    /** Ligne de panneau qui tient dans sa largeur (coupée avec « … » sinon). */
    private static Component ligne(int largeur, String texte, Style style) {
        if (Lisible.largeurTexte(Component.text(texte, style)) <= largeur) {
            return Component.text(texte, style);
        }
        String court = texte;
        while (!court.isEmpty() && Lisible.largeurTexte(Component.text(court + "…", style)) > largeur) {
            court = court.substring(0, court.length() - 1);
        }
        return Component.text(court.trim() + "…", style);
    }

    /** Panneaux de toutes les boutiques de ce contenant (après un achat, une gestion, une fermeture du coffre). */
    void actualiserPanneaux(Block contenant) {
        if (contenant == null) {
            return;
        }
        for (Boutique b : magasins.boutiquesDuContenant(contenant)) {
            ecrirePanneau(b);
        }
    }

    /** État : fermée, contenant disparu, rupture (moins d'un lot), coffre plein (paiement en objet impossible). */
    Etat etat(Boutique b) {
        if (b.fermee) {
            return Etat.FERMEE;
        }
        Inventory inv = Magasins.inventaire(Magasins.contenantDu(b.panneau.getBlock()));
        if (inv == null) {
            return Etat.DISPARUE;
        }
        if (compter(inv.getContents(), b.objet) < b.quantite) {
            return Etat.RUPTURE;
        }
        if (!b.enPoints()) {
            Inventory essai = copie(inv.getContents());
            piles(b.objet, b.quantite).forEach(a -> essai.removeItem(a.clone()));
            if (!rentre(essai, piles(b.prixObjet, b.prixQuantite))) {
                return Etat.PLEIN;
            }
        }
        return Etat.OUVERTE;
    }

    /** État pour les menus (texte coloré). */
    Component etatTexte(Boutique b) {
        return switch (etat(b)) {
            case OUVERTE -> t("etat.ouverte", "<green><lots> lot(s) en stock", "lots", lotsEnStock(b));
            case RUPTURE -> t("etat.rupture", "<red><bold>Rupture de stock");
            case PLEIN -> t("etat.plein", "<gold><bold>Coffre plein : paiement impossible");
            case FERMEE -> t("etat.fermee", "<red>Fermée temporairement");
            case DISPARUE -> t("etat.disparue", "<dark_red>Contenant disparu");
        };
    }

    /** Nom d'une boutique pour un bouton, coloré selon son état. */
    Component boutonBoutique(Boutique b) {
        NamedTextColor couleur = switch (etat(b)) {
            case OUVERTE -> NamedTextColor.WHITE;
            case RUPTURE, FERMEE -> NamedTextColor.RED;
            case PLEIN -> NamedTextColor.GOLD;
            case DISPARUE -> NamedTextColor.DARK_RED;
        };
        return Component.text(couper(nomBoutique(b), 32), couleur);
    }

    /** Contenant d'une boutique refermé (propriétaire sur place) : état du panneau à jour. */
    @EventHandler
    public void onCloseContenant(InventoryCloseEvent event) {
        Location lieu = event.getInventory().getLocation();
        if (lieu == null || !Magasins.contenantAccepte(lieu.getBlock().getType())) {
            return;
        }
        Block bloc = lieu.getBlock();
        Bukkit.getScheduler().runTask(plugin, () -> actualiserPanneaux(bloc));
    }

    /** Chunk chargé : panneaux de ses boutiques à jour (ancienne présentation, stock changé). */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        String monde = event.getWorld().getName();
        int x = event.getChunk().getX();
        int z = event.getChunk().getZ();
        List<Boutique> ici = new ArrayList<>();
        for (Boutique b : magasins.boutiques.values()) {
            if (b.panneau.getWorld() != null && b.panneau.getWorld().getName().equals(monde)
                    && b.panneau.getBlockX() >> 4 == x && b.panneau.getBlockZ() >> 4 == z) {
                ici.add(b);
            }
        }
        if (!ici.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> ici.forEach(b -> {
                if (magasins.boutiques.containsKey(b.id)) {
                    ecrirePanneau(b);
                }
            }));
        }
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
        // 1.1.3 : pas d'achat dans sa propre boutique ; le propriétaire arrive sur sa gestion.
        if (b.proprio.equals(joueur.getUniqueId())) {
            plugin.menuMagasin().gererBoutique(joueur, b);
            return;
        }
        if (magasins.verrouillee(b)) {
            message(joueur, t("boutique.verrouillee", "<red>Le propriétaire gère cette boutique : réessaie dans un instant."));
            return;
        }
        if (!utiliser(joueur, b)) {
            return;
        }
        List<Component> corps = new ArrayList<>();
        corps.add(t("achat.nom", "<gold><bold><nom>", "nom", nomBoutique(b)));
        corps.add(t("achat.vend", "<white>Vend : ").append(lot(b.quantite, b.objet)));
        corps.add(t("achat.contre", "<white>Contre : ").append(prix(b)));
        corps.add(t("achat.etat", "<white>État : <etat>", "etat", etatTexte(b)));
        ActionButton acheter = gui.form(t("achat.bouton", "<green>Acheter"), null,
                (p, vue) -> acheter(p, b, entier(vue.getText("lots"))));
        ActionButton voir = gui.button(t("achat.bouton-voir", "<aqua>Voir l'objet"),
                t("achat.voir-info", "<gray>L'objet exact que tu vas recevoir"), p -> voirObjet(p, b));
        ActionButton signaler = gui.button(t("achat.bouton-signaler", "<red>Signaler la boutique"),
                t("achat.signaler-info", "<gray>Arnaque, contenu inapproprié..."),
                p -> plugin.signalements().signalerBoutique(p, b, q -> ouvrirAchat(q, b)));
        // 1.2.0 : favori.
        boolean favori = plugin.favoris().contient(joueur.getUniqueId(), Favoris.boutique(b.id));
        ActionButton etoile = gui.button(favori ? t("favoris.retirer", "<yellow>Retirer des favoris")
                : t("favoris.ajouter", "<yellow>Ajouter aux favoris"), null, p -> {
            plugin.favoris().basculer(p.getUniqueId(), Favoris.boutique(b.id));
            ouvrirAchat(p, b);
        });
        gui.open(joueur, t("achat.titre", "<gold><bold>Boutique de <proprio>", "proprio", nomJoueur(b.proprio)), corps,
                List.of(gui.text("lots", t("achat.champ-lots", "Nombre de lots"), "1", 4)),
                List.of(acheter, voir, etoile, signaler), gui.close(), 1);
        lang.saveIfNeeded();
    }

    /**
     * 1.1.3 : « Voir l'objet » : coffre en lecture seule avec l'objet vendu tel quel (nom, enchantements, description :
     * l'achat donne exactement des objets identiques), le résumé de l'offre et l'objet demandé (ou les points).
     * Refermé : retour au menu d'achat.
     */
    void voirObjet(Player joueur, Boutique b) {
        if (!utiliser(joueur, b)) {
            return;
        }
        Inventory vue = Bukkit.createInventory(new Vue(joueur.getUniqueId(), b.id), 27,
                t("voir.titre", "Ce que tu achètes"));
        ItemStack vendu = b.objet.clone();
        vendu.setAmount(Math.max(1, Math.min(b.quantite, vendu.getMaxStackSize())));
        vue.setItem(11, vendu);
        ItemStack resume = new ItemStack(Material.PAPER);
        ItemMeta meta = resume.getItemMeta();
        meta.displayName(t("voir.resume", "<!italic><gold>Vend <lot>", "lot", lot(b.quantite, b.objet)));
        meta.lore(List.of(t("voir.contre", "<!italic><white>contre <prix>", "prix", prix(b)),
                t("voir.exact", "<!italic><gray>Tu reçois exactement l'objet de gauche"),
                t("voir.exact-2", "<!italic><gray>(nom, enchantements, description)."),
                t("voir.paiement", "<!italic><gray>À droite : ce que tu paies.")));
        resume.setItemMeta(meta);
        vue.setItem(13, resume);
        ItemStack paye;
        if (b.enPoints()) {
            paye = new ItemStack(Material.EMERALD);
            ItemMeta m = paye.getItemMeta();
            m.displayName(t("voir.points", "<!italic><green><points>", "points", KSEconomy.points(b.prixPoints)));
            paye.setItemMeta(m);
        } else {
            paye = b.prixObjet.clone();
            paye.setAmount(Math.max(1, Math.min(b.prixQuantite, paye.getMaxStackSize())));
        }
        vue.setItem(15, paye);
        joueur.openInventory(vue);
        lang.saveIfNeeded();
    }

    @EventHandler
    public void onClickVue(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Vue) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDragVue(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Vue) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCloseVue(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Vue vue)
                || event.getReason() != InventoryCloseEvent.Reason.PLAYER
                || !(event.getPlayer() instanceof Player joueur)) {
            return;
        }
        Boutique b = magasins.boutiques.get(vue.boutique());
        if (b != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (joueur.isOnline()) {
                    ouvrirAchat(joueur, b);
                }
            });
        }
    }

    static String nomJoueur(UUID joueur) {
        String nom = Bukkit.getOfflinePlayer(joueur).getName();
        return nom == null ? "?" : nom;
    }

    private void acheter(Player joueur, Boutique b, int lots) {
        if (b.proprio.equals(joueur.getUniqueId())) {
            message(joueur, t("achat.soi-meme", "<red>Tu ne peux pas acheter dans ta propre boutique."));
            return;
        }
        if (!magasins.boutiques.containsKey(b.id)) {
            message(joueur, t("achat.disparue", "<red>Cette boutique n'existe plus."));
            return;
        }
        if (magasins.verrouillee(b)) {
            message(joueur, t("boutique.verrouillee", "<red>Le propriétaire gère cette boutique : réessaie dans un instant."));
            return;
        }
        if (b.fermee) {
            message(joueur, t("achat.fermee", "<red>Cette boutique est fermée pour le moment."));
            return;
        }
        if (!utiliser(joueur, b)) {
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
        actualiserPanneaux(Magasins.contenantDu(b.panneau.getBlock()));
        plugin.getLogger().info("Boutique " + b.id + " : " + joueur.getName() + " achète " + lots + " lot(s) à "
                + nomJoueur(b.proprio));
        // 1.2.0 : vente enregistrée (statistiques, notification au propriétaire) ; boutique libérée.
        plugin.ventes().enregistrer(b, joueur, lots, b.enPoints() ? Component.text(KSEconomy.points(points))
                : lot(b.prixQuantite * lots, b.prixObjet));
        liberer(joueur.getUniqueId());
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
            supprimer(b, joueur, false);
        }
    }

    /**
     * Supprime une boutique (les points en attente sont rendus au propriétaire). 1.1.2 : sa place dans le magasin
     * reste prise pendant le délai (3 h, anti « switch ») ; retirerPanneau (suppression depuis le menu, même à
     * distance) : le panneau est retiré et rendu à l'auteur (sinon il est cassé par le joueur, ou tombe avec son
     * contenant).
     */
    void supprimer(Boutique b, Player auteur, boolean retirerPanneau) {
        supprimer(b, auteur, retirerPanneau, true);
    }

    /** delai false : suppression par le staff (1.1.3, signalements), la place n'est pas bloquée. */
    void supprimer(Boutique b, Player auteur, boolean retirerPanneau, boolean delai) {
        magasins.boutiques.remove(b.id);
        if (delai) {
            magasins.noterSuppression(b.proprio);
        }
        if (b.pointsEnAttente > 0) {
            KSEconomy.crediter(b.proprio, b.pointsEnAttente);
        }
        magasins.sauver();
        Block bloc = b.panneau.getBlock();
        boolean rendu = false;
        if (retirerPanneau && bloc.getState() instanceof Sign) {
            Material objet = Material.matchMaterial(bloc.getType().name().replace("_WALL_", "_"));
            bloc.setType(Material.AIR, false);
            if (objet != null && objet.isItem() && auteur != null) {
                for (ItemStack reste : auteur.getInventory().addItem(new ItemStack(objet)).values()) {
                    auteur.getWorld().dropItemNaturally(auteur.getLocation(), reste);
                }
                rendu = true;
            }
        } else if (bloc.getState() instanceof Sign panneau) {
            panneau.setWaxed(false);
            panneau.update();
        }
        if (auteur != null) {
            long attente = delai ? magasins.delaiSuppressionMs() : 0;
            auteur.sendMessage(t("boutique.supprimee-2", "<yellow>Boutique supprimée<points><panneau>.<delai>",
                    "points", b.pointsEnAttente > 0 ? " (" + KSEconomy.points(b.pointsEnAttente) + " rendus)" : "",
                    "panneau", rendu ? ", panneau rendu" : "",
                    "delai", attente > 0 ? " Sa place dans le magasin se libère dans " + duree(attente) + "." : ""));
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
