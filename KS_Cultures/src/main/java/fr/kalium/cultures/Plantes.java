package fr.kalium.cultures;

import fr.kalium.blocs.EcouteurBloc;
import fr.kalium.blocs.KSBlocs;
import fr.kalium.blocs.TypeBloc;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.Directional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Vie des cultures : semis, pousse, récolte, appui (terre labourée, piétinement), poudre d'os, grandes fleurs, graines
 * dans les herbes. Écouteur de tous les types de blocs custom de KS_Cultures (KS_Blocs).
 */
final class Plantes implements Listener, EcouteurBloc {

    private final KSCultures plugin;
    /** Terre labourée piétinée pendant ce tick (elle redevient de la terre : la plante dessus casse). */
    private final Set<Block> pietinees = new HashSet<>();

    Plantes(KSCultures plugin) {
        this.plugin = plugin;
    }

    private static int entre(int min, int max) {
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }

    private static ItemStack objet(String id, int n) {
        return KSCultures.objet(id, n);
    }

    private static void lacher(Block b, List<ItemStack> objets) {
        Location l = b.getLocation().add(0.5, 0.3, 0.5);
        for (ItemStack o : objets) {
            if (o != null && o.getAmount() > 0) {
                b.getWorld().dropItemNaturally(l, o);
            }
        }
    }

    // ------------------------------------------------------------------ butin (KS_Blocs : casse, explosion, entité)

    List<ItemStack> butin(TypeBloc.Casse c) {
        TypeBloc t = KSBlocs.type(c.bloc());
        String type = t == null ? null : t.id();
        List<ItemStack> res = new ArrayList<>();
        String graineFleur = Fleurs.graineDuType(type);
        if (graineFleur != null) {
            res.add(objet(graineFleur, 1));
            return res;
        }
        Vegetal v = Vegetal.parType(type);
        if (v == null) {
            return res;
        }
        String variante = c.variante();
        boolean mur = variante.equals(v.mur) || (v == Vegetal.MAIS && variante.equals("mais_haut"));
        if (v.lieu == Vegetal.Lieu.ARBUSTE) {
            // l'arbuste lui-même (le fruit planté), plus ses fruits s'il en porte
            int n = 1 + (mur ? entre(v.fruitsMin, v.fruitsMax) : 0);
            res.add(objet(v.fruit(), n));
        } else if (!mur) {
            res.add(objet(v.semence(), 1));
        } else {
            res.add(objet(v.fruit(), entre(v.fruitsMin, v.fruitsMax)));
            if (v.aDesGraines()) {
                res.add(objet(v.graine(), entre(v.grainesMin, v.grainesMax)));
            }
        }
        if (mur && c.joueur() != null) {
            plugin.decouvrir(c.joueur(), v);
        }
        return res;
    }

    // ------------------------------------------------------------------ écouteur KS_Blocs

    /**
     * Maïs : les deux blocs partent ensemble, tout de suite (le butin vient du bloc cassé ; plus tard, le jeu casserait
     * le haut et son butin tomberait une 2e fois). KS_Blocs a déjà oublié ce bloc-ci : pas de boucle.
     */
    @Override
    public void retire(Block bloc, String variante) {
        if (variante.equals("mais_bas") || variante.equals("mais_haut")) {
            Block autre = bloc.getRelative(variante.equals("mais_bas") ? BlockFace.UP : BlockFace.DOWN);
            TypeBloc t = KSBlocs.type(autre);
            String va = KSBlocs.variante(autre);
            if (t != null && Vegetal.parType(t.id()) == Vegetal.MAIS && va != null && !va.equals(variante)
                    && (va.equals("mais_bas") || va.equals("mais_haut"))) {
                KSBlocs.retirer(autre, true);
            }
        }
    }

    /** Chunk chargé, voisin changé, ou le jeu a voulu casser la plante : elle casse si elle n'a plus d'appui. */
    @Override
    public void verifie(Block bloc, String variante) {
        if (!appui(bloc, variante)) {
            casser(bloc);
        }
    }

