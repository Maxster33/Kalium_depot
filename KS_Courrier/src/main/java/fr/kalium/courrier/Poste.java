package fr.kalium.courrier;

import fr.kalium.courrier.Donnees.Boite;
import fr.kalium.courrier.Donnees.Envoi;
import fr.kalium.courrier.Donnees.Etat;
import fr.kalium.courrier.Donnees.Type;
import fr.kalium.economy.KSEconomy;
import fr.kalium.menu.api.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Règles du courrier (choix de LeKiwi06 du 02/11/2026) :
 * - pas de boîte = pas d'envoi : l'expéditeur et le destinataire doivent avoir leur boîte aux lettres ;
 * - frais selon la distance entre les deux boîtes : 1 point par tranche de 1 000 blocs, gratuit sous 1 000 blocs
 *   (Nether : coordonnées x 8, comme un portail) ;
 * - livré même hors ligne, dans la boîte du destinataire ; rien ne revient à l'expéditeur ;
 * - l'expéditeur est écrit sur l'objet scellé et dans l'historique (modération).
 */
final class Poste {

    static final int CASES_COLIS = 27;

    private final KSCourrier plugin;
    private final Lang lang;
    private final Donnees donnees;
    private final Objets objets;

    Poste(KSCourrier plugin, Donnees donnees, Objets objets) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        this.donnees = donnees;
        this.objets = objets;
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    static UUID compte(Player p) {
        return CompteTest.de(p.getUniqueId());
    }

    /** Le joueur en ligne qui joue ce compte (null : hors ligne, ou dans l'autre monde). */
    static Player enLigne(UUID compte) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (compte(p).equals(compte)) {
                return p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ frais

    /** Distance horizontale entre deux boîtes, en blocs de la surface (Nether x 8). */
    static long distance(Boite a, Boite b) {
        double ax = a.x() * echelle(a), az = a.z() * echelle(a);
        double bx = b.x() * echelle(b), bz = b.z() * echelle(b);
        return Math.round(Math.hypot(ax - bx, az - bz));
    }

    private static int echelle(Boite b) {
        World w = Bukkit.getWorld(b.monde());
        return w != null && w.getEnvironment() == World.Environment.NETHER ? 8 : 1;
    }

    /** 1 point par tranche de 1 000 blocs entamée, gratuit sous 1 000 blocs. */
    static long frais(long distance) {
        return distance < 1000 ? 0 : (distance + 999) / 1000;
    }

    static String points(long n) {
        return n == 0 ? "gratuit" : n + (n > 1 ? " points" : " point");
    }

    // ------------------------------------------------------------------ envoi

    /** Refus commun aux lettres et aux colis (boîtes, destinataire), ou null. */
    Component refusEnvoi(Player p, UUID destinataire) {
        UUID compte = compte(p);
        if (donnees.boite(compte) == null) {
            return t("refus.sans-boite", "<red>Pose d'abord ta boîte aux lettres dans un de tes claims.");
        }
        if (destinataire.equals(compte)) {
            return t("refus.soi", "<red>Tu ne peux pas t'envoyer du courrier.");
        }
        if (donnees.boite(destinataire) == null) {
            return t("refus.destinataire-sans-boite", "<red>Ce joueur n'a pas de boîte aux lettres : envoi impossible.");
        }
        return null;
    }

    long fraisVers(Player p, UUID destinataire) {
        Boite a = donnees.boite(compte(p));
        Boite b = donnees.boite(destinataire);
        return a == null || b == null ? 0 : frais(distance(a, b));
    }

    /** Une lettre : le livre écrit (cette pile précise de l'inventaire) et une enveloppe. Refus, ou null. */
    Component envoyerLettre(Player p, UUID destinataire, String nomDestinataire, ItemStack livre) {
        Component refus = refusEnvoi(p, destinataire);
        if (refus != null) {
            return refus;
        }
        PlayerInventory inv = p.getInventory();
        int placeLivre = -1;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack o = inv.getItem(i);
            if (o != null && o.getType() == Material.WRITTEN_BOOK && o.isSimilar(livre)) {
                placeLivre = i;
                break;
            }
        }
        if (placeLivre < 0) {
            return t("refus.livre-absent", "<red>Ce livre n'est plus dans ton inventaire.");
        }
        int placeEnveloppe = chercher(inv, "enveloppe");
        if (placeEnveloppe < 0) {
            return t("refus.sans-enveloppe", "<red>Il te faut une enveloppe (2 papiers à l'établi).");
        }
        Boite a = donnees.boite(compte(p)), b = donnees.boite(destinataire);
        long distance = distance(a, b);
        long frais = frais(distance);
        if (frais > 0 && !KSEconomy.debiterCompte(compte(p), frais)) {
            return t("refus.solde", "<red>Solde insuffisant : l'envoi coûte <frais>.", "frais", points(frais));
        }
        ItemStack pile = inv.getItem(placeLivre);
        ItemStack un = pile.clone();
        un.setAmount(1);
        retirerUn(inv, placeLivre);
        retirerUn(inv, placeEnveloppe);
        Envoi e = nouveau(Type.LETTRE, p, destinataire, nomDestinataire, distance, frais);
        e.contenu.add(un);
        enregistrer(e);
        return null;
    }

