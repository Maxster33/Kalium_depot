package fr.kalium.core.claims;

import fr.kalium.core.KaliumCore;
import fr.kalium.core.menu.DialogHelper;
import io.papermc.paper.registry.data.dialog.ActionButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Ecran Claims cote joueur : claim/unclaim du chunk courant, base, conversion en jeton. */
public final class ClaimsMenu {

    private final KaliumCore plugin;
    private final DialogHelper dialogs;

    public ClaimsMenu(KaliumCore plugin, DialogHelper dialogs) {
        this.plugin = plugin;
        this.dialogs = dialogs;
    }

    public void openMain(Player player) {
        ClaimsService claims = plugin.claims();
        Chunk chunk = player.getLocation().getChunk();
        UUID owner = claims.ownerOf(chunk);
        boolean mine = player.getUniqueId().equals(owner);

        List<Component> body = new ArrayList<>();
        body.add(plugin.mm().deserialize(plugin.msg("claims-header")));
        body.add(plugin.mm().deserialize(plugin.msg("claims-slots"),
                Placeholder.unparsed("used", String.valueOf(claims.used(player.getUniqueId()))),
                Placeholder.unparsed("total", String.valueOf(claims.total(player.getUniqueId())))));
        body.add(chunkStatusLine(owner, mine));

        List<ActionButton> buttons = new ArrayList<>();

        if (claims.claimActionEnabled() && owner == null) {
            buttons.add(dialogs.button("claims-claim-button", null, this::doClaim));
        }
        if (claims.unclaimActionEnabled() && mine) {
            buttons.add(dialogs.button("claims-unclaim-button", null, this::doUnclaim));
        }
        if (claims.homeEnabled() && mine && !claims.isHome(player)) {
            buttons.add(dialogs.button("claims-home-set-button", null, this::doSetHome));
        }
        if (claims.itemConversionEnabled()) {
            buttons.add(dialogs.button("claims-token-to-item-button", "claims-token-to-item-description", this::doSlotToItem));
            buttons.add(dialogs.button("claims-item-to-token-button", "claims-item-to-token-description", this::doItemToSlot));
        }

        buttons.add(dialogs.button("back-menu-button", null, p -> plugin.menu().openMain(p)));

        dialogs.show(player, "claims-title", body, buttons);
    }

    private Component chunkStatusLine(UUID owner, boolean mine) {
        if (owner == null) {
            return plugin.mm().deserialize(plugin.msg("claims-chunk-free"));
        }
        if (mine) {
            return plugin.mm().deserialize(plugin.msg("claims-chunk-mine"));
        }
        OfflinePlayer offline = plugin.getServer().getOfflinePlayer(owner);
        String name = offline.getName() != null ? offline.getName() : "?";
        return plugin.mm().deserialize(plugin.msg("claims-chunk-other"), Placeholder.unparsed("owner", name));
    }

    // ------------------------------------------------------------------ actions

    private void doClaim(Player player) {
        ClaimsService.ClaimResult result = plugin.claims().claim(player);
        switch (result) {
            case OK -> player.sendMessage(plugin.mm().deserialize(plugin.msg("claims-claim-ok")));
            case ALREADY_CLAIMED -> player.sendMessage(plugin.mm().deserialize(plugin.msg("claims-claim-taken")));
            case NO_SLOTS -> player.sendMessage(plugin.mm().deserialize(plugin.msg("claims-claim-no-slots")));
            case DISABLED -> player.sendMessage(plugin.mm().deserialize(plugin.msg("no-permission")));
        }
        openMain(player);
    }

    private void doUnclaim(Player player) {
        ClaimsService.UnclaimResult result = plugin.claims().unclaim(player);
        switch (result) {
            case OK -> player.sendMessage(plugin.mm().deserialize(plugin.msg("claims-unclaim-ok")));
            case NOT_CLAIMED, NOT_OWNER -> player.sendMessage(plugin.mm().deserialize(plugin.msg("claims-unclaim-not-owner")));
            case DISABLED -> player.sendMessage(plugin.mm().deserialize(plugin.msg("no-permission")));
        }
        openMain(player);
    }

    private void doSetHome(Player player) {
        if (plugin.claims().setHome(player)) {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("claims-home-set-ok")));
        } else {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("claims-home-set-not-owner")));
        }
        openMain(player);
    }

    private void doSlotToItem(Player player) {
        if (plugin.claims().convertSlotToItem(player)) {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("claims-token-to-item-ok")));
        } else {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("claims-token-to-item-none")));
        }
        openMain(player);
    }

    private void doItemToSlot(Player player) {
        if (plugin.claims().convertItemToSlot(player)) {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("claims-item-to-token-ok")));
        } else {
            player.sendMessage(plugin.mm().deserialize(plugin.msg("claims-item-to-token-none")));
        }
        openMain(player);
    }
}
