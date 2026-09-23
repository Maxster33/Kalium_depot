package fr.kalium.bingo.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Monde permanent "bingo_lobby" (jamais supprime par le nettoyage des parties,
 * contrairement aux mondes bingo_<gameId>_<n>) contenant jusqu'a
 * instances.max-simultaneous-games exemplaires de la salle d'attente, espaces le
 * long de l'axe X. Le modele (LobbyTemplateService) est colle sur TOUS les
 * emplacements des qu'il est (re)capture, pas seulement a la reservation - pour que
 * l'utilisateur puisse verifier visuellement chaque emplacement a tout moment.
 */
public final class LobbySlots {

    private final JavaPlugin plugin;
    private final LobbyTemplateService templateService;
    private World world;
    private int spacing;
    private int slotCount;
    private int startOffset;

    /** Emplacement occupe par chaque partie en attente (gameId -> numero d'emplacement). */
    private final Map<String, Integer> occupied = new HashMap<>();

    public LobbySlots(JavaPlugin plugin, LobbyTemplateService templateService) {
        this.plugin = plugin;
        this.templateService = templateService;
    }

    public boolean init() {
        spacing = Math.max(64, plugin.getConfig().getInt("lobby.spacing", 300));
        slotCount = Math.max(1, plugin.getConfig().getInt("instances.max-simultaneous-games", 4));
        // Le modele lui-meme (capture par l'admin, voir LobbyTemplateService) reste construit EN PLACE,
        // la ou l'admin l'a batis - le plus souvent pres du spawn du monde (0,80,0), la ou on atterrit
        // en arrivant dans un monde vide flambant neuf. Sans decalage, l'emplacement 0 (colle a l'origine
        // du monde) tombe donc pile a cote du modele original : "on voit les deux" (signale par
        // l'utilisateur). startOffset eloigne tous les emplacements colles de l'origine du monde.
        startOffset = Math.max(0, plugin.getConfig().getInt("lobby.slots-start-offset", 2000));

        String worldName = plugin.getConfig().getString("lobby.world-name", "bingo_lobby");
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            world = existing;
        } else {
            WorldCreator creator = new WorldCreator(worldName)
                    .generator(new VoidGenerator())
                    .environment(World.Environment.NORMAL)
                    .generateStructures(false);
            world = Bukkit.createWorld(creator);
        }
        if (world == null) {
            plugin.getLogger().severe("[KalBingo] Impossible de creer/charger le monde '" + worldName + "'.");
            return false;
        }
        // IMPORTANT : ce spawn (utilise par Bukkit tant qu'un joueur n'a pas encore ete teleporte
        // vers son emplacement reserve, voir AssignmentService.handleResponse - il y a toujours un
        // court delai reseau avant cette teleportation) doit rester loin du modele original de
        // l'admin, EXACTEMENT comme les emplacements colles (startOffset) - sinon un joueur dont
        // l'affectation n'est pas encore resolue "atterrit sur le modele" le temps de l'attente
        // (signale par l'utilisateur, 0.1.9). Auparavant fixe a (0,80,0), pres de l'origine du
        // monde ou l'admin construit naturellement en arrivant dans un monde vide neuf.
        world.setSpawnLocation(startOffset, 80, 0);
        world.setAutoSave(true); // monde permanent : contrairement aux instances de jeu, il doit survivre aux redemarrages

        if (templateService.get() != null) {
            pasteAllSlots();
        }
        return true;
    }

    public World world() {
        return world;
    }

    public int slotCount() {
        return slotCount;
    }

    /** Origine (coin le plus bas) de l'emplacement, le long de l'axe X, decalee de startOffset. */
    public Location slotOrigin(int slot) {
        return new Location(world, (double) startOffset + slot * spacing, 64, 0);
    }

    /**
     * Supprime toutes les salles d'attente collees avec l'ANCIEN modele (avant une recapture, voir
     * LobbyCaptureService.capture) - demande explicite de l'utilisateur (0.1.20) : "lorsqu'un modèle
     * est recapturé, les salles d'attente déjà générées doivent se supprimer". Sans ca, le nouveau
     * modele etait colle PAR-DESSUS l'ancien et tout ce qui depassait de l'ancien restait en place.
     */
    public void clearAllSlots(LobbyTemplate previous) {
        if (previous == null || world == null) {
            return;
        }
        // 0.1.21 : effacements mis en file d'attente, executes sur plusieurs ticks (voir LobbyTemplateService).
        for (int slot = 0; slot < slotCount; slot++) {
            templateService.clear(previous, slotOrigin(slot), null);
        }
    }

    /**
     * true si cette position est dans la zone des salles d'attente collees (tous les emplacements +
     * une marge de spacing/2 autour), c'est-a-dire la ou jouent les joueurs en attente - PAS la zone
     * du modele de l'admin (construit pres de l'origine du monde, loin des emplacements grace a
     * lobby.slots-start-offset). Seule cette zone est protegee (voir LobbyProtectionListener, 0.1.20 -
     * le modele doit rester modifiable).
     */
    public boolean isInSlotArea(Location location) {
        LobbyTemplate template = templateService.get();
        if (template == null || world == null || location == null || !world.equals(location.getWorld())) {
            return false;
        }
        int margin = spacing / 2;
        int minX = startOffset - margin;
        int maxX = startOffset + (slotCount - 1) * spacing + template.sizeX() + margin;
        int minZ = -margin;
        int maxZ = template.sizeZ() + margin;
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return x >= minX && x < maxX && z >= minZ && z < maxZ;
    }

    /** Colle le modele actuel sur tous les emplacements (appele apres chaque (re)capture, et au demarrage). */
    public void pasteAllSlots() {
        pasteAllSlots(null);
    }

    /**
     * 0.1.21 : collages mis en file d'attente, executes sur plusieurs ticks (voir LobbyTemplateService) ;
     * allDone est appele quand le dernier emplacement est colle.
     */
    public void pasteAllSlots(Runnable allDone) {
        LobbyTemplate template = templateService.get();
        if (template == null) {
            if (allDone != null) {
                allDone.run();
            }
            return;
        }
        int[] remaining = {slotCount};
        for (int slot = 0; slot < slotCount; slot++) {
            templateService.paste(template, slotOrigin(slot), () -> {
                if (--remaining[0] == 0 && allDone != null) {
                    allDone.run();
                }
            });
        }
    }

    /**
     * Reserve un emplacement pour cette partie et renvoie le point d'apparition des
     * joueurs. Null si aucun emplacement libre, ou si aucun modele n'a encore ete
     * capture (voir BingoAdminCommand : lobby pos1/pos2/spawn/capture).
     */
    public synchronized Location reserve(String gameId) {
        LobbyTemplate template = templateService.get();
        if (template == null) {
            return null;
        }
        Integer existing = occupied.get(gameId);
        if (existing != null) {
            return template.spawnLocation(slotOrigin(existing));
        }
        for (int slot = 0; slot < slotCount; slot++) {
            if (!occupied.containsValue(slot)) {
                occupied.put(gameId, slot);
                return template.spawnLocation(slotOrigin(slot));
            }
        }
        return null; // toutes les salles d'attente sont occupees
    }

    public synchronized void release(String gameId) {
        occupied.remove(gameId);
    }
}
