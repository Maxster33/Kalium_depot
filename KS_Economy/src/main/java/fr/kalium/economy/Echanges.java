package fr.kalium.economy;

import fr.kalium.menu.api.Lang;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /echange (cahier des charges, catégorie 2) : échange d'objets et de points entre 2 joueurs à 5 blocs au plus.
 *
 * - « /echange &lt;joueur&gt; » envoie une demande (valable 60 s) ; l'autre accepte d'un clic ou par « /echange accepter ».
 * - Panneau d'échange (coffre de 6 rangées) : à gauche sa part, à droite celle de l'autre (objets et montant), leurs
 *   validations. Bouton « Objets » : coffre où le joueur dépose ses objets (16 cases) ; bouton « Montant » : saisie
 *   du montant (limité à son solde).
 * - L'échange a lieu quand les deux ont validé ; toute modification d'une part annule les deux validations.
 * - Annulé (objets rendus) si un joueur s'éloigne à plus de 5 blocs, ferme le panneau, se déconnecte, ou au bout de
 *   2 minutes dans la saisie du montant.
 *
 * Les objets déposés restent dans le coffre de l'échange (hors de l'inventaire du joueur) jusqu'à la fin : rendus à
 * l'annulation, donnés à l'autre à la conclusion.
 */
final class Echanges implements Listener, TabExecutor {

    private static final double DISTANCE_MAX = 5;
    private static final long DEMANDE_MS = 60_000;
    private static final long SAISIE_MS = 120_000;
    private static final int CASES_OBJETS = 16;

    // Cases du panneau.
    private static final int OBJETS = 45, MONTANT = 46, VALIDER = 48, ANNULER = 49, MONTANT_AUTRE = 52, VALIDE_AUTRE = 53;

    /** Où se trouve le joueur dans l'échange. */
    private enum Vue { PANNEAU, OBJETS, MONTANT, AILLEURS }

    /** La part d'un joueur. */
    private final class Part implements InventoryHolder {
        final Player joueur;
        final Inventory panneau;
        final Inventory objets;
        long montant;
        boolean valide;
        Vue vue = Vue.AILLEURS;
        long debutSaisie;
        /** Objets au moment d'ouvrir le coffre des objets (pour savoir s'ils ont changé). */
        List<ItemStack> avant = List.of();
        Echange echange;

        Part(Player joueur, String autre) {
            this.joueur = joueur;
            this.panneau = Bukkit.createInventory(this, 54, lang.c("echange.titre", "Échange avec <autre>",
                    "autre", autre));
            this.objets = Bukkit.createInventory(this, 18, lang.c("echange.objets-titre", "Tes objets à échanger"));
            for (int i = CASES_OBJETS; i < 18; i++) {
                objets.setItem(i, bouton(Material.BLACK_STAINED_GLASS_PANE, Component.text(" ")));
            }
        }

        @Override
        public Inventory getInventory() {
            return panneau;
        }

        Part autre() {
            return echange.a == this ? echange.b : echange.a;
        }

        /** Objets déposés (hors cases bloquées). */
        List<ItemStack> objetsDeposes() {
            List<ItemStack> liste = new ArrayList<>();
            for (int i = 0; i < CASES_OBJETS; i++) {
                ItemStack objet = objets.getItem(i);
                if (objet != null && !objet.getType().isAir()) {
                    liste.add(objet);
                }
            }
            return liste;
        }
    }

    private final class Echange {
        Part a;
        Part b;
        boolean fini;
    }

    private record Demande(UUID demandeur, long expire) {
    }

    private final KSEconomy plugin;
    private final Lang lang;
    /** Joueur -> sa part (échange en cours). */
    private final Map<UUID, Part> parts = new HashMap<>();
    /** Joueur demandé -> demande reçue. */
    private final Map<UUID, Demande> demandes = new HashMap<>();

