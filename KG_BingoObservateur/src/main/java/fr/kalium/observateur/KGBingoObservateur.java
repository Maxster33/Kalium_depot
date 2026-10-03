package fr.kalium.observateur;

import fr.kalium.bingo.BingoPlugin;
import fr.kalium.menu.api.Gui;
import fr.kalium.menu.api.Lang;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * KG_BingoObservateur (0.1.0, demande de Maxster33, 03/10/2026) : sur Serveur Jeux, les operateurs observent n'importe
 * quel joueur en spectateur.
 *
 * - /observer : donne la longue-vue « Observer un joueur » (clic droit : liste des joueurs, groupes par partie) ; en
 *   cours d'observation (le mode spectateur ne permet pas d'utiliser un objet), /observer ouvre directement la liste ;
 * - pour chaque joueur, deux vues : libre (teleporte a cote, deplacement libre) ou dans ses yeux (camera attachee,
 *   Maj pour se detacher ; suit le joueur quand il change de dimension ou reapparait) ;
 * - /observer &lt;pseudo&gt; : vue libre directe ; /observer quitter : retour a la position et au mode de jeu d'avant ;
 * - partie observee terminee (ses mondes vont etre supprimes) ou deconnexion : retour automatique.
 *
 * Plugin separe (« un plugin = un role ») : lit les parties et equipes de KG_BingoGame sans le modifier.
 */
public final class KGBingoObservateur extends JavaPlugin {

    private ObservationService service;

    @Override
    public void onEnable() {
        BingoPlugin bingo = (BingoPlugin) getServer().getPluginManager().getPlugin("KG_BingoGame");
        Lang lang = new Lang(this);
        Gui gui = new Gui(this, lang);
        ObserverItem item = new ObserverItem(this, lang);
        service = new ObservationService(this, bingo, lang);
        ObservationMenu menu = new ObservationMenu(bingo, service, gui, lang);
        ObserverCommand command = new ObserverCommand(service, menu, item, lang);
        getServer().getPluginManager().registerEvents(service, this);
        getServer().getPluginManager().registerEvents(item.listener(menu), this);
        var observer = getCommand("observer");
        if (observer != null) {
            observer.setExecutor(command);
            observer.setTabCompleter(command);
        }
        getServer().getScheduler().runTaskTimer(this, service::tick, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, lang::saveIfNeeded, 200L, 1200L);
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.restoreAll(); // arret ou rechargement : personne ne reste coince en spectateur
        }
    }
}
