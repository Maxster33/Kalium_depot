package fr.kalium.scoreboards.api;

import fr.kalium.scoreboards.data.StatsService.Row;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.Map;

/**
 * 1.8.0 (catégorie 4 « Récompenses ») : fin d'une semaine (samedi 15 h) ou d'un mois aligné (1er vendredi 21 h) ;
 * classements finaux triés par mini-jeu (KG_Rewards : tops hebdomadaires et mensuels). Clôture faite pendant le
 * démarrage du serveur : l'événement est envoyé au premier tick, une fois tous les plugins chargés.
 */
public final class PeriodeClotureeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String type;
    private final String cle;
    private final Map<String, List<Row>> classements;

    public PeriodeClotureeEvent(String type, String cle, Map<String, List<Row>> classements) {
        this.type = type;
        this.cle = cle;
        this.classements = classements;
    }

    /** « semaine » ou « mois ». */
    public String type() {
        return type;
    }

    /** Clé de la période : date du samedi de début (semaine) ou aaaa-mm (mois). */
    public String cle() {
        return cle;
    }

    public Map<String, List<Row>> classements() {
        return classements;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
