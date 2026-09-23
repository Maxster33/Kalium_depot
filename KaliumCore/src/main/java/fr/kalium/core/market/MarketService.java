package fr.kalium.core.market;

import fr.kalium.core.KaliumCore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Systeme de commerce entre joueurs : annonces de vente ("Marche"), ventes flash programmees par les
 * operateurs ("Vente flash"), recherche (bilingue FR/EN, voir ItemNames), historiques separes des
 * ventes et des achats, et coffre de recompenses personnel (les objets achetes et les emeraudes
 * issues d'une vente y sont deposes, pour rester accessibles meme hors-ligne). La monnaie est
 * l'emeraude (objet physique compte/retire dans l'inventaire du joueur, pas un solde virtuel).
 * <p>
 * Stockage autonome : un seul fichier {@code plugins/KaliumCore/market.yml} (meme approche que
 * claims.yml), avec les ItemStack serialises nativement par YamlConfiguration (ConfigurationSerializable).
 */
public final class MarketService {

    public enum ListResult { OK, DISABLED, EMPTY_HAND, LIMIT_REACHED, BAD_PRICE }

    public enum BuyResult { OK, DISABLED, NOT_FOUND, OWN_LISTING, NOT_ENOUGH_EMERALDS }

    public enum ScheduleResult { OK, DISABLED, EMPTY_HAND, BAD_PARAMS }

    /** Nombre d'objets par page pour toutes les pages type "coffre" du commerce (Marche, Recherche, Vente flash, Mes ventes, Rewards). */
    public static final int PAGE_SIZE = 45;

    /** Duree maximale d'une annonce, choisie par le joueur a la mise en vente (voir createListing). */
    public static final int MAX_LISTING_DURATION_DAYS = 7;

    private final KaliumCore plugin;
    private final File file;
    private final ItemNames itemNames;

    private final Map<Long, MarketListing> listings = new ConcurrentHashMap<>();
    private final Map<Long, FlashSale> flashSales = new ConcurrentHashMap<>();
    /** Coffre de recompenses par joueur : une liste simple (pas de case fixe), paginee a l'affichage (voir rewardPageItems). */
    private final Map<UUID, List<ItemStack>> rewardStores = new ConcurrentHashMap<>();
    private final Map<UUID, List<TransactionRecord>> history = new ConcurrentHashMap<>();
    private final Map<Material, long[]> priceStats = new ConcurrentHashMap<>(); // [0]=count, [1]=total

    private final AtomicLong nextId = new AtomicLong(1);

    private static final int HISTORY_LIMIT = 200;

