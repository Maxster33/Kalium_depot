package fr.kalium.supertool;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry.EnchantmentCost;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlotGroup;

/**
 * Enregistre l'enchantement Magnétisme au démarrage du serveur (le registre des enchantements se ferme ensuite).
 *
 * Mêmes réglages que Fortune (choix de LeKiwi06 du 20/11 : « même proba que le fortune ») : poids 2, 3 niveaux, coût
 * minimal 15 + 9 par niveau, maximal 65 + 9 par niveau, coût à l'enclume 4. Rayon d'attraction : 3 / 5 / 7 blocs.
 *
 * L'enchantement n'est mis dans aucun tag du jeu (table d'enchantement, butin, villageois) : seul KS_SuperTool le propose
 * à la table (classe Table), pour que le déploiement en deux temps (KS_MondeTest) le garde dans le monde de test tant
 * que le plugin n'est pas validé. Objets acceptés : les bases des machines (masse, hache, pelle, houe en diamant et
 * en netherite) ; l'enclume le refuse sur un outil ordinaire (classe Enclume).
 */
public final class Demarrage implements PluginBootstrap {

    /** Clé de l'enchantement : « kalium » plutôt que le nom du plugin, d'autres outils pourront le recevoir. */
    public static final Key MAGNETISME = Key.key("kalium", "magnetisme");

    @Override
    public void bootstrap(BootstrapContext contexte) {
        contexte.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event ->
                event.registry().register(TypedKey.create(RegistryKey.ENCHANTMENT, MAGNETISME), b -> b
                        .description(Component.text("Magnétisme"))
                        .supportedItems(RegistrySet.keySet(RegistryKey.ITEM, ItemTypeKeys.MACE,
                                ItemTypeKeys.DIAMOND_AXE, ItemTypeKeys.NETHERITE_AXE, ItemTypeKeys.DIAMOND_SHOVEL,
                                ItemTypeKeys.NETHERITE_SHOVEL, ItemTypeKeys.DIAMOND_HOE, ItemTypeKeys.NETHERITE_HOE))
                        .weight(2)
                        .maxLevel(3)
                        .minimumCost(EnchantmentCost.of(15, 9))
                        .maximumCost(EnchantmentCost.of(65, 9))
                        .anvilCost(4)
                        .activeSlots(EquipmentSlotGroup.HAND))));
    }
}
