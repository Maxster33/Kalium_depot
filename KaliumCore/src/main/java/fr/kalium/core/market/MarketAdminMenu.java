package fr.kalium.core.market;

import fr.kalium.core.KaliumCore;
import fr.kalium.core.menu.DialogHelper;
import fr.kalium.core.util.Format;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Ecrans d'administration du systeme de commerce, ouverts depuis Parametres (reserve aux
 * operateurs) : limites (annonces actives max par joueur, duree de vente par defaut) et gestion des
 * ventes flash (programmation et liste des ventes en cours/a venir, avec annulation). Seul
 * l'interrupteur maitre du module vit dans l'ecran commun "Options actives" (meme convention que
 * ClaimsAdminMenu).
 */
public final class MarketAdminMenu {

    // Cles de DialogInput : lettres/chiffres/underscore uniquement (voir ClaimsAdminMenu pour le piege).
    private static final String KEY_MASTER = "market_enabled";

    private final KaliumCore plugin;
    private final DialogHelper dialogs;

    public MarketAdminMenu(KaliumCore plugin, DialogHelper dialogs) {
        this.plugin = plugin;
        this.dialogs = dialogs;
    }

    // ------------------------------------------------------------------ interrupteur maitre (pour "Options actives")

    public List<DialogInput> toggleInputs() {
        return List.of(DialogInput.bool(KEY_MASTER, plugin.mm().deserialize(plugin.msg("settings-market-toggle-master")))
                .initial(plugin.market().masterEnabled())
                .onTrue(plugin.msg("toggle-on"))
                .onFalse(plugin.msg("toggle-off"))
                .build());
    }

    public void applyToggles(DialogResponseView view) {
        Boolean master = view.getBoolean(KEY_MASTER);
        if (master != null) {
            plugin.modules().setEnabled("market", master);
        }
    }

    // ------------------------------------------------------------------ parametres (limites)

