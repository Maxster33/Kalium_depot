package fr.kalium.contrats;

import fr.kalium.contrats.Contrat.Ligne;
import fr.kalium.contrats.api.Livraison;
import fr.kalium.economy.KSEconomy;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.1.0 : demandes publiques (cahier local Blocs_Cultures_Objets.md, partie 9, choix de LeKiwi06 du 02/11/2026) :
 * - un contrat public que n'importe qui peut remplir, sans réservation ; le premier qui livre est payé ;
 * - récompense en points et / ou en objets, retenue dès la création, rendue si le demandeur annule (ou à
 *   l'expiration, facultative) ;
 * - livrée en une seule fois, depuis une boîte aux lettres (KS_Courrier) : les objets demandés partent dans le
 *   courrier du demandeur, la récompense en objets dans celui du livreur ;
 * - 3 demandes ouvertes au plus par joueur.
 * Enregistré dans plugins/KS_Contrats/demandes.yml, à chaque changement. Rien n'est effacé.
 */
final class Demandes {

    static final int OUVERTES_MAX = 3, LIGNES_MAX = 5, OBJETS_MAX = 5;

    enum Etat { OUVERTE, LIVREE, ANNULEE, EXPIREE }

    static final class Demande {
        String id;
        UUID auteur;
        String nomAuteur;
        String texte = "";
        List<Ligne> lignes = new ArrayList<>();
        long points;
        /** Récompense en objets (piles complètes), gardée par le serveur. */
        List<ItemStack> objets = new ArrayList<>();
        long creation;
        /** 0 : pas d'expiration. */
        long expiration;
        Etat etat = Etat.OUVERTE;
        UUID livreur;
        String nomLivreur;
        long fin;

        boolean test() {
            return CompteTest.estTest(auteur);
        }
    }

    /** Demande en cours d'écriture (gardée en mémoire jusqu'à la publication ou au redémarrage). */
    static final class Brouillon {
        String texte = "";
        List<Ligne> lignes = new ArrayList<>();
        long points;
        /** Copies des piles choisies dans l'inventaire : retirées seulement à la publication. */
        List<ItemStack> objets = new ArrayList<>();
        /** Jours avant l'expiration (0 : aucune). */
        int jours;
    }

    private final KSContrats plugin;
    private final Lang lang;
    private final Registre registre;
    private final File fichier;
    final Map<String, Demande> demandes = new LinkedHashMap<>();
    private int prochain;

    Demandes(KSContrats plugin, Registre registre) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.registre = registre;
        this.fichier = new File(plugin.getDataFolder(), "demandes.yml");
        charger();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    // ------------------------------------------------------------------ fichier

    private void charger() {
        if (!fichier.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(fichier);
        prochain = y.getInt("prochain");
        ConfigurationSection cs = y.getConfigurationSection("demandes");
        if (cs == null) {
            return;
        }
        for (String id : cs.getKeys(false)) {
            ConfigurationSection s = cs.getConfigurationSection(id);
            try {
                Demande d = new Demande();
                d.id = id;
                d.auteur = UUID.fromString(s.getString("auteur"));
                d.nomAuteur = s.getString("nom", "?");
                d.texte = s.getString("texte", "");
                d.points = s.getLong("points");
                d.creation = s.getLong("creation");
                d.expiration = s.getLong("expiration");
                d.etat = Etat.valueOf(s.getString("etat", "OUVERTE"));
                d.livreur = s.getString("livreur") == null ? null : UUID.fromString(s.getString("livreur"));
                d.nomLivreur = s.getString("nom-livreur");
                d.fin = s.getLong("fin");
                ConfigurationSection ls = s.getConfigurationSection("lignes");
                if (ls != null) {
                    for (String k : ls.getKeys(false)) {
                        ItemStack modele = Contrat.texteVersObjet(ls.getString(k + ".objet"));
                        if (modele != null) {
                            Ligne l = new Ligne();
                            l.modele = modele;
                            l.exact = ls.getBoolean(k + ".exact");
                            l.quantite = ls.getInt(k + ".quantite");
                            d.lignes.add(l);
                        }
                    }
                }
                for (String o : s.getStringList("objets")) {
                    ItemStack objet = pileDepuisTexte(o);
                    if (objet != null) {
                        d.objets.add(objet);
                    }
                }
                demandes.put(id, d);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Demande publique " + id + " illisible : " + e);
            }
        }
    }

    void sauver() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("prochain", prochain);
        ConfigurationSection cs = y.createSection("demandes");
        for (Demande d : demandes.values()) {
            ConfigurationSection s = cs.createSection(d.id);
            s.set("auteur", d.auteur.toString());
            s.set("nom", d.nomAuteur);
            s.set("texte", d.texte);
            s.set("points", d.points);
            s.set("creation", d.creation);
            s.set("expiration", d.expiration);
            s.set("etat", d.etat.name());
            s.set("livreur", d.livreur == null ? null : d.livreur.toString());
            s.set("nom-livreur", d.nomLivreur);
            s.set("fin", d.fin);
            ConfigurationSection ls = s.createSection("lignes");
            for (int i = 0; i < d.lignes.size(); i++) {
                Ligne l = d.lignes.get(i);
                ls.set(i + ".objet", Contrat.objetVersTexte(l.modele));
                ls.set(i + ".exact", l.exact);
                ls.set(i + ".quantite", l.quantite);
            }
            s.set("objets", d.objets.stream().map(Demandes::pileVersTexte).toList());
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(fichier);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer demandes.yml : " + e.getMessage());
        }
    }

    /** Pile complète (quantité gardée), contrairement à Contrat.objetVersTexte qui garde un modèle. */
    static String pileVersTexte(ItemStack objet) {
        return Base64.getEncoder().encodeToString(objet.serializeAsBytes());
    }

    static ItemStack pileDepuisTexte(String texte) {
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(texte));
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ listes

    /** Demandes ouvertes visibles par ce compte (survie et monde de test séparés), les plus récentes d'abord. */
    List<Demande> ouvertes(UUID compte) {
        boolean test = CompteTest.estTest(compte);
        long maintenant = System.currentTimeMillis();
        return demandes.values().stream()
                .filter(d -> d.etat == Etat.OUVERTE && d.test() == test
                        && (d.expiration == 0 || d.expiration > maintenant))
                .sorted(Comparator.comparingLong((Demande d) -> d.creation).reversed()).toList();
    }

    /** Toutes les demandes de ce compte, les ouvertes puis les plus récentes. */
    List<Demande> de(UUID compte) {
        return demandes.values().stream().filter(d -> d.auteur.equals(compte))
                .sorted(Comparator.comparing((Demande d) -> d.etat != Etat.OUVERTE)
                        .thenComparing(Comparator.comparingLong((Demande d) -> d.creation).reversed())).toList();
    }

    int ouvertesDe(UUID compte) {
        return (int) demandes.values().stream().filter(d -> d.etat == Etat.OUVERTE && d.auteur.equals(compte)).count();
    }

    static Livraison livraison() {
        return Bukkit.getServicesManager().load(Livraison.class);
    }

    // ------------------------------------------------------------------ création, annulation

    /** Publie le brouillon : retient la récompense (points, puis objets retirés de l'inventaire). Refus, ou null. */
    Component publier(Player p, Brouillon b) {
        UUID compte = Registre.compte(p);
        if (b.lignes.isEmpty()) {
            return t("publique.refus-vide", "<red>Ajoute au moins un objet demandé.");
        }
        if (b.points <= 0 && b.objets.isEmpty()) {
            return t("publique.refus-recompense", "<red>Ajoute une récompense (des points ou des objets).");
        }
        if (ouvertesDe(compte) >= OUVERTES_MAX) {
            return t("publique.refus-max", "<red>Tu as déjà <n> demandes ouvertes : annule-en une ou attends qu'elle "
                    + "soit livrée.", "n", OUVERTES_MAX);
        }
        if (!b.objets.isEmpty() && livraison() == null) {
            return t("publique.refus-courrier", "<red>Les boîtes aux lettres sont indisponibles : récompense en objets "
                    + "impossible pour l'instant.");
        }
        for (ItemStack o : b.objets) {
            if (!p.getInventory().containsAtLeast(o, o.getAmount())) {
                return t("publique.refus-objet-absent", "<red>Tu n'as plus <n> <objet> dans ton inventaire.", "n",
                        o.getAmount(), "objet", Textes.nomTexte(o));
            }
        }
        if (b.points > 0 && !KSEconomy.debiterCompte(compte, b.points)) {
            return t("publique.refus-solde", "<red>Solde insuffisant pour la récompense (<points>).", "points",
                    Textes.points(b.points));
        }
        for (ItemStack o : b.objets) {
            p.getInventory().removeItem(o.clone());
        }
        Demande d = new Demande();
        d.id = "D" + (++prochain);
        d.auteur = compte;
        d.nomAuteur = p.getName();
        d.texte = b.texte;
        b.lignes.forEach(l -> d.lignes.add(l.copie()));
        d.points = b.points;
        b.objets.forEach(o -> d.objets.add(o.clone()));
        d.creation = System.currentTimeMillis();
        d.expiration = b.jours > 0 ? d.creation + b.jours * Contrat.JOUR : 0;
        demandes.put(d.id, d);
        sauver();
        plugin.getLogger().info("Demande publique " + d.id + " creee par " + p.getName());
        return null;
    }

    /** Le demandeur annule : la récompense lui est rendue. Refus, ou null. */
    Component annuler(Player p, Demande d) {
        if (d.etat != Etat.OUVERTE || !d.auteur.equals(Registre.compte(p))) {
            return t("publique.refus-plus-ouverte", "<red>Cette demande n'est plus ouverte.");
        }
        if (!d.objets.isEmpty() && livraison() == null) {
            return t("publique.refus-courrier-rendre", "<red>Les boîtes aux lettres sont indisponibles : la récompense "
                    + "en objets ne peut pas t'être rendue pour l'instant.");
        }
        fermer(d, Etat.ANNULEE);
        return null;
    }

    /** Annulée ou expirée : points rendus au compte, objets déposés dans son courrier. */
    private void fermer(Demande d, Etat etat) {
        d.etat = etat;
        d.fin = System.currentTimeMillis();
        if (d.points > 0) {
            KSEconomy.crediterCompte(d.auteur, d.points);
        }
        if (!d.objets.isEmpty()) {
            livraison().deposer(d.auteur, d.nomAuteur, "Demandes publiques", "Récompense rendue (demande n°"
                    + d.id.substring(1) + ")", copies(d.objets));
        }
        sauver();
        registre.prevenir(d.auteur, etat == Etat.EXPIREE
                ? t("publique.expiree", "<gold>[Contrats] Ta demande publique n°<n> a expiré sans être livrée : "
                + "récompense rendue<objets>.", "n", d.id.substring(1), "objets", d.objets.isEmpty() ? ""
                : " (objets dans ta boîte aux lettres)")
                : t("publique.annulee", "<gold>[Contrats] Demande publique n°<n> annulée : récompense rendue<objets>.",
                "n", d.id.substring(1), "objets", d.objets.isEmpty() ? "" : " (objets dans ta boîte aux lettres)"));
        registre.sauverSiBesoin();
    }

    /** Expirations (appelé toutes les 30 s). */
    void tic() {
        long maintenant = System.currentTimeMillis();
        for (Demande d : new ArrayList<>(demandes.values())) {
            if (d.etat == Etat.OUVERTE && d.expiration > 0 && d.expiration <= maintenant
                    && (d.objets.isEmpty() || livraison() != null)) {
                fermer(d, Etat.EXPIREE);
            }
        }
    }

    // ------------------------------------------------------------------ livraison

    /**
     * Livraison en une fois depuis une boîte aux lettres : les objets demandés sont pris dans l'inventaire du livreur
     * et déposés dans le courrier du demandeur ; la récompense va au livreur. Refus, ou null.
     */
    Component livrer(Player p, Demande d) {
        UUID compte = Registre.compte(p);
        if (d.etat != Etat.OUVERTE || (d.expiration > 0 && d.expiration <= System.currentTimeMillis())) {
            return t("publique.refus-plus-ouverte", "<red>Cette demande n'est plus ouverte.");
        }
        if (d.auteur.equals(compte)) {
            return t("publique.refus-soi", "<red>Tu ne peux pas livrer ta propre demande.");
        }
        if (d.test() != CompteTest.estTest(compte)) {
            return t("publique.refus-plus-ouverte", "<red>Cette demande n'est plus ouverte.");
        }
        Livraison livraison = livraison();
        if (livraison == null) {
            return t("publique.refus-courrier-livrer", "<red>Les boîtes aux lettres sont indisponibles.");
        }
        ItemStack[] contenu = p.getInventory().getStorageContents();
        int[] pris = new int[contenu.length];
        // Lignes « objet précis » d'abord : une pile qui compte pour les deux types de ligne sert d'abord au plus exigeant.
        List<Ligne> ordre = new ArrayList<>(d.lignes);
        ordre.sort(Comparator.comparing((Ligne l) -> !l.exact));
        for (Ligne l : ordre) {
            int reste = l.quantite;
            for (int i = 0; i < contenu.length && reste > 0; i++) {
                ItemStack o = contenu[i];
                if (o != null && l.correspond(o)) {
                    int n = Math.min(reste, o.getAmount() - pris[i]);
                    pris[i] += n;
                    reste -= n;
                }
            }
            if (reste > 0) {
                return t("publique.refus-manque", "<red>Il te manque <n> <objet> (tout se livre en une fois).", "n",
                        Textes.nombre(reste), "objet", Textes.nomTexte(l.modele));
            }
        }
        List<ItemStack> livres = new ArrayList<>();
        for (int i = 0; i < contenu.length; i++) {
            if (pris[i] > 0) {
                ItemStack part = contenu[i].clone();
                part.setAmount(pris[i]);
                livres.add(part);
                int reste = contenu[i].getAmount() - pris[i];
                if (reste > 0) {
                    contenu[i].setAmount(reste);
                } else {
                    contenu[i] = null;
                }
            }
        }
        p.getInventory().setStorageContents(contenu);
        d.etat = Etat.LIVREE;
        d.livreur = compte;
        d.nomLivreur = p.getName();
        d.fin = System.currentTimeMillis();
        String n = d.id.substring(1);
        livraison.deposer(d.auteur, d.nomAuteur, p.getName(), "Demande n°" + n + " livrée par " + p.getName(), livres);
        if (d.points > 0) {
            KSEconomy.crediterCompte(compte, d.points);
        }
        if (!d.objets.isEmpty()) {
            livraison.deposer(compte, p.getName(), d.nomAuteur, "Récompense de la demande n°" + n, copies(d.objets));
        }
        sauver();
        plugin.getLogger().info("Demande publique " + d.id + " livree par " + p.getName());
        registre.prevenir(d.auteur, t("publique.livree", "<green>[Contrats] <joueur> a livré ta demande publique n°<n> : "
                + "les objets sont dans ta boîte aux lettres.", "joueur", p.getName(), "n", n));
        registre.sauverSiBesoin();
        return null;
    }

    private static List<ItemStack> copies(List<ItemStack> objets) {
        return objets.stream().map(ItemStack::clone).toList();
    }
}
