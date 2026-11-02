package fr.kalium.encapsuleur;

import fr.kalium.menu.api.Contenant;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.structure.Structure;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Aperçu d'une pose (choix de LeKiwi06 le 02/11) : la construction se cale sur le bloc visé, face avant vers le joueur
 * ; le contour (faux blocs envoyés au seul joueur : vitre bleue, face avant en vitre verte, coins en lanternes de mer)
 * montre où elle sera. Le contour du bloc de structure du jeu ne sert pas : seuls les opérateurs le voient, et Bedrock
 * ne l'a pas. Changer d'objet en main, de monde ou se déconnecter annule l'aperçu.
 */
final class Apercu implements Listener {

    static final class Etat {
        final String id;
        final Structure construction;
        final List<BlockState> blocs;
        final BlockFace avant;
        final Block ancre;
        final BlockFace regard;
        int tours;
        boolean miroir;
        int avance;
        int droite;
        int haut;
        final Map<BlockPosition, BlockData> envoyes = new HashMap<>();

        Etat(String id, Structure construction, Block ancre, BlockFace regard) {
            this.id = id;
            this.construction = construction;
            this.blocs = Constructions.aPoser(construction);
            String a = construction.getPersistentDataContainer().get(Constructions.AVANT,
                    org.bukkit.persistence.PersistentDataType.STRING);
            BlockFace f;
            try {
                f = a == null ? BlockFace.SOUTH : BlockFace.valueOf(a);
            } catch (IllegalArgumentException inconnu) {
                f = BlockFace.SOUTH;
            }
            this.avant = f;
            this.ancre = ancre;
            this.regard = regard;
        }

        int sx() {
            return construction.getSize().getBlockX();
        }

        int sy() {
            return construction.getSize().getBlockY();
        }

        int sz() {
            return construction.getSize().getBlockZ();
        }

        Forme forme() {
            Mirror m = miroir ? Mirror.LEFT_RIGHT : Mirror.NONE;
            int base = 0;
            for (int k = 0; k < 4; k++) {
                if (new Forme(sx(), sy(), sz(), Forme.tours(k), m).tourner(avant) == regard.getOppositeFace()) {
                    base = k;
                    break;
                }
            }
            return new Forme(sx(), sy(), sz(), Forme.tours(base + tours), m);
        }

        /** Coin du bas (x, y, z les plus petits) de la boîte posée. */
        int[] origine(Forme f) {
            int tx = f.tx(), tz = f.tz();
            int ax = ancre.getX(), az = ancre.getZ();
            int ox, oz;
            switch (regard) {
                case NORTH -> { ox = ax - tx / 2; oz = az - tz + 1; }
                case SOUTH -> { ox = ax - tx / 2; oz = az; }
                case EAST -> { ox = ax; oz = az - tz / 2; }
                default -> { ox = ax - tx + 1; oz = az - tz / 2; }
            }
            BlockFace droiteCap = Forme.CAP.get((Forme.CAP.indexOf(regard) + 1) % 4);
            ox += regard.getModX() * avance + droiteCap.getModX() * droite;
            oz += regard.getModZ() * avance + droiteCap.getModZ() * droite;
            return new int[]{ox, ancre.getY() + haut, oz};
        }
    }

    private final KSEncapsuleur plugin;
    private final Map<UUID, Etat> etats = new HashMap<>();

    Apercu(KSEncapsuleur plugin) {
        this.plugin = plugin;
    }

    /** Clic droit avec une construction : nouvel aperçu sur le bloc visé, ou menu de l'aperçu en cours. */
    void clic(Player joueur, ItemStack objet, String id, Block vise, BlockFace face) {
        Etat etat = etats.get(joueur.getUniqueId());
        if (etat != null && etat.id.equals(id)) {
            Menus.pose(plugin, joueur, etat);
            return;
        }
        if (etat != null) {
            annuler(joueur);
        }
        if (vise == null || face == null) {
            plugin.message(joueur, plugin.lang.c("message.viser", "<red>Vise le bloc où poser la construction."), true);
            return;
        }
        Structure construction = Constructions.charger(plugin, id);
        if (construction == null) {
            plugin.message(joueur, plugin.lang.c("message.introuvable",
                    "<red>Cette construction n'existe plus (déjà posée ?)."), true);
            return;
        }
        etat = new Etat(id, construction, vise.getRelative(face), joueur.getFacing());
        etats.put(joueur.getUniqueId(), etat);
        afficher(joueur, etat, Map.of());
        Menus.pose(plugin, joueur, etat);
    }

