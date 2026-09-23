package fr.kalium.core.module;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registre des modules optionnels du menu (systeme de statistiques, systeme de claim, systeme de
 * commerce a venir, ...).
 * <p>
 * Un module s'enregistre une fois au demarrage du plugin avec un identifiant, son bouton de menu
 * (facultatif) et son etat par defaut. Son activation reelle est ensuite pilotee par config.yml
 * (section "modules.&lt;id&gt;.enabled") et modifiable en jeu depuis l'ecran Parametres, reserve aux
 * operateurs. Le menu principal et l'ecran Statistiques masquent automatiquement tout bouton / toute
 * ligne de statistique rattache a un module desactive (voir StatSection).
 */
public final class ModuleRegistry {

    /**
     * @param id             identifiant stable, utilise comme cle dans config.yml ("modules.&lt;id&gt;...")
     * @param displayName    nom affiche dans l'ecran Parametres
     * @param menuLabel      libelle du bouton dans le menu principal, ou null si ce module n'ajoute pas de bouton
     * @param menuDescription info-bulle du bouton, peut etre null
     * @param defaultEnabled etat par defaut si rien n'est precise dans config.yml
     * @param menuAction     action executee au clic sur le bouton de menu principal (ignore si menuLabel est null)
     * @param settingsAction si non-null, l'ecran Parametres affiche un bouton de navigation vers cet ecran
     *                       (options avancees du module) au lieu du simple bouton active/desactive generique
     */
    public record ModuleInfo(String id, Component displayName, Component menuLabel, Component menuDescription,
                              boolean defaultEnabled, Consumer<Player> menuAction, Consumer<Player> settingsAction) {

        public ModuleInfo(String id, Component displayName, Component menuLabel, Component menuDescription,
                           boolean defaultEnabled, Consumer<Player> menuAction) {
            this(id, displayName, menuLabel, menuDescription, defaultEnabled, menuAction, null);
        }
    }

    private final JavaPlugin plugin;
    private final Map<String, ModuleInfo> modules = new LinkedHashMap<>();

    public ModuleRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(ModuleInfo info) {
        modules.put(info.id(), info);
    }

    public Collection<ModuleInfo> all() {
        return modules.values();
    }

    public ModuleInfo get(String id) {
        return modules.get(id);
    }

    public boolean isEnabled(String id) {
        ModuleInfo info = modules.get(id);
        if (info == null) {
            return false;
        }
        return plugin.getConfig().getBoolean("modules." + id + ".enabled", info.defaultEnabled());
    }

    public void setEnabled(String id, boolean enabled) {
        if (modules.containsKey(id)) {
            plugin.getConfig().set("modules." + id + ".enabled", enabled);
            plugin.saveConfig();
        }
    }
}
