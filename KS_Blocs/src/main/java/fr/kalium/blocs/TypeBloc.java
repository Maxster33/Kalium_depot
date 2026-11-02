package fr.kalium.blocs;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Un type de bloc custom, déclaré par {@link KSBlocs#declarer}.
 *
 * <ul>
 *   <li>id : unique, de la forme « plugin:nom » (ex. ks_blocs:jack_o_lantern_bleue), écrit dans les chunks ;</li>
 *   <li>apparence : entrée de apparences.json (états montrés aux joueurs, un par variante) ;</li>
 *   <li>base : le vrai bloc du serveur (collision, solidité, outil, redstone...). Pour un bloc plein, le bloc musical
 *       (le client prédit alors la bonne durée de casse) ; le bloc musical d'un bloc custom ne joue jamais de note et
 *       ne se règle pas ;</li>
 *   <li>objet : l'objet qui pose le bloc (sa base doit poser le bloc de base : un bloc musical pour un bloc plein) ;</li>
 *   <li>butin : ce que donne le bloc cassé (par défaut, son objet) ; reçoit le joueur et l'outil (toucher de soie...),
 *       null pour une explosion.</li>
 * </ul>
 */
public final class TypeBloc {

    /** Joueur (null pour une explosion), outil (vide si aucun), bloc encore en place. */
    public record Casse(Player joueur, ItemStack outil, Block bloc, String variante) {
    }

    private final String id;
    private final String apparence;
    private final Material base;
    private ItemStack objet;
    private Function<Casse, List<ItemStack>> butin;
    private java.util.function.BiFunction<Player, Block, String> variantePosee;
    private EcouteurBloc ecouteur = new EcouteurBloc() {
    };

    public TypeBloc(String id, String apparence, Material base) {
        this.id = Objects.requireNonNull(id);
        this.apparence = Objects.requireNonNull(apparence);
        this.base = Objects.requireNonNull(base);
    }

    /** L'objet qui pose ce bloc (KS_Blocs y ajoute sa marque). */
    public TypeBloc objet(ItemStack objet) {
        this.objet = objet.clone();
        return this;
    }

    public TypeBloc butin(Function<Casse, List<ItemStack>> butin) {
        this.butin = butin;
        return this;
    }

    /**
     * Variante d'un bloc posé par un joueur (par défaut : face vers le joueur si les variantes sont north, east, south
     * et west, comme une citrouille ; sinon la première variante de l'apparence).
     */
    public TypeBloc variantePosee(java.util.function.BiFunction<Player, Block, String> choix) {
        this.variantePosee = choix;
        return this;
    }

    java.util.function.BiFunction<Player, Block, String> variantePosee() {
        return variantePosee;
    }

    public TypeBloc ecouteur(EcouteurBloc ecouteur) {
        this.ecouteur = Objects.requireNonNull(ecouteur);
        return this;
    }

    public String id() {
        return id;
    }

    public String apparence() {
        return apparence;
    }

    public Material base() {
        return base;
    }

    ItemStack modeleObjet() {
        return objet;
    }

    EcouteurBloc ecouteur() {
        return ecouteur;
    }

    List<ItemStack> butin(Casse casse) {
        if (butin != null) {
            return butin.apply(casse);
        }
        ItemStack o = KSBlocs.objet(id, 1);
        return o == null ? List.of() : List.of(o);
    }
}
