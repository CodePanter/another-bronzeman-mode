/*
 * Copyright (c) 2017, Aria <aria@ar1as.space>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2021, Nicholas Denaro <ndenarodev@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nicholasdenaro.groupbronzemanmode;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.grounditems.GroundItemsConfig;
import net.runelite.client.plugins.grounditems.config.MenuHighlightMode;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Boolean.TRUE;
import static net.runelite.client.plugins.grounditems.config.ItemHighlightMode.OVERLAY;
import static net.runelite.client.plugins.grounditems.config.MenuHighlightMode.*;

public class GroundItemTracker
{
    @Inject
    private Client client;

    @Inject
    TrackableItemUtil trackableItemUtil;

    @Inject
    ItemManager itemManager;

    @Inject
    private KeyManager keyManager;

    @Inject
    private GroupBronzemanModeInputListener inputListener;

    @Inject
    private GroupBronzemanModePlugin plugin;

    @Inject
    private GroupBronzemanModeConfig bconfig;

    private GroundItemsConfig gconfig;
    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private boolean hotKeyPressed;
    @Getter
    private final Map<GroundItem.GroundItemKey, GroundItem> collectedGroundItems = new LinkedHashMap<>();
    private final Queue<Integer> droppedItemQueue = EvictingQueue.create(16); // recently dropped items
    private int lastUsedItem;
    private LoadingCache<NamedQuantity, Boolean> highlightedItems;
    private LoadingCache<NamedQuantity, Boolean> hiddenItems;
    private List<PriceHighlight> priceChecks = ImmutableList.of();
    // The game won't send anything higher than this value to the plugin -
    // so we replace any item quantity higher with "Lots" instead.
    static final int MAX_QUANTITY = 65535;
    // ItemID for coins
    private static final int COINS = ItemID.COINS_995;
    private static final int FIRST_OPTION = MenuAction.GROUND_ITEM_FIRST_OPTION.getId();
    private static final int SECOND_OPTION = MenuAction.GROUND_ITEM_SECOND_OPTION.getId();
    private static final int THIRD_OPTION = MenuAction.GROUND_ITEM_THIRD_OPTION.getId(); // this is Take
    private static final int FOURTH_OPTION = MenuAction.GROUND_ITEM_FOURTH_OPTION.getId();
    private static final int FIFTH_OPTION = MenuAction.GROUND_ITEM_FIFTH_OPTION.getId();
    private static final int EXAMINE_ITEM = MenuAction.EXAMINE_ITEM_GROUND.getId();
    private static final int CAST_ON_ITEM = MenuAction.SPELL_CAST_ON_GROUND_ITEM.getId();

    private static final String TELEGRAB_TEXT = ColorUtil.wrapWithColorTag("Telekinetic Grab", Color.GREEN) + ColorUtil.prependColorTag(" -> ", Color.WHITE);

    @Inject
    private ScheduledExecutorService executor;

    public void startUp(GroundItemsConfig config)
    {
        gconfig = config;
        executor.execute(this::reset);

        keyManager.registerKeyListener(inputListener);
    }

    private List<String> hiddenItemList = new CopyOnWriteArrayList<>();
    private List<String> highlightedItemsList = new CopyOnWriteArrayList<>();

    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (event.getMenuAction() == MenuAction.ITEM_FIFTH_OPTION)
        {
            int itemId = event.getId();
            // Keep a queue of recently dropped items to better detect
            // item spawns that are drops
            droppedItemQueue.add(itemId);
        }
        else if (event.getMenuAction() == MenuAction.ITEM_USE_ON_GAME_OBJECT)
        {
            final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
            if (inventory == null)
            {
                return;
            }

            final Item clickedItem = inventory.getItem(event.getSelectedItemIndex());
            if (clickedItem == null)
            {
                return;
            }

            lastUsedItem = clickedItem.getId();
        }
    }

    public void shutDown()
    {
        highlightedItems.invalidateAll();
        highlightedItems = null;
        hiddenItems.invalidateAll();
        hiddenItems = null;
        hiddenItemList = null;
        highlightedItemsList = null;
        collectedGroundItems.clear();

        keyManager.unregisterKeyListener(inputListener);
    }

    public void onGameStateChanged(GameStateChanged e)
    {
        if (e.getGameState() == GameState.LOADING)
        {
            collectedGroundItems.clear();
        }
    }

    public void onItemSpawned(ItemSpawned itemSpawned)
    {
        TileItem item = itemSpawned.getItem();
        Tile tile = itemSpawned.getTile();

        GroundItem groundItem = buildGroundItem(tile, item);

        GroundItem.GroundItemKey groundItemKey = new GroundItem.GroundItemKey(item.getId(), tile.getWorldLocation());
        GroundItem existing = collectedGroundItems.putIfAbsent(groundItemKey, groundItem);
        if (existing != null)
        {
            existing.setQuantity(existing.getQuantity() + groundItem.getQuantity());
            // The spawn time remains set at the oldest spawn
        }
    }

    public void onItemDespawned(ItemDespawned itemDespawned)
    {
        TileItem item = itemDespawned.getItem();
        Tile tile = itemDespawned.getTile();

        GroundItem.GroundItemKey groundItemKey = new GroundItem.GroundItemKey(item.getId(), tile.getWorldLocation());
        GroundItem groundItem = collectedGroundItems.get(groundItemKey);
        if (groundItem == null)
        {
            return;
        }

        if (groundItem.getQuantity() <= item.getQuantity())
        {
            collectedGroundItems.remove(groundItemKey);
        }
        else
        {
            groundItem.setQuantity(groundItem.getQuantity() - item.getQuantity());
            // When picking up an item when multiple stacks appear on the ground,
            // it is not known which item is picked up, so we invalidate the spawn
            // time
            groundItem.setSpawnTime(null);
        }
    }

    public void onItemQuantityChanged(ItemQuantityChanged itemQuantityChanged)
    {
        TileItem item = itemQuantityChanged.getItem();
        Tile tile = itemQuantityChanged.getTile();
        int oldQuantity = itemQuantityChanged.getOldQuantity();
        int newQuantity = itemQuantityChanged.getNewQuantity();

        int diff = newQuantity - oldQuantity;
        GroundItem.GroundItemKey groundItemKey = new GroundItem.GroundItemKey(item.getId(), tile.getWorldLocation());
        GroundItem groundItem = collectedGroundItems.get(groundItemKey);
        if (groundItem != null)
        {
            groundItem.setQuantity(groundItem.getQuantity() + diff);
        }
    }

    public void onInteractingChanged(InteractingChanged event)
    {
        if (event.getSource() != client.getLocalPlayer())
        {
            return;
        }

        Actor opponent = event.getTarget();

        if (opponent == null)
        {
            lastTime = Instant.now();
            return;
        }

        lastOpponent = opponent;
    }

    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals("grounditems"))
        {
            executor.execute(this::reset);
        }
    }

    public void onPlayerLootReceived(PlayerLootReceived playerLootReceived)
    {
        Collection<ItemStack> items = playerLootReceived.getItems();
        lootReceived(items, LootType.PVP);
    }

    public void onNpcLootReceived(NpcLootReceived npcLootReceived)
    {
        Collection<ItemStack> items = npcLootReceived.getItems();
        lootReceived(items, LootType.PVM);
    }

    @Value
    static class PriceHighlight
    {
        private final int price;
        private final Color color;
    }

    private void reset()
    {
        // gets the hidden items from the text box in the config
        hiddenItemList = Text.fromCSV(gconfig.getHiddenItems());

        // gets the highlighted items from the text box in the config
        highlightedItemsList = Text.fromCSV(gconfig.getHighlightItems());

        highlightedItems = CacheBuilder.newBuilder()
                .maximumSize(512L)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new WildcardMatchLoader(highlightedItemsList));

        hiddenItems = CacheBuilder.newBuilder()
                .maximumSize(512L)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new WildcardMatchLoader(hiddenItemList));

        // Cache colors
        ImmutableList.Builder<PriceHighlight> priceCheckBuilder = ImmutableList.builder();

        if (gconfig.insaneValuePrice() > 0)
        {
            priceCheckBuilder.add(new PriceHighlight(gconfig.insaneValuePrice(), gconfig.insaneValueColor()));
        }

        if (gconfig.highValuePrice() > 0)
        {
            priceCheckBuilder.add(new PriceHighlight(gconfig.highValuePrice(), gconfig.highValueColor()));
        }

        if (gconfig.mediumValuePrice() > 0)
        {
            priceCheckBuilder.add(new PriceHighlight(gconfig.mediumValuePrice(), gconfig.mediumValueColor()));
        }

        if (gconfig.lowValuePrice() > 0)
        {
            priceCheckBuilder.add(new PriceHighlight(gconfig.lowValuePrice(), gconfig.lowValueColor()));
        }

        priceChecks = priceCheckBuilder.build();
    }

    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (gconfig.itemHighlightMode() != OVERLAY)
        {
            final boolean telegrabEntry = event.getOption().equals("Cast") && event.getTarget().startsWith(TELEGRAB_TEXT) && event.getType() == CAST_ON_ITEM;
            if (!(event.getOption().equals("Take") && event.getType() == THIRD_OPTION) && !telegrabEntry)
            {
                return;
            }

            final int itemId = event.getIdentifier();
            final int sceneX = event.getActionParam0();
            final int sceneY = event.getActionParam1();

            MenuEntry[] menuEntries = client.getMenuEntries();
            MenuEntry lastEntry = menuEntries[menuEntries.length - 1];

            final WorldPoint worldPoint = WorldPoint.fromScene(client, sceneX, sceneY, client.getPlane());
            GroundItem.GroundItemKey groundItemKey = new GroundItem.GroundItemKey(itemId, worldPoint);
            GroundItem groundItem = collectedGroundItems.get(groundItemKey);

            if (groundItem.isMine())
            {
                String icon = "<img=" + plugin.bronzemanIndicatorOffset + ">";

                if (!plugin.getUnlockedItems().contains(groundItem.getId()) && bconfig.markUnlockableLoot())
                {
                    icon = "<img=" + plugin.bronzemanUnlockableIndicatorOffset + ">";
                }

                String target = lastEntry.getTarget();
                target += icon;
                lastEntry.setTarget(target);
            }

            client.setMenuEntries(menuEntries);
        }
    }

    private static final Duration WAIT = Duration.ofSeconds(5);

    @Getter(AccessLevel.PACKAGE)
    private Actor lastOpponent;

    @Getter(AccessLevel.PACKAGE)
    @VisibleForTesting
    private Instant lastTime;

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        if (lastOpponent != null
                && lastTime != null
                && client.getLocalPlayer().getInteracting() == null)
        {
            if (Duration.between(lastTime, Instant.now()).compareTo(WAIT) > 0)
            {
                lastOpponent = null;
            }
        }
    }

    private GroundItem buildGroundItem(final Tile tile, final TileItem item)
    {
        // Collect the data for the item
        final int itemId = item.getId();
        final ItemComposition itemComposition = itemManager.getItemComposition(itemId);
        final int realItemId = itemComposition.getNote() != -1 ? itemComposition.getLinkedNoteId() : itemId;
        final int alchPrice = itemComposition.getHaPrice();
        final boolean dropped = tile.getWorldLocation().equals(client.getLocalPlayer().getWorldLocation()) && droppedItemQueue.remove(itemId);
        final boolean isNaturalSpawn = trackableItemUtil.isNaturalSpawn(tile.getWorldLocation(), item);
        final boolean opponentTile = lastOpponent != null && tile.getWorldLocation().distanceTo(lastOpponent.getWorldLocation()) < 3;
        final boolean projectile = opponentTile && trackableItemUtil.isProjectile(item);
        final boolean table = itemId == lastUsedItem && tile.getItemLayer().getHeight() > 0;

        final GroundItem groundItem = GroundItem.builder()
                .id(itemId)
                .location(tile.getWorldLocation())
                .itemId(realItemId)
                .quantity(item.getQuantity())
                .name(itemComposition.getName())
                .haPrice(alchPrice)
                .height(tile.getItemLayer().getHeight())
                .tradeable(itemComposition.isTradeable())
                .lootType(
                        isNaturalSpawn ?
                                LootType.WORLD :
                                projectile ?
                                        LootType.PROJECTILES :
                                        dropped ?
                                                LootType.DROPPED :
                                                table ?
                                                        LootType.TABLE :
                                                        LootType.UNKNOWN)
                .spawnTime(Instant.now())
                .stackable(itemComposition.isStackable())
                .build();

        // Update item price in case it is coins
        if (realItemId == COINS)
        {
            groundItem.setHaPrice(1);
            groundItem.setGePrice(1);
        }
        else
        {
            groundItem.setGePrice(itemManager.getItemPrice(realItemId));
        }

        groundItem.setHidden(getHidden(new NamedQuantity(itemComposition.getName(), item.getQuantity()), groundItem.getGePrice(), groundItem.getHaPrice(), groundItem.isTradeable()) != null);


        return groundItem;
    }

    private void lootReceived(Collection<ItemStack> items, LootType lootType)
    {
        for (ItemStack itemStack : items)
        {
            WorldPoint location = WorldPoint.fromLocal(client, itemStack.getLocation());
            GroundItem.GroundItemKey groundItemKey = new GroundItem.GroundItemKey(itemStack.getId(), location);
            GroundItem groundItem = collectedGroundItems.get(groundItemKey);
            if (groundItem != null)
            {
                groundItem.setLootType(lootType);
            }
        }
    }

    Color getHighlighted(NamedQuantity item, int gePrice, int haPrice)
    {
        return Color.white;
//        if (TRUE.equals(highlightedItems.getUnchecked(item)))
//        {
//            return config.highlightedColor();
//        }
//
//        // Explicit hide takes priority over implicit highlight
//        if (TRUE.equals(hiddenItems.getUnchecked(item)))
//        {
//            return null;
//        }
//
//        final int price = getValueByMode(gePrice, haPrice);
//        for (PriceHighlight highlight : priceChecks)
//        {
//            if (price > highlight.getPrice())
//            {
//                return highlight.getColor();
//            }
//        }
//
//        return null;
    }

    Color getHidden(NamedQuantity item, int gePrice, int haPrice, boolean isTradeable)
    {
        final boolean isExplicitHidden = TRUE.equals(hiddenItems.getUnchecked(item));
        final boolean isExplicitHighlight = TRUE.equals(highlightedItems.getUnchecked(item));
        final boolean canBeHidden = gePrice > 0 || isTradeable || !gconfig.dontHideUntradeables();
        final boolean underGe = gePrice < gconfig.getHideUnderValue();
        final boolean underHa = haPrice < gconfig.getHideUnderValue();

        // Explicit highlight takes priority over implicit hide
        return isExplicitHidden || (!isExplicitHighlight && canBeHidden && underGe && underHa)
                ? gconfig.hiddenColor()
                : null;
    }

    Color getItemColor(Color highlighted, Color hidden)
    {
        if (highlighted != null)
        {
            return highlighted;
        }

        if (hidden != null)
        {
            return hidden;
        }

//        return config.defaultColor();
        return Color.white;
    }
}
