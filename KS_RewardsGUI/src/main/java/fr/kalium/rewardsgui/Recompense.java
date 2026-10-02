package fr.kalium.rewardsgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Une récompense reçue. Format du message déposé dans la boîte « event » de KaliumRelay (YAML, texte) :
 *
 * <pre>
 * joueur: 8667ba71-b85a-4004-af54-457a9734eed7   # UUID du joueur
 * nom: LeKiwi06                                  # pseudo (affichage, journal)
 * origine: Kal-Games                             # serveur ou plugin d'origine
 * raison: "Palier 500 points - Course de bateau"
 * date: 1790000000000                            # millisecondes (facultatif)
 * contenu:
 *   - type: objet          # objet vanilla ou quelconque : ItemStack.serializeAsBytes() en base64
 *     donnees: "..."
 *     nombre: 3            # quantité totale (facultatif : celle de l'objet)
 *   - type: custom         # objet custom d'Event : id_custom de /kaliumgive (KS_KaliumGive)
 *     id: tete_mouton_rouge
 *     nombre: 2
 *   - type: argent         # points du score (KS_Economy)
 *     montant: 500
 * </pre>
 */
final class Recompense {

    /** Un élément du contenu. */
    record Element(String type, String donnees, String id, long nombre) {
    }

    UUID joueur;
    String nom;
    String origine;
    String raison;
    long date;
    final List<Element> contenu = new ArrayList<>();
    /** Texte d'origine (enregistré tel quel). */
    String texte;

    /** Lit un message, ou null s'il est illisible (joueur ou contenu manquant). */
    static Recompense lire(String texte) {
        YamlConfiguration yaml = KSRewardsGUI.yaml(texte);
        if (yaml == null) {
            return null;
        }
        Recompense r = new Recompense();
        try {
            r.joueur = UUID.fromString(yaml.getString("joueur", ""));
        } catch (IllegalArgumentException e) {
            return null;
        }
        r.nom = yaml.getString("nom", "?");
        r.origine = yaml.getString("origine", "?");
        r.raison = yaml.getString("raison", "Récompense");
        r.date = yaml.getLong("date", System.currentTimeMillis());
        for (Map<?, ?> element : yaml.getMapList("contenu")) {
            String type = String.valueOf(element.get("type"));
            Object nombre = element.containsKey("nombre") ? element.get("nombre") : element.get("montant");
            long n = nombre instanceof Number num ? num.longValue() : 0;
            r.contenu.add(new Element(type, element.containsKey("donnees") ? String.valueOf(element.get("donnees")) : null,
                    element.containsKey("id") ? String.valueOf(element.get("id")) : null, n));
        }
        if (r.contenu.isEmpty()) {
            return null;
        }
        r.texte = texte;
        return r;
    }

    /** Points du score contenus. */
    long argent() {
        long total = 0;
        for (Element e : contenu) {
            if ("argent".equals(e.type())) {
                total += Math.max(0, e.nombre());
            }
        }
        return total;
    }

    /**
     * Objets à donner (piles de taille normale), ou null si l'un d'eux ne peut pas être créé (objet custom inconnu sur
     * Event, KS_KaliumGive absent, données illisibles).
     */
    List<ItemStack> objets() {
        List<ItemStack> objets = new ArrayList<>();
        for (Element e : contenu) {
            ItemStack modele;
            long nombre;
            if ("objet".equals(e.type())) {
                try {
                    modele = ItemStack.deserializeBytes(Base64.getDecoder().decode(e.donnees()));
                } catch (RuntimeException ex) {
                    return null;
                }
                nombre = e.nombre() > 0 ? e.nombre() : modele.getAmount();
            } else if ("custom".equals(e.type())) {
                if (!Bukkit.getPluginManager().isPluginEnabled("KS_KaliumGive")) {
                    return null;
                }
                modele = fr.kalium.kaliumgive.KSKaliumGive.creer(e.id());
                if (modele == null) {
                    return null;
                }
                nombre = Math.max(1, e.nombre());
            } else {
                continue;
            }
            // Un objet custom est recréé à chaque unité (certains ne s'empilent pas).
            while (nombre > 0) {
                ItemStack pile = "custom".equals(e.type()) ? fr.kalium.kaliumgive.KSKaliumGive.creer(e.id()) : modele.clone();
                int taille = (int) Math.min(nombre, Math.max(1, pile.getMaxStackSize()));
                pile.setAmount(taille);
                objets.add(pile);
                nombre -= taille;
            }
        }
        return objets;
    }

    /** Résumé lisible du contenu (journal, menu) : « 3 x Diamant, 2 x tete_mouton_rouge, 500 points ». */
    String resume() {
        List<String> parties = new ArrayList<>();
        for (Element e : contenu) {
            switch (e.type()) {
                case "objet" -> {
                    String nom;
                    long nombre = e.nombre();
                    try {
                        ItemStack modele = ItemStack.deserializeBytes(Base64.getDecoder().decode(e.donnees()));
                        Component affiche = modele.hasItemMeta() && modele.getItemMeta().hasDisplayName()
                                ? modele.getItemMeta().displayName() : Component.translatable(modele.translationKey());
                        nom = PlainTextComponentSerializer.plainText().serialize(affiche);
                        if (nombre <= 0) {
                            nombre = modele.getAmount();
                        }
                    } catch (RuntimeException ex) {
                        nom = "objet illisible";
                    }
                    parties.add(nombre + " x " + nom);
                }
                case "custom" -> parties.add(Math.max(1, e.nombre()) + " x " + e.id());
                case "argent" -> parties.add(e.nombre() + " points");
                default -> parties.add(e.type());
            }
        }
        return String.join(", ", parties);
    }

    /** Contenu affichable (noms traduits par le jeu du joueur). */
    Component description() {
        Component sortie = Component.empty();
        boolean premier = true;
        for (Element e : contenu) {
            Component partie;
            switch (e.type()) {
                case "objet" -> {
                    try {
                        ItemStack modele = ItemStack.deserializeBytes(Base64.getDecoder().decode(e.donnees()));
                        Component nom = modele.hasItemMeta() && modele.getItemMeta().hasDisplayName()
                                ? modele.getItemMeta().displayName() : Component.translatable(modele.translationKey());
                        partie = Component.text((e.nombre() > 0 ? e.nombre() : modele.getAmount()) + " x ").append(nom);
                    } catch (RuntimeException ex) {
                        partie = Component.text("objet illisible");
                    }
                }
                case "custom" -> {
                    ItemStack objet = Bukkit.getPluginManager().isPluginEnabled("KS_KaliumGive")
                            ? fr.kalium.kaliumgive.KSKaliumGive.creer(e.id()) : null;
                    Component nom = objet != null && objet.hasItemMeta() && objet.getItemMeta().hasDisplayName()
                            ? objet.getItemMeta().displayName() : Component.text(e.id());
                    partie = Component.text(Math.max(1, e.nombre()) + " x ").append(nom);
                }
                case "argent" -> partie = Component.text(e.nombre() + " points");
                default -> partie = Component.text(e.type());
            }
            sortie = sortie.append(premier ? Component.empty() : Component.text(", ")).append(partie);
            premier = false;
        }
        return sortie;
    }
}
