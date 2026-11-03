package fr.kalium.effets;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cumul des effets nourriture + boisson (cahier Blocs_Cultures_Objets.md, partie 6, LeKiwi06, 02/11/2026) : une
 * nourriture et une boisson qui donnent le même effet du jeu le font passer au niveau supérieur ; deux nourritures ou deux
 * boissons ne se cumulent pas (règle du jeu : le plus fort l'emporte).
 *
 * - La source d'un effet est la cause donnée par Paper : nourriture (FOOD : aliments du jeu, ragoût suspect,
 *   casse-croûte...), boisson (potion bue, jetable ou persistante reçue : POTION_DRINK, POTION_SPLASH,
 *   AREA_EFFECT_CLOUD ; alcools et élixirs compris), autre (balise, conduit, plugins, commandes...).
 * - Le cumul ajoute +1 au meilleur niveau des trois sources, plafonné à III (balise II + cumul = III). Il dure tant que
 *   la nourriture et la boisson qui donnent ce niveau sont actives (et la source « autre » si c'est elle qui donne le
 *   meilleur niveau) ; ensuite le jeu remet de lui-même le niveau d'avant (effet « caché » sous un effet plus fort).
 * - Tous les effets se cumulent, nocifs compris (LeKiwi06, 02/11), sauf ceux dont le niveau ne change rien
 *   ({@link #SANS_NIVEAU}) et les effets instantanés.
 * - Un effet retiré (lait, mort, totem, miel, plugin) oublie ses sources ; temps des sources gardés par compte (compte de
 *   test à part) dans cumul.yml, décomptés seulement en jeu, comme les effets du jeu.
 */
final class Cumul implements Listener {

    private static final int NIVEAU_MAX = 2;
    private static final int INFINI = Integer.MAX_VALUE;

    /** Effets dont le niveau ne change rien : pas de cumul. */
    private static final Set<PotionEffectType> SANS_NIVEAU = Set.of(PotionEffectType.NIGHT_VISION,
            PotionEffectType.FIRE_RESISTANCE, PotionEffectType.WATER_BREATHING, PotionEffectType.INVISIBILITY,
            PotionEffectType.SLOW_FALLING, PotionEffectType.DOLPHINS_GRACE, PotionEffectType.BLINDNESS,
            PotionEffectType.NAUSEA, PotionEffectType.GLOWING, PotionEffectType.DARKNESS);

    /** Un effet reçu d'une source : niveau (0 = I) et ticks restants (INFINI sans limite). */
    private static final class Source {
        final int niveau;
        int ticks;

        Source(int niveau, int ticks) {
            this.niveau = niveau;
            this.ticks = ticks;
        }
    }

    /** Sources d'un effet pour un compte, et cumul posé par ce plugin (niveau -1 : aucun). */
    private static final class Etat {
        final List<Source> nourriture = new ArrayList<>();
        final List<Source> boisson = new ArrayList<>();
        final List<Source> autre = new ArrayList<>();
        int niveauPose = -1;
        int ticksPose;

        boolean vide() {
            return nourriture.isEmpty() && boisson.isEmpty() && autre.isEmpty() && niveauPose < 0;
        }
    }

    private final KSEffets plugin;
    private final File fichier;
    private final Map<UUID, Map<PotionEffectType, Etat>> comptes = new HashMap<>();
    /** Vrai pendant que ce plugin pose un cumul (son propre événement est ignoré). */
    private boolean pose;
    private boolean modifie;

    /** Cle de deploiement (KS_MondeTest 1.2.0) : cumul dans le monde de test seulement jusqu'a sa validation. */
    static final String PHASE = "KS_Effets:cumul";

    Cumul(KSEffets plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "cumul.yml");
    }

    // ------------------------------------------------------------------ sources

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEffet(EntityPotionEffectEvent event) {
        if (pose || !(event.getEntity() instanceof Player joueur)) {
            return;
        }
        PotionEffectType type = event.getModifiedType();
        if (!concerne(type)) {
            return;
        }
        EntityPotionEffectEvent.Cause cause = event.getCause();
        switch (event.getAction()) {
            case ADDED, CHANGED -> {
                PotionEffect effet = event.getNewEffect();
                if (effet == null || cause == EntityPotionEffectEvent.Cause.EXPIRATION || !PhaseTest.ouvert(PHASE, joueur)) {
                    return;
                }
                Etat etat = comptes.computeIfAbsent(plugin.compteDe(joueur), c -> new LinkedHashMap<>())
                        .computeIfAbsent(type, t -> new Etat());
                List<Source> sources = switch (cause) {
                    case FOOD -> etat.nourriture;
                    case POTION_DRINK, POTION_SPLASH, AREA_EFFECT_CLOUD -> etat.boisson;
                    default -> etat.autre;
                };
                ajouter(sources, new Source(effet.getAmplifier(), effet.isInfinite() ? INFINI : effet.getDuration()));
                modifie = true;
                // L'effet est appliqué juste après l'événement (l'état a pu être oublié entre-temps : lait, mort).
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Map<PotionEffectType, Etat> effets = comptes.get(plugin.compteDe(joueur));
                    Etat actuel = effets == null ? null : effets.get(type);
                    if (joueur.isOnline() && actuel != null) {
                        evaluer(joueur, type, actuel);
                    }
                });
            }
            case REMOVED, CLEARED -> {
                if (cause == EntityPotionEffectEvent.Cause.EXPIRATION) {
                    return;
                }
                Map<PotionEffectType, Etat> effets = comptes.get(plugin.compteDe(joueur));
                if (effets != null && effets.remove(type) != null) {
                    modifie = true;
                }
            }
        }
    }

    private static boolean concerne(PotionEffectType type) {
        return type != null && !type.isInstant() && !SANS_NIVEAU.contains(type);
    }

    /** Ajoute une source ; celles qu'elle surpasse (niveau et temps inférieurs ou égaux) sont oubliées. */
    private static void ajouter(List<Source> sources, Source nouvelle) {
        for (Source s : sources) {
            if (s.niveau >= nouvelle.niveau && s.ticks >= nouvelle.ticks) {
                return;
            }
        }
        sources.removeIf(s -> s.niveau <= nouvelle.niveau && s.ticks <= nouvelle.ticks);
        sources.add(nouvelle);
    }

    /** La source de plus haut niveau (la plus longue à niveau égal), ou null. */
    private static Source meilleure(List<Source> sources) {
        Source best = null;
        for (Source s : sources) {
            if (best == null || s.niveau > best.niveau || (s.niveau == best.niveau && s.ticks > best.ticks)) {
                best = s;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ chaque seconde

    /** Chaque seconde, pour un joueur en jeu : temps des sources, puis cumul de chaque effet. */
    void seconde(Player joueur) {
        Map<PotionEffectType, Etat> effets = comptes.get(plugin.compteDe(joueur));
        if (effets == null) {
            return;
        }
        Iterator<Map.Entry<PotionEffectType, Etat>> it = effets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<PotionEffectType, Etat> e = it.next();
            Etat etat = e.getValue();
            decompter(etat.nourriture);
            decompter(etat.boisson);
            decompter(etat.autre);
            if (etat.niveauPose >= 0 && etat.ticksPose != INFINI && (etat.ticksPose -= 20) <= 0) {
                etat.niveauPose = -1;
            }
            if (etat.vide()) {
                it.remove();
            } else {
                evaluer(joueur, e.getKey(), etat);
            }
        }
        if (effets.isEmpty()) {
            comptes.remove(plugin.compteDe(joueur));
        }
        modifie = true;
    }

    private static void decompter(List<Source> sources) {
        for (Source s : sources) {
            if (s.ticks != INFINI) {
                s.ticks -= 20;
            }
        }
        sources.removeIf(s -> s.ticks <= 0);
    }

    /** Pose le cumul de cet effet s'il faut le poser ou le prolonger. */
    private void evaluer(Player joueur, PotionEffectType type, Etat etat) {
        Source n = meilleure(etat.nourriture);
        Source b = meilleure(etat.boisson);
        if (n == null || b == null) {
            return;
        }
        Source a = meilleure(etat.autre);
        int base = Math.max(n.niveau, b.niveau);
        int ticks = Math.min(n.ticks, b.ticks);
        if (a != null && a.niveau > base) {
            // La balise (ou une autre source) donne le meilleur niveau : le cumul dure aussi tant qu'elle est là.
            base = a.niveau;
            ticks = Math.min(ticks, a.ticks);
        }
        if (base >= NIVEAU_MAX) {
            return;
        }
        int niveau = base + 1;
        PotionEffect actuel = joueur.getPotionEffect(type);
        int niveauActuel = actuel == null ? -1 : actuel.getAmplifier();
        boolean poser = niveauActuel < niveau
                || (niveauActuel == niveau && etat.niveauPose == niveau && ticks > etat.ticksPose + 40);
        if (!poser) {
            return;
        }
        PotionEffect cumul = new PotionEffect(type, ticks == INFINI ? PotionEffect.INFINITE_DURATION : ticks, niveau,
                actuel != null && actuel.isAmbient(), actuel == null || actuel.hasParticles(),
                actuel == null || actuel.hasIcon());
        pose = true;
        try {
            joueur.addPotionEffect(cumul);
        } finally {
            pose = false;
        }
        etat.niveauPose = niveau;
        etat.ticksPose = ticks;
        modifie = true;
    }

    // ------------------------------------------------------------------ cumul.yml

    void charger() {
        comptes.clear();
        ConfigurationSection racine = YamlConfiguration.loadConfiguration(fichier).getConfigurationSection("comptes");
        if (racine == null) {
            return;
        }
        for (String cle : racine.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(cle);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection s = racine.getConfigurationSection(cle);
            Map<PotionEffectType, Etat> effets = new LinkedHashMap<>();
            for (String nom : s.getKeys(false)) {
                NamespacedKey key = NamespacedKey.fromString(nom.replaceFirst("_", ":"));
                PotionEffectType type = key == null ? null : Registry.MOB_EFFECT.get(key);
                ConfigurationSection t = s.getConfigurationSection(nom);
                if (type == null || t == null) {
                    continue;
                }
                Etat etat = new Etat();
                lire(t.getStringList("nourriture"), etat.nourriture);
                lire(t.getStringList("boisson"), etat.boisson);
                lire(t.getStringList("autre"), etat.autre);
                etat.niveauPose = t.getInt("pose.niveau", -1);
                etat.ticksPose = t.getInt("pose.ticks");
                if (!etat.vide()) {
                    effets.put(type, etat);
                }
            }
            if (!effets.isEmpty()) {
                comptes.put(uuid, effets);
            }
        }
    }

    private static void lire(List<String> lignes, List<Source> sources) {
        for (String l : lignes) {
            String[] p = l.split(":");
            try {
                sources.add(new Source(Integer.parseInt(p[0]), Integer.parseInt(p[1])));
            } catch (RuntimeException ignored) {
                // ligne illisible
            }
        }
    }

    /** Enregistre cumul.yml s'il a changé (forcer : à l'arrêt, toujours). */
    void enregistrer(boolean forcer) {
        if (!modifie && !forcer) {
            return;
        }
        YamlConfiguration yml = new YamlConfiguration();
        comptes.forEach((uuid, effets) -> effets.forEach((type, etat) -> {
            // « minecraft_speed » : ni point ni deux-points dans la clé.
            String base = "comptes." + uuid + "." + type.getKey().getNamespace() + "_" + type.getKey().getKey();
            yml.set(base + ".nourriture", ecrire(etat.nourriture));
            yml.set(base + ".boisson", ecrire(etat.boisson));
            yml.set(base + ".autre", ecrire(etat.autre));
            if (etat.niveauPose >= 0) {
                yml.set(base + ".pose.niveau", etat.niveauPose);
                yml.set(base + ".pose.ticks", etat.ticksPose);
            }
        }));
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(fichier);
            modifie = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Impossible d'enregistrer cumul.yml : " + ex.getMessage());
        }
    }

    private static List<String> ecrire(List<Source> sources) {
        List<String> lignes = new ArrayList<>();
        sources.forEach(s -> lignes.add(s.niveau + ":" + s.ticks));
        return lignes;
    }
}
