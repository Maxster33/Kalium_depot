package fr.kalium.claim;

import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import fr.xyness.SCS.API.SimpleClaimSystemAPI;
import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.WorldMode;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * KS_Claim (cahier des charges : catégorie 3 « Claims », LeKiwi06, 30/09/2026) : interface des claims du serveur Event,
 * sur le moteur SimpleClaimSystem (SCS, piloté par son API ; ses menus et commandes ne sont plus proposés aux joueurs).
 *
 * - 1 claim = 1 chunk. Prix du n-ième claim (n = claims possédés + 1) : gratuit jusqu'à 10, puis 16n + n²/2 émeraudes
 *   du score (KS_Economy), arrondi à l'unité supérieure ; 100 claims au plus.
 * - Supprimer un claim rembourse le dernier prix payé (liste des prix payés de chaque joueur).
 * - Groupes de claims : membres et réglages modifiés sur le groupe s'appliquent à tous ses claims.
 * - Vente entre joueurs : refusée si le prix est inférieur au prochain prix de claim de l'acheteur (sauf sous 10 claims).
 *
 * Données : plugins/KS_Claim/donnees.yml (prix payés, groupes).
 */
public final class KSClaim extends JavaPlugin {

    /** Un groupe de claims d'un joueur : nom et ids des claims (id de SCS, propre au propriétaire). */
    static final class Groupe {
        String nom;
        final List<Integer> claims = new ArrayList<>();
    }

    /** Données d'un joueur. */
    static final class Joueur {
        final List<Long> prixPayes = new ArrayList<>();
        final Map<String, Groupe> groupes = new LinkedHashMap<>();
    }

