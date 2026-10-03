package fr.kalium.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KLM_Chat 1.0.0 (demande de LeKiwi06, 03/10/2026) : tchat inter-serveur, present sur tous les serveurs Paper.
 * - /global send <message> : envoie le message aux joueurs de tous les serveurs ; /global false masque le tchat
 *   global pour soi, /global true le reaffiche (affiche par defaut).
 * - Un message global porte le prefixe du serveur de son auteur, dans la couleur de son portail, et son contenu est en
 *   gris clair : le tchat du serveur reste blanc.
 * - Entre le tchat du serveur et le tchat global, une ligne de separation (« GLOBAL » ou le nom du serveur) chaque fois
 *   qu'on passe de l'un a l'autre.
 *
 * Transport : canal BungeeCord « Forward » vers tous les serveurs (comme la navigation de KLM_Menu), supporte par
 * Velocity. Il n'est livre qu'aux serveurs ou un joueur est connecte, ce qui suffit : un tchat sans lecteur n'a pas
 * besoin d'arriver. Le proxy n'accepte ce canal que des serveurs, jamais d'un joueur : un message ne peut pas etre
 * fabrique par un client.
 */
public final class KlmChat extends JavaPlugin implements Listener, PluginMessageListener, CommandExecutor, TabCompleter {

    private static final String CANAL = "BungeeCord";
    private static final String SOUS_CANAL = "KLMChat";
    private static final int FORMAT = 1;

    /** Nom affiche et couleur d'un serveur. */
    private record Serveur(String nom, TextColor couleur) {
    }

