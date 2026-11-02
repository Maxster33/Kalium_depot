package fr.kalium.blocs;

import fr.kalium.menu.api.Lang;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * KS_Blocs (cahier local Blocs_Cultures_Objets.md, partie 0, LeKiwi06, 01-02/11/2026) : socle des blocs custom de
 * Kixster, par remplacement visuel.
 *
 * Le bloc musical, la canne à sucre et le varech ont un seul modèle pour tous leurs états. Le serveur garde de vrais
 * blocs (un bloc musical pour un bloc plein) ; KS_Blocs retient la position de chaque bloc custom (données des chunks,
 * {@link Registre}) et réécrit les paquets envoyés aux joueurs ({@link Reecriture}) : les vrais blocs de ces types sont
 * montrés dans un seul état, les autres états (apparences.json) sont réservés aux blocs custom. Le pack Java
 * (KS_Blocs/pack/blocs.py) et Geyser (geyser-bedrock, correspondance des blocs) donnent une texture à chaque état
 * réservé. Choix de LeKiwi06 : les vrais blocs musicaux restent utilisables.
 *
 * API pour les autres plugins (cultures, répéteur wifi, boîte aux lettres, encapsuleur) : {@link #declarer},
 * {@link #objet}, {@link #type}, {@link #variante}, {@link #poser}, {@link #enregistrer}, {@link #changerVariante},
 * {@link #retirer}, {@link #deplacer}, {@link #renvoyer}, {@link #verifierAutour}. Le plugin qui déclare un type
 * ajoute « depend: [KS_Blocs] » et déclare ses types dans son onEnable.
 *
 * Premier bloc : les jack o'lanterns bleue et cuivre ({@link JackOLanterns}). 0.2.0 : blocs custom à base de canne à
 * sucre ou de varech (cultures de KS_Cultures, {@link Plantes}).
 *
 * Code du serveur Paper 26.3 (paquets, sections de chunk) : à revérifier à chaque mise à jour de Minecraft. Ne pas
 * recharger le plugin à chaud (/reload).
 */
public final class KSBlocs extends JavaPlugin {

    private static final Key ECOUTEUR = Key.key("ks_blocs", "reecriture");
    private static final String NOM = "ks_blocs";
    private static final Set<String> HORIZONTALES = Set.of("north", "east", "south", "west");

    private static KSBlocs instance;

    private Lang lang;
    private org.bukkit.NamespacedKey cleObjet;
    private Registre registre;
    private Affichage affichage;
    private JackOLanterns jacks;

    @Override
    public void onEnable() {
        lang = new Lang(this);
        cleObjet = new org.bukkit.NamespacedKey(this, "type");
        Apparences apparences;
        try (InputStream flux = getResource("apparences.json")) {
            apparences = new Apparences(flux);
            Reecriture.preparer();
        } catch (Exception e) {
            getLogger().severe("Demarrage impossible : " + e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        instance = this;
        registre = new Registre(new org.bukkit.NamespacedKey(this, "blocs"));
        affichage = new Affichage(registre, apparences);
        ChannelInitializeListenerHolder.addListener(ECOUTEUR, canal -> {
            Reecriture r = new Reecriture(affichage, getLogger());
            if (canal.pipeline().get("packet_handler") != null) {
                canal.pipeline().addBefore("packet_handler", NOM, r);
            } else {
                canal.pipeline().addLast(NOM, r);
            }
        });
        getServer().getPluginManager().registerEvents(new Evenements(this, registre, affichage), this);
        getServer().getPluginManager().registerEvents(new Plantes(this, registre, affichage), this);
        for (World w : Bukkit.getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                registre.charger(c);
            }
        }
        jacks = new JackOLanterns(this);
        jacks.demarrer();
        getServer().getScheduler().runTaskTimer(this, this::chaqueSeconde, 20L, 20L);
        lang.saveIfNeeded();
        getLogger().info(apparences.nombre() + " apparences, " + affichage.types.size() + " types de blocs custom");
    }

    @Override
    public void onDisable() {
        ChannelInitializeListenerHolder.removeListener(ECOUTEUR);
        if (jacks != null) {
            jacks.arreter();
        }
        instance = null;
    }

    Component t(String cle, String defaut, Object... paires) {
        Component c = lang.c(cle, defaut, paires);
        lang.saveIfNeeded();
        return c;
    }

    private void chaqueSeconde() {
        affichage.nettoyer();
        Affichage.Pos p;
        while ((p = affichage.aVerifier.poll()) != null) {
            affichage.verifie(p);
            World w = Bukkit.getWorld(p.monde());
            if (w == null || !w.isChunkLoaded(p.x() >> 4, p.z() >> 4)) {
                continue;
            }
            Block b = w.getBlockAt(p.x(), p.y(), p.z());
            Registre.Entree e = registre.get(b);
            TypeBloc t = e == null ? null : affichage.types.get(e.type());
            if (t != null && b.getType() != t.base() && b.getType() != Material.MOVING_PISTON) {
                // remplacé sans passer par KS_Blocs (WorldEdit, /setblock...) : la position est oubliée
                supprimer(b, e, t);
            }
        }
    }

    /** Oublie un bloc custom (déjà cassé ou sur le point de l'être) : registre, écouteur, particules de casse. */
    void supprimer(Block b, Registre.Entree e, TypeBloc t) {
        registre.enlever(b);
        BlockState montre = affichage.etatCustom(e.type(), e.variante());
        if (montre != null) {
            affichage.retire(new Affichage.Pos(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ()), montre);
        }
        if (t != null) {
            t.ecouteur().retire(b, e.variante());
        }
    }

    String variantePosee(TypeBloc t, Player joueur, Block b) {
        if (t.variantePosee() != null) {
            return t.variantePosee().apply(joueur, b);
        }
        Set<String> v = affichage.apparences().variantes(t.apparence());
        if (v.equals(HORIZONTALES) && joueur != null) {
            return joueur.getFacing().getOppositeFace().name().toLowerCase(java.util.Locale.ROOT);
        }
        return v.iterator().next();
    }

    // ------------------------------------------------------------------ API

    /**
     * Déclare un type de bloc custom. Son apparence doit être dans apparences.json (KS_Blocs) ; erreur si l'id est déjà
     * pris ou l'apparence inconnue.
     */
    public static void declarer(TypeBloc type) {
        KSBlocs k = instance();
        if (!k.affichage.apparences().existe(type.apparence())) {
            throw new IllegalArgumentException("Apparence inconnue (apparences.json de KS_Blocs) : " + type.apparence());
        }
        if (k.affichage.types.containsKey(type.id())) {
            throw new IllegalArgumentException("Type de bloc deja declare : " + type.id());
        }
        if (type.id().contains(",") || type.id().contains(";")) {
            throw new IllegalArgumentException("Id interdit (virgule ou point-virgule) : " + type.id());
        }
        k.affichage.declarer(type, CraftMagicNumbers.getBlock(type.base()));
        // blocs déjà chargés : leurs écouteurs n'ont pas encore été prévenus
        for (World w : Bukkit.getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                Map<Integer, Registre.Entree> m = k.registre.chunk(w.getUID(), c.getX(), c.getZ());
                if (m == null) {
                    continue;
                }
                for (Map.Entry<Integer, Registre.Entree> e : m.entrySet()) {
                    if (e.getValue().type().equals(type.id())) {
                        int p = e.getKey();
                        Block b = c.getBlock(Registre.x(p), Registre.y(p), Registre.z(p));
                        type.ecouteur().verifie(b, e.getValue().variante());
                    }
                }
            }
        }
    }

    /** Le type déclaré sous cet id, ou null. */
    public static TypeBloc type(String id) {
        return instance == null ? null : instance.affichage.types.get(id);
    }

    /** Le type du bloc custom à cette place, ou null (pas de bloc custom, ou type pas déclaré). */
    public static TypeBloc type(Block bloc) {
        if (instance == null) {
            return null;
        }
        Registre.Entree e = instance.registre.get(bloc);
        return e == null ? null : instance.affichage.types.get(e.type());
    }

    /** La variante du bloc custom à cette place, ou null. */
    public static String variante(Block bloc) {
        Registre.Entree e = instance == null ? null : instance.registre.get(bloc);
        return e == null ? null : e.variante();
    }

    /** L'id de type écrit dans un objet de bloc custom, ou null. */
    public static String id(ItemStack objet) {
        if (instance == null || objet == null || !objet.hasItemMeta()) {
            return null;
        }
        return objet.getItemMeta().getPersistentDataContainer().get(instance.cleObjet, PersistentDataType.STRING);
    }

    /** Le type de l'objet, ou null. */
    public static TypeBloc type(ItemStack objet) {
        String id = id(objet);
        return id == null ? null : type(id);
    }

    /** n objets qui posent ce bloc (avec la marque de KS_Blocs), ou null si le type n'a pas d'objet. */
    public static ItemStack objet(String id, int n) {
        TypeBloc t = type(id);
        if (t == null || t.modeleObjet() == null) {
            return null;
        }
        ItemStack o = t.modeleObjet().clone();
        ItemMeta meta = o.getItemMeta();
        meta.getPersistentDataContainer().set(instance.cleObjet, PersistentDataType.STRING, id);
        o.setItemMeta(meta);
        o.setAmount(n);
        return o;
    }

    /**
     * Pose un bloc custom : le bloc du serveur devient la base du type (s'il ne l'est pas déjà), puis la position est
     * retenue et l'écouteur prévenu. Faux si la variante n'existe pas.
     */
    public static boolean poser(Block bloc, TypeBloc type, String variante) {
        KSBlocs k = instance();
        if (k.affichage.etatCustom(type.id(), variante) == null) {
            return false;
        }
        if (bloc.getType() != type.base()) {
            bloc.setType(type.base());
        }
        return enregistrer(bloc, type, variante);
    }

    /** Retient un bloc custom à une place où le serveur a déjà la base du type (sans toucher au bloc). */
    public static boolean enregistrer(Block bloc, TypeBloc type, String variante) {
        KSBlocs k = instance();
        if (k.affichage.etatCustom(type.id(), variante) == null || bloc.getType() != type.base()) {
            return false;
        }
        k.registre.mettre(bloc, new Registre.Entree(type.id(), variante));
        type.ecouteur().pose(bloc, variante);
        renvoyer(bloc);
        return true;
    }

    /** Change la variante d'un bloc custom (stade de pousse, mode...) sans le casser. Faux si rien à cette place. */
    public static boolean changerVariante(Block bloc, String variante) {
        KSBlocs k = instance();
        Registre.Entree e = k.registre.get(bloc);
        if (e == null || k.affichage.etatCustom(e.type(), variante) == null) {
            return false;
        }
        k.registre.mettre(bloc, new Registre.Entree(e.type(), variante));
        renvoyer(bloc);
        return true;
    }

    /**
     * Oublie le bloc custom de cette place (l'écouteur est prévenu) et, si enAir, met de l'air à sa place (sans butin).
     * Renvoie son type, ou null s'il n'y avait pas de bloc custom.
     */
    public static TypeBloc retirer(Block bloc, boolean enAir) {
        KSBlocs k = instance();
        Registre.Entree e = k.registre.get(bloc);
        if (e == null) {
            return null;
        }
        TypeBloc t = k.affichage.types.get(e.type());
        k.supprimer(bloc, e, t);
        if (enAir) {
            bloc.setType(Material.AIR);
        } else {
            renvoyer(bloc);
        }
        return t;
    }

    /**
     * Déplace la position retenue d'un bloc custom (le plugin appelant déplace lui-même les blocs du serveur, avant ou
     * après). L'écouteur est prévenu (retiré à l'ancienne place, posé à la nouvelle). Faux si rien à déplacer.
     */
    public static boolean deplacer(Block de, Block vers) {
        KSBlocs k = instance();
        Registre.Entree e = k.registre.enlever(de);
        if (e == null) {
            return false;
        }
        TypeBloc t = k.affichage.types.get(e.type());
        if (t != null) {
            t.ecouteur().retire(de, e.variante());
        }
        k.registre.mettre(vers, e);
        if (t != null) {
            t.ecouteur().pose(vers, e.variante());
        }
        renvoyer(de);
        renvoyer(vers);
        return true;
    }

    /** Renvoie ce bloc aux joueurs proches (après un changement que le jeu n'envoie pas de lui-même). */
    public static void renvoyer(Block bloc) {
        Location l = bloc.getLocation();
        int portee = (Bukkit.getViewDistance() + 1) * 16;
        for (Player p : bloc.getWorld().getPlayers()) {
            Location pl = p.getLocation();
            if (Math.abs(pl.getBlockX() - l.getBlockX()) <= portee && Math.abs(pl.getBlockZ() - l.getBlockZ()) <= portee) {
                p.sendBlockChange(l, bloc.getBlockData()); // réécrit par Reecriture comme tout changement de bloc
            }
        }
    }

    /** Prévient les blocs custom voisins (6 faces) qu'un bloc a changé ici ({@link EcouteurBloc#verifie}). */
    public static void verifierAutour(Block bloc) {
        KSBlocs k = instance();
        for (BlockFace f : List.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
                BlockFace.WEST)) {
            Block v = bloc.getRelative(f);
            Registre.Entree e = k.registre.get(v);
            TypeBloc t = e == null ? null : k.affichage.types.get(e.type());
            if (t != null && v.getType() == t.base()) {
                t.ecouteur().verifie(v, e.variante());
            }
        }
    }

    private static KSBlocs instance() {
        if (instance == null) {
            throw new IllegalStateException("KS_Blocs n'est pas actif");
        }
        return instance;
    }

    /**
     * Chaque bloc custom de ce type dans les chunks chargés (bloc, variante), 0.2.0 : public (pousse des cultures).
     * L'action ne doit pas poser ni retirer de bloc custom pendant le parcours : noter les blocs, puis les traiter.
     */
    public static void pourChaque(String id, java.util.function.BiConsumer<Block, String> action) {
        instance().registre.pourChaque(id, action);
    }
}
