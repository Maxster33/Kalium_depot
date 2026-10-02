package fr.kalium.rewards;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Interface admin de KG_Rewards (kgrewards.admin) : pools (entrées : contenu et poids) et niveaux de chaque période
 * (tirages, fréquence des pools, récompenses fixes). Contenu : objets déposés dans un coffre (vanilla ou non), objets
 * custom d'Event par id_custom (KS_KaliumGive, vérifiés à la récupération sur Event), argent (points du score).
 */
final class MenuAdmin implements Listener {

    /** Coffre de dépôt d'objets dans un contenu. */
    private record Depot(List<Map<String, Object>> cible, Consumer<Player> ensuite) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final KGRewards plugin;
    private final Butin butin;
    private final Lang lang;
    private final Gui gui;

    MenuAdmin(KGRewards plugin, Butin butin) {
        this.plugin = plugin;
        this.butin = butin;
        this.lang = plugin.lang();
        this.gui = plugin.gui();
    }

    private Component t(String cle, String defaut, Object... paires) {
        return lang.c(cle, defaut, paires);
    }

    private ActionButton retour(Consumer<Player> vers) {
        return gui.button(t("admin.retour", "<gray>Retour"), null, vers::accept);
    }

    private boolean admin(Player joueur) {
        return plugin.estAdmin(joueur);
    }

    private void sauver() {
        butin.sauver();
    }

    // ------------------------------------------------------------------ accueil

