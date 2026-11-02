package fr.kalium.encapsuleur;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Les autres plugins, chacun seulement s'il est activé : KS_ShulkerAmelioree (le craft), KS_Blocs (blocs custom),
 * SimpleClaimSystem (claims).
 */
final class Integration {

    private Integration() {
    }

    static boolean actif(String plugin) {
        return Bukkit.getPluginManager().isPluginEnabled(plugin);
    }

    // ------------------------------------------------------------------ KS_ShulkerAmelioree

    /** Une shulker améliorée vide (ingrédient de l'encapsuleur). */
    static boolean shulkerVide(ItemStack objet) {
        return actif("KS_ShulkerAmelioree") && fr.kalium.shulkeramelioree.KSShulkerAmelioree.estVide(objet);
    }

    // ------------------------------------------------------------------ KS_Blocs

    /** « type|variante » du bloc custom à cette place, ou null. */
    static String custom(Block b) {
        if (!actif("KS_Blocs")) {
            return null;
        }
        fr.kalium.blocs.TypeBloc type = fr.kalium.blocs.KSBlocs.type(b);
        if (type == null) {
            return null;
        }
        String variante = fr.kalium.blocs.KSBlocs.variante(b);
        return type.id() + "|" + (variante == null ? "" : variante);
    }

    static void retirerCustom(Block b) {
        if (actif("KS_Blocs")) {
            fr.kalium.blocs.KSBlocs.retirer(b, false);
        }
    }

    /** Retient le bloc custom à sa nouvelle place (le bloc du serveur, sa base, vient d'y être posé). */
    static void poserCustom(Block b, String id) {
        if (!actif("KS_Blocs")) {
            return;
        }
        String[] p = id.split("\\|", 2);
        fr.kalium.blocs.TypeBloc type = fr.kalium.blocs.KSBlocs.type(p[0]);
        if (type != null) {
            fr.kalium.blocs.KSBlocs.enregistrer(b, type, p.length > 1 && !p[1].isEmpty() ? p[1] : null);
        }
    }

    // ------------------------------------------------------------------ claims

    /** Propriétaire du claim de ce chunk, ou null (pas de claim, ou SimpleClaimSystem absent). */
    static UUID proprio(Chunk chunk) {
        if (!actif("SimpleClaimSystem")) {
            return null;
        }
        fr.xyness.SCS.Types.Claim claim = api().getClaimAtChunk(chunk);
        return claim == null ? null : claim.getUUID();
    }

    /** Le joueur a les droits dans le claim de ce chunk (propriétaire ou membre). */
    static boolean droits(Chunk chunk, Player joueur) {
        if (!actif("SimpleClaimSystem")) {
            return false;
        }
        fr.xyness.SCS.Types.Claim claim = api().getClaimAtChunk(chunk);
        return claim != null && (joueur.getUniqueId().equals(claim.getUUID()) || api().isClaimMember(claim, joueur));
    }

    private static fr.xyness.SCS.API.SimpleClaimSystemAPI api() {
        // SCS 1.13.1 ne crée pas son API lui-même (sans effet si un autre plugin l'a déjà fait).
        fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider.initialize((fr.xyness.SCS.SimpleClaimSystem)
                Bukkit.getPluginManager().getPlugin("SimpleClaimSystem"));
        return fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider.getAPI();
    }
}
