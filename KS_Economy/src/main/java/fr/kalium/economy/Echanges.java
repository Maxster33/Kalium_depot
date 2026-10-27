package fr.kalium.economy;

import fr.kalium.menu.api.Contenant;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /echange (cahier des charges, catégorie 2) : échange d'objets et de points entre 2 joueurs à 5 blocs au plus.
 *
 * - « /echange &lt;joueur&gt; » envoie une demande (valable 60 s) ; l'autre accepte d'un clic ou par « /echange accepter ».
 *   1.6.0 (LeKiwi06, 11/10/2026) : « /accepter » fait de même ; un joueur garde une demande par demandeur, et s'il en
 *   a reçu de plusieurs joueurs, un menu lui fait choisir laquelle accepter.
 * - Panneau d'échange (coffre de 6 rangées) : à gauche sa part, à droite celle de l'autre (objets et montant), leurs
 *   validations. Bouton « Objets » : coffre où le joueur dépose ses objets (16 cases) ; bouton « Montant » : saisie
 *   du montant (limité à son solde).
 * - L'échange a lieu quand les deux ont validé ; toute modification d'une part annule les deux validations.
 * - Annulé (objets rendus) si un joueur s'éloigne à plus de 5 blocs, ferme le panneau, se déconnecte, ou au bout de
 *   2 minutes dans la saisie du montant.
 *
 * Les objets déposés restent dans le coffre de l'échange (hors de l'inventaire du joueur) jusqu'à la fin : rendus à
 * l'annulation, donnés à l'autre à la conclusion.
 *
 * 1.12.0 (LeKiwi06, 27/10/2026, /contrat de KS_Contrats) : bouton « Contrat » (si KS_Contrats est là) : l'un des deux
 * choisit un contrat qu'ils ont ensemble ; chacun ne peut alors poser que les objets et les points que sa partie doit
 * encore livrer (le reste lui est rendu), et la livraison est comptée dans le contrat à la conclusion.
 *
 * 1.12.1 (LeKiwi06, 27/10/2026 : « on ne peut pas les mettre dedans dès le début ? il faut éviter qu'un joueur en
 * arnaque un autre en faisant croire que les items seront dans l'échange ») : avec un contrat, un objet que la partie
 * du joueur ne doit pas ne se pose plus dans le coffre des objets (clic refusé) ; une quantité au-delà de ce qui reste
 * à livrer est rendue juste après le clic ; des objets déjà posés avant le choix du contrat sont rendus et l'autre
 * joueur en est prévenu.
 */
final class Echanges implements Listener, TabExecutor {

    private static final double DISTANCE_MAX = 5;
    private static final long DEMANDE_MS = 60_000;
    private static final long SAISIE_MS = 120_000;
    private static final int CASES_OBJETS = 16;

    // Cases du panneau.
    private static final int OBJETS = 45, MONTANT = 46, CONTRAT = 47, VALIDER = 48, ANNULER = 49, MONTANT_AUTRE = 52,
            VALIDE_AUTRE = 53;

    /** Où se trouve le joueur dans l'échange (1.12.0 : CONTRAT, menu du choix d'un contrat). */
    private enum Vue { PANNEAU, OBJETS, MONTANT, CONTRAT, AILLEURS }

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
        /** 1.12.0 : contrat choisi (KS_Contrats), null : échange normal. */
        String contrat;
        String contratChoisiPar;
    }

    /** 1.12.0 : contrats de KS_Contrats (null s'il n'est pas là). */
    private static fr.kalium.economy.api.ContratsEchange contrats() {
        return Bukkit.getServicesManager().load(fr.kalium.economy.api.ContratsEchange.class);
    }

    private final KSEconomy plugin;
    private final Lang lang;
    /** Joueur -> sa part (échange en cours). */
    private final Map<UUID, Part> parts = new HashMap<>();
    /** Joueur demandé -> demandeur -> fin de validité de sa demande (1.6.0 : une demande par demandeur). */
    private final Map<UUID, Map<UUID, Long>> demandes = new HashMap<>();

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
        // 1.6.0 (LeKiwi06 : « il faut pouvoir faire /accepter pour accepter une demande d'échanger, c'est plus court »)
        if (command.getName().equalsIgnoreCase("accepter")) {
            accepter(joueur, args.length == 0 ? null : args[0]);
            lang.saveIfNeeded();
            return true;
        }
        String action = args.length == 0 ? "" : args[0].toLowerCase();
        if (args.length == 0 || args.length > 2 || (args.length == 2 && !action.equals("accepter") && !action.equals("refuser"))) {
            dire(joueur, lang.c("echange.usage", "<red>/echange <joueur>, /echange accepter ou /echange refuser"));
            return true;
        }
        switch (action) {
            case "accepter" -> accepter(joueur, args.length == 2 ? args[1] : null);
            case "refuser" -> refuser(joueur, args.length == 2 ? args[1] : null);
            default -> demander(joueur, Bukkit.getPlayerExact(args[0]));
        }
        lang.saveIfNeeded();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> choix = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("accepter") || (args.length == 2
                && (args[0].equalsIgnoreCase("accepter") || args[0].equalsIgnoreCase("refuser")))) {
            if (sender instanceof Player joueur && args.length >= 1) {
                recues(joueur).forEach(demandeur -> choix.add(demandeur.getName()));
                choix.removeIf(c -> !c.toLowerCase().startsWith(args[args.length - 1].toLowerCase()));
            }
            return choix;
        }
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
        demandes.computeIfAbsent(cible.getUniqueId(), u -> new LinkedHashMap<>())
                .put(joueur.getUniqueId(), System.currentTimeMillis() + DEMANDE_MS);
        dire(joueur, lang.c("echange.envoye", "<green>Demande d'échange envoyée à <cible>.", "cible", cible.getName()));
        // Les boutons nomment le demandeur : avec plusieurs demandes, chacun répond à la sienne.
        dire(cible, lang.c("echange.recu", "<yellow><joueur> te propose un échange. ", "joueur", joueur.getName())
                .append(lang.c("echange.bouton-accepter", "<green><bold>[Accepter]")
                        .clickEvent(ClickEvent.runCommand("/echange accepter " + joueur.getName())))
                .append(Component.text(" "))
                .append(lang.c("echange.bouton-refuser", "<red><bold>[Refuser]")
                        .clickEvent(ClickEvent.runCommand("/echange refuser " + joueur.getName()))));
    }

    /** 1.6.0 : joueurs en ligne dont la demande d'échange à ce joueur est encore valable, de la plus ancienne à la plus récente. */
    private List<Player> recues(Player joueur) {
        List<Player> demandeurs = new ArrayList<>();
        Map<UUID, Long> recues = demandes.get(joueur.getUniqueId());
        if (recues != null) {
            long maintenant = System.currentTimeMillis();
            recues.forEach((demandeur, expire) -> {
                Player p = Bukkit.getPlayer(demandeur);
                if (p != null && expire >= maintenant) {
                    demandeurs.add(p);
                }
            });
        }
        return demandeurs;
    }

    /** La demande de ce demandeur n'attend plus de réponse. */
    private void oublier(Player joueur, Player demandeur) {
        Map<UUID, Long> recues = demandes.get(joueur.getUniqueId());
        if (recues != null) {
            recues.remove(demandeur.getUniqueId());
        }
    }

    /** Refuse la demande du joueur nommé ; sans nom : toutes les demandes reçues. */
    private void refuser(Player joueur, String nom) {
        List<Player> demandeurs = recues(joueur);
        if (nom != null) {
            demandeurs.removeIf(d -> !d.getName().equalsIgnoreCase(nom));
        }
        if (demandeurs.isEmpty()) {
            dire(joueur, lang.c("echange.aucune", "<red>Aucune demande d'échange."));
            return;
        }
        for (Player demandeur : demandeurs) {
            oublier(joueur, demandeur);
            dire(demandeur, lang.c("echange.refuse", "<red><joueur> a refusé l'échange.", "joueur", joueur.getName()));
        }
        dire(joueur, lang.c("echange.refuse-toi", "<gray>Échange refusé."));
    }

    /**
     * Accepte la demande du joueur nommé. Sans nom (« /accepter », « /echange accepter ») : la seule demande reçue, ou,
     * s'il y en a de plusieurs joueurs, un menu pour choisir laquelle (1.6.0, LeKiwi06 : « ouvrir un menu cliquable
     * pour choisir quelle demande accepter »).
     */
    private void accepter(Player joueur, String nom) {
        List<Player> demandeurs = recues(joueur);
        if (nom != null) {
            demandeurs.removeIf(d -> !d.getName().equalsIgnoreCase(nom));
        }
        if (demandeurs.isEmpty()) {
            dire(joueur, lang.c("echange.aucune", "<red>Aucune demande d'échange."));
        } else if (demandeurs.size() == 1) {
            accepter(joueur, demandeurs.get(0));
        } else {
            choisir(joueur, demandeurs);
        }
    }

    /** 1.6.0 : menu des demandes reçues, une tête par demandeur ; un clic accepte sa demande. */
    private void choisir(Player joueur, List<Player> demandeurs) {
        Contenant menu = plugin.menus().menu(joueur, Math.min(6, (demandeurs.size() + 8) / 9 + 1),
                lang.c("echange.choix-titre", "<dark_gray>Demandes d'échange"), "echange-demandes");
        for (int i = 0; i < demandeurs.size() && i < menu.taille() - 9; i++) {
            Player demandeur = demandeurs.get(i);
            menu.poser(i, Contenant.objet(Menus.tete(demandeur.getUniqueId()),
                    lang.c("echange.choix-nom", "<yellow><joueur>", "joueur", demandeur.getName()),
                    List.of(lang.c("echange.choix-clic", "<gray>Clic : accepter son échange"))), j -> Contenant.apres(() -> {
                        if (demandeur.isOnline() && recues(j).contains(demandeur)) {
                            accepter(j, demandeur);
                        } else {
                            j.closeInventory();
                            dire(j, lang.c("echange.expiree", "<red>Cette demande d'échange n'est plus valable."));
                        }
                        lang.saveIfNeeded();
                    }));
        }
        plugin.menus().sortie(menu, null);
        menu.ouvrir(joueur);
    }

    private void accepter(Player joueur, Player demandeur) {
        oublier(joueur, demandeur);
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
        p.setItem(OBJETS, bouton(Material.CHEST, lang.c("echange.bouton-objets", "<yellow>Objets"),
                lang.c("echange.info-objets", "<gray>Déposer ou reprendre tes objets")));
        p.setItem(MONTANT, bouton(Material.GOLD_INGOT, lang.c("echange.bouton-montant", "<yellow>Montant : <montant>",
                "montant", KSEconomy.points(part.montant)), lang.c("echange.info-montant", "<gray>Points que tu donnes "
                + "(solde : <solde>)", "solde", KSEconomy.points(KSEconomy.solde(part.joueur.getUniqueId())))));
        p.setItem(VALIDER, part.valide
                ? bouton(Material.LIME_CONCRETE, lang.c("echange.bouton-valide", "<green><bold>Validé"),
                lang.c("echange.info-valide", "<gray>Clic : retirer ta validation"))
                : bouton(Material.WHITE_CONCRETE, lang.c("echange.bouton-valider", "<white><bold>Valider"),
                lang.c("echange.info-valider", "<gray>L'échange a lieu quand vous avez validé tous les deux")));
        p.setItem(ANNULER, bouton(Material.BARRIER, lang.c("echange.annuler", "<red>Annuler l'échange")));
        dessinerContrat(part);
        p.setItem(MONTANT_AUTRE, bouton(Material.GOLD_NUGGET, lang.c("echange.montant-autre", "<gold>Montant de "
                + "<autre> : <montant>", "autre", autre.joueur.getName(), "montant", KSEconomy.points(autre.montant))));
        p.setItem(VALIDE_AUTRE, autre.valide
                ? bouton(Material.LIME_CONCRETE, lang.c("echange.autre-valide", "<green><autre> a validé", "autre",
                autre.joueur.getName()))
                : bouton(Material.RED_CONCRETE, lang.c("echange.autre-attend", "<red><autre> n'a pas validé",
                "autre", autre.joueur.getName())));
    }

    /** 1.12.0 : bouton « Contrat » (rien sans KS_Contrats). */
    private void dessinerContrat(Part part) {
        fr.kalium.economy.api.ContratsEchange service = contrats();
        Echange echange = part.echange;
        if (service == null) {
            return;
        }
        ItemStack icone = echange.contrat == null ? null : service.icone(echange.contrat, part.joueur);
        if (icone == null) {
            part.panneau.setItem(CONTRAT, bouton(Material.WRITABLE_BOOK, lang.c("echange.contrat-aucun",
                    "<yellow>Contrat : aucun"), lang.c("echange.contrat-info1", "<gray>Clic : choisir un contrat que"),
                    lang.c("echange.contrat-info2", "<gray>vous avez ensemble ; seuls ses"),
                    lang.c("echange.contrat-info3", "<gray>objets et ses points passeront.")));
            return;
        }
        ItemMeta meta = icone.getItemMeta();
        List<Component> lignes = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lignes.add(lang.c("echange.contrat-par", "<gray>Choisi par <joueur>", "joueur", echange.contratChoisiPar)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        lignes.add(lang.c("echange.contrat-clic", "<gray>Clic : changer ou retirer le contrat")
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        meta.lore(lignes);
        icone.setItemMeta(meta);
        part.panneau.setItem(CONTRAT, icone);
    }

    /** 1.12.0 : menu du choix d'un contrat ; un clic le choisit pour les deux joueurs. */
    private void choisirContrat(Part part) {
        fr.kalium.economy.api.ContratsEchange service = contrats();
        if (service == null) {
            return;
        }
        Part autre = part.autre();
        List<String> communs = service.communs(part.joueur, autre.joueur);
        Contenant menu = plugin.menus().menu(part.joueur, 6, lang.c("echange.contrat-titre",
                "<dark_gray>Contrat de l'échange"), "echange-contrat");
        int place = 0;
        for (String id : communs) {
            ItemStack icone = service.icone(id, part.joueur);
            if (icone == null || place >= Contenant.PAR_PAGE) {
                continue;
            }
            menu.poser(place++, icone, j -> Contenant.apres(() -> {
                if (part.echange.fini || part.vue != Vue.CONTRAT) {
                    return;
                }
                part.echange.contrat = id;
                part.echange.contratChoisiPar = j.getName();
                appliquerContrat(part);
                appliquerContrat(autre);
                modifie(part.echange);
                ouvrirPanneau(part);
                lang.saveIfNeeded();
            }));
        }
        if (communs.isEmpty()) {
            menu.poser(22, Contenant.objet(Material.PAPER, lang.c("echange.contrat-vide",
                    "<gray>Aucun contrat actif entre vous"), Contenant.lignes(lang.c("echange.contrat-vide-info",
                    "<gray>Il faut un contrat actif où vous êtes chacun d'un côté (/contrat)."))), null);
        }
        if (part.echange.contrat != null) {
            menu.poser(menu.bas(4), Contenant.objet(Material.BARRIER, lang.c("echange.contrat-retirer",
                    "<red>Retirer le contrat"), List.of(lang.c("echange.contrat-retirer-info",
                    "<gray>Revient à un échange normal"))), j -> Contenant.apres(() -> {
                if (part.echange.fini || part.vue != Vue.CONTRAT) {
                    return;
                }
                part.echange.contrat = null;
                part.echange.contratChoisiPar = null;
                modifie(part.echange);
                ouvrirPanneau(part);
            }));
        }
        plugin.menus().sortie(menu, j -> Contenant.apres(() -> {
            if (!part.echange.fini && part.vue == Vue.CONTRAT) {
                ouvrirPanneau(part);
            }
        }));
        part.vue = Vue.CONTRAT;
        menu.ouvrir(part.joueur);
        lang.saveIfNeeded();
    }

    /**
     * 1.12.0 : contrat choisi : les objets posés qui ne sont pas dus (ou en trop) sont rendus, et le montant ramené à
     * ce qui reste à livrer. Contrat qui n'est plus valable : retour à un échange normal. Vrai si rien n'a changé.
     */
    private boolean appliquerContrat(Part part) {
        Echange echange = part.echange;
        if (echange.contrat == null) {
            return true;
        }
        fr.kalium.economy.api.ContratsEchange service = contrats();
        Part autre = part.autre();
        fr.kalium.economy.api.ContratsEchange.Tri tri = service == null ? null
                : service.trier(echange.contrat, part.joueur, autre.joueur, copie(part.objetsDeposes()));
        long max = service == null ? -1 : service.pointsMax(echange.contrat, part.joueur, autre.joueur);
        if (tri == null || max < 0) {
            echange.contrat = null;
            echange.contratChoisiPar = null;
            Component plus = lang.c("echange.contrat-invalide", "<red>Ce contrat n'est plus valable : échange normal.");
            dire(part.joueur, plus);
            dire(autre.joueur, plus);
            return false;
        }
        boolean inchange = true;
        if (!tri.refuses().isEmpty()) {
            for (int i = 0; i < CASES_OBJETS; i++) {
                part.objets.setItem(i, null);
            }
            for (int i = 0; i < tri.acceptes().size() && i < CASES_OBJETS; i++) {
                part.objets.setItem(i, tri.acceptes().get(i));
            }
            donner(part.joueur, tri.refuses());
            dire(part.joueur, lang.c("echange.contrat-rendus", "<yellow>Objets rendus : ta partie ne les doit pas dans "
                    + "ce contrat (ou ils dépassent ce qui reste à livrer)."));
            // 1.12.1 : l'autre joueur voit que ces objets ne font pas partie de l'échange.
            dire(autre.joueur, lang.c("echange.contrat-rendus-autre", "<yellow>Les objets de <joueur> qui ne sont pas "
                    + "dus dans ce contrat lui sont rendus : ils ne font pas partie de l'échange.", "joueur",
                    part.joueur.getName()));
            inchange = false;
        }
        if (part.montant > max) {
            part.montant = max;
            dire(part.joueur, lang.c("echange.contrat-montant", "<yellow>Montant ramené à <montant> : ce qui reste à "
                    + "livrer du contrat.", "montant", KSEconomy.points(max)));
            inchange = false;
        }
        return inchange;
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
                return;
            }
            // 1.12.1 : avec un contrat, un objet que sa partie ne doit pas ne se pose pas.
            if (part.echange.contrat != null) {
                ItemStack pose = objetPose(event);
                if (pose != null && !pose.getType().isAir() && !dansLeContrat(part, pose)) {
                    event.setCancelled(true);
                    Contenant.message(part.joueur, lang.c("echange.contrat-refuse", "<red>Ta partie ne doit pas cet "
                            + "objet dans ce contrat (ou tout est déjà livré)."), true);
                    return;
                }
                // Quantité en trop, ou clic d'un autre genre (bundle...) : rendu juste après le clic.
                Bukkit.getScheduler().runTask(plugin, () -> rogner(part));
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
            case CONTRAT -> {
                if (contrats() != null) {
                    modifie(part.echange);
                    choisirContrat(part);
                }
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

    /** 1.12.1 : objet qu'un clic pose dans le coffre des objets (null : aucun, ou impossible à savoir). */
    private static ItemStack objetPose(InventoryClickEvent event) {
        boolean dansHaut = event.getClickedInventory() == event.getView().getTopInventory();
        return switch (event.getAction()) {
            case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> dansHaut ? event.getCursor() : null;
            case MOVE_TO_OTHER_INVENTORY -> dansHaut ? null : event.getCurrentItem();
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> !dansHaut ? null : event.getHotbarButton() >= 0
                    ? event.getWhoClicked().getInventory().getItem(event.getHotbarButton())
                    : event.getWhoClicked().getInventory().getItemInOffHand();
            default -> null;
        };
    }

    /** 1.12.1 : cet objet (un seul) peut-il s'ajouter à ce que le joueur a déjà posé, au titre du contrat ? */
    private boolean dansLeContrat(Part part, ItemStack objet) {
        fr.kalium.economy.api.ContratsEchange service = contrats();
        if (service == null || part.echange.contrat == null) {
            return true;
        }
        List<ItemStack> essai = copie(part.objetsDeposes());
        ItemStack un = objet.clone();
        un.setAmount(1);
        essai.add(un);
        fr.kalium.economy.api.ContratsEchange.Tri tri = service.trier(part.echange.contrat, part.joueur,
                part.autre().joueur, essai);
        return tri != null && tri.refuses().isEmpty();
    }

    /** 1.12.1 : juste après un clic dans le coffre des objets : ce qui n'est pas dû (ou en trop) est rendu. */
    private void rogner(Part part) {
        if (!part.echange.fini && part.echange.contrat != null && part.vue == Vue.OBJETS) {
            appliquerContrat(part);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Part part)) {
            return;
        }
        // 1.12.1 : glisser un objet que sa partie ne doit pas dans le coffre des objets : refusé.
        if (event.getView().getTopInventory() == part.objets && part.echange.contrat != null
                && event.getRawSlots().stream().anyMatch(c -> c < CASES_OBJETS)) {
            if (!dansLeContrat(part, event.getOldCursor())) {
                event.setCancelled(true);
                Contenant.message(part.joueur, lang.c("echange.contrat-refuse", "<red>Ta partie ne doit pas cet "
                        + "objet dans ce contrat (ou tout est déjà livré)."), true);
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> rogner(part));
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
        // 1.12.0 : menu du choix d'un contrat fermé (Échap) : retour au panneau.
        if (event.getInventory().getHolder() instanceof Contenant
                && parts.get(event.getPlayer().getUniqueId()) instanceof Part enChoix && enChoix.vue == Vue.CONTRAT) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!enChoix.echange.fini && enChoix.vue == Vue.CONTRAT && enChoix.joueur.isOnline()
                        && Contenant.ouvert(enChoix.joueur, "echange-contrat") == null) {
                    ouvrirPanneau(enChoix);
                }
            });
            return;
        }
        if (!(event.getInventory().getHolder() instanceof Part part) || part.echange.fini) {
            return;
        }
        if (event.getInventory() == part.objets && part.vue == Vue.OBJETS) {
            // Retour au panneau ; une modification des objets retire les validations.
            // 1.12.0 : avec un contrat, les objets qui ne sont pas dus sont rendus.
            appliquerContrat(part);
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
        // 1.0.3 : boutons élargis à leur texte, qui ne défile jamais (Lisible de KLM_Menu 2.5.0).
        fr.kalium.menu.api.Lisible.Fenetre fenetre = fr.kalium.menu.api.Lisible.ajuster(List.of(),
                List.of(valider, retour), null, 2);
        part.joueur.showDialog(Dialog.create(f -> f.empty().base(base)
                .type(DialogType.multiAction(fenetre.boutons(), null, fenetre.colonnes()))));
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
            fr.kalium.economy.api.ContratsEchange service = contrats();
            long max = part.echange.contrat == null || service == null ? Long.MAX_VALUE
                    : service.pointsMax(part.echange.contrat, part.joueur, part.autre().joueur);
            if (montant < 0 || montant > solde) {
                dire(part.joueur, lang.c("montant.invalide", "<red>Montant invalide (entre 0 et ton solde : <solde>).",
                        "solde", KSEconomy.points(solde)));
            } else if (montant > Math.max(0, max)) {
                dire(part.joueur, lang.c("montant.contrat", "<red>Le contrat ne demande plus que <max> à ta partie.",
                        "max", KSEconomy.points(Math.max(0, max))));
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
        demandes.values().forEach(recues -> recues.values().removeIf(expire -> expire < maintenant));
        demandes.values().removeIf(Map::isEmpty);
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
        // 1.12.0 : le contrat a pu changer depuis le dépôt (livraison d'un coéquipier, litige) : tout est revérifié.
        boolean contratInchange = appliquerContrat(a);
        contratInchange &= appliquerContrat(b);
        if (!contratInchange) {
            modifie(echange);
            return;
        }
        String contrat = echange.contrat;
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
        // 1.1.5 : signal pour l'anti-triche (revente suspecte).
        Bukkit.getPluginManager().callEvent(new fr.kalium.economy.api.EchangeTermineEvent(a.joueur, b.joueur, objetsA,
                objetsB, a.montant, b.montant));
        fr.kalium.economy.api.ContratsEchange service = contrats();
        if (contrat != null && service != null) {
            service.livrer(contrat, a.joueur, b.joueur, copie(objetsA), a.montant);
            service.livrer(contrat, b.joueur, a.joueur, copie(objetsB), b.montant);
        }
        Component fait = lang.c("echange.fait", "<green>Échange effectué.");
        dire(a.joueur, fait);
        dire(b.joueur, fait);
        plugin.getLogger().info("Échange : " + a.joueur.getName() + " (" + objetsA.size() + " piles, " + a.montant
                + " points) <-> " + b.joueur.getName() + " (" + objetsB.size() + " piles, " + b.montant + " points)"
                + (contrat == null ? "" : ", contrat " + contrat));
        lang.saveIfNeeded();
    }
}