    void ouvrir(Player joueur) {
        if (!admin(joueur)) {
            return;
        }
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("admin.pools", "<white>Pools de butin"), null, this::pools));
        for (String periode : Butin.PERIODES) {
            boutons.add(gui.button(t("admin.niveaux-" + periode, "<white>Niveaux : <periode>", "periode",
                    Moteur.nomPeriode(periode)), null, p -> niveaux(p, periode)));
        }
        gui.open(joueur, t("admin.titre", "<red><bold>Tables de butin"),
                List.of(t("admin.aide", "<gray>Les pools (par rareté) sont communs ; chaque niveau de récompense de chaque "
                        + "période tire dans les pools selon ses fréquences, puis ajoute ses récompenses fixes.")),
                List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ pools

    private void pools(Player joueur) {
        List<ActionButton> boutons = new ArrayList<>();
        List<Component> corps = new ArrayList<>();
        butin.pools.forEach((nom, entrees) -> {
            corps.add(t("admin.pool-ligne", "<white><nom> <gray>: <n> entrée(s)", "nom", nom, "n", entrees.size()));
            boutons.add(gui.button(Component.text(nom), null, p -> pool(p, nom)));
        });
        boutons.add(gui.button(t("admin.creer-pool", "<green>Créer un pool"), null, this::creerPool));
        boutons.add(retour(this::ouvrir));
        gui.open(joueur, t("admin.titre-pools", "<red><bold>Pools de butin"), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void creerPool(Player joueur) {
        ActionButton creer = gui.form(t("admin.creer", "<green>Créer"), null, (p, vue) -> {
            String nom = vue.getText("nom") == null ? "" : vue.getText("nom").trim();
            if (!nom.isEmpty() && !nom.contains(".") && !butin.pools.containsKey(nom)) {
                butin.pools.put(nom, new ArrayList<>());
                sauver();
            }
            pools(p);
        });
        gui.open(joueur, t("admin.titre-creer-pool", "<red><bold>Nouveau pool"), List.of(),
                List.of(gui.text("nom", t("admin.champ-nom-pool", "Nom (ex. Rare)"), "", 24)),
                List.of(creer, retour(this::pools)), gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void pool(Player joueur, String nom) {
        List<Butin.Entree> entrees = butin.pools.get(nom);
        if (entrees == null) {
            pools(joueur);
            return;
        }
        List<Component> corps = new ArrayList<>();
        List<ActionButton> boutons = new ArrayList<>();
        int total = entrees.stream().mapToInt(e -> Math.max(0, e.poids)).sum();
        for (int i = 0; i < entrees.size(); i++) {
            Butin.Entree e = entrees.get(i);
            int index = i;
            corps.add(t("admin.entree-ligne", "<white><n>. <gray>poids <poids> (<chance> %) : ", "n", i + 1, "poids",
                    e.poids, "chance", total == 0 ? 0 : Math.round(100.0 * Math.max(0, e.poids) / total))
                    .append(description(e.contenu)));
            boutons.add(gui.button(t("admin.bouton-entree", "<white>Entrée <n>", "n", i + 1), null, p -> entree(p, nom, index)));
        }
        if (entrees.isEmpty()) {
            corps.add(t("admin.pool-vide", "<gray>Aucune entrée."));
        }
        boutons.add(gui.button(t("admin.ajouter-entree", "<green>Ajouter une entrée"), null, p -> {
            entrees.add(new Butin.Entree());
            sauver();
            entree(p, nom, entrees.size() - 1);
        }));
        boutons.add(gui.button(t("admin.supprimer-pool", "<red>Supprimer le pool"), null,
                p -> gui.confirm(p, t("admin.titre-supprimer-pool", "<red><bold>Supprimer le pool <nom>", "nom", nom),
                        t("admin.texte-supprimer-pool", "<white>Ses entrées sont perdues ; les niveaux ne tirent plus dans ce pool."),
                        q -> {
                            butin.pools.remove(nom);
                            sauver();
                            pools(q);
                        }, q -> pool(q, nom))));
        boutons.add(retour(this::pools));
        gui.open(joueur, t("admin.titre-pool", "<red><bold>Pool <nom>", "nom", nom), corps, List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void entree(Player joueur, String pool, int index) {
        List<Butin.Entree> entrees = butin.pools.get(pool);
        if (entrees == null || index >= entrees.size()) {
            pools(joueur);
            return;
        }
        Butin.Entree e = entrees.get(index);
        ActionButton poids = gui.form(t("admin.enregistrer-poids", "<green>Enregistrer le poids"), null, (p, vue) -> {
            try {
                e.poids = Math.max(0, Integer.parseInt(vue.getText("poids").trim()));
                sauver();
            } catch (RuntimeException ex) {
                // poids invalide : inchangé
            }
            entree(p, pool, index);
        });
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(poids);
        boutons.add(gui.button(t("admin.contenu", "<white>Contenu"), null,
                p -> contenu(p, t("admin.titre-contenu-entree", "<red><bold>Entrée <n> (<pool>)", "n", index + 1, "pool", pool),
                        e.contenu, q -> entree(q, pool, index))));
        boutons.add(gui.button(t("admin.supprimer-entree", "<red>Supprimer l'entrée"), null, p -> {
            entrees.remove(index);
            sauver();
            pool(p, pool);
        }));
        boutons.add(retour(p -> pool(p, pool)));
        gui.open(joueur, t("admin.titre-entree", "<red><bold>Entrée <n> (<pool>)", "n", index + 1, "pool", pool),
                List.of(t("admin.contenu-actuel", "<white>Contenu : ").append(description(e.contenu))),
                List.of(gui.text("poids", t("admin.champ-poids", "Poids"), String.valueOf(e.poids), 6)), boutons,
                gui.close(), 1);
        lang.saveIfNeeded();
    }

    // ------------------------------------------------------------------ niveaux

    private void niveaux(Player joueur, String periode) {
        List<ActionButton> boutons = new ArrayList<>();
        Butin.NIVEAUX.forEach((id, nom) -> boutons.add(gui.button(Component.text(nom), null, p -> niveau(p, periode, id))));
        boutons.add(retour(this::ouvrir));
        gui.open(joueur, t("admin.titre-niveaux", "<red><bold>Niveaux : <periode>", "periode", Moteur.nomPeriode(periode)),
                List.of(), List.of(), boutons, gui.close(), 2);
        lang.saveIfNeeded();
    }

    private void niveau(Player joueur, String periode, String id) {
        Butin.Niveau n = butin.niveau(periode, id);
        List<Component> corps = new ArrayList<>();
        corps.add(t("admin.tirages", "<white>Tirages : <n>", "n", n.tirages));
        StringBuilder freq = new StringBuilder();
        butin.pools.keySet().forEach(pool -> freq.append(freq.isEmpty() ? "" : ", ").append(pool).append(" ")
                .append(n.frequences.getOrDefault(pool, 0)).append(" %"));
        corps.add(t("admin.frequences", "<white>Fréquences : <gray><liste>", "liste", freq.toString()));
        corps.add(t("admin.fixes", "<white>Fixes : ").append(description(n.fixes)));
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("admin.bouton-tirages", "<white>Tirages et fréquences"), null,
                p -> tirages(p, periode, id)));
        boutons.add(gui.button(t("admin.bouton-fixes", "<white>Récompenses fixes"), null,
                p -> contenu(p, t("admin.titre-fixes", "<red><bold>Fixes : <niveau>", "niveau", Butin.NIVEAUX.get(id)),
                        n.fixes, q -> niveau(q, periode, id))));
        boutons.add(retour(p -> niveaux(p, periode)));
        gui.open(joueur, t("admin.titre-niveau", "<red><bold><niveau> (<periode>)", "niveau", Butin.NIVEAUX.get(id),
                "periode", Moteur.nomPeriode(periode)), corps, List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void tirages(Player joueur, String periode, String id) {
        Butin.Niveau n = butin.niveau(periode, id);
        List<DialogInput> champs = new ArrayList<>();
        champs.add(gui.text("tirages", t("admin.champ-tirages", "Nombre de tirages"), String.valueOf(n.tirages), 3));
        List<String> pools = new ArrayList<>(butin.pools.keySet());
        for (int i = 0; i < pools.size(); i++) {
            champs.add(gui.text("p" + i, Component.text(pools.get(i) + " (%)"),
                    String.valueOf(n.frequences.getOrDefault(pools.get(i), 0)), 3));
        }
        ActionButton enregistrer = gui.form(t("admin.enregistrer", "<green>Enregistrer"), null, (p, vue) -> {
            n.tirages = entier(vue.getText("tirages"), n.tirages);
            for (int i = 0; i < pools.size(); i++) {
                n.frequences.put(pools.get(i), entier(vue.getText("p" + i), n.frequences.getOrDefault(pools.get(i), 0)));
            }
            sauver();
            niveau(p, periode, id);
        });
        gui.open(joueur, t("admin.titre-tirages", "<red><bold>Tirages et fréquences"),
                List.of(t("admin.aide-tirages", "<gray>À chaque tirage, un pool est choisi selon les fréquences (elles sont "
                        + "ramenées à 100 % si leur total diffère), puis une entrée du pool selon les poids.")),
                champs, List.of(enregistrer, retour(p -> niveau(p, periode, id))), gui.close(), 1);
        lang.saveIfNeeded();
    }

    private static int entier(String texte, int defaut) {
        try {
            return Math.max(0, Integer.parseInt(texte == null ? "" : texte.trim()));
        } catch (NumberFormatException e) {
            return defaut;
        }
    }

    // ------------------------------------------------------------------ contenu

    private void contenu(Player joueur, Component titre, List<Map<String, Object>> liste, Consumer<Player> retour) {
        List<ActionButton> boutons = new ArrayList<>();
        boutons.add(gui.button(t("admin.deposer", "<white>Déposer des objets"), null, p -> {
            Inventory coffre = Bukkit.createInventory(new Depot(liste, q -> contenu(q, titre, liste, retour)), 27,
                    t("admin.titre-deposer", "Objets de la récompense"));
            p.openInventory(coffre);
        }));
        boutons.add(gui.button(t("admin.custom", "<white>Objet custom (id)"), null, p -> ajouterCustom(p, titre, liste, retour)));
        boutons.add(gui.button(t("admin.argent", "<white>Argent (points)"), null, p -> ajouterArgent(p, titre, liste, retour)));
        if (!liste.isEmpty()) {
            boutons.add(gui.button(t("admin.retirer-dernier", "<red>Retirer le dernier"), null, p -> {
                liste.remove(liste.size() - 1);
                sauver();
                contenu(p, titre, liste, retour);
            }));
        }
        boutons.add(retour(retour));
        gui.open(joueur, titre, List.of(t("admin.contenu-actuel", "<white>Contenu : ").append(description(liste)),
                t("admin.aide-deposer", "<gray>Déposer : les objets posés dans le coffre sont ajoutés (copie exacte) puis "
                        + "te sont rendus.")), List.of(), boutons, gui.close(), 1);
        lang.saveIfNeeded();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Depot depot) || !(event.getPlayer() instanceof Player joueur)) {
            return;
        }
        for (ItemStack objet : event.getInventory().getContents()) {
            if (objet == null || objet.getType().isAir()) {
                continue;
            }
            ItemStack modele = objet.clone();
            int nombre = modele.getAmount();
            modele.setAmount(1);
            Map<String, Object> el = new LinkedHashMap<>();
            el.put("type", "objet");
            el.put("donnees", Base64.getEncoder().encodeToString(modele.serializeAsBytes()));
            el.put("nombre", nombre);
            depot.cible().add(el);
            for (ItemStack reste : joueur.getInventory().addItem(objet).values()) {
                joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste);
            }
        }
        event.getInventory().clear();
        sauver();
        Bukkit.getScheduler().runTask(plugin, () -> depot.ensuite().accept(joueur));
    }

    private void ajouterCustom(Player joueur, Component titre, List<Map<String, Object>> liste, Consumer<Player> retour) {
        ActionButton ajouter = gui.form(t("admin.ajouter", "<green>Ajouter"), null, (p, vue) -> {
            String id = vue.getText("id") == null ? "" : vue.getText("id").trim().toLowerCase();
            if (!id.isEmpty()) {
                Map<String, Object> el = new LinkedHashMap<>();
                el.put("type", "custom");
                el.put("id", id);
                el.put("nombre", Math.max(1, entier(vue.getText("nombre"), 1)));
                liste.add(el);
                sauver();
            }
            contenu(p, titre, liste, retour);
        });
        gui.open(joueur, t("admin.titre-custom", "<red><bold>Objet custom"),
                List.of(t("admin.aide-custom", "<gray>id_custom de /kaliumgive sur Event (ex. elixir_titan, "
                        + "tete_mouton_rouge, fiole_exp(10)) ; vérifié à la récupération sur Event.")),
                List.of(gui.text("id", t("admin.champ-id", "id_custom"), "", 48),
                        gui.text("nombre", t("admin.champ-nombre", "Nombre"), "1", 5)),
                List.of(ajouter, retour(p -> contenu(p, titre, liste, retour))), gui.close(), 1);
        lang.saveIfNeeded();
    }

    private void ajouterArgent(Player joueur, Component titre, List<Map<String, Object>> liste, Consumer<Player> retour) {
        ActionButton ajouter = gui.form(t("admin.ajouter", "<green>Ajouter"), null, (p, vue) -> {
            int montant = entier(vue.getText("montant"), 0);
            if (montant > 0) {
                Map<String, Object> el = new LinkedHashMap<>();
                el.put("type", "argent");
                el.put("montant", montant);
                liste.add(el);
                sauver();
            }
            contenu(p, titre, liste, retour);
        });
        gui.open(joueur, t("admin.titre-argent", "<red><bold>Argent"), List.of(),
                List.of(gui.text("montant", t("admin.champ-montant", "Points"), "", 9)),
                List.of(ajouter, retour(p -> contenu(p, titre, liste, retour))), gui.close(), 1);
        lang.saveIfNeeded();
    }

    /** « 3 x Diamant, 2 x elixir_titan, 500 points » (noms traduits par le jeu). */
    static Component description(List<Map<String, Object>> liste) {
        if (liste.isEmpty()) {
            return Component.text("rien");
        }
        Component sortie = Component.empty();
        boolean premier = true;
        for (Map<String, Object> el : liste) {
            Component partie;
            switch (String.valueOf(el.get("type"))) {
                case "objet" -> {
                    Component nom;
                    try {
                        ItemStack objet = ItemStack.deserializeBytes(Base64.getDecoder().decode(String.valueOf(el.get("donnees"))));
                        nom = objet.hasItemMeta() && objet.getItemMeta().hasDisplayName() ? objet.getItemMeta().displayName()
                                : Component.translatable(objet.translationKey());
                    } catch (RuntimeException e) {
                        nom = Component.text("objet illisible");
                    }
                    partie = Component.text(Butin.nombre(el.get("nombre")) + " x ").append(nom);
                }
                case "custom" -> partie = Component.text(Butin.nombre(el.get("nombre")) + " x " + el.get("id"));
                case "argent" -> partie = Component.text(Butin.nombre(el.get("montant")) + " points");
                default -> partie = Component.text(String.valueOf(el.get("type")));
            }
            sortie = sortie.append(premier ? Component.empty() : Component.text(", ")).append(partie);
            premier = false;
        }
        return sortie;
    }
}
