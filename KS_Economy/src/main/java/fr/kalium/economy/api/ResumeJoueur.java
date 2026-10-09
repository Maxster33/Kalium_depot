package fr.kalium.economy.api;

import org.bukkit.Location;

import java.util.List;

/**
 * 1.4.1 (modération : fiche d'un joueur de KS_AntiCheat 1.3.0, demande de LeKiwi06 du 09/10/2026) : ce que KS_Economy
 * sait d'un joueur, en ligne ou hors ligne, en lecture seule. Solde, magasin (nom et position, null s'il n'en a pas),
 * boutiques et points qui y attendent, ventes de ses boutiques (nombre, lots des 7 derniers jours, points gagnés),
 * dernières ventes et derniers achats (la plus récente d'abord).
 */
public record ResumeJoueur(long solde, boolean soldeMasque, String magasin, Location positionMagasin, int boutiques,
                           long pointsEnAttente, int ventes, int lots7j, long pointsGagnes, List<Vente> dernieresVentes,
                           List<Vente> derniersAchats) {

    /**
     * Une vente d'une boutique : nom de la boutique (null si elle n'existe plus), pseudos du vendeur et de l'acheteur,
     * lots, prix payé (en points, ou en nombre d'objets pour une boutique à prix en objet).
     */
    public record Vente(long date, String boutique, String vendeur, String acheteur, int lots, long points, int objets) {
    }
}
