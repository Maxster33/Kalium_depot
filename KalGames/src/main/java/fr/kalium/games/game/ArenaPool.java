package fr.kalium.games.game;

import fr.kalium.games.KalGames;
import fr.kalium.games.model.Arena;
import fr.kalium.games.model.Minigame;
import fr.kalium.games.world.Template;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reserve d'arenes deja collees dans le monde des instances. A la fin d'une partie l'arene n'est pas supprimee :
 * elle est remise a l'etat d'origine puis gardee de cote pour la partie suivante (aucun chargement, donc pas de lag).
 * Certains mini-jeux peuvent en pre-generer plusieurs des le demarrage du serveur (reglage "prewarm-arenas"),
 * sauf les courses de bateau qui n'en pre-generent jamais (voir {@link #prewarm}).
 */
final class ArenaPool {

    /** Un emplacement du monde des instances contenant une copie de l'arene. */
    static final class Cell {
        final String arenaId;
        final String minigameId;
        final int slot;
        final Template template;
        /** Le collage est termine. */
        boolean pasted;
        BukkitTask paste;
        /** Partie qui utilise l'arene, null si elle est libre. */
        GameInstance owner;
        /** Nombre de parties deja jouees dans cette copie. */
        int uses;
        boolean discarded;

        Cell(String arenaId, String minigameId, int slot, Template template) {
            this.arenaId = arenaId;
            this.minigameId = minigameId;
            this.slot = slot;
            this.template = template;
        }

        boolean free() {
            return owner == null && !discarded;
        }
    }

    private final KalGames plugin;
    private final List<Cell> cells = new ArrayList<>();

