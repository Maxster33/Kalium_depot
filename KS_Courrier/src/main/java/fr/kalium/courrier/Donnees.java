package fr.kalium.courrier;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Boîtes aux lettres et envois, dans plugins/KS_Courrier/courrier.yml, enregistré à chaque changement (un envoi ne doit
 * jamais exister deux fois : contenu gardé par le serveur, voir {@link Envoi}). Rien n'est effacé : l'historique des
 * envois et des réceptions reste (choix de LeKiwi06 du 02/11/2026 : le courrier non relevé ne revient pas à
 * l'expéditeur, chacun a son historique).
 */
final class Donnees {

    /** Boîte aux lettres d'un compte (une seule par joueur, LeKiwi06 du 02/11/2026). */
    record Boite(UUID monde, int x, int y, int z, String nom) {

        boolean a(Block b) {
            return b.getWorld().getUID().equals(monde) && b.getX() == x && b.getY() == y && b.getZ() == z;
        }

        Block bloc() {
            World w = Bukkit.getWorld(monde);
            return w == null ? null : w.getBlockAt(x, y, z);
        }
    }

    enum Type { LETTRE, COLIS }

    /** Dans la boîte du destinataire, sorti de la boîte (objet scellé dans un inventaire), ouvert. */
    enum Etat { BOITE, SORTI, OUVERT }

    /**
     * Une lettre ou un colis. Le contenu est gardé ici, pas dans l'objet scellé (qui ne porte que l'identifiant) :
     * une copie de l'objet scellé s'ouvre vide (aucune duplication possible des objets d'un colis).
     */
    static final class Envoi {
        String id;
        Type type;
        /** null : envoi du serveur (demandes publiques). */
        UUID expediteur;
        String nomExpediteur;
        UUID destinataire;
        String nomDestinataire;
        /** Objet d'un envoi du serveur (ex. « Demande n°3 livrée par Kiwi »), sinon vide. */
        String titre = "";
        long date;
        long frais;
        long distance;
        /** Lettre : le livre écrit ; colis : les objets (vidé à l'ouverture d'un colis, le résumé reste). */
        List<ItemStack> contenu = new ArrayList<>();
        /** Résumé du contenu d'un colis, en MiniMessage (« 64 × <lang:...> »). */
        List<String> resume = new ArrayList<>();
        Etat etat = Etat.BOITE;
        long retrait;
        long ouverture;
        String ouvreur;
    }

    private final JavaPlugin plugin;
    private final File fichier;
    final Map<UUID, Boite> boites = new HashMap<>();
    final Map<String, Envoi> envois = new LinkedHashMap<>();
    private int prochain;

    Donnees(JavaPlugin plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "courrier.yml");
        charger();
    }

    String nouvelId() {
        return "E" + (++prochain);
    }

    // ------------------------------------------------------------------ boîtes

    Boite boite(UUID compte) {
        return boites.get(compte);
    }

    /** Le compte propriétaire de la boîte à cette place, ou null. */
    UUID proprio(Block b) {
        for (Map.Entry<UUID, Boite> e : boites.entrySet()) {
            if (e.getValue().a(b)) {
                return e.getKey();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ envois

    /** Courrier dans la boîte de ce compte (le plus ancien d'abord). */
    List<Envoi> recus(UUID compte) {
        return envois.values().stream().filter(e -> e.etat == Etat.BOITE && compte.equals(e.destinataire)).toList();
    }

    /** Historique d'un compte : envois et réceptions, les plus récents d'abord. */
    List<Envoi> historique(UUID compte) {
        return envois.values().stream()
                .filter(e -> compte.equals(e.expediteur) || compte.equals(e.destinataire))
                .sorted(Comparator.comparingLong((Envoi e) -> e.date).reversed()).toList();
    }

    // ------------------------------------------------------------------ fichier

    private void charger() {
        if (!fichier.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(fichier);
        prochain = y.getInt("prochain");
        ConfigurationSection bs = y.getConfigurationSection("boites");
        if (bs != null) {
            for (String k : bs.getKeys(false)) {
                ConfigurationSection s = bs.getConfigurationSection(k);
                try {
                    boites.put(UUID.fromString(k), new Boite(UUID.fromString(s.getString("monde")), s.getInt("x"),
                            s.getInt("y"), s.getInt("z"), s.getString("nom", "?")));
                } catch (RuntimeException e) {
                    plugin.getLogger().warning("Boite " + k + " illisible : " + e);
                }
            }
        }
        ConfigurationSection es = y.getConfigurationSection("envois");
        if (es == null) {
            return;
        }
        for (String id : es.getKeys(false)) {
            ConfigurationSection s = es.getConfigurationSection(id);
            try {
                Envoi e = new Envoi();
                e.id = id;
                e.type = Type.valueOf(s.getString("type"));
                e.expediteur = uuid(s.getString("expediteur"));
                e.nomExpediteur = s.getString("nom-expediteur", "?");
                e.destinataire = uuid(s.getString("destinataire"));
                e.nomDestinataire = s.getString("nom-destinataire", "?");
                e.titre = s.getString("titre", "");
                e.date = s.getLong("date");
                e.frais = s.getLong("frais");
                e.distance = s.getLong("distance");
                for (String o : s.getStringList("contenu")) {
                    ItemStack objet = depuisTexte(o);
                    if (objet != null) {
                        e.contenu.add(objet);
                    } else {
                        plugin.getLogger().warning("Objet illisible dans l'envoi " + id);
                    }
                }
                e.resume = new ArrayList<>(s.getStringList("resume"));
                e.etat = Etat.valueOf(s.getString("etat", "BOITE"));
                e.retrait = s.getLong("retrait");
                e.ouverture = s.getLong("ouverture");
                e.ouvreur = s.getString("ouvreur");
                envois.put(id, e);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Envoi " + id + " illisible : " + ex);
            }
        }
    }

    void sauver() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("prochain", prochain);
        ConfigurationSection bs = y.createSection("boites");
        boites.forEach((compte, b) -> {
            ConfigurationSection s = bs.createSection(compte.toString());
            s.set("monde", b.monde().toString());
            s.set("x", b.x());
            s.set("y", b.y());
            s.set("z", b.z());
            s.set("nom", b.nom());
        });
        ConfigurationSection es = y.createSection("envois");
        for (Envoi e : envois.values()) {
            ConfigurationSection s = es.createSection(e.id);
            s.set("type", e.type.name());
            s.set("expediteur", e.expediteur == null ? null : e.expediteur.toString());
            s.set("nom-expediteur", e.nomExpediteur);
            s.set("destinataire", e.destinataire.toString());
            s.set("nom-destinataire", e.nomDestinataire);
            s.set("titre", e.titre);
            s.set("date", e.date);
            s.set("frais", e.frais);
            s.set("distance", e.distance);
            s.set("contenu", e.contenu.stream().map(Donnees::versTexte).toList());
            s.set("resume", e.resume);
            s.set("etat", e.etat.name());
            s.set("retrait", e.retrait);
            s.set("ouverture", e.ouverture);
            s.set("ouvreur", e.ouvreur);
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(fichier);
        } catch (IOException ex) {
            plugin.getLogger().severe("Impossible d'enregistrer courrier.yml : " + ex.getMessage());
        }
    }

    private static UUID uuid(String s) {
        return s == null ? null : UUID.fromString(s);
    }

    static String versTexte(ItemStack objet) {
        return Base64.getEncoder().encodeToString(objet.serializeAsBytes());
    }

    static ItemStack depuisTexte(String texte) {
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(texte));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