    private final Map<UUID, Joueur> joueurs = new LinkedHashMap<>();
    private File fichier;
    private Lang lang;
    private Gui gui;
    private Menus menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        gui = new Gui(this, lang);
        fichier = new File(getDataFolder(), "donnees.yml");
        charger();
        menus = new Menus(this);
        getCommand("ksclaim").setExecutor(this);
        getCommand("ksclaims").setExecutor(this);
        if (getServer().getPluginManager().isPluginEnabled("KS_Menu")) {
            fr.kalium.ksmenu.KSMenu.ajouterBouton(this, "claims", lang.c("bouton.nom", "<aqua><bold>Claims"),
                    lang.c("bouton.description", "<gray>Tes claims, leurs membres et leurs réglages"), 20, menus::ouvrir);
        }
        // 1.1.2 : plus de bannissement dans les claims (LeKiwi06, 03/10/2026) : ceux déjà posés sont levés au démarrage
        // (sinon personne ne pourrait plus les lever).
        getServer().getScheduler().runTaskLater(this, this::leverBannissements, 100L);
        lang.saveIfNeeded();
    }

    private void leverBannissements() {
        java.util.List<java.util.Map.Entry<fr.xyness.SCS.Types.Claim, String>> bans = new java.util.ArrayList<>();
        for (fr.xyness.SCS.Types.Claim claim : api().getAllClaims()) {
            for (java.util.UUID banni : claim.getBans()) {
                String pseudo = Bukkit.getOfflinePlayer(banni).getName();
                if (pseudo != null) {
                    bans.add(java.util.Map.entry(claim, pseudo));
                }
            }
        }
        if (bans.isEmpty()) {
            return;
        }
        async(() -> bans.forEach(e -> api().unbanPlayerFromClaim(e.getKey(), e.getValue())),
                () -> getLogger().info(bans.size() + " bannissement(s) de claim levé(s) (plus de bannissement depuis 1.1.2)."));
    }

    @Override
    public void onDisable() {
        sauver();
    }

    Lang lang() {
        return lang;
    }

    Gui gui() {
        return gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("ksclaim")) {
            menus.claimerIci(joueur);
        } else {
            menus.ouvrir(joueur);
        }
        return true;
    }

    // ------------------------------------------------------------------ SCS

    static SimpleClaimSystemAPI api() {
        // 1.1.1 : SCS 1.13.1 ne crée pas son API lui-même ; c'est au plugin qui l'utilise de l'initialiser (sans
        // effet si c'est déjà fait).
        SimpleClaimSystemAPI_Provider.initialize(scs());
        return SimpleClaimSystemAPI_Provider.getAPI();
    }

    static SimpleClaimSystem scs() {
        return (SimpleClaimSystem) Bukkit.getPluginManager().getPlugin("SimpleClaimSystem");
    }

    /** Exécute une opération de SCS hors du fil principal (base de données), puis la suite sur le fil principal. */
    void async(Runnable operation, Runnable ensuite) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                operation.run();
            } catch (RuntimeException e) {
                getLogger().warning("Erreur de SimpleClaimSystem : " + e);
            }
            if (ensuite != null) {
                Bukkit.getScheduler().runTask(this, ensuite);
            }
        });
    }

    /** Claims d'un joueur, triés par id. */
    static List<Claim> claimsDe(Player joueur) {
        Set<Claim> claims = api().getPlayerClaims(joueur);
        List<Claim> liste = claims == null ? new ArrayList<>() : new ArrayList<>(claims);
        liste.sort(Comparator.comparingInt(Claim::getId));
        return liste;
    }

    static Claim claimParId(Player joueur, int id) {
        for (Claim claim : claimsDe(joueur)) {
            if (claim.getId() == id) {
                return claim;
            }
        }
        return null;
    }

    /** Nombre de claims possédés (achetés compris). */
    static int nombreDeClaims(UUID joueur) {
        return api().getPlayerClaimsCount(joueur);
    }

    /** Pourquoi ce chunk ne peut pas être claimé (texte), ou null s'il peut l'être (hors prix et nombre). */
    String refusChunk(Chunk chunk) {
        if (api().isClaimed(chunk)) {
            return "deja";
        }
        SimpleClaimSystem scs = scs();
        if (scs.getSettings().getWorldMode(chunk.getWorld().getName()) == WorldMode.DISABLED) {
            return "monde";
        }
        if (scs.getSettings().getBooleanSetting("worldguard") && !scs.getWorldGuard().checkFlagClaimInChunk(chunk)) {
            return "protege";
        }
        return null;
    }

    // ------------------------------------------------------------------ prix

    int claimsGratuits() {
        return Math.max(0, getConfig().getInt("claims-gratuits", 10));
    }

    int claimsMax() {
        return Math.max(1, getConfig().getInt("claims-max", 100));
    }

    /** Prix du n-ième claim : 0 jusqu'aux claims gratuits, puis 16n + n²/2 arrondi à l'unité supérieure. */
    long prix(int n) {
        if (n <= claimsGratuits()) {
            return 0;
        }
        return (long) Math.ceil(16.0 * n + n * (double) n / 2);
    }

    /** Prix du prochain claim du joueur. */
    long prochainPrix(UUID joueur) {
        return prix(nombreDeClaims(joueur) + 1);
    }

    synchronized void noterPrixPaye(UUID joueur, long prix) {
        if (prix > 0) {
            joueur(joueur).prixPayes.add(prix);
            sauver();
        }
    }

    /** Retire et renvoie le dernier prix payé (0 si aucun). */
    synchronized long retirerDernierPrix(UUID joueur) {
        List<Long> prix = joueur(joueur).prixPayes;
        if (prix.isEmpty()) {
            return 0;
        }
        long dernier = prix.remove(prix.size() - 1);
        sauver();
        return dernier;
    }

    // ------------------------------------------------------------------ groupes

    synchronized Joueur joueur(UUID joueur) {
        return joueurs.computeIfAbsent(joueur, u -> new Joueur());
    }

    /** Groupe d'un claim, ou null. */
    synchronized String groupeDuClaim(UUID proprietaire, int id) {
        for (Map.Entry<String, Groupe> entree : joueur(proprietaire).groupes.entrySet()) {
            if (entree.getValue().claims.contains(id)) {
                return entree.getKey();
            }
        }
        return null;
    }

    /** Range un claim dans un groupe (null : hors groupe) ; un claim n'est que dans un seul groupe. */
    synchronized void ranger(UUID proprietaire, int id, String groupe) {
        joueur(proprietaire).groupes.values().forEach(g -> g.claims.remove((Integer) id));
        if (groupe != null && joueur(proprietaire).groupes.containsKey(groupe)) {
            joueur(proprietaire).groupes.get(groupe).claims.add(id);
        }
        sauver();
    }

    /** Crée un groupe ; renvoie son identifiant, ou null si le nom existe déjà. */
    synchronized String creerGroupe(UUID proprietaire, String nom) {
        Joueur j = joueur(proprietaire);
        for (Groupe g : j.groupes.values()) {
            if (g.nom.equalsIgnoreCase(nom)) {
                return null;
            }
        }
        int numero = 1;
        while (j.groupes.containsKey("g" + numero)) {
            numero++;
        }
        Groupe groupe = new Groupe();
        groupe.nom = nom;
        j.groupes.put("g" + numero, groupe);
        sauver();
        return "g" + numero;
    }

    synchronized void supprimerGroupe(UUID proprietaire, String groupe) {
        joueur(proprietaire).groupes.remove(groupe);
        sauver();
    }

    synchronized void renommerGroupe(UUID proprietaire, String groupe, String nom) {
        Groupe g = joueur(proprietaire).groupes.get(groupe);
        if (g != null) {
            g.nom = nom;
            sauver();
        }
    }

    /** Claims (existants) d'un groupe. */
    List<Claim> claimsDuGroupe(Player proprietaire, String groupe) {
        Groupe g;
        synchronized (this) {
            g = joueur(proprietaire.getUniqueId()).groupes.get(groupe);
        }
        List<Claim> liste = new ArrayList<>();
        if (g != null) {
            for (Claim claim : claimsDe(proprietaire)) {
                if (g.claims.contains(claim.getId())) {
                    liste.add(claim);
                }
            }
        }
        return liste;
    }

    // ------------------------------------------------------------------ sauvegarde

    private synchronized void charger() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        ConfigurationSection section = yaml.getConfigurationSection("joueurs");
        if (section == null) {
            return;
        }
        for (String cle : section.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(cle);
            } catch (IllegalArgumentException e) {
                continue;
            }
            Joueur j = joueur(uuid);
            for (Object prix : section.getList(cle + ".prix-payes", List.of())) {
                if (prix instanceof Number nombre) {
                    j.prixPayes.add(nombre.longValue());
                }
            }
            ConfigurationSection groupes = section.getConfigurationSection(cle + ".groupes");
            if (groupes != null) {
                for (String id : groupes.getKeys(false)) {
                    Groupe g = new Groupe();
                    g.nom = groupes.getString(id + ".nom", id);
                    g.claims.addAll(groupes.getIntegerList(id + ".claims"));
                    j.groupes.put(id, g);
                }
            }
        }
    }

    synchronized void sauver() {
        YamlConfiguration yaml = new YamlConfiguration();
        joueurs.forEach((uuid, j) -> {
            String cle = "joueurs." + uuid;
            yaml.set(cle + ".prix-payes", new ArrayList<>(j.prixPayes));
            j.groupes.forEach((id, g) -> {
                yaml.set(cle + ".groupes." + id + ".nom", g.nom);
                yaml.set(cle + ".groupes." + id + ".claims", new ArrayList<>(g.claims));
            });
        });
        try {
            getDataFolder().mkdirs();
            yaml.save(fichier);
        } catch (IOException e) {
            getLogger().severe("Impossible d'enregistrer donnees.yml : " + e.getMessage());
        }
    }
}
