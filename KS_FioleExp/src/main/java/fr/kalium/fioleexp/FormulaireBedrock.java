package fr.kalium.fioleexp;

import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * KS_FioleExp 1.2.0 - demande de Maxster33, 29/09/2026 : sur Bedrock, le nombre tapé dans le champ du nom de l'enclume
 * n'est envoyé au serveur qu'à la prise du résultat, le coût ne peut donc pas être affiché avant. Les joueurs Bedrock
 * remplissent donc une fiole par un formulaire Floodgate.
 *
 * 1.3.0 : le déclencheur, les textes et le remplissage sont dans MenuFiole (communs avec le dialogue Java) ; cette
 * classe n'affiche plus que les formulaires Bedrock.
 *
 * Classe chargée seulement si Floodgate est activé (elle utilise ses classes et celles de Cumulus).
 */
final class FormulaireBedrock {

    boolean estBedrock(Player player) {
        return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }

    /** 1er formulaire : nombre de niveaux à stocker (1.5.0 ; points avant), erreur éventuelle en tête, en rouge. */
    void demanderNiveaux(Player player, String erreur, String texte, Consumer<String> suite) {
        CustomForm form = CustomForm.builder()
                .title(MenuFiole.TITRE)
                .label((erreur != null ? "§c" + erreur + "§r\n\n" : "") + texte)
                .input(MenuFiole.CHAMP, "ex. 30")
                .validResultHandler(response -> suite.accept(response.asInput(1)))
                .build();
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    /** 2e formulaire : Confirmer / Annuler. */
    void confirmer(Player player, String texte, Runnable suite) {
        ModalForm form = ModalForm.builder()
                .title(MenuFiole.TITRE)
                .content(texte)
                .button1("Confirmer")
                .button2("Annuler")
                .validResultHandler(response -> {
                    if (response.clickedFirst()) {
                        suite.run();
                    }
                })
                .build();
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }
}