    /**
     * Un colis : les objets posés dans l'emballage (déjà sortis de l'inventaire) et un colis vide pris dans
     * l'inventaire. Refus (les objets restent dans l'emballage), ou null.
     */
    Component envoyerColis(Player p, UUID destinataire, String nomDestinataire, List<ItemStack> contenu) {
        Component refus = refusEnvoi(p, destinataire);
        if (refus != null) {
            return refus;
        }
        if (contenu.isEmpty()) {
            return t("refus.colis-vide", "<red>Pose les objets à envoyer dans les cases du colis.");
        }
        PlayerInventory inv = p.getInventory();
        int placeColis = chercher(inv, "colis");
        if (placeColis < 0) {
            return t("refus.sans-colis", "<red>Il te faut un colis vide dans ton inventaire (coffre, ficelle et 2 "
                    + "papiers à l'établi).");
        }
        Boite a = donnees.boite(compte(p)), b = donnees.boite(destinataire);
        long distance = distance(a, b);
        long frais = frais(distance);
        if (frais > 0 && !KSEconomy.debiterCompte(compte(p), frais)) {
            return t("refus.solde", "<red>Solde insuffisant : l'envoi coûte <frais>.", "frais", points(frais));
        }
        retirerUn(inv, placeColis);
        Envoi e = nouveau(Type.COLIS, p, destinataire, nomDestinataire, distance, frais);
        contenu.forEach(o -> e.contenu.add(o.clone()));
        e.resume = resume(contenu);
        enregistrer(e);
        return null;
    }

    /** Dépôt du serveur (demandes publiques de KS_Contrats) : colis de 27 cases au plus chacun. */
    void deposer(UUID compte, String nomCompte, String expediteur, String titre, List<ItemStack> liste) {
        List<ItemStack> reste = new ArrayList<>();
        for (ItemStack o : liste) {
            if (o != null && !o.getType().isAir()) {
                reste.add(o.clone());
            }
        }
        while (!reste.isEmpty()) {
            List<ItemStack> part = new ArrayList<>(reste.subList(0, Math.min(CASES_COLIS, reste.size())));
            reste.subList(0, part.size()).clear();
            Envoi e = new Envoi();
            e.id = donnees.nouvelId();
            e.type = Type.COLIS;
            e.nomExpediteur = expediteur;
            e.destinataire = compte;
            e.nomDestinataire = nomCompte;
            e.titre = titre;
            e.date = System.currentTimeMillis();
            e.contenu.addAll(part);
            e.resume = resume(part);
            enregistrer(e);
        }
    }

    private Envoi nouveau(Type type, Player p, UUID destinataire, String nomDestinataire, long distance, long frais) {
        Envoi e = new Envoi();
        e.id = donnees.nouvelId();
        e.type = type;
        e.expediteur = compte(p);
        e.nomExpediteur = p.getName();
        e.destinataire = destinataire;
        e.nomDestinataire = nomDestinataire;
        e.date = System.currentTimeMillis();
        e.distance = distance;
        e.frais = frais;
        return e;
    }

    private void enregistrer(Envoi e) {
        donnees.envois.put(e.id, e);
        donnees.sauver();
        plugin.getLogger().info((e.type == Type.LETTRE ? "Lettre " : "Colis ") + e.id + " de " + e.nomExpediteur
                + " pour " + e.nomDestinataire + (e.type == Type.COLIS ? " : " + e.contenu.size() + " pile(s)" : ""));
        Player d = enLigne(e.destinataire);
        if (d != null) {
            d.sendMessage(e.type == Type.LETTRE
                    ? t("recu.lettre", "<gold>[Courrier] Lettre de <joueur> : elle t'attend dans ta boîte aux lettres.",
                    "joueur", e.nomExpediteur)
                    : t("recu.colis", "<gold>[Courrier] Colis de <joueur> : il t'attend dans ta boîte aux lettres.",
                    "joueur", e.nomExpediteur));
        }
    }