    /** Casse une culture comme si elle tombait d'elle-même : son butin (sans découverte), puis de l'air. */
    private void casser(Block b) {
        TypeBloc t = KSBlocs.type(b);
        String variante = KSBlocs.variante(b);
        if (t == null || variante == null) {
            return;
        }
        lacher(b, butin(new TypeBloc.Casse(null, null, b, variante)));
        KSBlocs.retirer(b, true);
    }

    private static boolean terre(Material m) {
        return Tag.DIRT.isTagged(m) || m == Material.FARMLAND;
    }

    /** Vrai si la culture tient à cette place (terre labourée, terre pour un arbuste, fond sous l'eau pour le riz). */
    private boolean appui(Block b, String variante) {
        TypeBloc t = KSBlocs.type(b);
        if (t == null) {
            return true;
        }
        Material dessous = b.getRelative(BlockFace.DOWN).getType();
        if (variante.equals("mais_haut")) {
            return "mais_bas".equals(KSBlocs.variante(b.getRelative(BlockFace.DOWN)));
        }
        Vegetal v = Vegetal.parType(t.id());
        if (v == null) {
            return dessous == Material.FARMLAND; // fleur en croissance
        }
        return switch (v.lieu) {
            case TERRE -> dessous == Material.FARMLAND;
            case ARBUSTE -> terre(dessous);
            case EAU -> fondDuRiz(dessous);
        };
    }

    private static boolean fondDuRiz(Material m) {
        return terre(m) || m == Material.CLAY || m == Material.SAND || m == Material.RED_SAND || m == Material.GRAVEL;
    }

    // ------------------------------------------------------------------ pousse

    /** Chaque minute : chaque culture des chunks chargés avance d'un stade avec la chance de sa durée moyenne. */
    void pousser() {
        List<Object[]> aTraiter = new ArrayList<>();
        for (Vegetal v : Vegetal.values()) {
            KSBlocs.pourChaque(v.type(), (b, variante) -> aTraiter.add(new Object[]{b, variante}));
        }
        for (String g : Fleurs.GRAINES.keySet()) {
            KSBlocs.pourChaque(Fleurs.type(g), (b, variante) -> aTraiter.add(new Object[]{b, variante}));
        }
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (Object[] o : aTraiter) {
            Block b = (Block) o[0];
            String variante = (String) o[1];
            TypeBloc t = KSBlocs.type(b);
            if (t == null || b.getType() != t.base() || !variante.equals(KSBlocs.variante(b))) {
                continue;
            }
            if (!appui(b, variante)) {
                casser(b);
                continue;
            }
            Vegetal v = Vegetal.parType(t.id());
            double chance = 1.0 / plugin.minutesParStade(v == null ? "fleur" : v.cle());
            Block dessous = b.getRelative(BlockFace.DOWN);
            if (dessous.getBlockData() instanceof Farmland f && f.getMoisture() < f.getMaximumMoisture()) {
                chance /= 2; // terre labourée sèche : deux fois plus lent, comme les cultures vanilla
            }
            if (Math.max(b.getLightFromSky(), b.getLightFromBlocks()) < 9 || r.nextDouble() >= chance) {
                continue;
            }
            avancer(b, t.id(), variante);
        }
    }

    /** Fait passer une culture au stade suivant. Faux si elle est déjà au bout (ou bloquée : pas de place au-dessus). */
    private boolean avancer(Block b, String type, String variante) {
        String graineFleur = Fleurs.graineDuType(type);
        if (graineFleur != null) {
            return fleurir(b, Fleurs.GRAINES.get(graineFleur));
        }
        Vegetal v = Vegetal.parType(type);
        if (v == null) {
            return false;
        }
        switch (variante) {
            case "pousse" -> {
                return KSBlocs.changerVariante(b, "jeune");
            }
            case "jeune" -> {
                if (v == Vegetal.MAIS) {
                    Block haut = b.getRelative(BlockFace.UP);
                    if (!haut.getType().isAir()) {
                        return false;
                    }
                    KSBlocs.changerVariante(b, "mais_bas");
                    return KSBlocs.poser(haut, KSBlocs.type(type), "mais_haut");
                }
                return KSBlocs.changerVariante(b, v.lieu == Vegetal.Lieu.ARBUSTE ? v.arbuste() : v.mur);
            }
            default -> {
                if (v.lieu == Vegetal.Lieu.ARBUSTE && variante.equals(v.arbuste())) {
                    return KSBlocs.changerVariante(b, v.mur);
                }
                return false;
            }
        }
    }