    /** Un bouton du menu : réglage changé, contour renvoyé, menu rouvert. */
    void changer(Player joueur, Consumer<Etat> reglage) {
        Etat etat = etats.get(joueur.getUniqueId());
        if (etat == null) {
            Contenant.fermer(joueur);
            return;
        }
        reglage.accept(etat);
        afficher(joueur, etat, Map.of());
        Menus.pose(plugin, joueur, etat);
    }

    void annuler(Player joueur) {
        Etat etat = etats.remove(joueur.getUniqueId());
        if (etat != null) {
            effacer(joueur, etat);
        }
    }

    void toutAnnuler() {
        for (UUID u : List.copyOf(etats.keySet())) {
            Player joueur = Bukkit.getPlayer(u);
            if (joueur != null) {
                annuler(joueur);
            }
        }
        etats.clear();
    }

    // ------------------------------------------------------------------ contour

    private void effacer(Player joueur, Etat etat) {
        if (etat.envoyes.isEmpty() || !joueur.isOnline() || !joueur.getWorld().equals(etat.ancre.getWorld())) {
            etat.envoyes.clear();
            return;
        }
        Map<BlockPosition, BlockData> vrais = new HashMap<>();
        World monde = etat.ancre.getWorld();
        for (BlockPosition p : etat.envoyes.keySet()) {
            vrais.put(p, monde.getBlockAt(p.blockX(), p.blockY(), p.blockZ()).getBlockData());
        }
        joueur.sendMultiBlockChange(vrais);
        etat.envoyes.clear();
    }

    /** Contour de la boîte, et les blocs gênants en vitre rouge (400 au plus). */
    private void afficher(Player joueur, Etat etat, Map<BlockPosition, BlockData> genants) {
        effacer(joueur, etat);
        Forme f = etat.forme();
        int[] o = etat.origine(f);
        int x2 = o[0] + f.tx() - 1, y2 = o[1] + etat.sy() - 1, z2 = o[2] + f.tz() - 1;
        BlockData bleu = Material.LIGHT_BLUE_STAINED_GLASS.createBlockData();
        BlockData vert = Material.LIME_STAINED_GLASS.createBlockData();
        BlockData coin = Material.SEA_LANTERN.createBlockData();
        Map<BlockPosition, BlockData> faux = new LinkedHashMap<>(genants);
        for (int x = o[0]; x <= x2; x++) {
            for (int y = o[1]; y <= y2; y++) {
                for (int z = o[2]; z <= z2; z++) {
                    int bords = (x == o[0] || x == x2 ? 1 : 0) + (y == o[1] || y == y2 ? 1 : 0)
                            + (z == o[2] || z == z2 ? 1 : 0);
                    if (bords < 2) {
                        // Seulement les arêtes : on saute l'intérieur des faces.
                        if (bords == 0 && z < z2) {
                            z = z2 - 1;
                        }
                        continue;
                    }
                    boolean faceAvant = switch (etat.regard) {
                        case NORTH -> z == z2;
                        case SOUTH -> z == o[2];
                        case EAST -> x == o[0];
                        default -> x == x2;
                    };
                    faux.putIfAbsent(Position.block(x, y, z), bords == 3 ? coin : faceAvant ? vert : bleu);
                }
            }
        }
        etat.envoyes.putAll(faux);
        joueur.sendMultiBlockChange(faux);
    }

    // ------------------------------------------------------------------ valider