    ArenaPool(KalGames plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ config

    private boolean keepEnabled() {
        return plugin.getConfig().getBoolean("instances.keep-arena-after-match", true);
    }

    private int keepIdlePerArena() {
        return Math.max(0, plugin.getConfig().getInt("instances.keep-idle-per-arena", 2));
    }

    private int recycleAfter() {
        return Math.max(1, plugin.getConfig().getInt("instances.recycle-after", 25));
    }

    /**
     * Nombre de copies pre-generees de chaque arene d'un mini-jeu (0 = aucune). Les courses de bateau n'en
     * pre-generent plus jamais (ignore toute valeur "prewarm-arenas" deja enregistree) : leurs arenes ne sont
     * collees qu'a la demande, comme les autres mini-jeux.
     */
    private int prewarm(Minigame minigame) {
        if (minigame.type() == fr.kalium.games.model.MinigameType.BOAT_RACE) {
            return 0;
        }
        int columns = Math.max(1, plugin.getConfig().getInt("instances.per-arena", plugin.getConfig().getInt("instances.columns", 10)));
        return Math.max(0, Math.min(columns, minigame.getInt("prewarm-arenas", 0)));
    }

    // ------------------------------------------------------------------ acces

    /** Copies libres (pretes ou en cours de collage) de cette arene. */
    boolean hasFree(String arenaId) {
        for (Cell cell : cells) {
            if (cell.free() && cell.arenaId.equals(arenaId)) {
                return true;
            }
        }
        return false;
    }

    /** Copies pretes (collage termine et libres) de cette arene. */
    boolean hasReady(String arenaId) {
        for (Cell cell : cells) {
            if (cell.free() && cell.pasted && cell.arenaId.equals(arenaId)) {
                return true;
            }
        }
        return false;
    }

    int count(String arenaId) {
        int n = 0;
        for (Cell cell : cells) {
            if (!cell.discarded && cell.arenaId.equals(arenaId)) {
                n++;
            }
        }
        return n;
    }

    int freeCount() {
        int n = 0;
        for (Cell cell : cells) {
            if (cell.free()) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ prise / retour

    /** Prend une copie libre de l'arene (prete de preference). Une copie d'un ancien modele est supprimee. */
    Cell acquire(Arena arena, Template template) {
        Cell best = null;
        for (Cell cell : new ArrayList<>(cells)) {
            if (!cell.free() || !cell.arenaId.equals(arena.id())) {
                continue;
            }
            if (cell.template != template) {
                // L'arene a ete recapturee depuis : cette copie est perimee (blocs effaces, 1.12.2).
                discard(cell, true);
                continue;
            }
            if (best == null || (cell.pasted && !best.pasted)) {
                best = cell;
            }
        }
        return best;
    }

    /** Colle une nouvelle copie de l'arene (null si aucun emplacement n'est libre). */
    Cell create(Arena arena, Minigame minigame, Template template) {
        int slot = plugin.worlds().allocate(arena.id());
        if (slot < 0) {
            return null;
        }
        Cell cell = new Cell(arena.id(), minigame.id(), slot, template);
        cells.add(cell);
        cell.paste = plugin.templates().paste(template, plugin.worlds().world(),
                plugin.worlds().slotX(slot), plugin.worlds().slotZ(slot), () -> pasted(cell));
        return cell;
    }

    private void pasted(Cell cell) {
        cell.paste = null;
        if (cell.discarded) {
            return;
        }
        cell.pasted = true;
        GameInstance owner = cell.owner;
        if (owner != null) {
            if (!owner.closing()) {
                owner.onPasted();
            }
            return;
        }
        // Copie pre-generee : suivante.
        trim(cell.minigameId, cell.arenaId);
        prewarmSoon();
    }

    /**
     * La partie est terminee (ou fermee) : l'arene est remise a l'etat d'origine et gardee de cote, ou supprimee si
     * elle est perimee, trop utilisee, ou en surnombre.
     */
    void release(Cell cell, GameInstance instance) {
        if (cell.owner == instance) {
            cell.owner = null;
        }
        cell.uses++;
        Template current = plugin.templates().get(cell.arenaId);
        boolean reusable = keepEnabled() && plugin.isEnabled() && plugin.worlds().ready() && !cell.discarded
                && current == cell.template && cell.uses < recycleAfter();
        if (!reusable) {
            // Modele recapture pendant la partie : la copie perimee est reellement effacee (1.12.2).
            discard(cell, current != cell.template);
            prewarmSoon();
            return;
        }
        try {
            instance.restoreForReuse();
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Remise en état de l'arène " + cell.arenaId + " impossible : " + e);
            discard(cell);
            prewarmSoon();
            return;
        }
        if (instance.minigame().type() == fr.kalium.games.model.MinigameType.RUSH) {
            // 1.12.1 : Rush -> en plus des blocs suivis, TOUTE la zone de l'arene est comparee au modele (air
            // compris) et corrigee avant d'etre reutilisee - demande explicite de l'utilisateur. La copie n'est
            // proposee a une nouvelle partie qu'une fois cette verification terminee (comme apres un collage).
            cell.pasted = false;
            int x = plugin.worlds().slotX(cell.slot);
            int z = plugin.worlds().slotZ(cell.slot);
            String arenaId = cell.arenaId;
            cell.paste = plugin.templates().reconcile(cell.template, plugin.worlds().world(), x, z, fixed -> {
                if (fixed > 0) {
                    plugin.getLogger().info("Arène " + arenaId + " remise à l'identique du modèle : " + fixed + " bloc(s) corrigé(s).");
                }
                pasted(cell);
            });
            return;
        }
        trim(cell.minigameId, cell.arenaId);
    }

    /** Supprime les copies libres en surnombre (les plus utilisees d'abord). */
    private void trim(String minigameId, String arenaId) {
        Minigame minigame = plugin.repository().minigame(minigameId);
        int keep = Math.max(keepIdlePerArena(), minigame == null ? 0 : prewarm(minigame));
        List<Cell> free = new ArrayList<>();
        for (Cell cell : cells) {
            if (cell.free() && cell.arenaId.equals(arenaId)) {
                free.add(cell);
            }
        }
        if (free.size() <= keep) {
            return;
        }
        // Les copies les plus utilisees sont retirees en premier.
        free.sort(Comparator.comparingInt((Cell cell) -> -cell.uses));
        for (int i = 0; i < free.size() - keep; i++) {
            discard(free.get(i));
        }
    }

    private void prewarmSoon() {
        if (plugin.isEnabled()) {
            plugin.later(5L, this::ensurePrewarm);
        }
    }

    /** Supprime la copie : chunks dechargees sans sauvegarde, emplacement libere. */
    void discard(Cell cell) {
        discard(cell, false);
    }

    /**
     * @param wipe Efface reellement les blocs (voir {@link fr.kalium.games.world.TemplateService#clearArea}) avant
     *             de liberer l'emplacement, au lieu de se contenter de decharger les chunks. Necessaire pour un
     *             abandon DEFINITIF (arene supprimee ou recapturee) : depuis que le monde des instances peut etre
     *             conserve entre deux redemarrages propres, une copie juste dechargee laisserait sinon ses blocs
     *             sur le disque indefiniment.
     */
    private void discard(Cell cell, boolean wipe) {
        if (cell.discarded) {
            return;
        }
        cell.discarded = true;
        cells.remove(cell);
        if (cell.paste != null) {
            cell.paste.cancel();
            cell.paste = null;
        }
        int x = plugin.worlds().slotX(cell.slot);
        int z = plugin.worlds().slotZ(cell.slot);
        if (wipe && plugin.isEnabled() && plugin.worlds().ready()) {
            plugin.templates().clearArea(plugin.worlds().world(), x, z, () -> plugin.worlds().release(cell.slot));
            return;
        }
        plugin.templates().release(cell.template, plugin.worlds().world(), x, z);
        if (plugin.isEnabled()) {
            // L'emplacement n'est reutilisable qu'apres une seconde passe de dechargement.
            plugin.later(20L, () -> {
                plugin.templates().unloadAgain(cell.template, plugin.worlds().world(), x, z);
                plugin.worlds().release(cell.slot);
            });
        } else {
            plugin.worlds().release(cell.slot);
        }
    }

    /** Supprime (et efface reellement les blocs de) les copies libres d'une arene : modele modifie, arene supprimee. */
    void purgeArena(String arenaId) {
        for (Cell cell : new ArrayList<>(cells)) {
            if (cell.free() && cell.arenaId.equals(arenaId)) {
                discard(cell, true);
            }
        }
    }

    /** Supprime (et efface reellement les blocs de) les copies libres de toutes les arenes d'un mini-jeu supprime. */
    void purgeMinigame(String minigameId) {
        for (Cell cell : new ArrayList<>(cells)) {
            if (cell.free() && cell.minigameId.equals(minigameId)) {
                discard(cell, true);
            }
        }
    }

    /** Arret du plugin : plus rien a gerer (le monde des instances n'est normalement plus sauvegarde depuis {@link #saveCells()}). */
    void shutdown() {
        for (Cell cell : new ArrayList<>(cells)) {
            if (cell.paste != null) {
                cell.paste.cancel();
                cell.paste = null;
            }
            cell.discarded = true;
        }
        cells.clear();
    }

    // ------------------------------------------------------------------ persistance (arret propre)

    private File cellsFile() {
        return new File(plugin.getDataFolder(), "instances-cells.yml");
    }

    /**
     * Sauvegarde les copies d'arenes deja collees (pretes, non perimees) pour qu'elles survivent a un redemarrage
     * propre du serveur au lieu d'etre re-generees. A appeler avant {@link #shutdown()}, a l'extinction du plugin ;
     * voir {@link fr.kalium.games.world.InstanceWorld#markClean()} pour la sauvegarde du monde lui-meme.
     */
    void saveCells() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Cell cell : cells) {
            if (!cell.pasted || cell.discarded) {
                continue;
            }
            File templateFile = plugin.templates().file(cell.arenaId);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("arena", cell.arenaId);
            entry.put("minigame", cell.minigameId);
            entry.put("slot", cell.slot);
            entry.put("uses", cell.uses);
            entry.put("template-modified", templateFile.exists() ? templateFile.lastModified() : -1L);
            list.add(entry);
        }
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("cells", list);
        try {
            cfg.save(cellsFile());
        } catch (IOException e) {
            plugin.getLogger().warning("Sauvegarde des arènes pré-générées impossible : " + e.getMessage());
        }
    }

    /**
     * Restaure les copies d'arenes sauvegardees par {@link #saveCells()} : a n'appeler qu'apres un redemarrage
     * propre confirme (InstanceWorld.cleanRestart()), le monde et les emplacements etant alors intacts. Une copie
     * dont l'arene a ete supprimee ou recapturee depuis (modele modifie) n'est pas restauree.
     */
    void loadCells() {
        File file = cellsFile();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<?> list = cfg.getList("cells");
        if (list == null) {
            return;
        }
        int restored = 0;
        for (Object raw : list) {
            if (!(raw instanceof Map<?, ?> entry)) {
                continue;
            }
            Object arenaObj = entry.get("arena");
            Object minigameObj = entry.get("minigame");
            if (arenaObj == null || minigameObj == null) {
                continue;
            }
            String arenaId = String.valueOf(arenaObj);
            String minigameId = String.valueOf(minigameObj);
            int slot = entry.get("slot") instanceof Number n ? n.intValue() : -1;
            int uses = entry.get("uses") instanceof Number n ? n.intValue() : 0;
            long savedModified = entry.get("template-modified") instanceof Number n ? n.longValue() : -2L;
            if (slot < 0) {
                continue;
            }
            Template template = plugin.templates().get(arenaId);
            File templateFile = plugin.templates().file(arenaId);
            long currentModified = templateFile.exists() ? templateFile.lastModified() : -1L;
            if (template == null || currentModified != savedModified) {
                // Arene supprimee ou recapturee depuis : la copie collee ne correspond plus au modele actuel.
                continue;
            }
            Cell cell = new Cell(arenaId, minigameId, slot, template);
            cell.pasted = true;
            cell.uses = uses;
            cells.add(cell);
            plugin.worlds().markUsed(slot);
            restored++;
        }
        if (restored > 0) {
            plugin.getLogger().info("Arènes déjà générées restaurées après redémarrage : " + restored + ".");
        }
    }

    // ------------------------------------------------------------------ pre-generation

    /**
     * Complete la reserve : chaque arene prete d'un mini-jeu qui pre-genere des copies (prewarm-arenas) en garde ce
     * nombre au total (parties en cours comprises). Une seule copie est collee a la fois pour ne pas surcharger le serveur.
     */
    void ensurePrewarm() {
        if (!plugin.isEnabled() || !plugin.worlds().ready()) {
            return;
        }
        for (Cell cell : cells) {
            if (cell.owner == null && !cell.pasted && !cell.discarded) {
                return; // un collage de pre-generation est deja en cours
            }
        }
        for (Minigame minigame : plugin.repository().minigames()) {
            int wanted = prewarm(minigame);
            if (wanted <= 0 || !minigame.playable()) {
                continue;
            }
            for (Arena arena : plugin.instances().usableArenas(minigame)) {
                Template template = plugin.templates().get(arena.id());
                if (template == null) {
                    continue;
                }
                int room = plugin.worlds().spacing() - Math.max(0, plugin.getConfig().getInt("instances.min-distance", 1000));
                if (template.sizeX() > room || template.sizeZ() > room) {
                    continue;
                }
                if (count(arena.id()) < wanted) {
                    Cell cell = create(arena, minigame, template);
                    if (cell != null) {
                        plugin.getLogger().info("Préchargement de l'arène " + arena.id() + " : copie " + count(arena.id()) + "/" + wanted + "…");
                        return;
                    }
                }
            }
        }
    }
}
