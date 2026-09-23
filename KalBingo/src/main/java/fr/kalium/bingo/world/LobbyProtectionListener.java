package fr.kalium.bingo.world;

import fr.kalium.bingo.game.GameEndService;
import fr.kalium.bingo.gui.LobbyItems;
import fr.kalium.bingo.gui.PartyMenu;
import fr.kalium.bingo.gui.PostGameMenu;
import org.bukkit.World;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * CORRIGE en 0.1.20 (demande explicite de l'utilisateur : "il fallait bloquer que pour les salles
 * d'attente, pas le modèle. il faut débloquer le modèle sinon je ne peux pas le modifier") : la
 * casse/pose de blocs, les seaux et l'interaction avec les blocs ne sont plus bloques que dans la
 * zone des salles d'attente collees (LobbySlots.isInSlotArea) - le modele de l'admin, construit
 * ailleurs dans le meme monde, est de nouveau modifiable. Le blocage des monstres et du PvP reste
 * valable dans tout le monde (n'empeche pas de modifier le modele).
 *
 * Protection de la salle d'attente Bingo (monde bingo_lobby uniquement - pas les mondes de
 * partie, voir InstanceWorldManager, ou le gameplay normal s'applique) et gestion du clic sur
 * l'objet de menu (LobbyItems) - demande explicite de l'utilisateur : "ne pas pouvoir casser de
 * bloc, ne pas pouvoir poser de bloc, ne pas pouvoir interragir avec les bloc, empêcher le spawn
 * de monstres, désactiver le pvp".
 *
 * Le clic sur la nether star a DEUX comportements distincts selon le contexte (AJOUTE avec
 * GameEndService, 23/09/2026) : pour un joueur en salle d'attente AVANT une partie (cas normal),
 * il ouvre PartyMenu (equipes, lancer/annuler) ; pour un joueur qui vient de terminer une partie
 * (victoire OU temps ecoule) et patiente ici (GameEndService.isLingering), PartyMenu echouerait
 * avec "aucune partie en attente" (sa BingoParty a deja ete consommee au lancement) - le clic ouvre
 * donc PostGameMenu a la place (demande explicite de l'utilisateur, reprecisee le 23/09/2026:
 * "une netherstar pour qu'ils puissent retourner au hub kalgames (via un menu)" - remplace le
 * depart immediat sans confirmation utilise jusqu'ici).
 */
public final class LobbyProtectionListener implements Listener {

    private final LobbySlots lobbySlots;
    private final LobbyItems lobbyItems;
    private final PartyMenu partyMenu;
    private final GameEndService gameEndService;
    private final PostGameMenu postGameMenu;

    public LobbyProtectionListener(LobbySlots lobbySlots, LobbyItems lobbyItems, PartyMenu partyMenu,
                                    GameEndService gameEndService, PostGameMenu postGameMenu) {
        this.lobbySlots = lobbySlots;
        this.lobbyItems = lobbyItems;
        this.partyMenu = partyMenu;
        this.gameEndService = gameEndService;
        this.postGameMenu = postGameMenu;
    }

    private boolean isLobbyWorld(World world) {
        return world != null && world.equals(lobbySlots.world());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() == EquipmentSlot.HAND && lobbyItems.isOurs(event.getItem())
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            if (gameEndService.isLingering(player.getUniqueId())) {
                postGameMenu.open(player);
            } else {
                partyMenu.open(player);
            }
            return;
        }
        if (!isLobbyWorld(player.getWorld())) {
            return;
        }
        if (event.getHand() == EquipmentSlot.HAND && lobbyItems.isOurs(event.getItem())) {
            event.setCancelled(true);
            return; // pas de manipulation de l'objet verrouille (clic gauche etc.)
        }
        // Casse/pose deja gerees par onBreak/onPlace ; ici : interaction avec un bloc existant
        // (portes, boutons, leviers, coffres...) et plaques de pression (Action.PHYSICAL).
        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL)
                && event.getClickedBlock() != null && lobbySlots.isInSlotArea(event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        if (lobbySlots.isInSlotArea(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent event) {
        if (lobbySlots.isInSlotArea(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (lobbySlots.isInSlotArea(event.getBlockClicked().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (lobbySlots.isInSlotArea(event.getBlockClicked().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (isLobbyWorld(event.getLocation().getWorld()) && event.getEntity() instanceof Monster) {
            event.setCancelled(true);
        }
    }

    /** Desactive le PvP dans la salle d'attente (independant d'un eventuel reglage PvP en partie, section 10). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player
                && isLobbyWorld(victim.getWorld())) {
            event.setCancelled(true);
        }
    }
}
