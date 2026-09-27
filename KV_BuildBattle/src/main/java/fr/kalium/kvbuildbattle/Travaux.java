package fr.kalium.kvbuildbattle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import net.kyori.adventure.text.Component;

/**
 * Capture, collage et effacement de zones, petit à petit (un coût maximal par tick, chunk par chunk, chunks chargés en
 * arrière-plan) pour ne jamais bloquer le serveur. Les travaux passent un par un dans une file d'attente. Repris de la
 * salle d'attente du Bingo (KG_BingoGame, LobbyTemplateService).
 */
final class Travaux {

    private final KVBuildBattle plugin;
    private final ArrayDeque<Zone> file = new ArrayDeque<>();
    private Zone actif;

    Travaux(KVBuildBattle plugin) {
        this.plugin = plugin;
    }

    boolean occupe() {
        return actif != null || !file.isEmpty();
    }

    private int coutParTick() {
        return Math.max(500, Math.min(500_000, plugin.getConfig().getInt("blocs-par-tick", 10_000)));
    }

    /**
     * Capture le cuboïde entre deux coins (inclus ; le coin le plus bas devient l'origine du modèle) avec un point
     * d'apparition. La taille est vérifiée tout de suite (IllegalArgumentException).
     */
    void capturer(Location c1, Location c2, Location apparition, Consumer<Modele> fini, Consumer<String> erreur) {
        World monde = c1.getWorld();
        int minX = Math.min(c1.getBlockX(), c2.getBlockX()), maxX = Math.max(c1.getBlockX(), c2.getBlockX());
        int minY = Math.max(monde.getMinHeight(), Math.min(c1.getBlockY(), c2.getBlockY()));
        int maxY = Math.min(monde.getMaxHeight() - 1, Math.max(c1.getBlockY(), c2.getBlockY()));
        int minZ = Math.min(c1.getBlockZ(), c2.getBlockZ()), maxZ = Math.max(c1.getBlockZ(), c2.getBlockZ());
        int sx = maxX - minX + 1, sy = maxY - minY + 1, sz = maxZ - minZ + 1;
        int max = Math.max(4, plugin.getConfig().getInt("taille-max", 256));
        if (sx > max || sy > max || sz > max) {
            throw new IllegalArgumentException("Sélection trop grande (" + max + " blocs par côté au plus, réglage taille-max).");
        }
        if (sy <= 0) throw new IllegalArgumentException("Hauteur de sélection invalide.");

        // Entités relevées tout de suite (l'admin est sur place : les chunks sont chargés).
        List<Modele.Entite> entites = new ArrayList<>();
        for (Entity e : monde.getNearbyEntities(new BoundingBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1))) {
            if (!copiable(e)) continue;
            EntitySnapshot copie = e.createSnapshot();
            if (copie == null) continue;
            Location l = e.getLocation();
            entites.add(new Modele.Entite(l.getX() - minX, l.getY() - minY, l.getZ() - minZ, l.getYaw(), l.getPitch(),
                    copie.getAsString()));
        }
        double ax = apparition.getX() - minX, ay = apparition.getY() - minY, az = apparition.getZ() - minZ;
        float yaw = apparition.getYaw(), pitch = apparition.getPitch();

