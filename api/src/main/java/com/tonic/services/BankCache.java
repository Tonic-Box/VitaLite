package com.tonic.services;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.widgets.BankAPI;
import com.tonic.data.ItemEx;
import com.tonic.queries.InventoryQuery;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.eventbus.Subscribe;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches the contents of the bank while it is open, allowing for
 * quick lookups without repeatedly querying the bank widget.
 */
public class BankCache
{
    private static BankCache INSTANCE;

    /**
     * Retrieves the cached bank items for the current player.
     * If the bank is not open or the player is not logged in, returns an empty list.
     *
     * @return Map of item IDs to their quantities in the cached bank.
     */
    public static Int2IntMap getCachedBank()
    {
        Client client = Static.getClient();
        if(client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
            return EMPTY;
        return bankCache.getOrDefault(client.getLocalPlayer().getName(), EMPTY);
    }

    /**
     * Checks if the cached bank contains at least one instance of the specified item ID.
     *
     * @param itemId The item ID to check for.
     * @return true if the item is present in the cached bank, false otherwise.
     */
    public static boolean cachedBankContains(int itemId)
    {
        return cachedBankCount(itemId) != 0;
    }

    /**
     * Retrieves the first item ID from the cached bank that matches any of the provided item IDs.
     *
     * @param itemIds Array of item IDs to search for.
     * @return The first matching item ID, or -1 if none are found.
     */
    public static int cachedBankGetFirst(int... itemIds) {
        Int2IntMap bank = getCachedBank();
        for(int id : itemIds) {
            if(bank.containsKey(id)) // Direct containsKey is faster
                return id;
        }
        return -1;
    }

    /**
     * Counts the number of instances of the specified item ID in the cached bank.
     *
     * @param itemId The item ID to count.
     * @return The count of the specified item ID in the cached bank.
     */
    public static int cachedBankCount(int itemId) {
        var bank = getCachedBank();
        return bank.getOrDefault(itemId, 0);
    }

    private static final ConcurrentHashMap<String,Int2IntMap> bankCache = new ConcurrentHashMap<>();
    private static final
    Int2IntMap EMPTY = Int2IntMaps.unmodifiable(new Int2IntOpenHashMap());
    private final ConfigManager configManager = new ConfigManager("CachedBanks");

    @Subscribe
    protected void onGameTick(GameTick event)
    {
        Client client = Static.getClient();

        if(client.getLocalPlayer() == null)
            return;
        String playerName = client.getLocalPlayer().getName();
        if(playerName == null)
            return;

        if(!bankCache.containsKey(playerName) && Static.getVitaConfig().shouldCacheBank())
        {
            fetch();
        }

        if(BankAPI.isOpen())
        {
            Int2IntMap emptyMap = new Int2IntOpenHashMap();
            List<ItemEx> items = InventoryQuery.fromInventoryId(InventoryID.BANK).collect();
            for(ItemEx item : items)
            {
                int currentQty = emptyMap.getOrDefault(item.getCanonicalId(), 0);
                emptyMap.put(item.getCanonicalId(), item.getQuantity() + currentQty);
            }

            Int2IntMap itemMap = bankCache.getOrDefault(playerName, EMPTY);
            if(!itemMap.equals(emptyMap))
            {
                bankCache.put(playerName, emptyMap);
                if(Static.getVitaConfig().shouldCacheBank())
                {
                    String serialized = serialize(emptyMap);
                    configManager.setProperty(playerName, serialized);
                }
            }
        }
    }

    private void fetch()
    {
        Client client = Static.getClient();
        if(client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
            return;

        String serialized = configManager.getStringOrDefault(client.getLocalPlayer().getName(), "");
        Int2IntMap map = deserialize(serialized);
        bankCache.put(client.getLocalPlayer().getName(), map);
        Logger.info("[Loaded] cached bank for " + client.getLocalPlayer().getName());
    }

    static void init()
    {
        if(INSTANCE != null)
            return;

        INSTANCE = new BankCache();
        Static.getRuneLite()
                .getEventBus()
                .register(INSTANCE);
    }

    public static String serialize(Int2IntMap map) {
        if (map.isEmpty()) return "";

        ByteBuffer buffer = ByteBuffer.allocate(4 + map.size() * 8);
        buffer.putInt(map.size());

        map.int2IntEntrySet().forEach(entry -> {
            buffer.putInt(entry.getIntKey());
            buffer.putInt(entry.getIntValue());
        });

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    public static Int2IntMap deserialize(String str) {
        if (str == null || str.isEmpty()) {
            return EMPTY;
        }

        try
        {
            byte[] bytes = Base64.getDecoder().decode(str);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            int size = buffer.getInt();
            Int2IntOpenHashMap map = new Int2IntOpenHashMap(size);
            for (int i = 0; i < size; i++) {
                map.put(buffer.getInt(), buffer.getInt());
            }
            return map;
        }
        catch (Throwable e)
        {
            Logger.error("Failed to deserialize bank cache: " + e.getMessage());
            return EMPTY;
        }
    }
}
