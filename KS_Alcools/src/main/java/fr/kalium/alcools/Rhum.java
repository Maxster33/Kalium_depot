package fr.kalium.alcools;

import fr.kalium.effets.EffetFinEvent;
import fr.kalium.effets.KSEffets;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rhum (cahier, partie 3.2.2, solution A) : tant que l'effet « Rhum » dure, la musique d'un disque du jeu choisi au
 * hasard (22 disques, choix de LeKiwi06 du 29/10 : synchronisés sans pack Java, contrairement aux musiques de fond)
 * joue autour du buveur.
 *
 * - Au début d'un morceau, seuls les joueurs à 5 blocs ou moins du buveur l'entendent, tous au même moment. Un joueur
 *   qui s'éloigne au-delà de 5 blocs (ou change de monde) ne l'entend plus ; celui qui arrive pendant un morceau attend
 *   le suivant. À la fin d'un morceau, le suivant (un autre disque) commence pour les joueurs proches.
 * - Java : le son du disque est attaché au buveur et le suit (catégorie « jukebox »).
 * - Bedrock : le son « kalium.rhum.&lt;disque&gt; » du pack geyser-bedrock (même fichier que le disque, sans position :
 *   même volume où qu'on soit), envoyé seulement à ce joueur ; Geyser le transmet tel quel (nom inconnu de lui).
 * - Fin de l'effet (temps écoulé, lait, mort), déconnexion du buveur : la musique s'arrête, même au milieu d'un morceau.
 */
final class Rhum implements Listener {

    static final String EFFET_RHUM = "ks_alcools:rhum";

    /** Un disque : nom du jeu, durée (données « jukebox_song » du jeu 26.3). */
    private record Disque(String id, int secondes) {
    }

    private static final List<Disque> DISQUES = List.of(new Disque("11", 71), new Disque("13", 178),
            new Disque("5", 178), new Disque("blocks", 345), new Disque("bounce", 234), new Disque("cat", 185),
            new Disque("chirp", 185), new Disque("creator", 176), new Disque("creator_music_box", 73),
            new Disque("far", 174), new Disque("lava_chicken", 134), new Disque("mall", 197),
            new Disque("mellohi", 96), new Disque("otherside", 195), new Disque("pigstep", 149),
            new Disque("precipice", 299), new Disque("relic", 218), new Disque("stal", 150), new Disque("strad", 188),
            new Disque("tears", 175), new Disque("wait", 238), new Disque("ward", 251));

    private static final double PORTEE = 5;

    /** Morceau en cours d'un buveur : disque, fin (ms), joueurs qui l'entendent. */
    private static final class Seance {
        Disque disque;
        long fin;
        final Set<UUID> auditeurs = new HashSet<>();
    }

    private final KSAlcools plugin;
    private final Map<UUID, Seance> seances = new HashMap<>();

    Rhum(KSAlcools plugin) {
        this.plugin = plugin;
    }

    void demarrer() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::verifier, 10L, 10L);
    }

    /** Arrêt du plugin : toutes les musiques s'arrêtent. */
    void toutArreter() {
        for (Seance s : seances.values()) {
            arreter(s);
        }
        seances.clear();
    }

    /** Tous les 10 ticks : auditeurs trop loin, fin des morceaux, effets terminés. */
    private void verifier() {
        long maintenant = System.currentTimeMillis();
        for (Player buveur : Bukkit.getOnlinePlayers()) {
            Seance s = seances.get(buveur.getUniqueId());
            if (!KSEffets.actif(buveur, EFFET_RHUM)) {
                if (s != null) {
                    arreter(s);
                    seances.remove(buveur.getUniqueId());
                }
                continue;
            }
            if (s == null) {
                s = new Seance();
                seances.put(buveur.getUniqueId(), s);
            }
            for (UUID uuid : new ArrayList<>(s.auditeurs)) {
                Player auditeur = Bukkit.getPlayer(uuid);
                if (auditeur == null) {
                    s.auditeurs.remove(uuid);
                } else if (!proche(auditeur, buveur)) {
                    couper(auditeur, s.disque);
                    s.auditeurs.remove(uuid);
                }
            }
            if (s.disque == null || maintenant >= s.fin) {
                lancer(buveur, s, maintenant);
            }
        }
    }

    /** Nouveau morceau (un autre disque que le précédent) pour les joueurs proches du buveur. */
    private void lancer(Player buveur, Seance s, long maintenant) {
        Disque d;
        do {
            d = DISQUES.get(ThreadLocalRandom.current().nextInt(DISQUES.size()));
        } while (d == s.disque);
        s.disque = d;
        s.fin = maintenant + d.secondes() * 1000L + 1000L;
        s.auditeurs.clear();
        for (Player auditeur : buveur.getWorld().getPlayers()) {
            if (proche(auditeur, buveur)) {
                jouer(auditeur, buveur, d);
                s.auditeurs.add(auditeur.getUniqueId());
            }
        }
    }

    private static boolean proche(Player auditeur, Player buveur) {
        return auditeur.getWorld().equals(buveur.getWorld())
                && auditeur.getLocation().distanceSquared(buveur.getLocation()) <= PORTEE * PORTEE;
    }

    private static void jouer(Player auditeur, Player buveur, Disque d) {
        if (estBedrock(auditeur)) {
            auditeur.playSound(buveur.getLocation(), son(auditeur, d), SoundCategory.RECORDS, 1f, 1f);
        } else {
            auditeur.playSound(buveur, son(auditeur, d), SoundCategory.RECORDS, 1f, 1f);
        }
    }

    private static void couper(Player auditeur, Disque d) {
        if (d != null) {
            auditeur.stopSound(son(auditeur, d), SoundCategory.RECORDS);
        }
    }

    /** Java : le son du disque ; Bedrock : le son sans position du pack (Geyser retire « minecraft: »). */
    private static String son(Player auditeur, Disque d) {
        return estBedrock(auditeur) ? "minecraft:kalium.rhum." + d.id() : "minecraft:music_disc." + d.id();
    }

    private void arreter(Seance s) {
        for (UUID uuid : s.auditeurs) {
            Player auditeur = Bukkit.getPlayer(uuid);
            if (auditeur != null) {
                couper(auditeur, s.disque);
            }
        }
        s.auditeurs.clear();
        s.disque = null;
    }

    @EventHandler
    public void onFin(EffetFinEvent event) {
        if (event.getId().equals(EFFET_RHUM)) {
            Seance s = seances.remove(event.getJoueur().getUniqueId());
            if (s != null) {
                arreter(s);
            }
        }
    }

    @EventHandler
    public void onQuitter(PlayerQuitEvent event) {
        Seance s = seances.remove(event.getPlayer().getUniqueId());
        if (s != null) {
            arreter(s);
        }
    }

    /** Joueur Bedrock (Floodgate, lu sans dépendance de compilation ; sinon UUID Floodgate : 64 premiers bits à 0). */
    private static boolean estBedrock(Player player) {
        UUID uuid = player.getUniqueId();
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return (Boolean) api.getMethod("isFloodgatePlayer", UUID.class).invoke(instance, uuid);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return uuid.getMostSignificantBits() == 0;
        }
    }
}
