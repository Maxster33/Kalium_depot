package fr.kalium.trains;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.JukeboxPlayable;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.JukeboxSong;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 1.3.0 (LeKiwi06 : « il faut aussi créer le minecart jukebox, qui peut récupérer et reposer les disques dans les
 * minecart coffres devant lui (le plus proche de la loco, pas de l'arrière du train) » ; réponses : le coffre juste
 * devant lui uniquement, les mêmes modes que le bateau à coffre et jukebox (lecture, pause) et un mode aléatoire ;
 * maquette validée).
 *
 * Le wagon jukebox prend un disque dans le wagon à coffre juste devant lui (côté locomotive ; sortie du coffre
 * autorisée), le joue (la musique sort du wagon et le suit, volume `jukebox.volume`), puis le remet dans le coffre (à sa
 * case si elle est libre ; entrée autorisée) à la fin du morceau. Modes : lecture (dans l'ordre des cases, en boucle),
 * aléatoire (un disque au hasard à chaque morceau), pause (le disque revient dans le coffre). Rien ne joue sans joueur à
 * portée de la musique. Un disque qui ne peut pas revenir (coffre plein, dételé) reste dans le wagon et tombe s'il est
 * cassé.
 */
final class Jukebox {

    static final byte PAUSE = 0;
    static final byte LECTURE = 1;
    static final byte ALEATOIRE = 2;
    private static final long SILENCE = 20;

    private final KSTrains plugin;
    private final Entonnoirs entonnoirs;
    private final NamespacedKey cleMode;
    private final NamespacedKey cleDisque;
    private final NamespacedKey clePlace;
    private final Map<UUID, Long> fins = new HashMap<>();

    Jukebox(KSTrains plugin, Entonnoirs entonnoirs) {
        this.plugin = plugin;
        this.entonnoirs = entonnoirs;
        this.cleMode = new NamespacedKey(plugin, "jukebox-mode");
        this.cleDisque = new NamespacedKey(plugin, "jukebox-disque");
        this.clePlace = new NamespacedKey(plugin, "jukebox-case");
    }

    static JukeboxSong chanson(ItemStack objet) {
        if (objet == null || objet.isEmpty()) {
            return null;
        }
        JukeboxPlayable lisible = objet.getData(DataComponentTypes.JUKEBOX_PLAYABLE);
        return lisible == null ? null : lisible.jukeboxSong();
    }

    byte mode(Entity wagon) {
        Byte mode = wagon.getPersistentDataContainer().get(cleMode, PersistentDataType.BYTE);
        return mode == null ? LECTURE : mode;
    }

    void mode(Entity wagon, byte mode) {
        wagon.getPersistentDataContainer().set(cleMode, PersistentDataType.BYTE, mode);
    }

    /** Clic droit : lecture, aléatoire, pause, puis de nouveau lecture. */
    Component suivant(Entity wagon) {
        byte mode = mode(wagon);
        byte nouveau = mode == LECTURE ? ALEATOIRE : mode == ALEATOIRE ? PAUSE : LECTURE;
        mode(wagon, nouveau);
        return nomMode(nouveau);
    }

    Component nomMode(byte mode) {
        return mode == LECTURE ? plugin.t("jukebox.lecture", "<green>Lecture (dans l'ordre du coffre)")
                : mode == ALEATOIRE ? plugin.t("jukebox.aleatoire", "<green>Lecture aléatoire")
                : plugin.t("jukebox.pause", "<yellow>Pause");
    }

    /** Disque tenu par le wagon (pris dans le coffre), ou null. */
    ItemStack disque(Entity wagon) {
        byte[] donnees = wagon.getPersistentDataContainer().get(cleDisque, PersistentDataType.BYTE_ARRAY);
        if (donnees == null) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(donnees);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void disque(Entity wagon, ItemStack disque) {
        if (disque == null) {
            wagon.getPersistentDataContainer().remove(cleDisque);
        } else {
            wagon.getPersistentDataContainer().set(cleDisque, PersistentDataType.BYTE_ARRAY, disque.serializeAsBytes());
        }
    }

    private float volume() {
        return (float) plugin.getConfig().getDouble("jukebox.volume", 4.0);
    }

    private boolean ecoute(Entity wagon) {
        double portee = 16 * Math.max(1f, volume());
        for (Player joueur : wagon.getWorld().getPlayers()) {
            if (joueur.getLocation().distanceSquared(wagon.getLocation()) <= portee * portee) {
                return true;
            }
        }
        return false;
    }

    /** Arrête la musique du wagon (le disque reste tenu). */
    void couper(Entity wagon) {
        fins.remove(wagon.getUniqueId());
        JukeboxSong chanson = chanson(disque(wagon));
        if (chanson == null) {
            return;
        }
        double portee = 16 * Math.max(1f, volume()) + 32;
        for (Player joueur : wagon.getWorld().getPlayers()) {
            if (joueur.getLocation().distanceSquared(wagon.getLocation()) <= portee * portee) {
                joueur.stopSound(chanson.getSound(), SoundCategory.RECORDS);
            }
        }
    }

    /** Wagon cassé : la musique s'arrête et le disque tenu tombe. */
    void lacher(Entity wagon) {
        couper(wagon);
        ItemStack disque = disque(wagon);
        if (disque != null) {
            wagon.getWorld().dropItemNaturally(wagon.getLocation(), disque);
            disque(wagon, null);
        }
    }

    /** Toutes les 10 ticks, pour un wagon jukebox d'un train (rang dans le train). */
    void avancer(Train train, int rang) {
        Minecart wagon = train.wagons.get(rang);
        StorageMinecart coffre = rang > 0 && train.wagons.get(rang - 1) instanceof StorageMinecart c ? c : null;
        byte mode = mode(wagon);
        long tick = Bukkit.getCurrentTick();
        boolean ecoute = ecoute(wagon);
        Long fin = fins.get(wagon.getUniqueId());
        if (fin != null) {
            if (mode != PAUSE && ecoute && coffre != null && tick < fin) {
                return;
            }
            couper(wagon);
        }
        ItemStack tenu = disque(wagon);
        if (tenu != null && coffre != null && entonnoirs.entree(coffre) && rendre(coffre.getInventory(), wagon, tenu)) {
            disque(wagon, null);
            tenu = null;
        }
        if (mode == PAUSE || !ecoute || coffre == null || tenu != null || !entonnoirs.sortie(coffre)) {
            return;
        }
        Inventory inventaire = coffre.getInventory();
        List<Integer> places = new ArrayList<>();
        for (int i = 0; i < inventaire.getSize(); i++) {
            if (chanson(inventaire.getItem(i)) != null) {
                places.add(i);
            }
        }
        if (places.isEmpty()) {
            return;
        }
        int place;
        if (mode == ALEATOIRE) {
            place = places.get(ThreadLocalRandom.current().nextInt(places.size()));
        } else {
            Integer avant = wagon.getPersistentDataContainer().get(clePlace, PersistentDataType.INTEGER);
            int depart = avant == null ? 0 : avant + 1;
            place = places.get(0);
            for (int p : places) {
                if (p >= depart) {
                    place = p;
                    break;
                }
            }
        }
        ItemStack pile = inventaire.getItem(place);
        ItemStack un = pile.asOne();
        pile.setAmount(pile.getAmount() - 1);
        inventaire.setItem(place, pile.isEmpty() ? null : pile);
        disque(wagon, un);
        wagon.getPersistentDataContainer().set(clePlace, PersistentDataType.INTEGER, place);
        JukeboxSong chanson = chanson(un);
        wagon.getWorld().playSound(wagon, chanson.getSound(), SoundCategory.RECORDS, volume(), 1f);
        fins.put(wagon.getUniqueId(), tick + (long) Math.ceil(chanson.getLengthInSeconds() * 20) + SILENCE);
    }

    /** Remet le disque dans le coffre : à sa case si elle est libre, sinon où il y a de la place. */
    private boolean rendre(Inventory coffre, Entity wagon, ItemStack disque) {
        Integer place = wagon.getPersistentDataContainer().get(clePlace, PersistentDataType.INTEGER);
        if (place != null && place >= 0 && place < coffre.getSize()
                && (coffre.getItem(place) == null || coffre.getItem(place).isEmpty())) {
            coffre.setItem(place, disque);
            return true;
        }
        return coffre.addItem(disque).isEmpty();
    }

    /** Pour le menu : le disque en cours (tenu), ou null. */
    ItemStack enCours(Entity wagon) {
        return fins.containsKey(wagon.getUniqueId()) ? disque(wagon) : null;
    }
}
