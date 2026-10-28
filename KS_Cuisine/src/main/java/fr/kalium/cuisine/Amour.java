package fr.kalium.cuisine;

import fr.kalium.effets.KSEffets;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pomme d'amour (cahier, partie 3.4).
 *
 * Mobs : clic droit avec la pomme sur un mob qui ne se reproduit pas (hostiles compris ; ni les 4 boss, ni les animaux
 * qui se reproduisent déjà, ni les villageois, ni un bébé) : il est amoureux 30 s (cœurs). Deux amoureux de la même
 * espèce à 8 blocs ou moins vont l'un vers l'autre ; à leur contact, un bébé naît et les parents attendent 5 min avant
 * de recommencer (comme les animaux). Bébé : le vrai bébé du jeu si l'espèce en a un (zombie, piglin...), sinon un
 * mini-mob (taille 0,5, marqué « bebe ») qui grandit au bout de 20 min passées dans un chunk chargé. Les bébés ne
 * disparaissent pas loin des joueurs.
 *
 * Joueurs : manger une pomme d'amour rend « Amoureux » 30 s (effet de KS_Effets). Deux amoureux qui se touchent : son
 * de glissade sur un bloc de miel, un bébé villageois apparaît, l'effet s'arrête pour les deux et chacun est fatigué
 * 5 min ; pendant ce délai, une pomme mangée ne rend pas amoureux et la barre d'action le dit.
 */
final class Amour implements Listener {

    static final String EFFET_AMOUREUX = "ks_cuisine:amoureux";

    private static final int AMOUR = 30 * 20;
    private static final long REPOS = 5 * 60 * 1000L;
    private static final int CROISSANCE = 20 * 60;
    private static final double RECHERCHE = 8;

    private static NamespacedKey cleBebe;

    private final KSCuisine plugin;

    /** Mobs amoureux : ticks restants. */
    private final Map<UUID, Integer> amoureux = new HashMap<>();
    /** Parents qui se reposent (mobs) et joueurs fatigués : fin du repos (ms). */
    private final Map<UUID, Long> repos = new HashMap<>();
    /** Mini-mobs chargés (pour les faire grandir). */
    private final Set<UUID> minis = new HashSet<>();
    /** Animaux qui se reproduisent déjà dans le jeu (calculé une fois par espèce). */
    private final Map<EntityType, Boolean> reproductibles = new EnumMap<>(EntityType.class);

    Amour(KSCuisine plugin) {
        this.plugin = plugin;
        cleBebe = new NamespacedKey(plugin, "bebe");
    }