    Echanges(KSEconomy plugin) {
        this.plugin = plugin;
        this.lang = plugin.lang();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::surveiller, 10L, 10L);
    }

    // ------------------------------------------------------------------ commande

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        if (args.length != 1) {
            dire(joueur, lang.c("echange.usage", "<red>/echange <joueur>, /echange accepter ou /echange refuser"));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "accepter" -> accepter(joueur);
            case "refuser" -> refuser(joueur);
            default -> demander(joueur, Bukkit.getPlayerExact(args[0]));
        }
        lang.saveIfNeeded();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> choix = new ArrayList<>();
        if (args.length == 1) {
            choix.add("accepter");
            choix.add("refuser");
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p != sender) {
                    choix.add(p.getName());
                }
            });
            choix.removeIf(c -> !c.toLowerCase().startsWith(args[0].toLowerCase()));
        }
        return choix;
    }

    private void dire(Player joueur, Component texte) {
        joueur.sendMessage(texte);
    }

    private static boolean proches(Player a, Player b) {
        return a.isOnline() && b.isOnline() && a.getWorld().equals(b.getWorld())
                && a.getLocation().distance(b.getLocation()) <= DISTANCE_MAX;
    }

    private void demander(Player joueur, Player cible) {
        if (cible == null || cible == joueur) {
            dire(joueur, lang.c("echange.introuvable", "<red>Joueur introuvable."));
            return;
        }
        if (parts.containsKey(joueur.getUniqueId()) || parts.containsKey(cible.getUniqueId())) {
            dire(joueur, lang.c("echange.occupe", "<red>Un échange est déjà en cours."));
            return;
        }
        if (!proches(joueur, cible)) {
            dire(joueur, lang.c("echange.trop-loin", "<red>Vous devez être à 5 blocs au plus l'un de l'autre."));
            return;
        }
        demandes.put(cible.getUniqueId(), new Demande(joueur.getUniqueId(), System.currentTimeMillis() + DEMANDE_MS));
        dire(joueur, lang.c("echange.envoye", "<green>Demande d'échange envoyée à <cible>.", "cible", cible.getName()));
        dire(cible, lang.c("echange.recu", "<yellow><joueur> te propose un échange. ", "joueur", joueur.getName())
                .append(lang.c("echange.bouton-accepter", "<green><bold>[Accepter]")
                        .clickEvent(ClickEvent.runCommand("/echange accepter")))
                .append(Component.text(" "))
                .append(lang.c("echange.bouton-refuser", "<red><bold>[Refuser]")
                        .clickEvent(ClickEvent.runCommand("/echange refuser"))));
    }

    private void refuser(Player joueur) {
        Demande demande = demandes.remove(joueur.getUniqueId());
        if (demande == null) {
            dire(joueur, lang.c("echange.aucune", "<red>Aucune demande d'échange."));
            return;
        }
        Player demandeur = Bukkit.getPlayer(demande.demandeur());
        if (demandeur != null) {
            dire(demandeur, lang.c("echange.refuse", "<red><joueur> a refusé l'échange.", "joueur", joueur.getName()));
        }
        dire(joueur, lang.c("echange.refuse-toi", "<gray>Échange refusé."));
    }

    private void accepter(Player joueur) {
        Demande demande = demandes.remove(joueur.getUniqueId());
        Player demandeur = demande == null ? null : Bukkit.getPlayer(demande.demandeur());
        if (demande == null || demande.expire() < System.currentTimeMillis() || demandeur == null) {
            dire(joueur, lang.c("echange.aucune", "<red>Aucune demande d'échange."));
            return;
        }
        if (parts.containsKey(joueur.getUniqueId()) || parts.containsKey(demandeur.getUniqueId())) {
            dire(joueur, lang.c("echange.occupe", "<red>Un échange est déjà en cours."));
            return;
        }
        if (!proches(joueur, demandeur)) {
            dire(joueur, lang.c("echange.trop-loin", "<red>Vous devez être à 5 blocs au plus l'un de l'autre."));
            return;
        }
        Echange echange = new Echange();
        echange.a = new Part(demandeur, joueur.getName());
        echange.b = new Part(joueur, demandeur.getName());
        echange.a.echange = echange;
        echange.b.echange = echange;
        parts.put(demandeur.getUniqueId(), echange.a);
        parts.put(joueur.getUniqueId(), echange.b);
        ouvrirPanneau(echange.a);
        ouvrirPanneau(echange.b);
    }

    // ------------------------------------------------------------------ panneau

    private static ItemStack bouton(Material type, Component nom, Component... lignes) {
        ItemStack objet = new ItemStack(type);
        ItemMeta meta = objet.getItemMeta();
        meta.displayName(nom.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        List<Component> description = new ArrayList<>();
        for (Component ligne : lignes) {
            description.add(ligne.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        meta.lore(description);
        objet.setItemMeta(meta);
        return objet;
    }

    /** Case de l'aperçu (rangées 2 à 5, 4 colonnes) : gauche = sa part, droite = celle de l'autre. */
    private static int caseApercu(int index, boolean gauche) {
        return (1 + index / 4) * 9 + (index % 4) + (gauche ? 0 : 5);
    }

    private void dessiner(Part part) {
        Part autre = part.autre();
        Inventory p = part.panneau;
        p.clear();
        ItemStack separateur = bouton(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
        for (int rangee = 0; rangee < 6; rangee++) {
            p.setItem(rangee * 9 + 4, separateur);
        }
        p.setItem(0, bouton(Material.PAPER, lang.c("echange.ta-part", "<green><bold>Ta part")));
        p.setItem(8, bouton(Material.PAPER, lang.c("echange.sa-part", "<gold><bold>Part de <autre>", "autre",
                autre.joueur.getName())));
        List<ItemStack> miens = part.objetsDeposes();
        for (int i = 0; i < miens.size(); i++) {
            p.setItem(caseApercu(i, true), miens.get(i).clone());
        }
        List<ItemStack> siens = autre.objetsDeposes();
        for (int i = 0; i < siens.size(); i++) {
            p.setItem(caseApercu(i, false), siens.get(i).clone());
        }
        p.setItem(OBJETS, bouton(Material.CHEST, lang.c("echange.objets", "<yellow>Objets"),
                lang.c("echange.objets.info", "<gray>Déposer ou reprendre tes objets")));
        p.setItem(MONTANT, bouton(Material.GOLD_INGOT, lang.c("echange.montant", "<yellow>Montant : <montant>",
                "montant", KSEconomy.points(part.montant)), lang.c("echange.montant.info", "<gray>Points que tu donnes "
                + "(solde : <solde>)", "solde", KSEconomy.points(KSEconomy.solde(part.joueur.getUniqueId())))));
        p.setItem(VALIDER, part.valide
                ? bouton(Material.LIME_CONCRETE, lang.c("echange.valide", "<green><bold>Validé"),
                lang.c("echange.valide.info", "<gray>Clic : retirer ta validation"))
                : bouton(Material.WHITE_CONCRETE, lang.c("echange.valider", "<white><bold>Valider"),
                lang.c("echange.valider.info", "<gray>L'échange a lieu quand vous avez validé tous les deux")));
        p.setItem(ANNULER, bouton(Material.BARRIER, lang.c("echange.annuler", "<red>Annuler l'échange")));
        p.setItem(MONTANT_AUTRE, bouton(Material.GOLD_NUGGET, lang.c("echange.montant-autre", "<gold>Montant de "
                + "<autre> : <montant>", "autre", autre.joueur.getName(), "montant", KSEconomy.points(autre.montant))));
        p.setItem(VALIDE_AUTRE, autre.valide
                ? bouton(Material.LIME_CONCRETE, lang.c("echange.autre-valide", "<green><autre> a validé", "autre",
                autre.joueur.getName()))
                : bouton(Material.RED_CONCRETE, lang.c("echange.autre-attend", "<red><autre> n'a pas validé",
                "autre", autre.joueur.getName())));
    }

    private void ouvrirPanneau(Part part) {
        dessiner(part);
        part.vue = Vue.PANNEAU;
        part.joueur.openInventory(part.panneau);
    }

    /** Une part a changé : validations retirées, panneaux redessinés. */
    private void modifie(Echange echange) {
        echange.a.valide = false;
        echange.b.valide = false;
        dessiner(echange.a);
        dessiner(echange.b);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Part part) || part.echange.fini) {
            return;
        }
        Inventory haut = event.getView().getTopInventory();
        if (haut == part.objets) {
            // Coffre des objets : libre, sauf les cases bloquées.
            if (event.getClickedInventory() == haut && event.getSlot() >= CASES_OBJETS) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != haut) {
            return;
        }
        int caseCliquee = event.getSlot();
        // Ouvrir ou fermer un inventaire pendant un clic est déconseillé : action au tick suivant.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!part.echange.fini && part.vue == Vue.PANNEAU) {
                bouton(part, caseCliquee);
            }
        });
    }

    private void bouton(Part part, int caseCliquee) {
        switch (caseCliquee) {
            case OBJETS -> {
                // Ouvrir sa part retire les validations (rien ne peut changer après une validation).
                modifie(part.echange);
                part.avant = copie(part.objetsDeposes());
                part.vue = Vue.OBJETS;
                part.joueur.openInventory(part.objets);
            }
            case MONTANT -> {
                modifie(part.echange);
                saisirMontant(part);
            }
            case VALIDER -> {
                part.valide = !part.valide;
                if (part.valide && part.autre().valide) {
                    conclure(part.echange);
                } else {
                    dessiner(part);
                    dessiner(part.autre());
                }
            }
            case ANNULER -> annuler(part.echange, lang.c("echange.annule-par", "<red>Échange annulé par <joueur>.",
                    "joueur", part.joueur.getName()));
            default -> {
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Part part)) {
            return;
        }
        int taille = event.getView().getTopInventory().getSize();
        boolean haut = event.getRawSlots().stream().anyMatch(caseBrute -> caseBrute < taille);
        if (event.getView().getTopInventory() != part.objets
                ? haut : event.getRawSlots().stream().anyMatch(c -> c >= CASES_OBJETS && c < taille)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Part part) || part.echange.fini) {
            return;
        }
        if (event.getInventory() == part.objets && part.vue == Vue.OBJETS) {
            // Retour au panneau ; une modification des objets retire les validations.
            if (!memes(part.avant, part.objetsDeposes())) {
                modifie(part.echange);
            } else {
                dessiner(part);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!part.echange.fini && part.vue == Vue.OBJETS) {
                    ouvrirPanneau(part);
                }
            });
        } else if (event.getInventory() == part.panneau && part.vue == Vue.PANNEAU) {
            annuler(part.echange, lang.c("echange.ferme", "<red>Échange annulé : <joueur> a fermé le panneau.",
                    "joueur", part.joueur.getName()));
        }
    }

    private static List<ItemStack> copie(List<ItemStack> objets) {
        List<ItemStack> liste = new ArrayList<>();
        objets.forEach(o -> liste.add(o.clone()));
        return liste;
    }

    private static boolean memes(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).isSimilar(b.get(i)) || a.get(i).getAmount() != b.get(i).getAmount()) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ montant

    private void saisirMontant(Part part) {
        part.vue = Vue.MONTANT;
        part.debutSaisie = System.currentTimeMillis();
        ClickCallback.Options options = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(5)).build();
        ActionButton valider = ActionButton.create(lang.c("montant.valider", "<green>Valider le montant"), null, 200,
                DialogAction.customClick((vue, audience) -> Bukkit.getScheduler().runTask(plugin,
                        () -> finSaisie(part, vue.getText("montant"))), options));
        ActionButton retour = ActionButton.create(lang.c("gui.retour", "<gray>Retour"), null, 200,
                DialogAction.customClick((vue, audience) -> Bukkit.getScheduler().runTask(plugin,
                        () -> finSaisie(part, null)), options));
        DialogBase base = DialogBase.builder(lang.c("montant.titre", "<yellow><bold>Montant à donner"))
                .body(List.of(DialogBody.plainMessage(lang.c("montant.texte", "<white>Solde : <green><solde>",
                        "solde", KSEconomy.points(KSEconomy.solde(part.joueur.getUniqueId()))), 300)))
                .inputs(List.<DialogInput>of(DialogInput.text("montant", lang.c("montant.champ", "Points"))
                        .initial(String.valueOf(part.montant)).maxLength(12).width(200).build()))
                .canCloseWithEscape(false)
                .build();
        part.joueur.closeInventory();
        part.joueur.showDialog(Dialog.create(f -> f.empty().base(base)
                .type(DialogType.multiAction(List.of(valider, retour), null, 2))));
        lang.saveIfNeeded();
    }

    private void finSaisie(Part part, String texte) {
        if (part.echange.fini || part.vue != Vue.MONTANT || !part.joueur.isOnline()) {
            return;
        }
        if (texte != null) {
            long montant;
            try {
                montant = Long.parseLong(texte.replaceAll("\\s", ""));
            } catch (NumberFormatException e) {
                montant = -1;
            }
            long solde = KSEconomy.solde(part.joueur.getUniqueId());
            if (montant < 0 || montant > solde) {
                dire(part.joueur, lang.c("montant.invalide", "<red>Montant invalide (entre 0 et ton solde : <solde>).",
                        "solde", KSEconomy.points(solde)));
            } else if (montant != part.montant) {
                part.montant = montant;
                modifie(part.echange);
            }
        }
        part.joueur.closeDialog();
        ouvrirPanneau(part);
    }

    // ------------------------------------------------------------------ fin de l'échange

    /** Toutes les demi-secondes : distance, joueurs en ligne, saisie du montant trop longue, demandes expirées. */
    private void surveiller() {
        long maintenant = System.currentTimeMillis();
        demandes.values().removeIf(d -> d.expire() < maintenant);
        for (Part part : List.copyOf(parts.values())) {
            Echange echange = part.echange;
            if (echange.fini || part != echange.a) {
                continue;
            }
            if (!proches(echange.a.joueur, echange.b.joueur)) {
                annuler(echange, lang.c("echange.eloigne", "<red>Échange annulé : vous êtes à plus de 5 blocs."));
            } else if ((echange.a.vue == Vue.MONTANT && maintenant - echange.a.debutSaisie > SAISIE_MS)
                    || (echange.b.vue == Vue.MONTANT && maintenant - echange.b.debutSaisie > SAISIE_MS)) {
                annuler(echange, lang.c("echange.trop-long", "<red>Échange annulé : saisie du montant trop longue."));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        demandes.remove(event.getPlayer().getUniqueId());
        Part part = parts.get(event.getPlayer().getUniqueId());
        if (part != null) {
            annuler(part.echange, lang.c("echange.deconnecte", "<red>Échange annulé : <joueur> s'est déconnecté.",
                    "joueur", event.getPlayer().getName()));
        }
    }

    private void terminer(Echange echange) {
        echange.fini = true;
        parts.remove(echange.a.joueur.getUniqueId());
        parts.remove(echange.b.joueur.getUniqueId());
        for (Part part : List.of(echange.a, echange.b)) {
            if (part.vue == Vue.MONTANT && part.joueur.isOnline()) {
                part.joueur.closeDialog();
            }
            Inventory haut = part.joueur.getOpenInventory().getTopInventory();
            if (haut == part.panneau || haut == part.objets) {
                part.joueur.closeInventory();
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Part part = parts.get(event.getPlayer().getUniqueId());
        if (part != null) {
            annuler(part.echange, lang.c("echange.mort", "<red>Échange annulé : <joueur> est mort.", "joueur",
                    event.getPlayer().getName()));
        }
    }

    /** Objets rendus ou donnés : dans l'inventaire, au sol s'il est plein (ou si le joueur est mort). */
    private static void donner(Player joueur, List<ItemStack> objets) {
        for (ItemStack objet : objets) {
            if (joueur.isDead()) {
                joueur.getWorld().dropItemNaturally(joueur.getLocation(), objet);
                continue;
            }
            for (ItemStack reste : joueur.getInventory().addItem(objet).values()) {
                joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
            }
        }
    }

    /** Annule : chacun récupère ses objets (au sol si l'inventaire est plein). */
    private void annuler(Echange echange, Component raison) {
        if (echange.fini) {
            return;
        }
        terminer(echange);
        for (Part part : List.of(echange.a, echange.b)) {
            donner(part.joueur, part.objetsDeposes());
            part.objets.clear();
            if (part.joueur.isOnline()) {
                dire(part.joueur, raison);
            }
        }
        lang.saveIfNeeded();
    }

    /** Arrêt du serveur. */
    void toutAnnuler() {
        for (Part part : List.copyOf(parts.values())) {
            annuler(part.echange, lang.c("echange.arret", "<red>Échange annulé : arrêt du serveur."));
        }
    }

    /** Chacun a-t-il la place de recevoir les objets de l'autre ? */
    private static boolean aDeLaPlace(Player joueur, List<ItemStack> objets) {
        Inventory essai = Bukkit.createInventory(null, 36);
        ItemStack[] contenu = joueur.getInventory().getStorageContents();
        for (int i = 0; i < contenu.length && i < 36; i++) {
            essai.setItem(i, contenu[i] == null ? null : contenu[i].clone());
        }
        for (ItemStack objet : objets) {
            if (!essai.addItem(objet.clone()).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void conclure(Echange echange) {
        Part a = echange.a;
        Part b = echange.b;
        List<ItemStack> objetsA = a.objetsDeposes();
        List<ItemStack> objetsB = b.objetsDeposes();
        Component probleme = null;
        if (KSEconomy.solde(a.joueur.getUniqueId()) < a.montant || KSEconomy.solde(b.joueur.getUniqueId()) < b.montant) {
            probleme = lang.c("echange.solde", "<red>Un des soldes ne suffit plus pour le montant proposé.");
        } else if (!aDeLaPlace(a.joueur, objetsB)) {
            probleme = lang.c("echange.plein", "<red>L'inventaire de <joueur> est trop plein.", "joueur",
                    a.joueur.getName());
        } else if (!aDeLaPlace(b.joueur, objetsA)) {
            probleme = lang.c("echange.plein", "<red>L'inventaire de <joueur> est trop plein.", "joueur",
                    b.joueur.getName());
        }
        if (probleme != null) {
            dire(a.joueur, probleme);
            dire(b.joueur, probleme);
            modifie(echange);
            return;
        }
        // Points : les deux débits d'abord (un échec ne crédite rien).
        if (!KSEconomy.debiter(a.joueur.getUniqueId(), a.montant)) {
            modifie(echange);
            return;
        }
        if (!KSEconomy.debiter(b.joueur.getUniqueId(), b.montant)) {
            KSEconomy.crediter(a.joueur.getUniqueId(), a.montant);
            modifie(echange);
            return;
        }
        terminer(echange);
        KSEconomy.crediter(b.joueur.getUniqueId(), a.montant);
        KSEconomy.crediter(a.joueur.getUniqueId(), b.montant);
        a.objets.clear();
        b.objets.clear();
        donner(a.joueur, objetsB);
        donner(b.joueur, objetsA);
        Component fait = lang.c("echange.fait", "<green>Échange effectué.");
        dire(a.joueur, fait);
        dire(b.joueur, fait);
        plugin.getLogger().info("Échange : " + a.joueur.getName() + " (" + objetsA.size() + " piles, " + a.montant
                + " points) <-> " + b.joueur.getName() + " (" + objetsB.size() + " piles, " + b.montant + " points)");
        lang.saveIfNeeded();
    }
}