        char[] blocs = new char[sx * sy * sz];
        List<String> palette = new ArrayList<>();
        Map<BlockData, Integer> indices = new HashMap<>();
        palette.add(Modele.AIR);
        ajouter(new Zone("capture", monde, minX, minY, minZ, sx, sy, sz) {
            @Override
            int bloc(Block b, int rx, int ry, int rz) {
                if (b.getType().isAir()) return 1;
                BlockData d = b.getBlockData();
                Integer i = indices.get(d);
                if (i == null) {
                    i = palette.size();
                    if (i > Modele.MAX_PALETTE) throw new IllegalStateException("trop de blocs différents");
                    palette.add(d.getAsString());
                    indices.put(d, i);
                }
                blocs[rx + ry * sx + rz * sx * sy] = (char) i.intValue();
                return 2;
            }

            @Override
            void fin(Throwable err) {
                if (err != null) {
                    erreur.accept("Capture échouée : " + err.getMessage());
                    return;
                }
                fini.accept(new Modele(sx, sy, sz, palette.toArray(new String[0]), blocs, ax, ay, az, yaw, pitch, entites));
            }
        });
    }

    /** Colle le modèle, coin le plus bas en (x, y, z), puis recrée ses entités (les anciennes copies sont retirées). */
    void coller(Modele m, World monde, int x, int y, int z, Runnable fini) {
        ajouter(new Zone("collage", monde, x, y, z, m.sx, m.sy, m.sz) {
            @Override
            int bloc(Block b, int rx, int ry, int rz) {
                int i = m.indice(rx, ry, rz);
                if (i == 0) {
                    if (b.getType().isAir()) return 1;
                    b.setBlockData(m.etat(0), false);
                    return 4;
                }
                BlockData voulu = m.etat(i);
                if (b.getBlockData().equals(voulu)) return 2;
                b.setBlockData(voulu, false);
                return 4;
            }

            @Override
            void fin(Throwable err) {
                placerEntites(m, monde, x, y, z);
                if (fini != null) fini.run();
            }
        });
    }

    /** Remplace par de l'air un cuboïde (coin le plus bas en (x, y, z)) et retire ses entités (hors joueurs). */
    void effacer(World monde, int x, int y, int z, int sx, int sy, int sz, Runnable fini) {
        BlockData air = Bukkit.createBlockData(Material.AIR);
        ajouter(new Zone("effacement", monde, x, y, z, sx, sy, sz) {
            @Override
            int bloc(Block b, int rx, int ry, int rz) {
                if (b.getType().isAir()) return 1;
                b.setBlockData(air, false);
                return 4;
            }

            @Override
            void fin(Throwable err) {
                retirerEntites(monde, new BoundingBox(x, y, z, x + sx, y + sy, z + sz));
                if (fini != null) fini.run();
            }
        });
    }

    private static boolean copiable(Entity e) {
        return !(e instanceof Player) && !(e instanceof Item) && !(e instanceof ExperienceOrb) && !(e instanceof Projectile);
    }

    private static void retirerEntites(World monde, BoundingBox zone) {
        for (Entity e : monde.getNearbyEntities(zone)) {
            if (copiable(e)) e.remove();
        }
    }

    private static final Pattern NOM = Pattern.compile("CustomName:\"((?:[^\"\\\\]|\\\\.)*)\"");

    /**
     * Retire toutes les entités de la zone collée (hors joueurs), puis recrée celles du modèle. 0.3.5 : le retrait se
     * faisait seulement si le modèle avait des entités ; les mobs posés en partie restaient d'une partie à l'autre.
     */
    private void placerEntites(Modele m, World monde, int x, int y, int z) {
        for (int cx = x >> 4; cx <= (x + m.sx) >> 4; cx++) {
            for (int cz = z >> 4; cz <= (z + m.sz) >> 4; cz++) {
                monde.getChunkAt(cx, cz).getEntities(); // charge le chunk et ses entités
            }
        }
        retirerEntites(monde, new BoundingBox(x, y, z, x + m.sx, y + m.sy, z + m.sz));
        if (m.entites.isEmpty()) return;
        for (Modele.Entite e : m.entites) {
            try {
                Entity cree = Bukkit.getEntityFactory().createEntitySnapshot(e.copie())
                        .createEntity(new Location(monde, x + e.dx(), y + e.dy(), z + e.dz(), e.yaw(), e.pitch()));
                cree.setInvulnerable(true); // décor
                Matcher nom = NOM.matcher(e.copie());
                if (nom.find()) {
                    cree.customName(Component.text(nom.group(1).replace("\\\"", "\"").replace("\\\\", "\\")));
                    cree.setCustomNameVisible(e.copie().contains("CustomNameVisible:1b")
                            || e.copie().contains("CustomNameVisible:true"));
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Entité non recréée : " + ex.getMessage());
            }
        }
    }

    /** Lance l'action une fois tous les travaux déjà demandés terminés. */
    void apres(Runnable action) {
        ajouter(new Zone(null, null, 0, 0, 0, 1, 1, 1) {
            @Override
            public void run() {
                terminer(null);
            }

            @Override
            int bloc(Block b, int rx, int ry, int rz) {
                return 0;
            }

            @Override
            void fin(Throwable err) {
                action.run();
            }
        });
    }

    // --- File d'attente ---

    private void ajouter(Zone z) {
        file.add(z);
        suivant();
    }

    private void suivant() {
        if (actif != null || file.isEmpty() || !plugin.isEnabled()) return;
        actif = file.poll();
        actif.runTaskTimer(plugin, 1L, 1L);
    }

    /** Parcourt une zone chunk par chunk, bloc par bloc, dans la limite du coût par tick. */
    private abstract class Zone extends BukkitRunnable {
        private final String nom;
        private final World monde;
        private final int x0, y0, z0, sx, sy, sz;
        private final int cx0, cz0, largeurCx, totalChunks;
        private int chunk, colonne, dy;
        private boolean demande, charge, termine;
        private final long debut = System.currentTimeMillis();

        Zone(String nom, World monde, int x0, int y0, int z0, int sx, int sy, int sz) {
            this.nom = nom;
            this.monde = monde;
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.cx0 = x0 >> 4;
            this.cz0 = z0 >> 4;
            this.largeurCx = ((x0 + sx - 1) >> 4) - cx0 + 1;
            this.totalChunks = largeurCx * (((z0 + sz - 1) >> 4) - cz0 + 1);
        }

        /** Traite un bloc (coordonnées relatives) ; renvoie son coût. */
        abstract int bloc(Block b, int rx, int ry, int rz);

        /** Fin des travaux (err = null si tout s'est bien passé). */
        abstract void fin(Throwable err);

        @Override
        public void run() {
            try {
                int budget = coutParTick();
                while (budget > 0 && chunk < totalChunks) {
                    int cx = cx0 + chunk % largeurCx, cz = cz0 + chunk / largeurCx;
                    if (!charge) {
                        if (monde.isChunkLoaded(cx, cz)) {
                            monde.addPluginChunkTicket(cx, cz, plugin);
                            charge = true;
                            colonne = 0;
                            dy = 0;
                        } else {
                            if (!demande) {
                                demande = true;
                                monde.getChunkAtAsync(cx, cz, true).whenComplete((c, err) -> {
                                    if (termine) return;
                                    if (c != null) {
                                        monde.addPluginChunkTicket(cx, cz, plugin);
                                        charge = true;
                                        colonne = 0;
                                        dy = 0;
                                    } else {
                                        demande = false; // nouvel essai au tick suivant
                                    }
                                });
                            }
                            return;
                        }
                    }
                    int xa = Math.max(x0, cx << 4), xb = Math.min(x0 + sx - 1, (cx << 4) + 15);
                    int za = Math.max(z0, cz << 4), zb = Math.min(z0 + sz - 1, (cz << 4) + 15);
                    int largeur = xb - xa + 1, colonnes = largeur * (zb - za + 1);
                    while (budget > 0 && colonne < colonnes) {
                        int x = xa + colonne % largeur, z = za + colonne / largeur;
                        while (budget > 0 && dy < sy) {
                            budget -= bloc(monde.getBlockAt(x, y0 + dy, z), x - x0, dy, z - z0);
                            dy++;
                        }
                        if (dy >= sy) {
                            dy = 0;
                            colonne++;
                        }
                    }
                    if (colonne >= colonnes) {
                        monde.removePluginChunkTicket(cx, cz, plugin);
                        chunk++;
                        charge = false;
                        demande = false;
                    }
                }
                if (chunk >= totalChunks) terminer(null);
            } catch (Throwable t) {
                plugin.getLogger().severe(nom + " échoué : " + t);
                terminer(t);
            }
        }

        void terminer(Throwable err) {
            if (termine) return;
            termine = true;
            cancel();
            if (charge) monde.removePluginChunkTicket(cx0 + chunk % largeurCx, cz0 + chunk / largeurCx, plugin);
            if (err == null && nom != null) {
                plugin.getLogger().info(nom + " terminé (" + sx + "x" + sy + "x" + sz + ", "
                        + (System.currentTimeMillis() - debut) / 1000 + " s).");
            }
            actif = null;
            try {
                fin(err);
            } finally {
                suivant();
            }
        }
    }
}