    public void openSettings(Player player) {
        if (!plugin.isAdmin(player)) {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("no-permission")));
            return;
        }
        MarketService market = plugin.market();

        List<Component> body = new ArrayList<>();
        body.add(plugin.mm().deserialize(plugin.msg("settings-market-header")));
        body.add(plugin.mm().deserialize(plugin.msg("settings-market-max-listings-current"),
                Placeholder.unparsed("value", String.valueOf(market.maxListingsPerPlayer()))));

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(dialogs.inputButton("settings-market-max-listings-save", null, (view, p) -> {
            Float maxListings = view.getFloat("market_max_listings");
            Float duration = view.getFloat("market_duration");
            if (maxListings != null) {
                market.maxListingsPerPlayer(Math.round(maxListings));
            }
            if (duration != null) {
                market.defaultListingDurationDays(Math.round(duration));
            }
            plugin.saveConfig();
            p.sendMessage(plugin.mm().deserialize(plugin.msg("settings-market-max-listings-saved"),
                    Placeholder.unparsed("value", String.valueOf(market.maxListingsPerPlayer()))));
            openSettings(p);
        }));
        buttons.add(dialogs.button("settings-market-schedule-flash-button", null, this::openScheduleFlashSale));
        buttons.add(dialogs.button("settings-market-flash-list-button", null, this::openFlashSaleList));
        buttons.add(dialogs.button("back-menu-button", null, p -> plugin.menu().openSettings(p)));

        List<DialogInput> inputs = List.of(
                DialogInput.numberRange("market_max_listings", plugin.mm().deserialize(plugin.msg("settings-market-max-listings-label")),
                                0f, 500f)
                        .initial((float) market.maxListingsPerPlayer()).step(1f).build(),
                DialogInput.numberRange("market_duration", plugin.mm().deserialize(plugin.msg("settings-market-duration-label")),
                                1f, (float) MarketService.MAX_LISTING_DURATION_DAYS)
                        .initial((float) market.defaultListingDurationDays()).step(1f).build());

        dialogs.show(player, "settings-market-title", body, buttons, inputs);
    }

    // ------------------------------------------------------------------ programmer une vente flash

    public void openScheduleFlashSale(Player admin) {
        if (!plugin.isAdmin(admin)) {
            admin.sendMessage(plugin.mm().deserialize(plugin.msg("no-permission")));
            return;
        }

        List<Component> body = List.of(plugin.mm().deserialize(plugin.msg("settings-market-flash-schedule-header")));
        List<DialogInput> inputs = List.of(
                DialogInput.numberRange("market_flash_price", plugin.mm().deserialize(plugin.msg("settings-market-flash-price-label")),
                                1f, 8640f).initial(1f).step(1f).build(),
                DialogInput.numberRange("market_flash_stock", plugin.mm().deserialize(plugin.msg("settings-market-flash-stock-label")),
                                1f, 1000f).initial(1f).step(1f).build(),
                DialogInput.numberRange("market_flash_delay", plugin.mm().deserialize(plugin.msg("settings-market-flash-delay-label")),
                                0f, 10080f).initial(0f).step(1f).build(),
                DialogInput.numberRange("market_flash_duration", plugin.mm().deserialize(plugin.msg("settings-market-flash-duration-label")),
                                1f, 10080f).initial(60f).step(1f).build());

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(dialogs.inputButton("settings-market-flash-confirm-button", null, (view, p) -> {
            Float price = view.getFloat("market_flash_price");
            Float stock = view.getFloat("market_flash_stock");
            Float delay = view.getFloat("market_flash_delay");
            Float duration = view.getFloat("market_flash_duration");
            MarketService.ScheduleResult result = plugin.market().scheduleFlashSale(p,
                    price == null ? 0 : Math.round(price),
                    stock == null ? 0 : Math.round(stock),
                    delay == null ? 0 : Math.round(delay),
                    duration == null ? 0 : Math.round(duration));
            switch (result) {
                case OK -> p.sendMessage(plugin.mm().deserialize(plugin.msg("settings-market-flash-ok")));
                case DISABLED -> p.sendMessage(plugin.mm().deserialize(plugin.msg("market-sell-disabled")));
                case EMPTY_HAND -> p.sendMessage(plugin.mm().deserialize(plugin.msg("settings-market-flash-empty-hand")));
                case BAD_PARAMS -> p.sendMessage(plugin.mm().deserialize(plugin.msg("settings-market-flash-bad-params")));
            }
            openSettings(p);
        }));
        buttons.add(dialogs.button("back-menu-button", null, this::openSettings));

        dialogs.show(admin, "settings-market-flash-schedule-title", body, buttons, inputs);
    }

    // ------------------------------------------------------------------ liste / annulation des ventes flash

    public void openFlashSaleList(Player admin) {
        if (!plugin.isAdmin(admin)) {
            admin.sendMessage(plugin.mm().deserialize(plugin.msg("no-permission")));
            return;
        }

        List<FlashSale> sales = plugin.market().allFlashSales();
        List<Component> body = new ArrayList<>();
        body.add(plugin.mm().deserialize(plugin.msg("settings-market-flash-list-header")));
        if (sales.isEmpty()) {
            body.add(plugin.mm().deserialize(plugin.msg("settings-market-flash-list-empty")));
        }

        List<ActionButton> buttons = new ArrayList<>();
        long now = System.currentTimeMillis();
        // Les Dialogs n'affichent qu'un nombre raisonnable de boutons a la fois : on se limite aux 20
        // ventes les plus recentes (largement suffisant en usage normal).
        int limit = Math.min(sales.size(), 20);
        for (int i = 0; i < limit; i++) {
            FlashSale sale = sales.get(i);
            buttons.add(dialogs.rawButton(flashLabel(sale, now), null, p -> {
                plugin.market().cancelFlashSale(sale.id());
                p.sendMessage(plugin.mm().deserialize(plugin.msg("settings-market-flash-cancel-ok")));
                openFlashSaleList(p);
            }));
        }
        buttons.add(dialogs.button("back-menu-button", null, this::openSettings));

        dialogs.show(admin, "settings-market-flash-list-title", body, buttons);
    }

    private Component flashLabel(FlashSale sale, long now) {
        String itemName = sale.item().getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ')
                + " x" + sale.item().getAmount();
        String status = switch (sale.status(now)) {
            case UPCOMING -> "des le " + Format.dateTime(sale.startAt());
            case ACTIVE -> "en cours, " + sale.remaining() + "/" + sale.stock() + " restants";
            case SOLD_OUT -> "epuisee";
            case EXPIRED -> "terminee";
        };
        return plugin.mm().deserialize(plugin.msg("settings-market-flash-list-entry"),
                Placeholder.unparsed("item", itemName), Placeholder.unparsed("status", status));
    }
}
