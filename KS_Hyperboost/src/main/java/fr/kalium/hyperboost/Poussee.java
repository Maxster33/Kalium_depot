package fr.kalium.hyperboost;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Camel;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Une poussée Hyperboost : modificateurs d'attributs temporaires sur la monture, retirés à la fin.
 *
 * Vitesse (code du jeu 26.3, LivingEntity.travelRidden : la monture conduite se déplace chez le client) :
 * - cheval, âne, mule, chevaux squelette et zombie : vitesse = attribut movement_speed (AbstractHorse.getRiddenSpeed).
 *   Au sol, accélération = vitesse × 0,216 / f³ si f > 0,6 (sinon vitesse), freinage × f × 0,91 : vitesse limite
 *   = 2,2026 × vitesse par tick sur un bloc normal, 2,2327 sur la glace bleue (le pire cas). En l'air : vitesse ×
 *   0,1, freinage 0,91 (plus lent). Plafond calé sur la glace bleue : 40 blocs/s = 2 blocs/tick → 0,8957.
 * - chameau : getRiddenSpeed ajoute 0,1 au sprint → 0,7957. Son élan (dash) vaut 22,2 × saut × movement_speed :
 *   plus de 100 blocs/s avec la vitesse poussée, donc jump_strength à 0 pendant la poussée (ni saut ni élan).
 * - happy ghast : travel = travelFlying(flying_speed × 5/3), freinage 0,91 ; entrée = (côté, haut, avant) ×
 *   3,9 × flying_speed, ramenée à 1 si plus longue. En diagonale l'entrée horizontale mesure √1,5 : plafond calé
 *   sur ce cas (40 blocs/s à l'horizontale) → 0,1504 ; tout droit : 32,7 blocs/s.
 */
final class Poussee {

    /** Durée de la poussée (ticks) et marge laissée à l'anti-triche après la fin (ralentissement, latence). */
    static final int DUREE = 10 * 20;
    static final long MARGE_MS = 5_000;
    /** Métadonnée lue par KS_AntiCheat : date (ms) jusqu'à laquelle les signalements de GrimAC sont ignorés. */
    static final String METADONNEE = "ks_hyperboost";

    /** Vitesse limite (blocs/tick) par unité de movement_speed au sol, glace bleue comprise. */
    private static final double SOL = 0.216 / (0.989 * 0.989 * 0.989) / (1 - 0.989 * 0.91);

    private final KSHyperboost plugin;
    private final Player joueur;
    private final LivingEntity monture;
    private final NamespacedKey cle;
    private final List<AttributeInstance> modifies = new ArrayList<>();
    private BukkitTask tache;
    private PermissionAttachment grim;
    private long finAntiTriche;
    private boolean finie;

    Poussee(KSHyperboost plugin, Player joueur, LivingEntity monture) {
        this.plugin = plugin;
        this.joueur = joueur;
        this.monture = monture;
        this.cle = new NamespacedKey(plugin, "hyperboost");
    }

    void demarrer() {
        double max = Math.max(1, plugin.getConfig().getDouble("vitesse-max", 40)) / 20.0;
        // Anti-triche d'abord : l'attribut arrive au client presque aussitôt.
        finAntiTriche = System.currentTimeMillis() + DUREE * 50L + MARGE_MS;
        joueur.setMetadata(METADONNEE, new FixedMetadataValue(plugin, finAntiTriche));
        grim = joueur.addAttachment(plugin);
        grim.setPermission("grim.nosetback", true);
        actualiserGrim(grim);

        if (monture instanceof HappyGhast) {
            // Régime normal : v = (5/3) fs × 3,9 fs × √1,5 / 0,09 ; si l'entrée dépasse 1 : v = (5/3) fs / 0,09.
            double fs = Math.sqrt(max * 0.09 / (5.0 / 3.0 * 3.9 * Math.sqrt(1.5)));
            if (3.9 * fs * Math.sqrt(1.5) >= 1) {
                fs = max * 0.09 / (5.0 / 3.0);
            }
            porter(Attribute.FLYING_SPEED, fs);
        } else {
            double vitesse = max / SOL;
            if (monture instanceof Camel) {
                vitesse -= 0.1;
                annuler(Attribute.JUMP_STRENGTH);
            }
            porter(Attribute.MOVEMENT_SPEED, vitesse);
        }

        monture.getWorld().playSound(monture.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.8f);
        joueur.sendActionBar(plugin.lang().c("debut", "<light_purple>Hyperboost !"));
        plugin.lang().saveIfNeeded();
        int[] ticks = {0};
        tache = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticks[0] += 2;
            if (!monture.isValid() || !joueur.isOnline() || !monture.getPassengers().contains(joueur)) {
                arreter(false);
                return;
            }
            if (ticks[0] >= DUREE) {
                arreter(true);
                return;
            }
            monture.getWorld().spawnParticle(Particle.FIREWORK, monture.getLocation().add(0, 0.6, 0), 3,
                    0.2, 0.2, 0.2, 0.02);
        }, 2L, 2L);
    }

    /** Monte la valeur finale de l'attribut à cible (jamais plus bas qu'avant). */
    private void porter(Attribute attribut, double cible) {
        AttributeInstance inst = monture.getAttribute(attribut);
        if (inst == null) {
            return;
        }
        double avant = inst.getValue();
        if (avant >= cible) {
            return;
        }
        // Ajout avant les multiplicateurs éventuels : la valeur finale est linéaire en l'ajout, une correction suffit.
        double ajout = cible - avant;
        inst.addTransientModifier(new AttributeModifier(cle, ajout, AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.ANY));
        modifies.add(inst);
        double obtenu = inst.getValue();
        if (Math.abs(obtenu - cible) > 1e-4 && obtenu - avant > 1e-9) {
            inst.removeModifier(cle);
            inst.addTransientModifier(new AttributeModifier(cle, ajout * (cible - avant) / (obtenu - avant),
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        }
    }

    /** Ramène la valeur de l'attribut à 0 pendant la poussée. */
    private void annuler(Attribute attribut) {
        AttributeInstance inst = monture.getAttribute(attribut);
        if (inst == null) {
            return;
        }
        inst.addTransientModifier(new AttributeModifier(cle, -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                EquipmentSlotGroup.ANY));
        modifies.add(inst);
    }

    /** Fin de la poussée : attributs rendus tels qu'avant ; l'anti-triche reste prévenu MARGE_MS de plus. */
    void arreter(boolean message) {
        if (finie) {
            return;
        }
        finie = true;
        if (tache != null) {
            tache.cancel();
        }
        for (AttributeInstance inst : modifies) {
            inst.removeModifier(cle);
        }
        modifies.clear();
        plugin.oublier(joueur.getUniqueId());
        if (message && joueur.isOnline()) {
            joueur.sendActionBar(plugin.lang().c("fin", "<gray>Hyperboost terminé"));
            plugin.lang().saveIfNeeded();
        }
        long reste = finAntiTriche - System.currentTimeMillis();
        if (!plugin.isEnabled() || !joueur.isOnline() || reste <= 0) {
            finAntiTriche();
        } else {
            finAntiTriche = System.currentTimeMillis() + MARGE_MS;
            joueur.setMetadata(METADONNEE, new FixedMetadataValue(plugin, finAntiTriche));
            Bukkit.getScheduler().runTaskLater(plugin, this::finAntiTriche, MARGE_MS / 50);
        }
    }

    private void finAntiTriche() {
        // Une nouvelle poussée a pu commencer entre-temps : on ne retire que notre propre date.
        for (MetadataValue v : joueur.getMetadata(METADONNEE)) {
            if (v.getOwningPlugin() == plugin && v.value() instanceof Long l && l == finAntiTriche) {
                joueur.removeMetadata(METADONNEE, plugin);
            }
        }
        if (grim != null) {
            try {
                joueur.removeAttachment(grim);
            } catch (IllegalArgumentException dejaRetiree) {
                // déconnecté : Bukkit a déjà retiré les permissions du joueur
            }
            grim = null;
            actualiserGrim(null);
        }
    }

    /**
     * GrimAC 2.3.74 : le renvoi en arrière d'une vérification (ex. Simulation, qui éjecte les passagers) est coupé par
     * la permission grim.nosetback.<vérification en minuscules>, lue seulement à GrimUser.updatePermissions().
     * attachement non nul : ces permissions y sont ajoutées pour chaque vérification du joueur. Sans GrimAC : rien.
     */
    private void actualiserGrim(PermissionAttachment attachement) {
        Plugin grimAc = Bukkit.getPluginManager().getPlugin("GrimAC");
        if (grimAc == null || !grimAc.isEnabled() || !joueur.isOnline()) {
            return;
        }
        try {
            ClassLoader cl = grimAc.getClass().getClassLoader();
            Class<?> fournisseur = Class.forName("ac.grim.grimac.api.GrimAPIProvider", true, cl);
            Class<?> apiItf = Class.forName("ac.grim.grimac.api.GrimAbstractAPI", true, cl);
            Class<?> userItf = Class.forName("ac.grim.grimac.api.GrimUser", true, cl);
            Class<?> checkItf = Class.forName("ac.grim.grimac.api.AbstractCheck", true, cl);
            Object api = fournisseur.getMethod("get").invoke(null);
            Object user = apiItf.getMethod("getGrimUser", Player.class).invoke(api, joueur);
            if (user == null) {
                return;
            }
            if (attachement != null) {
                for (Object check : (java.util.Collection<?>) userItf.getMethod("getChecks").invoke(user)) {
                    Object nom = checkItf.getMethod("getCheckName").invoke(check);
                    if (nom != null) {
                        attachement.setPermission("grim.nosetback." + nom.toString().toLowerCase(java.util.Locale.ROOT), true);
                    }
                }
            }
            userItf.getMethod("updatePermissions").invoke(user);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            plugin.getLogger().warning("GrimAC : renvoi en arrière non coupé pendant l'Hyperboost (" + e + ")");
        }
    }
}