    /** La graine de fleur devient la vraie fleur (deux blocs pour une grande fleur, s'il y a la place). */
    private boolean fleurir(Block b, Material fleur) {
        Block haut = b.getRelative(BlockFace.UP);
        BlockData bas = fleur.createBlockData();
        if (bas instanceof Bisected) {
            if (!haut.getType().isAir()) {
                return false;
            }
            ((Bisected) bas).setHalf(Bisected.Half.BOTTOM);
        }
        KSBlocs.retirer(b, false);
        b.setBlockData(bas, false);
        if (bas instanceof Bisected) {
            BlockData dessus = fleur.createBlockData();
            ((Bisected) dessus).setHalf(Bisected.Half.TOP);
            haut.setBlockData(dessus, false);
        }
        b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 6, 0.3, 0.3, 0.3);
        return true;
    }

    // ------------------------------------------------------------------ semis, récolte, poudre d'os

    @EventHandler(priority = EventPriority.HIGH)
    public void onClic(PlayerInteractEvent event) {
        ItemStack main = event.getItem();
        String id = KSCultures.id(main);
        Action a = event.getAction();
        if (id != null && main.getType() == Material.KNOWLEDGE_BOOK
                && (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK)) {
            // livre de connaissances : jamais son usage vanilla (un fruit, lui, se mange s'il n'est pas planté)
            event.setUseItemInHand(Event.Result.DENY);
        }
        if (a != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Block b = event.getClickedBlock();
        Player joueur = event.getPlayer();
        TypeBloc t = KSBlocs.type(b);
        if (t != null && t.id().startsWith("ks_cultures:")) {
            if (event.getHand() != EquipmentSlot.HAND) {
                return;
            }
            // pas d'usage vanilla de l'objet en main (poudre d'os sur le varech du riz, canne à sucre posée dessus...)
            event.setUseItemInHand(Event.Result.DENY);
            if (!peutConstruire(joueur, b, b)) {
                return;
            }
            if (main != null && main.getType() == Material.BONE_MEAL) {
                poudreDOs(joueur, b, t, main);
            } else {
                cueillir(joueur, b, t);
            }
            return;
        }
        if (id == null) {
            if (main != null && main.getType() == Material.BONE_MEAL && Fleurs.grande(b.getType())) {
                event.setUseItemInHand(Event.Result.DENY); // plus de duplication des grandes fleurs
            }
            return;
        }
        if (event.getBlockFace() != BlockFace.UP) {
            return;
        }
        if (semer(joueur, b, id, main)) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    /** Pose d'une graine, d'un grain de riz, de kiwi ou d'un mélange sur le bloc cliqué. Vrai si posé. */
    private boolean semer(Player joueur, Block sol, String id, ItemStack main) {
        Block place = sol.getRelative(BlockFace.UP);
        String type;
        if (Fleurs.GRAINES.containsKey(id)) {
            type = sol.getType() == Material.FARMLAND && place.getType().isAir() ? Fleurs.type(id) : null;
        } else {
            Vegetal v = KSCultures.MELANGE.equals(id)
                    ? Vegetal.duMelange().get(ThreadLocalRandom.current().nextInt(Vegetal.duMelange().size()))
                    : Vegetal.parSemence(id);
            if (v == null) {
                return false;
            }
            boolean ok = switch (v.lieu) {
                case TERRE -> sol.getType() == Material.FARMLAND && place.getType().isAir();
                case ARBUSTE -> terre(sol.getType()) && place.getType().isAir();
                case EAU -> fondDuRiz(sol.getType()) && place.getType() == Material.WATER
                        && place.getBlockData() instanceof Levelled l && l.getLevel() == 0;
            };
            type = ok ? v.type() : null;
        }
        if (type == null || !peutConstruire(joueur, place, sol)) {
            return false;
        }
        KSBlocs.poser(place, KSBlocs.type(type), Fleurs.graineDuType(type) != null ? "fleur" : "pousse");
        place.getWorld().playSound(place.getLocation(), Sound.ITEM_CROP_PLANT, 1f, 1f);
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            main.setAmount(main.getAmount() - 1);
        }
        return true;
    }

    /**
     * Les protections (claims) écoutent la pose des blocs : un faux événement de pose leur demande si le joueur peut
     * construire ici.
     */
    private boolean peutConstruire(Player joueur, Block place, Block contre) {
        BlockPlaceEvent essai = new BlockPlaceEvent(place, place.getState(), contre,
                joueur.getInventory().getItemInMainHand(), joueur, true, EquipmentSlot.HAND);
        plugin.getServer().getPluginManager().callEvent(essai);
        return !essai.isCancelled() && essai.canBuild();
    }

    /** Clic droit sur un arbuste avec fruits : 1 à 2 fruits, l'arbuste repart sans fruits. */
    private void cueillir(Player joueur, Block b, TypeBloc t) {
        Vegetal v = Vegetal.parType(t.id());
        if (v == null || v.lieu != Vegetal.Lieu.ARBUSTE || !v.mur.equals(KSBlocs.variante(b))) {
            return;
        }
        KSBlocs.changerVariante(b, v.arbuste());
        lacher(b, List.of(objet(v.fruit(), entre(v.fruitsMin, v.fruitsMax))));
        b.getWorld().playSound(b.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1f, 1f);
        plugin.decouvrir(joueur, v);
    }

    // ------------------------------------------------------------------ 1.1.0 : récolte par la Laboureuse (KS_SuperTool)

    /** Culture mûre que la Laboureuse récolte : légume, maïs (bas ou haut), riz, kiwi avec fruits. */
    static boolean mure(Block b) {
        TypeBloc t = KSBlocs.type(b);
        Vegetal v = t == null ? null : Vegetal.parType(t.id());
        String variante = KSBlocs.variante(b);
        return v != null && variante != null
                && (variante.equals(v.mur) || (v == Vegetal.MAIS && variante.equals("mais_haut")));
    }

    /**
     * Récolte une culture mûre au nom du joueur (claims : même essai que le semis) et la replante au stade « pousse »,
     * une semence reprise dans le butin (graine, ou grain de riz) ; le maïs perd son haut. Un kiwi est cueilli (clic
     * droit) : l'arbuste reste. Vrai si la culture a été récoltée.
     */
    boolean recolter(Player joueur, Block b) {
        if (!mure(b) || !peutConstruire(joueur, b, b)) {
            return false;
        }
        TypeBloc t = KSBlocs.type(b);
        Vegetal v = Vegetal.parType(t.id());
        if (v.lieu == Vegetal.Lieu.ARBUSTE) {
            cueillir(joueur, b, t);
            return true;
        }
        Block bas = v == Vegetal.MAIS && "mais_haut".equals(KSBlocs.variante(b)) ? b.getRelative(BlockFace.DOWN) : b;
        String variante = KSBlocs.variante(bas);
        if (variante == null) {
            return false;
        }
        List<ItemStack> recolte = new ArrayList<>(butin(new TypeBloc.Casse(joueur,
                joueur.getInventory().getItemInMainHand(), bas, variante)));
        boolean semence = reprendre(recolte, v.semence());
        if (v == Vegetal.MAIS) {
            // le haut parti, retire() enlève aussi le bas : on le repose au stade « pousse »
            KSBlocs.retirer(bas.getRelative(BlockFace.UP), true);
            if (KSBlocs.type(bas) != null) {
                KSBlocs.retirer(bas, true);
            }
            if (semence) {
                KSBlocs.poser(bas, t, "pousse");
            }
        } else if (semence) {
            KSBlocs.changerVariante(bas, "pousse");
        } else {
            KSBlocs.retirer(bas, true);
        }
        lacher(bas, recolte);
        bas.getWorld().playSound(bas.getLocation(), Sound.BLOCK_CROP_BREAK, 1f, 1f);
        return true;
    }

    /** Retire une semence (objet de KS_Cultures d'id donné) de la récolte ; faux s'il n'y en a pas. */
    private static boolean reprendre(List<ItemStack> recolte, String id) {
        for (ItemStack o : recolte) {
            if (o != null && o.getAmount() > 0 && id.equals(KSCultures.id(o))) {
                o.setAmount(o.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    /** Poudre d'os sur une culture : un stade de plus (la fleur fleurit), comme sur les cultures vanilla. */
    private void poudreDOs(Player joueur, Block b, TypeBloc t, ItemStack main) {
        if (!avancer(b, t.id(), KSBlocs.variante(b))) {
            return;
        }
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            main.setAmount(main.getAmount() - 1);
        }
        b.getWorld().playSound(b.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1f, 1f);
        b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3);
    }

    /** Distributeur de poudre d'os : rien sur les cultures custom ni sur les grandes fleurs. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDistributeur(BlockDispenseEvent event) {
        if (event.getItem().getType() != Material.BONE_MEAL
                || !(event.getBlock().getBlockData() instanceof Directional d)) {
            return;
        }
        Block cible = event.getBlock().getRelative(d.getFacing());
        TypeBloc t = KSBlocs.type(cible);
        if (Fleurs.grande(cible.getType()) || t != null && t.id().startsWith("ks_cultures:")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFertilise(BlockFertilizeEvent event) {
        if (Fleurs.grande(event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ terre labourée

    /** Piétinement : la terre labourée redevient de la terre, la plante dessus casse (tick suivant). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPietine(EntityChangeBlockEvent event) {
        Block b = event.getBlock();
        if (b.getType() == Material.FARMLAND && event.getTo() != Material.FARMLAND) {
            pietinees.add(b);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                pietinees.remove(b);
                Block dessus = b.getRelative(BlockFace.UP);
                String va = KSBlocs.variante(dessus);
                if (va != null && KSBlocs.type(dessus) != null && KSBlocs.type(dessus).id().startsWith("ks_cultures:")) {
                    verifie(dessus, va);
                }
            });
        }
    }

    /** Terre labourée sèche sous une culture : elle reste labourée, comme sous les cultures vanilla. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSeche(BlockFadeEvent event) {
        Block b = event.getBlock();
        if (b.getType() != Material.FARMLAND || pietinees.contains(b)) {
            return;
        }
        TypeBloc t = KSBlocs.type(b.getRelative(BlockFace.UP));
        if (t != null && t.id().startsWith("ks_cultures:")) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ graines dans les herbes

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHerbe(BlockBreakEvent event) {
        Block b = event.getBlock();
        Player joueur = event.getPlayer();
        if (b.getType() != Material.SHORT_GRASS && b.getType() != Material.TALL_GRASS || !event.isDropItems()
                || joueur.getGameMode() == GameMode.CREATIVE
                || joueur.getInventory().getItemInMainHand().getType() == Material.SHEARS) {
            return;
        }
        List<Vegetal> possibles = plugin.vegetauxDuBiome(b.getWorld().getBiome(b.getLocation()).getKey().asString());
        if (possibles.isEmpty() || ThreadLocalRandom.current().nextDouble() >= plugin.chanceHerbe()) {
            return;
        }
        Vegetal v = possibles.get(ThreadLocalRandom.current().nextInt(possibles.size()));
        lacher(b, List.of(objet(v.semence(), 1)));
    }
}
