package fr.kalium.core.market;

import fr.kalium.core.KaliumCore;
import fr.kalium.core.menu.DialogHelper;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import fr.kalium.core.util.Format;

/**
 * Ecrans du commerce : un menu Dialog "Commerce" (openHub) avec un bouton par fonction, et des pages
 * type "coffre" dediees pour celles qui affichent des objets (Marche, Vente flash, Mes ventes,
 * Resultats de recherche, Rewards) - chacune paginee a MarketService.PAGE_SIZE (45) objets par page,
 * avec la navigation (page precedente/suivante/retour) regroupee dans le coin en bas a droite. La
 * mise en vente et la recherche demandent une saisie (texte/nombres) : elles passent par un Dialog
 * natif, comme le reste du plugin. Les historiques (ventes/achats) sont de simples listes en Dialog,
 * en lecture seule.
 * <p>
 * Voir MarketGuiHolder pour le contrat "aucun objet du joueur n'y transite jamais" sur les pages
 * Marche/Vente flash/Mes ventes/Recherche, et MarketRewardHolder pour le coffre de recompenses (seul
 * ecran ou l'on peut reellement retirer des objets).
 */
public final class MarketGui {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = MarketService.PAGE_SIZE; // 45, slots 0-44
    private static final int NAV_START = 45;
    private static final int PAGE_PREV = 51;
    private static final int PAGE_NEXT = 52;
    private static final int PAGE_BACK = 53;

    private final KaliumCore plugin;
    private final DialogHelper dialogs;

    public MarketGui(KaliumCore plugin, DialogHelper dialogs) {
        this.plugin = plugin;
        this.dialogs = dialogs;
    }

    // ------------------------------------------------------------------ point d'entree (bouton menu principal)

    /** Menu Dialog "Commerce" : un bouton par fonction (voir la javadoc de classe). */
    public void openHub(Player player) {
        List<Component> body = List.of(msgComponent("market-hub-header"));

        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(dialogs.button("market-hub-sell-button", null, this::openCreateListingDialog));
        buttons.add(dialogs.button("market-hub-mesventes-button", null, p -> openMesVentes(p, 0)));
        buttons.add(dialogs.button("market-hub-search-button", null, this::openSearchDialog));
        buttons.add(dialogs.button("market-hub-marche-button", null, p -> openMarche(p, 0)));
        buttons.add(dialogs.button("market-hub-flash-button", null, p -> openVenteFlash(p, 0)));
        buttons.add(dialogs.button("market-hub-history-sales-button", null, this::openHistoriqueVentes));
        buttons.add(dialogs.button("market-hub-history-purchases-button", null, this::openHistoriqueAchats));
        buttons.add(dialogs.rawButton(rewardsButtonLabel(player), null, p -> openRewards(p, 0)));
        buttons.add(dialogs.button("back-menu-button", null, p -> plugin.menu().openMain(p)));

        dialogs.show(player, "market-hub-title", body, buttons);
    }

    private Component rewardsButtonLabel(Player player) {
        int count = plugin.market().rewardCount(player.getUniqueId());
        return plugin.mm().deserialize(plugin.msg("market-hub-rewards-button"),
                Placeholder.unparsed("count", String.valueOf(count)));
    }

    // ------------------------------------------------------------------ dialog : mettre en vente