    static List<String> resume(List<ItemStack> contenu) {
        List<String> r = new ArrayList<>();
        for (ItemStack o : contenu) {
            r.add(MiniMessage.miniMessage().serialize(Component.text(o.getAmount() + " × ")
                    .append(KSEconomy.nomObjet(o))));
        }
        return r;
    }

    /** Place d'un objet du courrier (enveloppe, colis vide) dans l'inventaire, ou -1. */
    private int chercher(PlayerInventory inv, String marque) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (marque.equals(objets.marque(inv.getItem(i)))) {
                return i;
            }
        }
        return -1;
    }

    private static void retirerUn(PlayerInventory inv, int place) {
        ItemStack o = inv.getItem(place);
        if (o.getAmount() > 1) {
            o.setAmount(o.getAmount() - 1);
            inv.setItem(place, o);
        } else {
            inv.setItem(place, null);
        }
    }

    // ------------------------------------------------------------------ réception

    /** Sort un envoi de la boîte : l'objet scellé va dans l'inventaire. Refus, ou null. */
    Component retirer(Player p, Envoi e) {
        if (e.etat != Etat.BOITE || !compte(p).equals(e.destinataire)) {
            return t("refus.plus-la", "<red>Cet envoi n'est plus dans ta boîte.");
        }
        if (p.getInventory().firstEmpty() < 0) {
            return t("refus.inventaire-plein", "<red>Inventaire plein : libère une case.");
        }
        e.etat = Etat.SORTI;
        e.retrait = System.currentTimeMillis();
        donnees.sauver();
        p.getInventory().addItem(objets.scelle(e));
        return null;
    }

    /**
     * Ouvre la lettre ou le colis scellé tenu en main. Un envoi déjà ouvert (copie de l'objet) ne donne rien : l'objet
     * disparaît et la tentative est notée dans la console.
     */
    void ouvrir(Player p, EquipmentSlot main, ItemStack scelle, String id) {
        Envoi e = donnees.envois.get(id);
        PlayerInventory inv = p.getInventory();
        if (e == null || e.etat == Etat.OUVERT) {
            inv.setItem(main, null);
            p.sendMessage(t("ouvrir.deja", "<red>Cet envoi a déjà été ouvert : cet objet était une copie, il disparaît."));
            plugin.getLogger().warning("Copie d'un envoi deja ouvert (" + id + ") chez " + p.getName());
            return;
        }
        if (e.type == Type.COLIS) {
            int libres = 1; // la case de l'objet scellé
            for (ItemStack o : inv.getStorageContents()) {
                if (o == null || o.getType().isAir()) {
                    libres++;
                }
            }
            if (libres < e.contenu.size()) {
                p.sendMessage(t("ouvrir.place", "<red>Il te faut <n> cases libres pour ouvrir ce colis.", "n",
                        e.contenu.size()));
                return;
            }
        }
        e.etat = Etat.OUVERT;
        e.ouverture = System.currentTimeMillis();
        e.ouvreur = p.getName();
        List<ItemStack> contenu = new ArrayList<>(e.contenu);
        if (e.type == Type.COLIS) {
            e.contenu.clear(); // le résumé reste dans l'historique
        }
        donnees.sauver();
        inv.setItem(main, null);
        if (e.type == Type.LETTRE) {
            ItemStack livre = contenu.isEmpty() ? null : contenu.get(0).clone();
            if (livre != null) {
                ItemMeta meta = livre.getItemMeta();
                meta.lore(List.of(Component.text("Lettre de " + e.nomExpediteur + ", envoyée le " + Objets.date(e.date),
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                livre.setItemMeta(meta);
                inv.setItem(main, livre);
            }
        } else {
            for (ItemStack reste : inv.addItem(contenu.toArray(new ItemStack[0])).values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), reste);
            }
            p.sendMessage(t("ouvrir.colis", "<green>Colis de <joueur> ouvert.", "joueur", e.nomExpediteur));
        }
    }
}