    /** Serveurs connus (nom dans velocity.toml, en minuscules) : valeurs par defaut, completees par config.yml. */
    private static final Map<String, Serveur> SERVEURS_PAR_DEFAUT = Map.of(
            "lobby", new Serveur("Lobby", TextColor.color(0x09add3)),
            "kal-games", new Serveur("Kal-Games", NamedTextColor.GOLD),
            "kixster", new Serveur("Kixster", NamedTextColor.GREEN),
            "event", new Serveur("Event", NamedTextColor.RED),
            "kanvas", new Serveur("Kanvas", NamedTextColor.LIGHT_PURPLE),
            "serveur-jeux", new Serveur("Bingo", NamedTextColor.YELLOW));

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, Serveur> serveurs = new HashMap<>();
    /** Joueurs qui ont masque le tchat global (/global false) sur ce serveur. */
    private final Set<UUID> masques = ConcurrentHashMap.newKeySet();
    /** Dernier tchat montre a chaque joueur : true = global, false = serveur (absent : rien encore). */
    private final Map<UUID, Boolean> dernierGlobal = new ConcurrentHashMap<>();
    /** Nom de CE serveur dans velocity.toml : config.yml, sinon demande au proxy a l'arrivee d'un joueur. */
    private volatile String nomServeur = "";
    private int longueurMax;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        charger();
        chargerMasques();
        getServer().getMessenger().registerOutgoingPluginChannel(this, CANAL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CANAL, this);
        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand commande = getCommand("global");
        if (commande != null) {
            commande.setExecutor(this);
            commande.setTabCompleter(this);
        }
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    /** Toute cle a sa valeur par defaut ici : un config.yml deja en place n'est jamais complete. */
    private void charger() {
        nomServeur = getConfig().getString("server-name", "").trim();
        longueurMax = Math.max(1, Math.min(256, getConfig().getInt("max-length", 256)));
        serveurs.clear();
        serveurs.putAll(SERVEURS_PAR_DEFAUT);
        ConfigurationSection section = getConfig().getConfigurationSection("servers");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                Serveur defaut = serveurs.get(id.toLowerCase(Locale.ROOT));
                String nom = section.getString(id + ".name", defaut == null ? id : defaut.nom());
                TextColor couleur = couleur(section.getString(id + ".color"));
                if (couleur == null) {
                    couleur = defaut == null ? NamedTextColor.WHITE : defaut.couleur();
                }
                serveurs.put(id.toLowerCase(Locale.ROOT), new Serveur(nom, couleur));
            }
        }
    }

    /** Couleur « #rrggbb » ou nom Minecraft (gold, red...) ; null si absente ou inconnue. */
    private TextColor couleur(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        String valeur = texte.trim().toLowerCase(Locale.ROOT);
        TextColor couleur = valeur.startsWith("#") ? TextColor.fromHexString(valeur) : NamedTextColor.NAMES.value(valeur);
        if (couleur == null) {
            getLogger().warning("Couleur inconnue dans config.yml : " + texte);
        }
        return couleur;
    }

    private Serveur serveur(String id) {
        Serveur connu = serveurs.get(id.toLowerCase(Locale.ROOT));
        if (connu != null) {
            return connu;
        }
        return new Serveur(id.isBlank() ? texte("unknown-server") : id, NamedTextColor.WHITE);
    }

    // ------------------------------------------------------------------ /global

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player joueur)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }
        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "send" -> {
                String message = nettoyer(String.join(" ", Arrays.copyOfRange(args, 1, args.length)), longueurMax);
                if (message.isEmpty()) {
                    joueur.sendMessage(mm.deserialize(texte("usage")));
                    return true;
                }
                envoyer(joueur, message);
            }
            case "true" -> {
                if (masques.remove(joueur.getUniqueId())) {
                    sauverMasques();
                }
                joueur.sendMessage(mm.deserialize(texte("shown")));
            }
            case "false" -> {
                if (masques.add(joueur.getUniqueId())) {
                    sauverMasques();
                }
                dernierGlobal.put(joueur.getUniqueId(), Boolean.FALSE);
                joueur.sendMessage(mm.deserialize(texte("hidden")));
            }
            default -> joueur.sendMessage(mm.deserialize(texte("usage")));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("send", "true", "false")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    options.add(option);
                }
            }
        }
        return options;
    }

    /** Retire les caracteres de controle et le « § » (codes de couleur), puis coupe a la longueur maximale. */
    private static String nettoyer(String texte, int longueurMax) {
        StringBuilder propre = new StringBuilder();
        for (int i = 0; i < texte.length() && propre.length() < longueurMax; i++) {
            char c = texte.charAt(i);
            if (c != '§' && !Character.isISOControl(c)) {
                propre.append(c);
            }
        }
        return propre.toString().trim();
    }

    private void envoyer(Player joueur, String message) {
        String origine = nomServeur;
        ByteArrayOutputStream contenu = new ByteArrayOutputStream();
        ByteArrayOutputStream paquet = new ByteArrayOutputStream();
        try {
            DataOutputStream out = new DataOutputStream(contenu);
            out.writeByte(FORMAT);
            out.writeUTF(origine);
            out.writeUTF(joueur.getName());
            out.writeUTF(message);
            byte[] octets = contenu.toByteArray();
            DataOutputStream enveloppe = new DataOutputStream(paquet);
            enveloppe.writeUTF("Forward");
            enveloppe.writeUTF("ALL"); // tous les autres serveurs ; celui-ci affiche le message lui-meme
            enveloppe.writeUTF(SOUS_CANAL);
            enveloppe.writeShort(octets.length);
            enveloppe.write(octets);
        } catch (IOException e) {
            getLogger().warning("Message global impossible à construire : " + e.getMessage());
            return;
        }
        joueur.sendPluginMessage(this, CANAL, paquet.toByteArray());
        afficher(origine, joueur.getName(), message);
        if (masques.contains(joueur.getUniqueId())) {
            joueur.sendMessage(mm.deserialize(texte("sent-while-hidden")));
        }
    }

    /** Montre un message global aux joueurs de ce serveur qui ne l'ont pas masque (et le note dans la console). */
    private void afficher(String origine, String pseudo, String message) {
        Serveur serveur = serveur(origine);
        Component ligne = mm.deserialize(texte("format"),
                Placeholder.component("prefix", Component.text("[" + serveur.nom() + "]", serveur.couleur())),
                Placeholder.unparsed("player", pseudo),
                Placeholder.unparsed("message", message));
        Component separation = mm.deserialize(texte("separator-global"));
        for (Player lecteur : getServer().getOnlinePlayers()) {
            if (masques.contains(lecteur.getUniqueId())) {
                continue;
            }
            if (!Boolean.TRUE.equals(dernierGlobal.put(lecteur.getUniqueId(), Boolean.TRUE))) {
                lecteur.sendMessage(separation);
            }
            lecteur.sendMessage(ligne);
        }
        getLogger().info("[GLOBAL] [" + serveur.nom() + "] " + pseudo + " : " + message);
    }

    // ------------------------------------------------------------------ separation avec le tchat du serveur

    /**
     * Un message du tchat de ce serveur va etre montre : les lecteurs qui venaient de lire du tchat global recoivent
     * d'abord la ligne au nom du serveur. Appele avant l'envoi du message, sur le fil du tchat.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Component separation = null;
        for (Audience lecteur : event.viewers()) {
            if (lecteur instanceof Player joueur
                    && Boolean.TRUE.equals(dernierGlobal.put(joueur.getUniqueId(), Boolean.FALSE))) {
                if (separation == null) {
                    Serveur ici = serveur(nomServeur);
                    separation = mm.deserialize(texte("separator-server"),
                            Placeholder.component("server", Component.text(ici.nom(), ici.couleur())));
                }
                joueur.sendMessage(separation);
            }
        }
    }

    // ------------------------------------------------------------------ canal BungeeCord / Velocity

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!nomServeur.isEmpty()) {
            return;
        }
        // Le nom de ce serveur se demande au proxy par un joueur connecte (un peu apres son arrivee).
        Player joueur = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> {
            if (joueur.isOnline() && nomServeur.isEmpty()) {
                ByteArrayOutputStream paquet = new ByteArrayOutputStream();
                try {
                    new DataOutputStream(paquet).writeUTF("GetServer");
                } catch (IOException e) {
                    return;
                }
                joueur.sendPluginMessage(this, CANAL, paquet.toByteArray());
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dernierGlobal.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(String canal, Player porteur, byte[] message) {
        if (!CANAL.equals(canal)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String sousCanal = in.readUTF();
            if (sousCanal.equals("GetServer")) {
                if (nomServeur.isEmpty()) {
                    nomServeur = in.readUTF();
                    getLogger().info("Nom de ce serveur (proxy) : " + nomServeur);
                }
            } else if (sousCanal.equals(SOUS_CANAL)) {
                byte[] octets = new byte[in.readUnsignedShort()];
                in.readFully(octets);
                DataInputStream contenu = new DataInputStream(new ByteArrayInputStream(octets));
                if (contenu.readByte() != FORMAT) {
                    return; // message d'une version plus recente du plugin : ignore
                }
                String origine = nettoyer(contenu.readUTF(), 64);
                String pseudo = nettoyer(contenu.readUTF(), 32);
                String texte = nettoyer(contenu.readUTF(), 256);
                if (!pseudo.isEmpty() && !texte.isEmpty()) {
                    afficher(origine, pseudo, texte);
                }
            }
        } catch (IOException ignored) {
            // Message incomplet ou d'un autre plugin : on l'ignore.
        }
    }

    // ------------------------------------------------------------------ choix des joueurs, textes

    private File fichierMasques() {
        return new File(getDataFolder(), "masques.yml");
    }

    private void chargerMasques() {
        for (String valeur : YamlConfiguration.loadConfiguration(fichierMasques()).getStringList("joueurs")) {
            try {
                masques.add(UUID.fromString(valeur));
            } catch (IllegalArgumentException ignored) {
                // ligne abimee : ignoree
            }
        }
    }

    private void sauverMasques() {
        List<String> liste = new ArrayList<>();
        for (UUID uuid : masques) {
            liste.add(uuid.toString());
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("joueurs", liste);
        try {
            getDataFolder().mkdirs();
            yaml.save(fichierMasques());
        } catch (IOException e) {
            getLogger().warning("Impossible d'enregistrer masques.yml : " + e.getMessage());
        }
    }

    /** Texte MiniMessage de config.yml (messages.<cle>), avec sa valeur par defaut. */
    private String texte(String cle) {
        String valeur = getConfig().getString("messages." + cle);
        if (valeur != null) {
            return valeur;
        }
        return switch (cle) {
            case "format" -> "<prefix> <gray><player> : <message>";
            case "separator-global" -> "<dark_gray>----- <white>GLOBAL</white> -----";
            case "separator-server" -> "<dark_gray>----- <server> -----";
            case "unknown-server" -> "Serveur";
            case "usage" -> "<gray>Tchat inter-serveur : <white>/global send \\<message></white> pour écrire, <white>/global false</white> pour le masquer, <white>/global true</white> pour le réafficher.";
            case "shown" -> "<green>Tchat global affiché. <gray>/global false pour le masquer.";
            case "hidden" -> "<yellow>Tchat global masqué sur ce serveur. <gray>/global true pour le réafficher.";
            case "sent-while-hidden" -> "<gray>Message envoyé. Tu as masqué le tchat global : <white>/global true</white> pour voir les réponses.";
            default -> "<red>[" + cle + "]";
        };
    }
}
