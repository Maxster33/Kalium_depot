package fr.kalium.kgbuildbattle;

import java.util.function.BiConsumer;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import fr.kalium.contacts.KlmContacts;
import fr.kalium.contacts.api.JeuDeGroupe;

/**
 * 0.5.0 - lien avec KLM_Contacts (étape 3 des contacts, demande de LeKiwi06 du 09/10/2026 : le groupe de jeu entre
 * ensemble dans une partie, y compris quand son chef clique sur « Rejouer »). Chaque joueur envoyé sur Kanvas (file
 * publique ou partie privée : Parties.envoyerPublic / envoyerPrive) est annoncé à KLM_Contacts ; s'il est chef d'un
 * groupe, le proxy fait passer ses membres par kal-games et {@link #rejoindre} les envoie dans la même file ou la même
 * partie.
 *
 * Cette classe n'est chargée que si KLM_Contacts est installé (voir KGBuildBattle.onEnable).
 */
final class LienContacts implements JeuDeGroupe {

    private static final String CODE = "code:";
    private static final String PUBLIC = "public:";

    private final KGBuildBattle plugin;
    private final Parties parties;
    private final KlmContacts contacts;

    private LienContacts(KGBuildBattle plugin, Parties parties, KlmContacts contacts) {
        this.plugin = plugin;
        this.parties = parties;
        this.contacts = contacts;
    }

    /** Déclare le Build Battle à KLM_Contacts ; renvoie ce qu'il faut appeler pour chaque joueur envoyé (joueur, référence). */
    static BiConsumer<Player, String> creer(KGBuildBattle plugin, Parties parties, Plugin contacts) {
        LienContacts lien = new LienContacts(plugin, parties, (KlmContacts) contacts);
        plugin.getServer().getServicesManager().register(JeuDeGroupe.class, lien, plugin, ServicePriority.Normal);
        return (joueur, reference) -> lien.contacts.annoncer(joueur, lien.id(), reference, "Build Battle");
    }

    static String publique(int tailleEquipes) {
        return PUBLIC + tailleEquipes;
    }

    static String privee(Partie partie) {
        return CODE + partie.code;
    }

    @Override
    public Plugin owner() {
        return plugin;
    }

    @Override
    public String id() {
        return "buildbattle";
    }

    @Override
    public void rejoindre(Player joueur, String reference) {
        if (reference.startsWith(PUBLIC)) {
            int taille;
            try {
                taille = Integer.parseInt(reference.substring(PUBLIC.length()));
            } catch (NumberFormatException e) {
                taille = 1;
            }
            parties.envoyerPublic(joueur, taille);
            return;
        }
        Partie partie = parties.parCode(reference.startsWith(CODE) ? reference.substring(CODE.length()) : reference);
        if (partie == null) {
            joueur.sendMessage("§cCette partie de Build Battle n'existe plus.");
            return;
        }
        if (!parties.rejoindre(joueur, partie)) {
            joueur.sendMessage("§cCette partie est complète.");
            return;
        }
        parties.envoyerPrive(joueur, partie);
    }
}