    void demarrer() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::coeurs, 10L, 10L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::contacts, 5L, 5L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::grandir, 1200L, 1200L);
        for (World monde : Bukkit.getWorlds()) {
            monde.getEntities().forEach(this::noterMini);
        }
    }

    static boolean estMini(Entity mob) {
        return cleBebe != null && mob.getPersistentDataContainer().has(cleBebe, PersistentDataType.INTEGER);
    }

    // ------------------------------------------------------------------ mobs

    /** Un mob qui peut tomber amoureux avec la pomme. */
    private boolean eligible(Entity entite) {
        if (!(entite instanceof Mob mob) || mob instanceof Wither || mob instanceof EnderDragon
                || mob instanceof Warden || mob instanceof ElderGuardian || mob instanceof Villager || estMini(mob)
                || (mob instanceof Ageable a && !a.isAdult())) {
            return false;
        }
        return !(mob instanceof Animals animal && reproductibles.computeIfAbsent(mob.getType(), t -> seReproduit(animal)));
    }

    /** L'animal a-t-il une nourriture de reproduction dans le jeu ? */
    private static boolean seReproduit(Animals animal) {
        for (Material m : Material.values()) {
            if (!m.isLegacy() && m.isItem() && animal.isBreedItem(m)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClic(PlayerInteractEntityEvent event) {
        Player joueur = event.getPlayer();
        ItemStack main = event.getHand() == EquipmentSlot.OFF_HAND ? joueur.getInventory().getItemInOffHand()
                : joueur.getInventory().getItemInMainHand();
        if (!KSCuisine.POMME.equals(KSCuisine.idObjet(main))) {
            return;
        }
        Entity mob = event.getRightClicked();
        if (!eligible(mob)) {
            return;
        }
        event.setCancelled(true);
        if (amoureux.containsKey(mob.getUniqueId()) || repos.getOrDefault(mob.getUniqueId(), 0L) > System.currentTimeMillis()) {
            return;
        }
        if (joueur.getGameMode() != GameMode.CREATIVE) {
            main.setAmount(main.getAmount() - 1);
        }
        amoureux.put(mob.getUniqueId(), AMOUR);
        mob.getWorld().spawnParticle(Particle.HEART, mob.getLocation().add(0, mob.getHeight(), 0), 5, 0.4, 0.3, 0.4);
    }

    /** Tous les 10 ticks : cœurs, rapprochement, naissance. */
    private void coeurs() {
        long maintenant = System.currentTimeMillis();
        repos.values().removeIf(fin -> fin <= maintenant);
        List<Mob> liste = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : new ArrayList<>(amoureux.entrySet())) {
            Entity entite = Bukkit.getEntity(e.getKey());
            int reste = e.getValue() - 10;
            if (!(entite instanceof Mob mob) || !mob.isValid() || reste <= 0) {
                amoureux.remove(e.getKey());
                continue;
            }
            amoureux.put(e.getKey(), reste);
            liste.add(mob);
            if (reste % 20 == 0) {
                mob.getWorld().spawnParticle(Particle.HEART, mob.getLocation().add(0, mob.getHeight(), 0), 1, 0.3, 0.2, 0.3);
            }
        }
        Set<UUID> faits = new HashSet<>();
        for (Mob mob : liste) {
            if (faits.contains(mob.getUniqueId())) {
                continue;
            }
            Mob partenaire = null;
            double meilleur = RECHERCHE * RECHERCHE;
            for (Mob autre : liste) {
                if (autre != mob && autre.getType() == mob.getType() && !faits.contains(autre.getUniqueId())
                        && autre.getWorld().equals(mob.getWorld())) {
                    double d = autre.getLocation().distanceSquared(mob.getLocation());
                    if (d < meilleur) {
                        meilleur = d;
                        partenaire = autre;
                    }
                }
            }
            if (partenaire == null) {
                continue;
            }
            double contact = mob.getWidth() + 0.5;
            if (meilleur <= contact * contact) {
                naissance(mob, partenaire);
                faits.add(mob.getUniqueId());
                faits.add(partenaire.getUniqueId());
            } else {
                mob.getPathfinder().moveTo(partenaire, 1.0);
                partenaire.getPathfinder().moveTo(mob, 1.0);
            }
        }
    }

    private void naissance(Mob a, Mob b) {
        amoureux.remove(a.getUniqueId());
        amoureux.remove(b.getUniqueId());
        long fin = System.currentTimeMillis() + REPOS;
        repos.put(a.getUniqueId(), fin);
        repos.put(b.getUniqueId(), fin);
        Location lieu = a.getLocation().add(b.getLocation()).multiply(0.5);
        lieu.setYaw(a.getLocation().getYaw());
        Entity ne = a.getWorld().spawnEntity(lieu, a.getType(), CreatureSpawnEvent.SpawnReason.BREEDING);
        if (!(ne instanceof Mob bebe)) {
            return;
        }
        bebe.setRemoveWhenFarAway(false);
        if (bebe instanceof Ageable ageable) {
            ageable.setBaby();
        } else {
            AttributeInstance taille = bebe.getAttribute(Attribute.SCALE);
            if (taille != null) {
                taille.setBaseValue(0.5);
            }
            bebe.getPersistentDataContainer().set(cleBebe, PersistentDataType.INTEGER, CROISSANCE);
            minis.add(bebe.getUniqueId());
        }
        a.getWorld().spawnParticle(Particle.HEART, lieu.clone().add(0, 0.5, 0), 7, 0.5, 0.4, 0.5);
    }

    // ------------------------------------------------------------------ mini-mobs

    private void noterMini(Entity e) {
        if (estMini(e)) {
            minis.add(e.getUniqueId());
        }
    }

    @EventHandler
    public void onCharger(EntitiesLoadEvent event) {
        event.getEntities().forEach(this::noterMini);
    }

    @EventHandler
    public void onDecharger(EntitiesUnloadEvent event) {
        event.getEntities().forEach(e -> minis.remove(e.getUniqueId()));
    }

    /** Chaque minute : les mini-mobs chargés vieillissent ; au bout de 20 min, taille normale. */
    private void grandir() {
        for (UUID uuid : new ArrayList<>(minis)) {
            Entity e = Bukkit.getEntity(uuid);
            if (e == null || !e.isValid() || !estMini(e)) {
                minis.remove(uuid);
                continue;
            }
            int reste = e.getPersistentDataContainer().get(cleBebe, PersistentDataType.INTEGER) - 60;
            if (reste > 0) {
                e.getPersistentDataContainer().set(cleBebe, PersistentDataType.INTEGER, reste);
                continue;
            }
            e.getPersistentDataContainer().remove(cleBebe);
            if (e instanceof Mob mob && mob.getAttribute(Attribute.SCALE) != null) {
                mob.getAttribute(Attribute.SCALE).setBaseValue(1.0);
            }
            minis.remove(uuid);
        }
    }

    // ------------------------------------------------------------------ joueurs

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onManger(PlayerItemConsumeEvent event) {
        if (!KSCuisine.POMME.equals(KSCuisine.idObjet(event.getItem()))) {
            return;
        }
        Player joueur = event.getPlayer();
        long fin = repos.getOrDefault(joueur.getUniqueId(), 0L);
        if (fin > System.currentTimeMillis()) {
            long minutes = Math.max(1, (fin - System.currentTimeMillis() + 59_999) / 60_000);
            joueur.sendActionBar(plugin.t("amour.fatigue", "<gray>Tu es fatigué... Encore <minutes> min avant d'aimer à nouveau.",
                    "minutes", minutes));
            return;
        }
        KSEffets.donner(joueur, EFFET_AMOUREUX, 30);
    }

    /** Tous les 5 ticks : deux joueurs amoureux qui se touchent. */
    private void contacts() {
        List<Player> liste = new ArrayList<>();
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            if (joueur.getGameMode() != GameMode.SPECTATOR && KSEffets.actif(joueur, EFFET_AMOUREUX)) {
                liste.add(joueur);
            }
        }
        Set<UUID> faits = new HashSet<>();
        for (int i = 0; i < liste.size(); i++) {
            Player a = liste.get(i);
            for (int j = i + 1; j < liste.size(); j++) {
                Player b = liste.get(j);
                if (faits.contains(a.getUniqueId()) || faits.contains(b.getUniqueId())
                        || !a.getWorld().equals(b.getWorld())
                        || !a.getBoundingBox().expand(0.1).overlaps(b.getBoundingBox())) {
                    continue;
                }
                faits.add(a.getUniqueId());
                faits.add(b.getUniqueId());
                bebeVillageois(a, b);
            }
        }
    }

    private void bebeVillageois(Player a, Player b) {
        Location lieu = a.getLocation().add(b.getLocation()).multiply(0.5);
        a.getWorld().playSound(lieu, Sound.BLOCK_HONEY_BLOCK_SLIDE, 1f, 1f);
        a.getWorld().spawnParticle(Particle.HEART, lieu.clone().add(0, 1.5, 0), 7, 0.5, 0.4, 0.5);
        Entity ne = a.getWorld().spawnEntity(lieu, EntityType.VILLAGER, CreatureSpawnEvent.SpawnReason.BREEDING);
        if (ne instanceof Villager villageois) {
            villageois.setBaby();
        }
        long fin = System.currentTimeMillis() + REPOS;
        for (Player p : List.of(a, b)) {
            KSEffets.retirer(p, EFFET_AMOUREUX);
            repos.put(p.getUniqueId(), fin);
        }
    }
}
