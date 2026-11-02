package fr.kalium.courrier;

import fr.kalium.courrier.Donnees.Envoi;
import fr.kalium.courrier.Donnees.Type;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Objets du courrier et leurs recettes (choix de LeKiwi06 du 02/11/2026, proposition A) :
 * - boîte aux lettres : 1 coffre + 5 lingots de fer en U (bloc custom de KS_Blocs, base : bloc musical) ;
 * - enveloppe : 2 papiers ; colis : 1 coffre + 1 ficelle + 2 papiers (27 cases) ;
 * - lettre et colis scellés : ne portent que le numéro de l'envoi (contenu gardé par le serveur).
 * Marque : ks_courrier:objet (enveloppe, colis, lettre, colis_ferme) et ks_courrier:envoi (numéro), lues aussi par
 * KS_Textures et geyser-bedrock pour les textures. Images provisoires : celles du jeu (item_model).
 */
final class Objets {

    static final String ID_BOITE = "ks_courrier:boite_aux_lettres";
    static final String APPARENCE_BOITE = "boite_aux_lettres";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    final NamespacedKey cleObjet;
    final NamespacedKey cleEnvoi;
    private final NamespacedKey recetteBoite;
    private final NamespacedKey recetteEnveloppe;
    private final NamespacedKey recetteColis;

    Objets(JavaPlugin plugin) {
        cleObjet = new NamespacedKey(plugin, "objet");
        cleEnvoi = new NamespacedKey(plugin, "envoi");
        recetteBoite = new NamespacedKey(plugin, "boite_aux_lettres");
        recetteEnveloppe = new NamespacedKey(plugin, "enveloppe");
        recetteColis = new NamespacedKey(plugin, "colis");
    }

    static String date(long ms) {
        return DATE.format(Instant.ofEpochMilli(ms).atZone(PARIS));
    }

    private static Component texte(String s, NamedTextColor couleur) {
        return Component.text(s, couleur).decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack objet(Material base, String modele, String marque, Component nom, List<Component> lignes) {
        ItemStack o = new ItemStack(base);
        ItemMeta meta = o.getItemMeta();
        meta.displayName(nom.decoration(TextDecoration.ITALIC, false));
        meta.lore(lignes);
        if (modele != null) {
            meta.setItemModel(NamespacedKey.minecraft(modele));
        }
        if (marque != null) {
            meta.getPersistentDataContainer().set(cleObjet, PersistentDataType.STRING, marque);
        }
        o.setItemMeta(meta);
        return o;
    }

    /** Modèle de l'objet de la boîte aux lettres (KS_Blocs y ajoute sa marque). */
    ItemStack modeleBoite() {
        return objet(Material.NOTE_BLOCK, "barrel", null, texte("Boîte aux lettres", NamedTextColor.GOLD), List.of(
                texte("Une seule par joueur, dans un de tes claims.", NamedTextColor.GRAY),
                texte("Clic droit : courrier, envois, demandes publiques.", NamedTextColor.GRAY)));
    }

    ItemStack enveloppe() {
        return objet(Material.PAPER, null, "enveloppe", texte("Enveloppe", NamedTextColor.WHITE), List.of(
                texte("Avec un livre écrit, s'envoie depuis", NamedTextColor.GRAY),
                texte("ta boîte aux lettres.", NamedTextColor.GRAY)));
    }

    ItemStack colis() {
        ItemStack o = objet(Material.PAPER, "bundle", "colis", texte("Colis", NamedTextColor.WHITE), List.of(
                texte("27 cases. S'envoie depuis ta boîte aux lettres.", NamedTextColor.GRAY)));
        ItemMeta meta = o.getItemMeta();
        meta.setMaxStackSize(16);
        o.setItemMeta(meta);
        return o;
    }

    /** Lettre ou colis scellé : son numéro seulement, l'expéditeur et le destinataire écrits dessus (modération). */
    ItemStack scelle(Envoi e) {
        boolean lettre = e.type == Type.LETTRE;
        List<Component> lignes = new ArrayList<>();
        lignes.add(texte("De : " + e.nomExpediteur, NamedTextColor.GRAY));
        lignes.add(texte("Pour : " + e.nomDestinataire, NamedTextColor.GRAY));
        if (!e.titre.isEmpty()) {
            lignes.add(texte(e.titre, NamedTextColor.GRAY));
        }
        lignes.add(texte("Envoyé le " + date(e.date), NamedTextColor.GRAY));
        lignes.add(texte("Clic droit : ouvrir", NamedTextColor.YELLOW));
        ItemStack o = objet(Material.PAPER, lettre ? null : "brown_bundle", lettre ? "lettre" : "colis_ferme",
                texte((lettre ? "Lettre de " : "Colis de ") + e.nomExpediteur, NamedTextColor.GOLD), lignes);
        ItemMeta meta = o.getItemMeta();
        meta.getPersistentDataContainer().set(cleEnvoi, PersistentDataType.STRING, e.id);
        meta.setMaxStackSize(1);
        if (lettre) {
            meta.setEnchantmentGlintOverride(true);
        }
        o.setItemMeta(meta);
        return o;
    }

    /** Marque de l'objet (enveloppe, colis, lettre, colis_ferme), ou null. */
    String marque(ItemStack o) {
        if (o == null || !o.hasItemMeta()) {
            return null;
        }
        return o.getItemMeta().getPersistentDataContainer().get(cleObjet, PersistentDataType.STRING);
    }

    String envoi(ItemStack o) {
        if (o == null || !o.hasItemMeta()) {
            return null;
        }
        return o.getItemMeta().getPersistentDataContainer().get(cleEnvoi, PersistentDataType.STRING);
    }

    // ------------------------------------------------------------------ recettes

    void ajouterRecettes(ItemStack boite) {
        if (boite != null) {
            ShapedRecipe r = new ShapedRecipe(recetteBoite, boite);
            r.shape("ICI", "III");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('C', Material.CHEST);
            Bukkit.addRecipe(r);
        }
        ShapelessRecipe e = new ShapelessRecipe(recetteEnveloppe, enveloppe());
        e.addIngredient(2, Material.PAPER);
        Bukkit.addRecipe(e);
        ShapelessRecipe c = new ShapelessRecipe(recetteColis, colis());
        c.addIngredient(Material.CHEST);
        c.addIngredient(Material.STRING);
        c.addIngredient(2, Material.PAPER);
        Bukkit.addRecipe(c);
    }

    void retirerRecettes() {
        Bukkit.removeRecipe(recetteBoite);
        Bukkit.removeRecipe(recetteEnveloppe);
        Bukkit.removeRecipe(recetteColis);
    }
}
