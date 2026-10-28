package fr.kalium.alcools;

import fr.kalium.effets.EffetFinEvent;
import fr.kalium.effets.KSEffets;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ce que font les alcools bus ou reçus (jetables) :
 * - nausée : 10 % de chance, 10 secondes, à chaque alcool (alcool neutre compris) ;
 * - effets du jeu (tequila, hydromel, manzana, bière) : appliqués par le jeu (effets de la potion) ;
 * - vodka : effet custom (KS_Effets) ; tant qu'il dure, Lévitation (II pour la vodka à la glowstone) sauf quand le
 *   joueur est accroupi ; le minuteur continue pendant la pause (choix de LeKiwi06) ;
 * - tabascoolisé : effet custom ; particules de feu, ni gel ni dégâts de la poudreuse.
 * Jetables : durée custom réduite selon la distance, comme le jeu le fait pour ses effets.
 */
final class Boire implements Listener {

    static final String EFFET_VODKA = "ks_alcools:vodka";
    static final String EFFET_VODKA_2 = "ks_alcools:vodka_2";
    static final String EFFET_TABASCO = "ks_alcools:tabascoolise";

    private static final double CHANCE_NAUSEE = 0.10;
    private static final int NAUSEE = 10 * 20;

    private final KSAlcools plugin;

    /** Joueurs à qui la vodka donne la Lévitation en ce moment (pour ne retirer que la nôtre). */
    private final Set<UUID> leves = new HashSet<>();
    /** Joueurs sous tabascoolisé (mis à jour chaque seconde). */
    private final Set<UUID> antigel = new HashSet<>();

    Boire(KSAlcools plugin) {
        this.plugin = plugin;
    }

    void demarrer() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::seconde, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tabasco, 5L, 5L);
    }

    // ------------------------------------------------------------------ boire, recevoir

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBoire(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        String id = Alcools.id(item);
        if (id == null || !(item.getType() == org.bukkit.Material.POTION)) {
            return;
        }
        Player joueur = event.getPlayer();
        nausee(joueur);
        // Après la gorgée (effets du jeu de la potion appliqués au même moment).
        Bukkit.getScheduler().runTask(plugin, () -> effetCustom(joueur, id, Alcools.variante(item), 1));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRecevoir(PotionSplashEvent event) {
        ItemStack item = event.getPotion().getItem();
        String id = Alcools.id(item);
        if (id == null) {
            return;
        }
        Alcools.Variante variante = Alcools.variante(item);
        for (LivingEntity touche : event.getAffectedEntities()) {
            double intensite = event.getIntensity(touche);
            if (touche instanceof Player joueur && intensite > 0) {
                nausee(joueur);
                effetCustom(joueur, id, variante, intensite);
            }
        }
    }

    private static void nausee(Player joueur) {
        if (ThreadLocalRandom.current().nextDouble() < CHANCE_NAUSEE) {
            joueur.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, NAUSEE, 0));
        }
    }

    private void effetCustom(Player joueur, String id, Alcools.Variante variante, double intensite) {
        if (variante == null || !joueur.isOnline()) {
            return;
        }
        String effet = switch (id) {
            case Alcools.VODKA -> variante == Alcools.Variante.FORT ? EFFET_VODKA_2 : EFFET_VODKA;
            case Alcools.TABASCOOLISE -> EFFET_TABASCO;
            default -> null;
        };
        int secondes = (int) Math.round(variante.secondes * intensite);
        if (effet == null || secondes < 1) {
            return;
        }
        KSEffets.donner(joueur, effet, secondes);
        vodka(joueur);
    }

    // ------------------------------------------------------------------ vodka

    /** Chaque seconde : lévitation de la vodka, joueurs sous tabascoolisé. */
    private void seconde() {
        antigel.clear();
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            vodka(joueur);
            if (KSEffets.actif(joueur, EFFET_TABASCO)) {
                antigel.add(joueur.getUniqueId());
            }
        }
    }

    /**
     * Lévitation de la vodka : présente (durée = temps restant de l'effet) si l'effet est actif et que le joueur n'est
     * pas accroupi ; retirée sinon (seulement celle donnée par la vodka).
     */
    private void vodka(Player joueur) {
        int niveau = KSEffets.actif(joueur, EFFET_VODKA_2) ? 1 : KSEffets.actif(joueur, EFFET_VODKA) ? 0 : -1;
        PotionEffect actuelle = joueur.getPotionEffect(PotionEffectType.LEVITATION);
        if (niveau < 0 || joueur.isSneaking()) {
            if (leves.remove(joueur.getUniqueId()) && actuelle != null) {
                joueur.removePotionEffect(PotionEffectType.LEVITATION);
            }
            return;
        }
        int restant = Math.max(KSEffets.restant(joueur, EFFET_VODKA_2), KSEffets.restant(joueur, EFFET_VODKA));
        int ticks = restant * 20 + 20;
        if (actuelle == null || actuelle.getAmplifier() != niveau || Math.abs(actuelle.getDuration() - ticks) > 40) {
            joueur.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, ticks, niveau));
        }
        leves.add(joueur.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAccroupir(PlayerToggleSneakEvent event) {
        // L'état « accroupi » change juste après l'événement.
        Player joueur = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (joueur.isOnline()) {
                vodka(joueur);
            }
        });
    }

    @EventHandler
    public void onFin(EffetFinEvent event) {
        if (event.getId().equals(EFFET_VODKA) || event.getId().equals(EFFET_VODKA_2)) {
            vodka(event.getJoueur());
        } else if (event.getId().equals(EFFET_TABASCO)) {
            antigel.remove(event.getJoueur().getUniqueId());
        }
    }

    @EventHandler
    public void onQuitter(PlayerQuitEvent event) {
        leves.remove(event.getPlayer().getUniqueId());
        antigel.remove(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------------ tabascoolisé

    /** Tous les 5 ticks : particules de feu, gel effacé. */
    private void tabasco() {
        for (UUID uuid : antigel) {
            Player joueur = Bukkit.getPlayer(uuid);
            if (joueur == null) {
                continue;
            }
            if (joueur.getFreezeTicks() > 0) {
                joueur.setFreezeTicks(0);
            }
            Location lieu = joueur.getLocation().add(0, 1, 0);
            joueur.getWorld().spawnParticle(Particle.FLAME, lieu, 2, 0.3, 0.5, 0.3, 0.01);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGel(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FREEZE && event.getEntity() instanceof Player joueur
                && KSEffets.actif(joueur, EFFET_TABASCO)) {
            event.setCancelled(true);
        }
    }
}