    public MarketService(KaliumCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "market.yml");
        this.itemNames = new ItemNames(plugin);
    }

    // ------------------------------------------------------------------ reglages (config.yml)

    public boolean masterEnabled() {
        return plugin.getConfig().getBoolean("modules.market.enabled", true);
    }

    public int maxListingsPerPlayer() {
        return Math.max(0, plugin.getConfig().getInt("modules.market.max-listings", 10));
    }

    public void maxListingsPerPlayer(int value) {
        plugin.getConfig().set("modules.market.max-listings", Math.max(0, value));
    }

    /** Duree (en jours) proposee par defaut dans le champ de saisie de la mise en vente - le joueur peut la modifier, plafonnee a MAX_LISTING_DURATION_DAYS. */
    public int defaultListingDurationDays() {
        return Math.max(1, Math.min(MAX_LISTING_DURATION_DAYS, plugin.getConfig().getInt("modules.market.listing-duration-days", 7)));
    }

    public void defaultListingDurationDays(int value) {
        plugin.getConfig().set("modules.market.listing-duration-days", Math.max(1, Math.min(MAX_LISTING_DURATION_DAYS, value)));
    }

    // ------------------------------------------------------------------ annonces ("Marche")

    public Collection<MarketListing> listingsRaw() {
        return listings.values();
    }

    /** Annonces actives triees des plus recentes aux plus anciennes, sans filtre (ecran "Marche"). */
    public List<MarketListing> activeListings() {
        List<MarketListing> all = new ArrayList<>(listings.values());
        all.sort(Comparator.comparingLong(MarketListing::listedAt).reversed());
        return all;
    }

    /** Annonces actives dont le nom (technique, anglais ou francais) contient le mot-cle (voir ItemNames). */
    public List<MarketListing> searchListings(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return activeListings();
        }
        String needle = ItemNames.normalize(keyword.trim());
        List<MarketListing> out = new ArrayList<>();
        for (MarketListing listing : activeListings()) {
            if (searchText(listing.item()).contains(needle)) {
                out.add(listing);
            }
        }
        return out;
    }

    public List<MarketListing> listingsBySeller(UUID seller) {
        List<MarketListing> out = new ArrayList<>();
        for (MarketListing listing : listings.values()) {
            if (listing.seller().equals(seller)) {
                out.add(listing);
            }
        }
        out.sort(Comparator.comparingLong(MarketListing::listedAt).reversed());
        return out;
    }

    public MarketListing listing(long id) {
        return listings.get(id);
    }

    /** Met en vente l'objet actuellement tenu en main par le joueur, pour la duree choisie (1-7 jours, voir la javadoc de classe). */
    public ListResult createListing(Player seller, int price, int quantity, int durationDays) {
        if (!masterEnabled()) {
            return ListResult.DISABLED;
        }
        if (price <= 0) {
            return ListResult.BAD_PRICE;
        }
        int limit = maxListingsPerPlayer();
        if (limit > 0 && listingsBySeller(seller.getUniqueId()).size() >= limit) {
            return ListResult.LIMIT_REACHED;
        }
        int slot = seller.getInventory().getHeldItemSlot();
        ItemStack held = seller.getInventory().getItem(slot);
        if (held == null || held.getType().isAir()) {
            return ListResult.EMPTY_HAND;
        }
        int qty = Math.max(1, Math.min(quantity, held.getAmount()));
        int days = Math.max(1, Math.min(MAX_LISTING_DURATION_DAYS, durationDays));

        ItemStack listed = held.clone();
        listed.setAmount(qty);

        // Meme technique que ClaimsService.convertItemToSlot : ecrire explicitement via setItem (et
        // updateInventory) plutot que de muter l'ItemStack en place, pour que le client se mette bien
        // a jour.
        if (held.getAmount() <= qty) {
            seller.getInventory().setItem(slot, null);
        } else {
            ItemStack remaining = held.clone();
            remaining.setAmount(held.getAmount() - qty);
            seller.getInventory().setItem(slot, remaining);
        }
        seller.updateInventory();

        long id = nextId.getAndIncrement();
        long now = System.currentTimeMillis();
        long expiresAt = now + days * 86_400_000L;
        listings.put(id, new MarketListing(id, seller.getUniqueId(), seller.getName(), listed, price, now, expiresAt));
        save();
        return ListResult.OK;
    }

    /** Annule une annonce : l'objet est rendu au vendeur via son coffre de recompenses. */
    public boolean cancelListing(Player player, long id) {
        MarketListing listing = listings.get(id);
        if (listing == null || !listing.seller().equals(player.getUniqueId())) {
            return false;
        }
        listings.remove(id);
        depositReward(player.getUniqueId(), listing.item().clone());
        save();
        return true;
    }

    public BuyResult buyListing(Player buyer, long id) {
        if (!masterEnabled()) {
            return BuyResult.DISABLED;
        }
        MarketListing listing = listings.get(id);
        if (listing == null) {
            return BuyResult.NOT_FOUND;
        }
        if (listing.seller().equals(buyer.getUniqueId())) {
            return BuyResult.OWN_LISTING;
        }
        if (countEmeralds(buyer) < listing.price()) {
            return BuyResult.NOT_ENOUGH_EMERALDS;
        }
        listings.remove(id);
        removeEmeralds(buyer, listing.price());
        depositReward(buyer.getUniqueId(), listing.item().clone());
        depositReward(listing.seller(), emeraldStack(listing.price()));

        long now = System.currentTimeMillis();
        recordSalePrice(listing.item().getType(), listing.price());
        recordTransaction(listing.seller(), new TransactionRecord(
                TransactionRecord.Type.VENTE, listing.item(), listing.price(), buyer.getName(), now));
        recordTransaction(buyer.getUniqueId(), new TransactionRecord(
                TransactionRecord.Type.ACHAT, listing.item(), listing.price(), listing.sellerName(), now));
        save();
        return BuyResult.OK;
    }

    // ------------------------------------------------------------------ ventes flash

    public Collection<FlashSale> flashSalesRaw() {
        return flashSales.values();
    }

    /** Ventes flash actives (en cours, stock disponible), triees par date de fin croissante. */
    public List<FlashSale> activeFlashSales() {
        long now = System.currentTimeMillis();
        List<FlashSale> out = new ArrayList<>();
        for (FlashSale sale : flashSales.values()) {
            if (sale.status(now) == FlashSale.Status.ACTIVE || sale.status(now) == FlashSale.Status.UPCOMING) {
                out.add(sale);
            }
        }
        out.sort(Comparator.comparingLong(FlashSale::startAt));
        return out;
    }

    /** Toutes les ventes flash non purgees (vue admin), triees par date de debut decroissante. */
    public List<FlashSale> allFlashSales() {
        List<FlashSale> out = new ArrayList<>(flashSales.values());
        out.sort(Comparator.comparingLong(FlashSale::startAt).reversed());
        return out;
    }

    public FlashSale flashSale(long id) {
        return flashSales.get(id);
    }

    /** Programme une vente flash a partir de l'objet tenu en main par l'operateur (quantite = lot vendu a chaque achat). */
    public ScheduleResult scheduleFlashSale(Player admin, int price, int stock, int delayMinutes, int durationMinutes) {
        if (!masterEnabled()) {
            return ScheduleResult.DISABLED;
        }
        if (price <= 0 || stock <= 0 || durationMinutes <= 0 || delayMinutes < 0) {
            return ScheduleResult.BAD_PARAMS;
        }
        ItemStack held = admin.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            return ScheduleResult.EMPTY_HAND;
        }
        ItemStack template = held.clone();
        long id = nextId.getAndIncrement();
        long now = System.currentTimeMillis();
        long start = now + delayMinutes * 60_000L;
        long end = start + durationMinutes * 60_000L;
        flashSales.put(id, new FlashSale(id, template, price, stock, start, end, admin.getName()));
        save();
        return ScheduleResult.OK;
    }

    public boolean cancelFlashSale(long id) {
        boolean removed = flashSales.remove(id) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public BuyResult buyFlashSale(Player buyer, long id) {
        if (!masterEnabled()) {
            return BuyResult.DISABLED;
        }
        FlashSale sale = flashSales.get(id);
        if (sale == null || sale.status(System.currentTimeMillis()) != FlashSale.Status.ACTIVE) {
            return BuyResult.NOT_FOUND;
        }
        if (countEmeralds(buyer) < sale.price()) {
            return BuyResult.NOT_ENOUGH_EMERALDS;
        }
        removeEmeralds(buyer, sale.price());
        sale.purchased(sale.purchased() + 1);
        depositReward(buyer.getUniqueId(), sale.item().clone());
        recordTransaction(buyer.getUniqueId(), new TransactionRecord(
                TransactionRecord.Type.VENTE_FLASH, sale.item(), sale.price(), sale.createdBy(), System.currentTimeMillis()));
        save();
        return BuyResult.OK;
    }

    // ------------------------------------------------------------------ coffre de recompenses (liste paginee)

    private List<ItemStack> rewardList(UUID id) {
        return rewardStores.computeIfAbsent(id, k -> new ArrayList<>());
    }

    public int rewardCount(UUID id) {
        List<ItemStack> list = rewardStores.get(id);
        return list == null ? 0 : list.size();
    }

    public int rewardTotalPages(UUID id) {
        return Math.max(1, (int) Math.ceil(rewardCount(id) / (double) PAGE_SIZE));
    }

    /** Copie des objets de la page demandee (0-based), pour affichage dans une case coffre synthetique. */
    public List<ItemStack> rewardPageItems(UUID id, int page) {
        List<ItemStack> list = rewardList(id);
        int start = Math.max(0, page) * PAGE_SIZE;
        if (start >= list.size()) {
            return List.of();
        }
        int end = Math.min(list.size(), start + PAGE_SIZE);
        return new ArrayList<>(list.subList(start, end));
    }

    /**
     * Reconcilie une page apres fermeture : remplace la tranche [page*PAGE_SIZE, ...) par les objets
     * restants (ceux que le joueur n'a pas retires), dans l'ordre. Les objets absents des "survivors"
     * ont ete retires par le joueur (voir MarketGuiListener) - jamais perdus, juste sortis de la liste.
     */
    public void reconcileRewardPage(UUID id, int page, List<ItemStack> survivors) {
        List<ItemStack> list = rewardList(id);
        int start = Math.max(0, page) * PAGE_SIZE;
        if (start > list.size()) {
            start = list.size();
        }
        int end = Math.min(list.size(), start + PAGE_SIZE);
        List<ItemStack> clean = new ArrayList<>();
        for (ItemStack stack : survivors) {
            if (stack != null && !stack.getType().isAir()) {
                clean.add(stack);
            }
        }
        list.subList(start, end).clear();
        list.addAll(start, clean);
        save();
    }

    /** Depose un gain (objet achete, emeraudes d'une vente...) : fusionne dans les piles compatibles existantes, sinon ajoute de nouvelles piles (max une taille de pile par entree, comme un vrai inventaire). */
    public void depositReward(UUID id, ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return;
        }
        List<ItemStack> list = rewardList(id);
        int remaining = item.getAmount();
        int maxStack = item.getMaxStackSize();

        for (ItemStack existing : list) {
            if (remaining <= 0) {
                break;
            }
            if (existing.getAmount() >= existing.getMaxStackSize() || !existing.isSimilar(item)) {
                continue;
            }
            int room = existing.getMaxStackSize() - existing.getAmount();
            int add = Math.min(room, remaining);
            existing.setAmount(existing.getAmount() + add);
            remaining -= add;
        }

        while (remaining > 0) {
            int amount = Math.min(maxStack, remaining);
            ItemStack stack = item.clone();
            stack.setAmount(amount);
            list.add(stack);
            remaining -= amount;
        }
        save();
    }

    // ------------------------------------------------------------------ emeraudes (monnaie)

    public int countEmeralds(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == Material.EMERALD) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void removeEmeralds(Player player, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != Material.EMERALD) {
                continue;
            }
            int take = Math.min(stack.getAmount(), remaining);
            if (stack.getAmount() <= take) {
                player.getInventory().setItem(i, null);
            } else {
                ItemStack copy = stack.clone();
                copy.setAmount(stack.getAmount() - take);
                player.getInventory().setItem(i, copy);
            }
            remaining -= take;
        }
        player.updateInventory();
    }

    private ItemStack emeraldStack(int amount) {
        return new ItemStack(Material.EMERALD, Math.max(1, amount));
    }

    // ------------------------------------------------------------------ historique (separe ventes / achats)

    public List<TransactionRecord> history(UUID id) {
        List<TransactionRecord> list = history.get(id);
        return list == null ? List.of() : list;
    }

    /** Historique des ventes (annonces vendues) - pas les ventes flash, qui n'ont pas de vendeur joueur. */
    public List<TransactionRecord> salesHistory(UUID id) {
        List<TransactionRecord> out = new ArrayList<>();
        for (TransactionRecord record : history(id)) {
            if (record.type() == TransactionRecord.Type.VENTE) {
                out.add(record);
            }
        }
        return out;
    }

    /** Historique des achats - annonces achetees a un autre joueur ET achats en vente flash. */
    public List<TransactionRecord> purchaseHistory(UUID id) {
        List<TransactionRecord> out = new ArrayList<>();
        for (TransactionRecord record : history(id)) {
            if (record.type() == TransactionRecord.Type.ACHAT || record.type() == TransactionRecord.Type.VENTE_FLASH) {
                out.add(record);
            }
        }
        return out;
    }

    private void recordTransaction(UUID id, TransactionRecord record) {
        List<TransactionRecord> list = history.computeIfAbsent(id, k -> new ArrayList<>());
        list.add(0, record);
        while (list.size() > HISTORY_LIMIT) {
            list.remove(list.size() - 1);
        }
    }

    // ------------------------------------------------------------------ prix de vente moyen

    /** Prix moyen historique (ventes normales uniquement, pas les ventes flash qui sont des prix reduits ponctuels). */
    public Double averagePrice(Material material) {
        long[] stat = priceStats.get(material);
        if (stat == null || stat[0] <= 0) {
            return null;
        }
        return (double) stat[1] / (double) stat[0];
    }

    private void recordSalePrice(Material material, int price) {
        long[] stat = priceStats.computeIfAbsent(material, k -> new long[2]);
        stat[0]++;
        stat[1] += price;
    }

    /** Texte de recherche d'un objet : identifiant technique + nom personnalise + noms anglais/francais officiels (voir ItemNames), normalise (minuscules, sans accents). */
    private String searchText(ItemStack item) {
        StringBuilder text = new StringBuilder(item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' '));
        String english = itemNames.english(item.getType());
        if (english != null) {
            text.append(' ').append(english);
        }
        String french = itemNames.french(item.getType());
        if (french != null) {
            text.append(' ').append(french);
        }
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            text.append(' ').append(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(item.getItemMeta().displayName()));
        }
        return ItemNames.normalize(text.toString());
    }

    // ------------------------------------------------------------------ tache periodique

    /** Expire les annonces (objet rendu au coffre de recompenses du vendeur) et purge les ventes flash terminees depuis longtemps. */
    public void tick() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (Iterator<MarketListing> it = listings.values().iterator(); it.hasNext(); ) {
            MarketListing listing = it.next();
            if (now > listing.expiresAt()) {
                depositReward(listing.seller(), listing.item().clone());
                it.remove();
                changed = true;
            }
        }

        // Grace de 24h apres la fin pour que l'ecran d'administration garde une trace recente.
        for (Iterator<FlashSale> it = flashSales.values().iterator(); it.hasNext(); ) {
            FlashSale sale = it.next();
            if (now > sale.endAt() + 86_400_000L) {
                it.remove();
                changed = true;
            }
        }

        if (changed) {
            save();
        }
    }

    // ------------------------------------------------------------------ chargement / sauvegarde

    public void load() {
        listings.clear();
        flashSales.clear();
        rewardStores.clear();
        history.clear();
        priceStats.clear();
        nextId.set(1);
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        nextId.set(Math.max(1, config.getLong("next-id", 1)));

        ConfigurationSection listingsSection = config.getConfigurationSection("listings");
        if (listingsSection != null) {
            for (String key : listingsSection.getKeys(false)) {
                try {
                    ConfigurationSection s = listingsSection.getConfigurationSection(key);
                    if (s == null) {
                        continue;
                    }
                    long id = Long.parseLong(key);
                    UUID seller = UUID.fromString(s.getString("seller"));
                    String sellerName = s.getString("seller-name", "?");
                    ItemStack item = s.getItemStack("item");
                    int price = s.getInt("price");
                    long listedAt = s.getLong("listed-at");
                    long expiresAt = s.getLong("expires-at");
                    if (item != null) {
                        listings.put(id, new MarketListing(id, seller, sellerName, item, price, listedAt, expiresAt));
                    }
                } catch (RuntimeException ignored) {
                    // Ligne corrompue/illisible : ignoree plutot que de bloquer le chargement.
                }
            }
        }

        ConfigurationSection flashSection = config.getConfigurationSection("flash-sales");
        if (flashSection != null) {
            for (String key : flashSection.getKeys(false)) {
                try {
                    ConfigurationSection s = flashSection.getConfigurationSection(key);
                    if (s == null) {
                        continue;
                    }
                    long id = Long.parseLong(key);
                    ItemStack item = s.getItemStack("item");
                    if (item == null) {
                        continue;
                    }
                    FlashSale sale = new FlashSale(id, item, s.getInt("price"), s.getInt("stock"),
                            s.getLong("start-at"), s.getLong("end-at"), s.getString("created-by", "?"));
                    sale.purchased(s.getInt("purchased", 0));
                    flashSales.put(id, sale);
                } catch (RuntimeException ignored) {
                    // Ligne corrompue/illisible : ignoree.
                }
            }
        }

        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String uuidStr : rewardsSection.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(uuidStr);
                    List<?> raw = rewardsSection.getList(uuidStr + ".items");
                    if (raw == null) {
                        continue;
                    }
                    List<ItemStack> list = rewardList(id);
                    for (Object entry : raw) {
                        if (entry instanceof ItemStack stack && !stack.getType().isAir()) {
                            list.add(stack);
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    // UUID invalide : ligne ignoree.
                }
            }
        }

        ConfigurationSection historySection = config.getConfigurationSection("history");
        if (historySection != null) {
            for (String uuidStr : historySection.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(uuidStr);
                    ConfigurationSection playerSection = historySection.getConfigurationSection(uuidStr);
                    if (playerSection == null) {
                        continue;
                    }
                    List<TransactionRecord> list = new ArrayList<>();
                    List<String> orderedKeys = new ArrayList<>(playerSection.getKeys(false));
                    orderedKeys.sort(Comparator.comparingInt(Integer::parseInt));
                    for (String indexKey : orderedKeys) {
                        ConfigurationSection entry = playerSection.getConfigurationSection(indexKey);
                        if (entry == null) {
                            continue;
                        }
                        ItemStack item = entry.getItemStack("item");
                        if (item == null) {
                            continue;
                        }
                        TransactionRecord.Type type = TransactionRecord.Type.valueOf(entry.getString("type", "ACHAT"));
                        list.add(new TransactionRecord(type, item, entry.getInt("price"),
                                entry.getString("counterpart", "?"), entry.getLong("at")));
                    }
                    if (!list.isEmpty()) {
                        history.put(id, list);
                    }
                } catch (RuntimeException ignored) {
                    // Ligne corrompue/illisible : ignoree.
                }
            }
        }

        ConfigurationSection priceSection = config.getConfigurationSection("price-stats");
        if (priceSection != null) {
            for (String materialName : priceSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(materialName);
                    ConfigurationSection s = priceSection.getConfigurationSection(materialName);
                    if (s == null) {
                        continue;
                    }
                    priceStats.put(material, new long[] { s.getLong("count"), s.getLong("total") });
                } catch (IllegalArgumentException ignored) {
                    // Materiau inconnu (version differente du jeu) : ligne ignoree.
                }
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("next-id", nextId.get());

        for (MarketListing listing : new ArrayList<>(listings.values())) {
            String base = "listings." + listing.id();
            config.set(base + ".seller", listing.seller().toString());
            config.set(base + ".seller-name", listing.sellerName());
            config.set(base + ".item", listing.item());
            config.set(base + ".price", listing.price());
            config.set(base + ".listed-at", listing.listedAt());
            config.set(base + ".expires-at", listing.expiresAt());
        }

        for (FlashSale sale : new ArrayList<>(flashSales.values())) {
            String base = "flash-sales." + sale.id();
            config.set(base + ".item", sale.item());
            config.set(base + ".price", sale.price());
            config.set(base + ".stock", sale.stock());
            config.set(base + ".purchased", sale.purchased());
            config.set(base + ".start-at", sale.startAt());
            config.set(base + ".end-at", sale.endAt());
            config.set(base + ".created-by", sale.createdBy());
        }

        for (Map.Entry<UUID, List<ItemStack>> entry : new HashMap<>(rewardStores).entrySet()) {
            if (!entry.getValue().isEmpty()) {
                config.set("rewards." + entry.getKey() + ".items", new ArrayList<>(entry.getValue()));
            }
        }

        for (Map.Entry<UUID, List<TransactionRecord>> entry : new HashMap<>(history).entrySet()) {
            List<TransactionRecord> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                TransactionRecord record = list.get(i);
                String base = "history." + entry.getKey() + "." + i;
                config.set(base + ".type", record.type().name());
                config.set(base + ".item", record.item());
                config.set(base + ".price", record.price());
                config.set(base + ".counterpart", record.counterpart());
                config.set(base + ".at", record.at());
            }
        }

        for (Map.Entry<Material, long[]> entry : new HashMap<>(priceStats).entrySet()) {
            String base = "price-stats." + entry.getKey().name();
            config.set(base + ".count", entry.getValue()[0]);
            config.set(base + ".total", entry.getValue()[1]);
        }

        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer market.yml : " + e.getMessage());
        }
    }
}
