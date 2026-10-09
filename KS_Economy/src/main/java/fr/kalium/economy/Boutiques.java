package fr.kalium.economy;

import fr.kalium.economy.Magasins.Boutique;
import fr.kalium.economy.Magasins.Magasin;
import fr.kalium.menu.api.Contenant;
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
import java.util.function.Consumer;

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
 *
 * 1.5.0 (LeKiwi06, 09/10/2026) : menus en coffres (voir Menus) : création (choix de l'objet dans son inventaire,
 * quantité, prix, récapitulatif), menu d'achat (l'objet exact, l'offre, le paiement, 1 / 5 / 10 lots). Restent en
 * fenêtre de Gui les saisies : nom d'un objet, quantité ou nombre de lots libre, prix en points, nom de la boutique.
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
        /** 1.5.0 : nom choisi au récapitulatif (null : nom de l'objet vendu). */
        String nom;
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

    private final KSEconomy plugin;
    private final Magasins magasins;
    private final Lang lang;
    private final Gui gui;
    private final Menus menus;
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
        this.menus = plugin.menus();
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

    /** Refus ou information : message court (1.5.0 : au-dessus de la barre d'objets, plus de fenêtre). */
    private void message(Player joueur, Component texte) {
        menus.message(joueur, texte, true);
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

    /** Abandon de la création : rien n'est gardé, le coffre se referme. */
    private void annuler(Player joueur) {
        creations.remove(joueur.getUniqueId());
        Contenant.fermer(joueur);
    }

    private void boutonAnnuler(Contenant c) {
        c.poser(c.bas(5), Contenant.objet(Material.BARRIER, t("boutique.annuler", "<red>Annuler"), List.of()),
                this::annuler);
    }

    /** 1.5.0 : coffre « Faire de ce panneau une boutique ? ». */
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
        Contenant menu = menus.menu(joueur, 4, t("boutique.titre-coffre", "<dark_gray>Nouvelle boutique ?"),
                "boutique-proposer");
        menu.poser(11, Menus.objet(Material.EMERALD, t("boutique.bouton-creer", "<green>Créer une boutique"),
                t("boutique.proposer-info", "<gray>Le contenu du contenant sera son stock ; les paiements en objets y "
                        + "arriveront.")), p -> choisirObjet(p, Etape.VENTE));
        menu.poser(15, Menus.objet(Material.OAK_SIGN, t("boutique.bouton-panneau", "<white>Non, un panneau ordinaire"),
                t("boutique.panneau-info", "<gray>Le panneau reste un simple panneau")), this::annuler);
        boutonAnnuler(menu);
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    private Creation creation(Player joueur) {
        return creations.get(joueur.getUniqueId());
    }

    /**
     * 1.5.0 : choix de l'objet (vendu, ou demandé en paiement) : le coffre montre l'inventaire du joueur, un clic
     * choisit l'objet tel quel (objets spéciaux compris) ; « Écrire le nom » pour un objet qu'on n'a pas sur soi.
     */
    private void choisirObjet(Player joueur, Etape etape) {
        if (creation(joueur) == null) {
            return;
        }
        Contenant c = menus.menu(joueur, 6, etape == Etape.VENTE
                ? t("boutique.titre-vente-coffre", "<dark_gray>Clique sur l'objet à vendre")
                : t("boutique.titre-prix-coffre", "<dark_gray>Clique sur l'objet demandé"), "boutique-objet-" + etape);
        ItemStack[] contenu = joueur.getInventory().getStorageContents();
        boolean vide = true;
        for (int i = 0; i < contenu.length && i < 36; i++) {
            if (contenu[i] == null || contenu[i].getType().isAir()) {
                continue;
            }
            vide = false;
            ItemStack objet = contenu[i].clone();
            ItemStack choix = objet.clone();
            choix.setAmount(1);
            // Même disposition que l'inventaire : ses 3 rangées, puis la barre d'objets.
            c.poser(i < 9 ? 27 + i : i - 9, objet, p -> choisi(p, etape, choix));
        }
        if (vide) {
            c.poser(13, Menus.objet(Material.PAPER, t("boutique.inventaire-vide", "<gray>Ton inventaire est vide"),
                    t("boutique.inventaire-vide-info", "<dark_gray>Utilise « Écrire le nom » en bas.")), null);
        }
        menus.aide(c, t("boutique.choix-titre", "<aqua>Choisir l'objet"),
                t("boutique.choix-aide-coffre", "<gray>Clique sur un objet de ton inventaire (objets spéciaux "
                        + "compris), ou écris son nom français ou son id (« diamant », « diamond »)."));
        c.poser(c.bas(4), Menus.objet(Material.NAME_TAG, t("boutique.ecrire", "<white>Écrire le nom"),
                t("boutique.ecrire-info", "<gray>Pour un objet que tu n'as pas sur toi")), p -> ecrireNom(p, etape));
        if (etape == Etape.VENTE) {
            boutonAnnuler(c);
        } else {
            menus.sortie(c, this::choisirPrix);
        }
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Nom de l'objet : saisie de texte (fenêtre de Gui) ; plusieurs objets trouvés : un coffre pour choisir. */
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
                Contenant c = menus.menu(p, 4, t("boutique.resultats-coffre", "<dark_gray>Quel objet ?"),
                        "boutique-resultats");
                for (int i = 0; i < trouves.size() && i < 27; i++) {
                    Material m = trouves.get(i);
                    c.poser(i, Contenant.objet(m, nomObjet(new ItemStack(m)), List.of()),
                            q -> choisi(q, etape, new ItemStack(m)));
                }
                c.poser(c.bas(4), Contenant.objet(Material.NAME_TAG, t("boutique.autre-nom-coffre", "<white>Autre nom"),
                        List.of()), q -> ecrireNom(q, etape));
                menus.sortie(c, q -> choisirObjet(q, etape));
                c.ouvrir(p);
            }
            lang.saveIfNeeded();
        });
        Contenant.saisie(joueur, () -> {
            gui.open(joueur, t("boutique.titre-nom", "<gold><bold>Nom de l'objet"), List.of(),
                    List.of(gui.text("nom", t("boutique.champ-nom", "Nom ou id"), "", 64)),
                    List.of(chercher, gui.button(t("boutique.retour", "<gray>Retour"), null,
                            p -> choisirObjet(p, etape))), gui.close(), 1);
            lang.saveIfNeeded();
        });
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

    /** Quantités proposées d'un clic ; les autres se saisissent. */
    private static final int[] QUANTITES = {1, 2, 4, 8, 16, 32, 64};

    /** 1.5.0 : quantité par lot : 1 à 64 d'un clic, ou « Autre quantité » (saisie, jusqu'à 2 304). */
    private void quantite(Player joueur, Etape etape) {
        Creation c = creation(joueur);
        if (c == null) {
            return;
        }
        ItemStack objet = etape == Etape.VENTE ? c.objet : c.prixObjet;
        Contenant menu = menus.menu(joueur, 4, etape == Etape.VENTE
                ? t("boutique.titre-quantite-vente", "<dark_gray>Quantité vendue par lot")
                : t("boutique.titre-quantite-prix", "<dark_gray>Quantité demandée par lot"), "boutique-quantite-" + etape);
        menu.poser(4, Menus.telQuel(objet, 1), null);
        for (int i = 0; i < QUANTITES.length; i++) {
            int n = QUANTITES[i];
            menu.poser(10 + i, Contenant.objet(Menus.telQuel(objet, n),
                    t("boutique.quantite-bouton", "<yellow><quantite> par lot", "quantite", n), List.of()),
                    p -> quantiteChoisie(p, etape, n));
        }
        menu.poser(menu.bas(4), Menus.objet(Material.OAK_SIGN, t("boutique.autre-quantite", "<aqua>Autre quantité"),
                t("boutique.autre-quantite-info", "<gray>Écrire la quantité (1 à <max>)", "max", QUANTITE_MAX)),
                p -> saisirQuantite(p, etape));
        menus.sortie(menu, p -> choisirObjet(p, etape));
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    private void quantiteChoisie(Player joueur, Etape etape, int n) {
        Creation c = creation(joueur);
        if (c == null) {
            return;
        }
        if (etape == Etape.VENTE) {
            c.quantite = n;
            choisirPrix(joueur);
        } else {
            c.prixQuantite = n;
            c.prixEnPoints = false;
            recapitulatif(joueur);
        }
    }

    private void saisirQuantite(Player joueur, Etape etape) {
        ActionButton valider = gui.form(t("boutique.valider", "<green>Valider"), null, (p, vue) -> {
            int n = entier(vue.getText("quantite"));
            if (n < 1 || n > QUANTITE_MAX) {
                gui.notice(p, t("boutique.titre", "<gold><bold>Boutique"),
                        t("boutique.quantite-invalide", "<red>Quantité invalide (1 à <max>).", "max", QUANTITE_MAX),
                        q -> saisirQuantite(q, etape));
                lang.saveIfNeeded();
                return;
            }
            quantiteChoisie(p, etape, n);
        });
        Contenant.saisie(joueur, () -> {
            Creation c = creation(joueur);
            if (c == null) {
                return;
            }
            gui.open(joueur, t("boutique.titre-quantite", "<gold><bold>Quantité"),
                    List.of(t("boutique.quantite-objet", "<white>Objet : ")
                            .append(nomObjet(etape == Etape.VENTE ? c.objet : c.prixObjet))),
                    List.of(gui.text("quantite", t("boutique.champ-quantite", "Quantité par lot"), "1", 5)),
                    List.of(valider, gui.button(t("boutique.retour", "<gray>Retour"), null, p -> quantite(p, etape))),
                    gui.close(), 1);
            lang.saveIfNeeded();
        });
    }

    /** 1.5.0 : prix d'un lot : en points (saisie) ou en objet (choix dans l'inventaire). */
    private void choisirPrix(Player joueur) {
        Creation c = creation(joueur);
        if (c == null) {
            return;
        }
        Contenant menu = menus.menu(joueur, 4, t("boutique.titre-prix-choix-coffre", "<dark_gray>Prix d'un lot"),
                "boutique-prix");
        menu.poser(4, Contenant.objet(Menus.telQuel(c.objet, c.quantite),
                t("boutique.lot-vendu", "<yellow>Lot vendu : <lot>", "lot", lot(c.quantite, c.objet)), List.of()), null);
        menu.poser(11, Menus.objet(Material.EMERALD, t("boutique.monnaie", "<green>Monnaie (points)"),
                t("boutique.monnaie-info", "<gray>Les points payés attendent dans la boutique : tu les récupères "
                        + "depuis ton magasin.")), this::prixEnPoints);
        menu.poser(15, Menus.objet(Material.CHEST, t("boutique.objet", "<white>Objet"),
                t("boutique.objet-info", "<gray>L'objet payé arrive dans le contenant de la boutique.")),
                p -> choisirObjet(p, Etape.PRIX));
        menus.sortie(menu, p -> quantite(p, Etape.VENTE));
        menu.ouvrir(joueur);
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
        Contenant.saisie(joueur, () -> {
            gui.open(joueur, t("boutique.titre-points", "<gold><bold>Prix en points"), List.of(),
                    List.of(gui.text("points", t("boutique.champ-points", "Points par lot"), "", 12)),
                    List.of(valider, gui.button(t("boutique.retour", "<gray>Retour"), null, this::choisirPrix)),
                    gui.close(), 1);
            lang.saveIfNeeded();
        });
    }

    /** Nom que portera la boutique : celui choisi, sinon l'objet vendu. */
    private static String nomPrevu(Creation c) {
        return c.nom != null ? c.nom : couper(texteObjet(c.objet), Magasins.NOM_MAX);
    }

    /**
     * Récapitulatif (1.5.0 : coffre) : l'objet vendu à gauche, le prix à droite ; en bas, le nom de la boutique (1.1.2 :
     * choisi ici, l'objet vendu par défaut) et « Créer la boutique ».
     */
    private void recapitulatif(Player joueur) {
        Creation c = creation(joueur);
        if (c == null) {
            return;
        }
        Component prix = c.prixEnPoints ? Component.text(KSEconomy.points(c.prixPoints)) : lot(c.prixQuantite, c.prixObjet);
        Contenant menu = menus.menu(joueur, 4, t("boutique.titre-recap-coffre", "<dark_gray>Nouvelle boutique"),
                "boutique-recap");
        menu.poser(11, Menus.telQuel(c.objet, c.quantite), null);
        menu.poser(13, Menus.objet(Material.PAPER, t("boutique.recap-nom", "<gold><nom>", "nom", nomPrevu(c)),
                t("boutique.recap-vend", "<white>Vend <lot>", "lot", lot(c.quantite, c.objet)),
                t("boutique.recap-contre", "<white>contre <prix>", "prix", prix)), null);
        menu.poser(15, c.prixEnPoints
                ? Contenant.objet(Material.EMERALD, t("voir.points", "<!italic><green><points>", "points",
                        KSEconomy.points(c.prixPoints)), List.of())
                : Menus.telQuel(c.prixObjet, c.prixQuantite), null);
        menu.poser(menu.bas(3), Menus.objet(Material.NAME_TAG, t("boutique.recap-renommer", "<white>Nom : <nom>", "nom",
                        nomPrevu(c)), t("boutique.recap-renommer-info", "<gray>Clic : choisir un autre nom")),
                this::nommer);
        menu.poser(menu.bas(4), Contenant.objet(Material.EMERALD_BLOCK,
                t("boutique.bouton-creer-nom", "<green>Créer la boutique"), List.of()), p -> {
            Creation cc = creation(p);
            creer(p, cc == null ? null : cc.nom);
        });
        boutonAnnuler(menu);
        menu.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Nom de la boutique : saisie de texte (fenêtre de Gui), puis retour au récapitulatif. */
    private void nommer(Player joueur) {
        ActionButton valider = gui.form(t("boutique.valider", "<green>Valider"), null, (p, vue) -> {
            Creation c = creation(p);
            if (c != null) {
                c.nom = nettoyerNom(vue.getText("nom"));
                recapitulatif(p);
            }
        });
        Contenant.saisie(joueur, () -> {
            Creation c = creation(joueur);
            if (c == null) {
                return;
            }
            gui.open(joueur, t("magasin.titre-renommer", "<gold><bold>Nom de la boutique"),
                    List.of(t("magasin.renommer-aide", "<gray>Vide : le nom de l'objet vendu.")),
                    List.of(gui.text("nom", t("boutique.champ-nom-boutique", "Nom de la boutique (20 caractères)"),
                            nomPrevu(c), Magasins.NOM_MAX)),
                    List.of(valider, gui.button(t("boutique.retour", "<gray>Retour"), null, this::recapitulatif)),
                    gui.close(), 1);
            lang.saveIfNeeded();
        });
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
            Contenant.fermer(joueur);
            message(joueur, t("boutique.plus-possible", "<red>Ce panneau ne peut plus devenir une boutique."));
            return;
        }
        Component refus = refusComplet(joueur, magasin);
        if (refus != null) {
            Contenant.fermer(joueur);
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
        Contenant.fermer(joueur);
        menus.message(joueur, t("boutique.creee", "<green>Boutique créée."), false);
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

    /**
     * 1.5.0 : l'objet d'une boutique dans une liste : l'objet vendu (pile du lot), nom de la boutique coloré selon son
     * état, puis l'offre, l'état et les lignes en plus.
     */
    ItemStack icone(Boutique b, Component... plus) {
        List<Component> lignes = new ArrayList<>(Menus.lignes(
                t("boutique.ligne-vend", "<gray>Vend <white><lot>", "lot", lot(b.quantite, b.objet)),
                t("boutique.ligne-contre", "<gray>contre <white><prix>", "prix", prix(b)),
                etatTexte(b)));
        lignes.addAll(Menus.lignes(plus));
        return Contenant.objet(Menus.telQuel(b.objet, b.quantite), boutonBoutique(b), lignes);
    }

    /** Menu d'achat ouvert en cliquant sur le panneau : pas d'écran précédent. */
    void ouvrirAchat(Player joueur, Boutique b) {
        ouvrirAchat(joueur, b, null);
    }

    /** Nombres de lots achetés d'un clic ; les autres se saisissent. */
    private static final int[] LOTS = {1, 5, 10};

    /**
     * Menu d'achat (sur place ou depuis le catalogue). 1.5.0 : coffre. À gauche l'objet exact vendu (nom,
     * enchantements, description : l'achat donne des objets identiques ; remplace « Voir l'objet » de 1.1.3), au
     * centre l'offre, à droite ce qu'on paie ; dessous, acheter 1, 5 ou 10 lots, ou un autre nombre (saisie).
     *
     * @param retour écran précédent (« Retour »), ou null (« Fermer »)
     */
    void ouvrirAchat(Player joueur, Boutique b, Consumer<Player> retour) {
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
        Contenant c = menus.menu(joueur, 5, t("achat.titre-coffre", "<dark_gray>Boutique de <proprio>", "proprio",
                nomJoueur(b.proprio)), "achat:" + b.id);
        c.poser(11, Menus.telQuel(b.objet, b.quantite), null);
        c.poser(13, Menus.objet(Material.PAPER, t("achat.nom", "<gold><bold><nom>", "nom", nomBoutique(b)),
                t("achat.vend", "<white>Vend : ").append(lot(b.quantite, b.objet)),
                t("achat.contre", "<white>Contre : ").append(prix(b)),
                etatTexte(b),
                t("voir.exact", "<!italic><gray>Tu reçois exactement l'objet de gauche"),
                t("voir.exact-2", "<!italic><gray>(nom, enchantements, description)."),
                t("voir.paiement", "<!italic><gray>À droite : ce que tu paies.")), null);
        c.poser(15, b.enPoints()
                ? Contenant.objet(Material.EMERALD, t("voir.points", "<!italic><green><points>", "points",
                        KSEconomy.points(b.prixPoints)), List.of())
                : Menus.telQuel(b.prixObjet, b.prixQuantite), null);
        for (int i = 0; i < LOTS.length; i++) {
            int lots = LOTS[i];
            c.poser(29 + i, Contenant.objet(Menus.telQuel(new ItemStack(Material.EMERALD), lots),
                    t("achat.bouton-lots", "<green>Acheter <lots> lot(s)", "lots", lots),
                    List.of(t("achat.recoit", "<gray>Tu reçois <white><lot>", "lot", lot(b.quantite * lots, b.objet)),
                            t("achat.paie", "<gray>Tu paies <white><prix>", "prix", b.enPoints()
                                    ? Component.text(KSEconomy.points(b.prixPoints * lots))
                                    : lot(b.prixQuantite * lots, b.prixObjet)))),
                    p -> acheter(p, b, lots, retour));
        }
        c.poser(33, Menus.objet(Material.OAK_SIGN, t("achat.bouton-autre", "<aqua>Autre nombre de lots"),
                t("achat.autre-info", "<gray>Écrire le nombre de lots à acheter")), p -> saisirLots(p, b, retour));
        // 1.2.0 : favori.
        boolean favori = plugin.favoris().contient(joueur.getUniqueId(), Favoris.boutique(b.id));
        c.poser(c.bas(3), Contenant.objet(favori ? Material.NETHER_STAR : Material.FIREWORK_STAR,
                favori ? t("favoris.retirer", "<yellow>Retirer des favoris")
                        : t("favoris.ajouter", "<yellow>Ajouter aux favoris"), List.of()), p -> {
            plugin.favoris().basculer(p.getUniqueId(), Favoris.boutique(b.id));
            ouvrirAchat(p, b, retour);
        });
        menus.sortie(c, retour);
        c.poser(c.bas(6), Menus.objet(Material.REDSTONE_TORCH, t("achat.bouton-signaler", "<red>Signaler la boutique"),
                t("achat.signaler-info", "<gray>Arnaque, contenu inapproprié...")),
                p -> plugin.signalements().signalerBoutique(p, b, q -> ouvrirAchat(q, b, retour)));
        c.ouvrir(joueur);
        lang.saveIfNeeded();
    }

    /** Nombre de lots : saisie (fenêtre de Gui), puis retour au menu d'achat. */
    private void saisirLots(Player joueur, Boutique b, Consumer<Player> retour) {
        ActionButton acheter = gui.form(t("achat.bouton", "<green>Acheter"), null, (p, vue) -> {
            int lots = entier(vue.getText("lots"));
            ouvrirAchat(p, b, retour);
            acheter(p, b, lots, retour);
        });
        Contenant.saisie(joueur, () -> {
            gui.open(joueur, t("achat.titre", "<gold><bold>Boutique de <proprio>", "proprio", nomJoueur(b.proprio)),
                    List.of(t("achat.vend", "<white>Vend : ").append(lot(b.quantite, b.objet)),
                            t("achat.contre", "<white>Contre : ").append(prix(b))),
                    List.of(gui.text("lots", t("achat.champ-lots", "Nombre de lots"), "1", 4)),
                    List.of(acheter, gui.button(t("boutique.retour", "<gray>Retour"), null,
                            p -> ouvrirAchat(p, b, retour))), gui.close(), 1);
            lang.saveIfNeeded();
        });
    }

    static String nomJoueur(UUID joueur) {
        String nom = Bukkit.getOfflinePlayer(joueur).getName();
        return nom == null ? "?" : nom;
    }

    private void acheter(Player joueur, Boutique b, int lots, Consumer<Player> retour) {
        if (b.proprio.equals(joueur.getUniqueId())) {
            message(joueur, t("achat.soi-meme", "<red>Tu ne peux pas acheter dans ta propre boutique."));
            return;
        }
        if (!magasins.boutiques.containsKey(b.id)) {
            if (retour != null) {
                retour.accept(joueur);
            } else {
                Contenant.fermer(joueur);
            }
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
        ouvrirAchat(joueur, b, retour);
        menus.message(joueur, t("achat.fait", "<green>Acheté : <lots> lot(s).", "lots", lots), false);
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