    void valider(Player joueur) {
        Etat etat = etats.get(joueur.getUniqueId());
        if (etat == null) {
            Contenant.fermer(joueur);
            return;
        }
        Forme f = etat.forme();
        int[] o = etat.origine(f);
        World monde = etat.ancre.getWorld();
        if (o[1] < monde.getMinHeight() || o[1] + etat.sy() > monde.getMaxHeight()) {
            plugin.message(joueur, plugin.lang.c("message.hauteur", "<red>La construction sort du monde (trop haut ou trop bas)."),
                    true);
            return;
        }
        for (int cx = o[0] >> 4; cx <= (o[0] + f.tx() - 1) >> 4; cx++) {
            for (int cz = o[2] >> 4; cz <= (o[2] + f.tz() - 1) >> 4; cz++) {
                if (!Integration.droits(monde.getChunkAt(cx, cz), joueur)) {
                    plugin.message(joueur, plugin.lang.c("message.claim-pose",
                            "<red>Toute la zone doit être dans des claims où tu as les droits."), true);
                    return;
                }
            }
        }
        Map<Material, List<Block>> genants = new LinkedHashMap<>();
        Map<BlockPosition, BlockData> rouges = new LinkedHashMap<>();
        BlockData rouge = Material.RED_STAINED_GLASS.createBlockData();
        for (BlockState b : etat.blocs) {
            int[] p = f.position(b.getX(), b.getY(), b.getZ());
            Block cible = monde.getBlockAt(o[0] + p[0], o[1] + p[1], o[2] + p[2]);
            if (!Analyse.remplacable(cible.getType()) || plugin.chantiers.protege(cible)) {
                genants.computeIfAbsent(cible.getType(), t -> new java.util.ArrayList<>()).add(cible);
                if (rouges.size() < 400) {
                    rouges.put(Position.block(cible.getX(), cible.getY(), cible.getZ()), rouge);
                }
            }
        }
        if (!genants.isEmpty()) {
            afficher(joueur, etat, rouges);
            plugin.message(joueur, plugin.lang.c("message.genants",
                    "<red>Pose refusée : des blocs gênent (en rouge). Seuls l'air, l'eau, l'herbe et la neige sont remplacés."),
                    true);
            Menus.liste(plugin, joueur, plugin.lang.c("genants.titre", "Blocs gênants"), genants,
                    j -> Menus.pose(plugin, j, etat));
            return;
        }
        if (plugin.chantiers.enCours(etat.id)) {
            plugin.message(joueur, plugin.lang.c("message.deja", "<red>Cette construction est déjà en cours de pose."),
                    true);
            return;
        }
        if (!plugin.consommer(joueur, etat.id)) {
            plugin.message(joueur, plugin.lang.c("message.plus-la", "<red>La construction n'est plus dans ton inventaire."),
                    true);
            annuler(joueur);
            Contenant.fermer(joueur);
            return;
        }
        annuler(joueur);
        Contenant.fermer(joueur);
        StructureRotation r = f.rotation();
        plugin.getLogger().info(joueur.getName() + " pose la construction " + etat.id + " dans " + monde.getName()
                + " à " + o[0] + " " + o[1] + " " + o[2] + " (" + r + ", " + f.miroir() + ").");
        plugin.chantiers.poser(etat.id, monde, o[0], o[1], o[2], r, f.miroir(), etat.construction);
        plugin.message(joueur, plugin.lang.c("message.pose", "<green>La construction se pose petit à petit."), false);
    }

    // ------------------------------------------------------------------ fin de l'aperçu

    @EventHandler
    public void onChangerObjet(PlayerItemHeldEvent event) {
        Etat etat = etats.get(event.getPlayer().getUniqueId());
        if (etat == null) {
            return;
        }
        ItemStack nouveau = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (!"construction".equals(plugin.sorte(nouveau)) || !etat.id.equals(plugin.idDe(nouveau))) {
            annuler(event.getPlayer());
        }
    }

    @EventHandler
    public void onMonde(PlayerChangedWorldEvent event) {
        etats.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuitter(PlayerQuitEvent event) {
        etats.remove(event.getPlayer().getUniqueId());
    }
}
