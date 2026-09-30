package fr.kalium.economy;

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;

import java.util.List;

/**
 * Économie Vault (nécessaire techniquement, signalé dans le cahier) : SimpleClaimSystem (catégorie 3) pourra faire payer
 * en score. Montants en points entiers : un dépôt est arrondi à l'unité inférieure, un retrait à l'unité supérieure.
 * Pas de banques. Chargée seulement si Vault (VaultUnlocked) est présent.
 */
final class VaultEconomie extends AbstractEconomy {

    private final KSEconomy plugin;

    private VaultEconomie(KSEconomy plugin) {
        this.plugin = plugin;
    }

    static void enregistrer(KSEconomy plugin) {
        Bukkit.getServicesManager().register(Economy.class, new VaultEconomie(plugin), plugin, ServicePriority.Highest);
    }

    @SuppressWarnings("deprecation")
    private static OfflinePlayer joueur(String nom) {
        return Bukkit.getOfflinePlayer(nom);
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "KS_Economy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 0;
    }

    @Override
    public String format(double montant) {
        return KSEconomy.points((long) Math.floor(montant));
    }

    @Override
    public String currencyNamePlural() {
        return "points";
    }

    @Override
    public String currencyNameSingular() {
        return "point";
    }

    // ------------------------------------------------------------------ comptes

    @Override
    public boolean hasAccount(OfflinePlayer joueur) {
        return KSEconomy.aUnCompte(joueur.getUniqueId());
    }

    @Override
    public boolean hasAccount(String nom) {
        return hasAccount(joueur(nom));
    }

    @Override
    public boolean hasAccount(String nom, String monde) {
        return hasAccount(nom);
    }

    @Override
    public double getBalance(OfflinePlayer joueur) {
        return KSEconomy.solde(joueur.getUniqueId());
    }

    @Override
    public double getBalance(String nom) {
        return getBalance(joueur(nom));
    }

    @Override
    public double getBalance(String nom, String monde) {
        return getBalance(nom);
    }

    @Override
    public boolean has(OfflinePlayer joueur, double montant) {
        return KSEconomy.solde(joueur.getUniqueId()) >= Math.ceil(montant);
    }

    @Override
    public boolean has(String nom, double montant) {
        return has(joueur(nom), montant);
    }

    @Override
    public boolean has(String nom, String monde, double montant) {
        return has(nom, montant);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer joueur, double montant) {
        if (montant < 0) {
            return new EconomyResponse(0, getBalance(joueur), ResponseType.FAILURE, "Montant négatif.");
        }
        long points = (long) Math.ceil(montant);
        if (!KSEconomy.debiter(joueur.getUniqueId(), points)) {
            return new EconomyResponse(0, getBalance(joueur), ResponseType.FAILURE, "Solde insuffisant.");
        }
        return new EconomyResponse(points, getBalance(joueur), ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String nom, double montant) {
        return withdrawPlayer(joueur(nom), montant);
    }

    @Override
    public EconomyResponse withdrawPlayer(String nom, String monde, double montant) {
        return withdrawPlayer(nom, montant);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer joueur, double montant) {
        if (montant < 0) {
            return new EconomyResponse(0, getBalance(joueur), ResponseType.FAILURE, "Montant négatif.");
        }
        long points = (long) Math.floor(montant);
        KSEconomy.crediter(joueur.getUniqueId(), points);
        return new EconomyResponse(points, getBalance(joueur), ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String nom, double montant) {
        return depositPlayer(joueur(nom), montant);
    }

    @Override
    public EconomyResponse depositPlayer(String nom, String monde, double montant) {
        return depositPlayer(nom, montant);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer joueur) {
        KSEconomy.creerCompte(joueur.getUniqueId());
        return true;
    }

    @Override
    public boolean createPlayerAccount(String nom) {
        return createPlayerAccount(joueur(nom));
    }

    @Override
    public boolean createPlayerAccount(String nom, String monde) {
        return createPlayerAccount(nom);
    }

    // ------------------------------------------------------------------ pas de banques

    private static EconomyResponse pasDeBanque() {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Pas de banques.");
    }

    @Override
    public EconomyResponse createBank(String nom, String joueur) {
        return pasDeBanque();
    }

    @Override
    public EconomyResponse deleteBank(String nom) {
        return pasDeBanque();
    }

    @Override
    public EconomyResponse bankBalance(String nom) {
        return pasDeBanque();
    }

    @Override
    public EconomyResponse bankHas(String nom, double montant) {
        return pasDeBanque();
    }

    @Override
    public EconomyResponse bankWithdraw(String nom, double montant) {
        return pasDeBanque();
    }

    @Override
    public EconomyResponse bankDeposit(String nom, double montant) {
        return pasDeBanque();
    }

    @Override
    public EconomyResponse isBankOwner(String nom, String joueur) {
        return pasDeBanque();
    }

    @Override
    public EconomyResponse isBankMember(String nom, String joueur) {
        return pasDeBanque();
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }
}