    public void openCreateListingDialog(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("market-sell-empty-hand")));
            return;
        }
        int maxQuantity = hand.getAmount();
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Component> body = List.of(msgComponent("market-sell-dialog-header"));
            List<DialogInput> inputs = List.of(
                    DialogInput.numberRange("market_price", msgComponent("market-sell-dialog-price-label"), 1f, 8640f)
                            .initial(1f).step(1f).build(),
                    DialogInput.numberRange("market_qty", msgComponent("market-sell-dialog-quantity-label"), 1f, (float) maxQuantity)
                            .initial((float) maxQuantity).step(1f).build(),
                    DialogInput.numberRange("market_duration", msgComponent("market-sell-dialog-duration-label"),
                                    1f, (float) MarketService.MAX_LISTING_DURATION_DAYS)
                            .initial((float) plugin.market().defaultListingDurationDays()).step(1f).build());

            List<ActionButton> buttons = new ArrayList<>();
            buttons.add(dialogs.inputButton("market-sell-dialog-confirm-button", null, (view, p) -> {
                Float price = view.getFloat("market_price");
                Float qty = view.getFloat("market_qty");
                Float duration = view.getFloat("market_duration");
                MarketService.ListResult result = plugin.market().createListing(p,
                        price == null ? 0 : Math.round(price),
                        qty == null ? 1 : Math.round(qty),
                        duration == null ? MarketService.MAX_LISTING_DURATION_DAYS : Math.round(duration));
                switch (result) {
                    case OK -> p.sendMessage(plugin.mm().deserialize(plugin.msg("market-sell-ok")));
                    case DISABLED -> p.sendMessage(plugin.mm().deserialize(plugin.msg("market-sell-disabled")));
                    case EMPTY_HAND -> p.sendMessage(plugin.mm().deserialize(plugin.msg("market-sell-empty-hand")));
                    case LIMIT_REACHED -> p.sendMessage(plugin.mm().deserialize(plugin.msg("market-sell-limit")));
                    case BAD_PRICE -> p.sendMessage(plugin.mm().deserialize(plugin.msg("market-sell-bad-price")));
                }
                openHub(p);
            }));
            buttons.add(dialogs.button("back-menu-button", null, this::openHub));

            dialogs.show(player, "market-sell-dialog-title", body, buttons, inputs);
        });
    }

    // ------------------------------------------------------------------ dialog : recherche

    public void openSearchDialog(Player player) {
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Component> body = List.of(msgComponent("market-search-dialog-header"));
            List<DialogInput> inputs = List.of(
                    DialogInput.text("market_search", msgComponent("market-search-dialog-label"))
                            .initial("").maxLength(32).build());

            List<ActionButton> buttons = new ArrayList<>();
            buttons.add(dialogs.inputButton("market-search-dialog-confirm-button", null, (view, p) ->
                    openSearchResults(p, view.getText("market_search"), 0)));
            buttons.add(dialogs.button("back-menu-button", null, this::openHub));

            dialogs.show(player, "market-search-dialog-title", body, buttons, inputs);
        });
    }

    public void openSearchResults(Player player, String keyword, int page) {
        List<MarketListing> results = plugin.market().searchListings(keyword);
        int totalPages = totalPages(results.size());
        int clamped = clampPage(page, totalPages);
        MarketGuiHolder holder = new MarketGuiHolder(MarketGuiHolder.Screen.RECHERCHE, clamped, keyword);
        Inventory inv = Bukkit.createInventory(holder, SIZE, msgComponent("market-title-recherche"));
        holder.inventory(inv);

        if (results.isEmpty()) {
            inv.setItem(4, namedItem(Material.BARRIER, msgComponent("market-empty-search"), List.of()));
        }
        fillSlice(inv, results, clamped, this::browseIcon);
        renderNav(inv, clamped, totalPages);
        player.openInventory(inv);
    }

    // ------------------------------------------------------------------ marche

    public void openMarche(Player player, int page) {
        List<MarketListing> listings = plugin.market().activeListings();
        int totalPages = totalPages(listings.size());
        int clamped = clampPage(page, totalPages);
        MarketGuiHolder holder = new MarketGuiHolder(MarketGuiHolder.Screen.MARCHE, clamped, null);
        Inventory inv = Bukkit.createInventory(holder, SIZE, msgComponent("market-title-browse"));
        holder.inventory(inv);

        if (listings.isEmpty()) {
            inv.setItem(4, namedItem(Material.BARRIER, msgComponent("market-empty-browse"), List.of()));
        }
        fillSlice(inv, listings, clamped, this::browseIcon);
        renderNav(inv, clamped, totalPages);
        player.openInventory(inv);
    }

    private ItemStack browseIcon(MarketListing listing) {
        List<Component> lore = new ArrayList<>();
        lore.add(plugin.mm().deserialize(plugin.msg("market-lore-price"),
                Placeholder.unparsed("price", String.valueOf(listing.price()))));
        Double average = plugin.market().averagePrice(listing.item().getType());
        lore.add(average == null
                ? msgComponent("market-lore-average-none")
                : plugin.mm().deserialize(plugin.msg("market-lore-average"),
                        Placeholder.unparsed("value", String.valueOf(Math.round(average * 10.0) / 10.0))));
        lore.add(plugin.mm().deserialize(plugin.msg("market-lore-seller"),
                Placeholder.unparsed("seller", listing.sellerName())));
        lore.add(plugin.mm().deserialize(plugin.msg("market-lore-expires"),
                Placeholder.unparsed("date", Format.dateTime(listing.expiresAt()))));
        lore.add(msgComponent("market-lore-buy-hint"));
        return displayClone(listing.item(), lore);
    }

    // ------------------------------------------------------------------ vente flash

    public void openVenteFlash(Player player, int page) {
        List<FlashSale> sales = plugin.market().activeFlashSales();
        int totalPages = totalPages(sales.size());
        int clamped = clampPage(page, totalPages);
        MarketGuiHolder holder = new MarketGuiHolder(MarketGuiHolder.Screen.VENTE_FLASH, clamped, null);
        Inventory inv = Bukkit.createInventory(holder, SIZE, msgComponent("market-title-flash"));
        holder.inventory(inv);

        if (sales.isEmpty()) {
            inv.setItem(4, namedItem(Material.BARRIER, msgComponent("market-empty-flash"), List.of()));
        }
        long now = System.currentTimeMillis();
        fillSlice(inv, sales, clamped, sale -> flashIcon(sale, now));
        renderNav(inv, clamped, totalPages);
        player.openInventory(inv);
    }

    private ItemStack flashIcon(FlashSale sale, long now) {
        List<Component> lore = new ArrayList<>();
        lore.add(plugin.mm().deserialize(plugin.msg("market-lore-price"),
                Placeholder.unparsed("price", String.valueOf(sale.price()))));
        lore.add(plugin.mm().deserialize(plugin.msg("market-lore-flash-stock"),
                Placeholder.unparsed("remaining", String.valueOf(sale.remaining())),
                Placeholder.unparsed("total", String.valueOf(sale.stock()))));
        boolean upcoming = sale.status(now) == FlashSale.Status.UPCOMING;
        lore.add(plugin.mm().deserialize(plugin.msg(upcoming ? "market-lore-flash-window-upcoming" : "market-lore-flash-window-active"),
                Placeholder.unparsed("date", Format.dateTime(upcoming ? sale.startAt() : sale.endAt()))));
        if (upcoming) {
            lore.add(msgComponent("market-lore-flash-soon"));
        } else {
            lore.add(msgComponent("market-lore-buy-hint"));
        }
        return displayClone(sale.item(), lore);
    }

    // ------------------------------------------------------------------ mes ventes

    public void openMesVentes(Player player, int page) {
        List<MarketListing> own = plugin.market().listingsBySeller(player.getUniqueId());
        int totalPages = totalPages(own.size());
        int clamped = clampPage(page, totalPages);
        MarketGuiHolder holder = new MarketGuiHolder(MarketGuiHolder.Screen.MES_VENTES, clamped, null);
        Inventory inv = Bukkit.createInventory(holder, SIZE, msgComponent("market-title-mesventes"));
        holder.inventory(inv);

        if (own.isEmpty()) {
            inv.setItem(4, namedItem(Material.BARRIER, msgComponent("market-empty-sell"), List.of()));
        }
        fillSlice(inv, own, clamped, this::ownListingIcon);
        renderNav(inv, clamped, totalPages);
        player.openInventory(inv);
    }

    private ItemStack ownListingIcon(MarketListing listing) {
        List<Component> lore = new ArrayList<>();
        lore.add(plugin.mm().deserialize(plugin.msg("market-lore-price"),
                Placeholder.unparsed("price", String.valueOf(listing.price()))));
        lore.add(plugin.mm().deserialize(plugin.msg("market-lore-expires"),
                Placeholder.unparsed("date", Format.dateTime(listing.expiresAt()))));
        lore.add(msgComponent("market-lore-cancel-hint"));
        return displayClone(listing.item(), lore);
    }

    // ------------------------------------------------------------------ rewards (coffre reel, liste paginee)

    public void openRewards(Player player, int page) {
        var id = player.getUniqueId();
        int totalPages = plugin.market().rewardTotalPages(id);
        int clamped = clampPage(page, totalPages);
        List<ItemStack> items = plugin.market().rewardPageItems(id, clamped);
        MarketRewardHolder holder = new MarketRewardHolder(id, clamped);
        Inventory inv = Bukkit.createInventory(holder, SIZE, msgComponent("market-reward-title"));
        holder.inventory(inv);

        for (int i = 0; i < PAGE_SIZE; i++) {
            inv.setItem(i, i < items.size() ? items.get(i) : null);
        }
        if (items.isEmpty() && clamped == 0) {
            inv.setItem(4, namedItem(Material.BARRIER, msgComponent("market-empty-rewards"), List.of()));
        }
        renderNav(inv, clamped, totalPages);
        player.openInventory(inv);
    }

    // ------------------------------------------------------------------ dialogs : historiques (lecture seule)

    public void openHistoriqueVentes(Player player) {
        List<TransactionRecord> records = plugin.market().salesHistory(player.getUniqueId());
        List<Component> body = new ArrayList<>();
        body.add(msgComponent("market-history-ventes-header"));
        if (records.isEmpty()) {
            body.add(msgComponent("market-history-ventes-empty"));
        }
        int limit = Math.min(records.size(), 40);
        for (int i = 0; i < limit; i++) {
            body.add(historyLine(records.get(i)));
        }

        List<ActionButton> buttons = List.of(dialogs.button("back-menu-button", null, this::openHub));
        dialogs.show(player, "market-history-ventes-title", body, buttons);
    }

    public void openHistoriqueAchats(Player player) {
        List<TransactionRecord> records = plugin.market().purchaseHistory(player.getUniqueId());
        List<Component> body = new ArrayList<>();
        body.add(msgComponent("market-history-achats-header"));
        if (records.isEmpty()) {
            body.add(msgComponent("market-history-achats-empty"));
        }
        int limit = Math.min(records.size(), 40);
        for (int i = 0; i < limit; i++) {
            body.add(historyLine(records.get(i)));
        }

        List<ActionButton> buttons = List.of(dialogs.button("back-menu-button", null, this::openHub));
        dialogs.show(player, "market-history-achats-title", body, buttons);
    }

    private Component historyLine(TransactionRecord record) {
        String key = switch (record.type()) {
            case VENTE -> "market-history-line-vente";
            case ACHAT -> "market-history-line-achat";
            case VENTE_FLASH -> "market-history-line-flash";
        };
        String itemName = record.item().getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return plugin.mm().deserialize(plugin.msg(key),
                Placeholder.unparsed("item", itemName),
                Placeholder.unparsed("qty", String.valueOf(record.item().getAmount())),
                Placeholder.unparsed("price", String.valueOf(record.price())),
                Placeholder.unparsed("counterpart", record.counterpart()),
                Placeholder.unparsed("date", Format.dateTime(record.at())));
    }

    // ------------------------------------------------------------------ pagination / controles (rangee du bas, coin en bas a droite)

    private int totalPages(int itemCount) {
        return Math.max(1, (int) Math.ceil(itemCount / (double) PAGE_SIZE));
    }

    private int clampPage(int page, int totalPages) {
        return Math.max(0, Math.min(page, totalPages - 1));
    }

    private <T> void fillSlice(Inventory inv, List<T> list, int page, java.util.function.Function<T, ItemStack> iconFn) {
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            if (index < list.size()) {
                inv.setItem(i, iconFn.apply(list.get(index)));
            }
        }
    }

    /** Navigation regroupee dans le coin en bas a droite : page precedente / page suivante / retour au menu Commerce. */
    private void renderNav(Inventory inv, int page, int totalPages) {
        if (page > 0) {
            inv.setItem(PAGE_PREV, namedItem(Material.ARROW, msgComponent("market-page-prev"), List.of()));
        }
        if (page < totalPages - 1) {
            inv.setItem(PAGE_NEXT, namedItem(Material.ARROW, msgComponent("market-page-next"), List.of()));
        }
        List<Component> backLore = List.of(plugin.mm().deserialize(plugin.msg("market-page-info"),
                Placeholder.unparsed("page", String.valueOf(page + 1)), Placeholder.unparsed("total", String.valueOf(totalPages))));
        inv.setItem(PAGE_BACK, namedItem(Material.BARRIER, msgComponent("market-back-menu"), backLore));
    }

    // ------------------------------------------------------------------ resolution slot -> entite (appelee par MarketGuiListener)

    public static boolean isNavSlot(int slot) {
        return slot >= NAV_START && slot < SIZE;
    }

    public static boolean isContentSlot(int slot) {
        return slot >= 0 && slot < PAGE_SIZE;
    }

    public static int navPrevSlot() {
        return PAGE_PREV;
    }

    public static int navNextSlot() {
        return PAGE_NEXT;
    }

    public static int navBackSlot() {
        return PAGE_BACK;
    }

    public Long listingIdAtSlot(MarketGuiHolder holder, int slot) {
        if (!isContentSlot(slot)) {
            return null;
        }
        List<MarketListing> list = switch (holder.screen()) {
            case MARCHE -> plugin.market().activeListings();
            case RECHERCHE -> plugin.market().searchListings(holder.keyword());
            default -> List.of();
        };
        int index = holder.page() * PAGE_SIZE + slot;
        return index >= 0 && index < list.size() ? list.get(index).id() : null;
    }

    public Long flashIdAtSlot(MarketGuiHolder holder, int slot) {
        if (!isContentSlot(slot) || holder.screen() != MarketGuiHolder.Screen.VENTE_FLASH) {
            return null;
        }
        List<FlashSale> sales = plugin.market().activeFlashSales();
        int index = holder.page() * PAGE_SIZE + slot;
        return index >= 0 && index < sales.size() ? sales.get(index).id() : null;
    }

    public Long ownListingIdAtSlot(Player viewer, MarketGuiHolder holder, int slot) {
        if (!isContentSlot(slot) || holder.screen() != MarketGuiHolder.Screen.MES_VENTES) {
            return null;
        }
        List<MarketListing> own = plugin.market().listingsBySeller(viewer.getUniqueId());
        int index = holder.page() * PAGE_SIZE + slot;
        return index >= 0 && index < own.size() ? own.get(index).id() : null;
    }

    // ------------------------------------------------------------------ actions (appelees par MarketGuiListener)

    public void buyListing(Player player, MarketGuiHolder holder, long id) {
        MarketService.BuyResult result = plugin.market().buyListing(player, id);
        sendBuyMessage(player, result);
        reopen(player, holder);
    }

    public void buyFlash(Player player, MarketGuiHolder holder, long id) {
        MarketService.BuyResult result = plugin.market().buyFlashSale(player, id);
        sendBuyMessage(player, result);
        reopen(player, holder);
    }

    public void cancelListing(Player player, MarketGuiHolder holder, long id) {
        boolean ok = plugin.market().cancelListing(player, id);
        if (ok) {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("market-cancel-ok")));
        }
        reopen(player, holder);
    }

    /** Rouvre le meme ecran (meme page) apres une action, pour que le joueur voie la liste rafraichie. */
    private void reopen(Player player, MarketGuiHolder holder) {
        switch (holder.screen()) {
            case MARCHE -> openMarche(player, holder.page());
            case VENTE_FLASH -> openVenteFlash(player, holder.page());
            case MES_VENTES -> openMesVentes(player, holder.page());
            case RECHERCHE -> openSearchResults(player, holder.keyword(), holder.page());
        }
    }

    public void navigate(Player player, MarketGuiHolder holder, int newPage) {
        switch (holder.screen()) {
            case MARCHE -> openMarche(player, newPage);
            case VENTE_FLASH -> openVenteFlash(player, newPage);
            case MES_VENTES -> openMesVentes(player, newPage);
            case RECHERCHE -> openSearchResults(player, holder.keyword(), newPage);
        }
    }

    private void sendBuyMessage(Player player, MarketService.BuyResult result) {
        switch (result) {
            case OK -> player.sendMessage(plugin.mm().deserialize(plugin.msg("market-buy-ok")));
            case DISABLED -> player.sendMessage(plugin.mm().deserialize(plugin.msg("market-buy-disabled")));
            case NOT_FOUND -> player.sendMessage(plugin.mm().deserialize(plugin.msg("market-buy-not-found")));
            case OWN_LISTING -> player.sendMessage(plugin.mm().deserialize(plugin.msg("market-buy-own-listing")));
            case NOT_ENOUGH_EMERALDS -> player.sendMessage(plugin.mm().deserialize(plugin.msg("market-buy-not-enough")));
        }
    }

    // ------------------------------------------------------------------ petits utilitaires d'affichage

    private Component msgComponent(String key) {
        return plugin.mm().deserialize(plugin.msg(key));
    }

    private ItemStack namedItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(noItalic(name));
        if (!lore.isEmpty()) {
            List<Component> italicFree = new ArrayList<>();
            for (Component line : lore) {
                italicFree.add(noItalic(line));
            }
            meta.lore(italicFree);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Clone d'un objet reel (annonce/vente flash) avec nos lignes d'info ajoutees a sa lore d'origine. */
    private ItemStack displayClone(ItemStack base, List<Component> extraLore) {
        ItemStack clone = base.clone();
        ItemMeta meta = clone.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if (meta.hasLore()) {
            lore.addAll(meta.lore());
            lore.add(Component.empty());
        }
        for (Component line : extraLore) {
            lore.add(noItalic(line));
        }
        meta.lore(lore);
        clone.setItemMeta(meta);
        return clone;
    }

    private Component noItalic(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
