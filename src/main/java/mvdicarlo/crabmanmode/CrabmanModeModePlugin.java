/*
 * Copyright (c) 2019, CodePanter <https://github.com/codepanter>
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
package mvdicarlo.crabmanmode;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Runnables;
import com.google.inject.Provides;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ChatPlayer;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.GameState;
import net.runelite.api.IconID;
import net.runelite.api.IndexedSprite;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.MessageNode;
import net.runelite.api.NameableContainer;
import net.runelite.api.Player;
import net.runelite.api.ScriptEvent;
import net.runelite.api.ScriptID;
import net.runelite.api.SoundEffectID;
import net.runelite.api.SpriteID;
import net.runelite.api.WorldType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.WorldService;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.game.chatbox.ChatboxTextInput;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

@Slf4j
@PluginDescriptor(name = "Crabman Mode", description = "Modification of bronzeman mode plugin. Limits access to buying an item on the Grand Exchange until it is obtained otherwise.", tags = {
        "overlay", "bronzeman", "crabman", "group bronzeman" })
public class CrabmanModeModePlugin extends Plugin {
    static final String CONFIG_GROUP = "crabmanmode";
    private static final String BM_UNLOCKS_STRING = "!bmunlocks";
    private static final String BM_COUNT_STRING = "!bmcount";

    final int COMBAT_ACHIEVEMENT_BUTTON = 20;
    final int COLLECTION_LOG_GROUP_ID = 621;
    final int COLLECTION_VIEW = 36;
    final int COLLECTION_VIEW_SCROLLBAR = 37;
    final int COLLECTION_VIEW_HEADER = 19;

    final int COLLECTION_VIEW_CATEGORIES_CONTAINER = 28;
    final int COLLECTION_VIEW_CATEGORIES_RECTANGLE = 33;
    final int COLLECTION_VIEW_CATEGORIES_TEXT = 34;
    final int COLLECTION_VIEW_CATEGORIES_SCROLLBAR = 28;

    final int MENU_INSPECT = 2;
    final int MENU_DELETE = 3;

    final int SELECTED_OPACITY = 200;
    final int UNSELECTED_OPACITY = 235;

    private static final int GE_SEARCH_RESULTS = 50;
    private static final int GE_SEARCH_BUILD_SCRIPT = 751;

    private static final int COLLECTION_LOG_OPEN_OTHER = 2728;
    private static final int COLLECTION_LOG_DRAW_LIST = 2730;
    private static final int COLLECTION_LOG_ITEM_CLICK = 2733;

    static final Set<Integer> OWNED_INVENTORY_IDS = ImmutableSet.of(
            0, // Reward from fishing trawler.
            93, // Standard player inventory.
            94, // Equipment inventory.
            95, // Bank inventory.
            141, // Barrows reward chest inventory.
            390, // Kingdom Of Miscellania reward inventory.
            581, // Chambers of Xeric chest inventory.
            612, // Theater of Blood reward chest inventory (Raids 2).
            626); // Seed vault located inside the Farming Guild.

    @Inject
    private Client client;

    @Inject
    private ChatboxPanelManager chatboxPanelManager;

    @Inject
    private Notifier notifier;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ItemManager itemManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private WorldService worldService;

    @Inject
    private ConfigManager configManager;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private ChatCommandManager chatCommandManager;

    @Inject
    private CrabmanModeModeConfig config;

    @Inject
    private CrabmanModeModeOverlay AnotherBronzemanModeOverlay;

    @Getter
    private BufferedImage unlockImage = null;

    private static final String SCRIPT_EVENT_SET_CHATBOX_INPUT = "setChatboxInput";

    private ChatboxTextInput searchInput;
    private Widget searchButton;
    private Collection<Widget> itemEntries;

    private List<String> namesBronzeman = new ArrayList<>();
    private String enabledCrabman = "";
    private int bronzemanIconOffset = -1; // offset for bronzeman icon
    private boolean onSeasonalWorld;
    private boolean deleteConfirmed = false;

    private final CrabmanModeStorageTableRepo db = new CrabmanModeStorageTableRepo();
    private ScheduledExecutorService scheduler;

    @Provides
    CrabmanModeModeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(CrabmanModeModeConfig.class);
    }

    private boolean isLoggedIntoCrabman() {
        String playerName = client.getLocalPlayer().getName();
        if (playerName == null || enabledCrabman == null || enabledCrabman.isEmpty()) {
            return false;
        }
        return playerName.equals(enabledCrabman);
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();
        onSeasonalWorld = false;
        updateNamesBronzeman();
        updateAllowedCrabman();
        initializeDatabase();
        loadResources();
        db.addUnlockedItemsListener((items) -> onItemsUnlocked(items));
        overlayManager.add(AnotherBronzemanModeOverlay);
        chatCommandManager.registerCommand(BM_UNLOCKS_STRING, this::OnUnlocksCountCommand);
        chatCommandManager.registerCommand(BM_COUNT_STRING, this::OnUnlocksCountCommand);

        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer == null) {
                return;
            }
            String username = localPlayer.getName();
            if (username == null || username.isEmpty()) {
                return;
            }
            if (username.equals(enabledCrabman)) {
                db.setUser(username);
                if (!db.isInitialized()) {
                    log.info(username + " is a crabman. Initializing database.");
                    initializeDatabase();
                }
            } else {
                db.close();
            }
        }, 0, 1, TimeUnit.SECONDS);

        clientThread.invoke(() -> {
            if (client.getGameState() == GameState.LOGGED_IN) {
                onSeasonalWorld = isSeasonalWorld(client.getWorld());
                // A player can not be a bronzeman on a seasonal world.
                if (!onSeasonalWorld) {
                    setChatboxName(getNameChatbox());
                }
            }
        });
    }

    @Override
    protected void shutDown() throws Exception {
        super.shutDown();
        itemEntries = null;
        db.close();
        overlayManager.remove(AnotherBronzemanModeOverlay);
        chatCommandManager.unregisterCommand(BM_UNLOCKS_STRING);
        chatCommandManager.unregisterCommand(BM_COUNT_STRING);

        clientThread.invoke(() -> {
            // Cleanup is not required after having played on a seasonal world.
            if (client.getGameState() == GameState.LOGGED_IN && !onSeasonalWorld) {
                setChatboxName(getNameDefault());
            }
        });
    }

    /** Loads players unlocks on login **/
    @Subscribe
    public void onGameStateChanged(GameStateChanged e) {
        if (e.getGameState() == GameState.LOGGED_IN) {
            unlockDefaultItems();
            loadResources();

            onSeasonalWorld = isSeasonalWorld(client.getWorld());
        }
        if (e.getGameState() == GameState.LOGIN_SCREEN) {
            itemEntries = null;
            db.close();
        }
    }

    @Subscribe
    public void onPluginChanged(PluginChanged e) {
        if (e.getPlugin() == this && client.getGameState() == GameState.LOGGED_IN) {
            // setupUnlockHistory();
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded e) {
        if (e.getGroupId() != COLLECTION_LOG_GROUP_ID || config.moveCollectionLogUnlocks()) {
            return;
        }

        Widget collectionViewHeader = client.getWidget(COLLECTION_LOG_GROUP_ID, COLLECTION_VIEW_HEADER);
        openBronzemanCategory(collectionViewHeader);
    }

    /** Unlocks all new items that are currently not unlocked **/
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged e) {
        if (OWNED_INVENTORY_IDS.contains(e.getContainerId())) {
            unlockItemContainerItems(e.getItemContainer());
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        if (event.getScriptId() == GE_SEARCH_BUILD_SCRIPT) {
            killSearchResults();
        }

        if ((event.getScriptId() == COLLECTION_LOG_OPEN_OTHER || event.getScriptId() == COLLECTION_LOG_ITEM_CLICK ||
                event.getScriptId() == COLLECTION_LOG_DRAW_LIST) && config.moveCollectionLogUnlocks()) {
            addBronzemanCategory();
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        // We do not care about this for our group ironmen
        // if ((event.getMenuOption().equals("Trade with") ||
        // event.getMenuOption().equals("Accept trade")) && !config.allowTrading()) {
        // // Scold the player for attempting to trade as a bronzeman
        // event.consume();
        // sendChatMessage("You are a bronzeman. You stand alone...Sort of.");
        // return;
        // }
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent scriptCallbackEvent) {
        if (scriptCallbackEvent.getEventName().equals(SCRIPT_EVENT_SET_CHATBOX_INPUT) && !onSeasonalWorld) {
            setChatboxName(getNameChatbox());
        }
    }

    @SuppressWarnings("incomplete-switch")
    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (client.getGameState() != GameState.LOADING && client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        String name = Text.removeTags(chatMessage.getName());
        switch (chatMessage.getType()) {
            case PRIVATECHAT:
            case MODPRIVATECHAT:
                // Note this is unable to change icon on PMs if they are not a friend or in
                // friends chat
            case CLAN_CHAT:
            case CLAN_GUEST_CHAT:
            case FRIENDSCHAT:
                if (isChatPlayerOnNormalWorld(name) && isChatPlayerBronzeman(name)) {
                    addBronzemanIconToMessage(chatMessage);
                }
                break;
            case PUBLICCHAT:
            case MODCHAT:
                if (!onSeasonalWorld && isChatPlayerBronzeman(name)) {
                    addBronzemanIconToMessage(chatMessage);
                }
                break;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals(CONFIG_GROUP)) {
            if (event.getKey().equals("namesBronzeman")) {
                updateNamesBronzeman();
            } else if (event.getKey().equals("enableCrabman")) {
                updateAllowedCrabman();
            } else if (event.getKey().equals("databaseString") || event.getKey().equals("databaseTable")) {
                initializeDatabase();
            }
        }
    }

    private void openBronzemanCategory(Widget widget) {
        widget.setOpacity(SELECTED_OPACITY);

        itemEntries = null;
        clientThread.invokeLater(() -> {
            Widget collectionViewHeader = client.getWidget(COLLECTION_LOG_GROUP_ID, COLLECTION_VIEW_HEADER);
            Widget combatAchievementsButton = client.getWidget(COLLECTION_LOG_GROUP_ID, COMBAT_ACHIEVEMENT_BUTTON);
            combatAchievementsButton.setHidden(true);
            Widget[] headerComponents = collectionViewHeader.getDynamicChildren();
            headerComponents[0].setText("Bronzeman Unlocks");
            headerComponents[1].setText("Unlocks: <col=ff0000>" + Integer.toString(db.getUnlockedItems().size()));
            if (headerComponents.length > 2) {
                headerComponents[2].setText("");
            }
            createSearchButton(collectionViewHeader);

            Widget collectionView = client.getWidget(COLLECTION_LOG_GROUP_ID, COLLECTION_VIEW);
            Widget scrollbar = client.getWidget(COLLECTION_LOG_GROUP_ID, COLLECTION_VIEW_SCROLLBAR);
            collectionView.deleteAllChildren();

            int index = 0;
            int x = 0;
            int y = 0;
            int yIncrement = 40;
            int xIncrement = 42;
            for (Integer itemId : db.getUnlockedItems().keySet()) {
                boolean tradeable = itemManager.getItemComposition(itemId).isTradeable();
                if (!tradeable)
                    continue;

                addItemToCollectionLog(collectionView, itemId, x, y, index);
                x = x + xIncrement;
                index++;
                if (x > 210) {
                    x = 0;
                    y = y + yIncrement;
                }
            }

            collectionView.setScrollHeight(y + 43); // y + image height (40) + 3 for padding at the bottom.
            int scrollHeight = (collectionView.getScrollY() * y) / collectionView.getScrollHeight();
            collectionView.revalidateScroll();
            client.runScript(ScriptID.UPDATE_SCROLLBAR, scrollbar.getId(), collectionView.getId(), scrollHeight);
            collectionView.setScrollY(0);
            scrollbar.setScrollY(0);
        });

    }

    private void createSearchButton(Widget header) {
        searchButton = header.createChild(-1, WidgetType.GRAPHIC);
        searchButton.setSpriteId(SpriteID.GE_SEARCH);
        searchButton.setOriginalWidth(18);
        searchButton.setOriginalHeight(17);
        searchButton.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
        searchButton.setOriginalX(5);
        searchButton.setOriginalY(20);
        searchButton.setHasListener(true);
        searchButton.setAction(1, "Open");
        searchButton.setOnOpListener((JavaScriptCallback) e -> openSearch());
        searchButton.setName("Search");
        searchButton.revalidate();
    }

    private void openSearch() {
        updateFilter("");
        client.playSoundEffect(SoundEffectID.UI_BOOP);
        searchButton.setAction(1, "Close");
        searchButton.setOnOpListener((JavaScriptCallback) e -> closeSearch());
        searchInput = chatboxPanelManager.openTextInput("Search unlock list")
                .onChanged(s -> clientThread.invokeLater(() -> updateFilter(s.trim())))
                .onClose(() -> {
                    clientThread.invokeLater(() -> updateFilter(""));
                    searchButton.setOnOpListener((JavaScriptCallback) e -> openSearch());
                    searchButton.setAction(1, "Open");
                })
                .build();
    }

    private void closeSearch() {
        updateFilter("");
        chatboxPanelManager.close();
        client.playSoundEffect(SoundEffectID.UI_BOOP);
    }

    private void updateFilter(String input) {
        final Widget collectionView = client.getWidget(COLLECTION_LOG_GROUP_ID, COLLECTION_VIEW);

        if (collectionView == null) {
            return;
        }

        String filter = input.toLowerCase();
        updateList(collectionView, filter);
    }

    private void updateList(Widget collectionView, String filter) {
        if (itemEntries == null) {
            itemEntries = Arrays.stream(collectionView.getDynamicChildren())
                    .sorted(Comparator.comparing(Widget::getRelativeY))
                    .collect(Collectors.toList());
        }

        itemEntries.forEach(w -> w.setHidden(true));

        Collection<Widget> matchingItems = itemEntries.stream()
                .filter(w -> w.getName().toLowerCase().contains(filter))
                .collect(Collectors.toList());

        int x = 0;
        int y = 0;
        for (Widget entry : matchingItems) {
            entry.setHidden(false);
            entry.setOriginalY(y);
            entry.setOriginalX(x);
            entry.revalidate();
            x = x + 42;
            if (x > 210) {
                x = 0;
                y = y + 40;
            }
        }

        y += 43; // y + image height (40) + 3 for padding at the bottom.

        int newHeight = 0;

        if (collectionView.getScrollHeight() > 0) {
            newHeight = (collectionView.getScrollY() * y) / collectionView.getScrollHeight();
        }

        collectionView.setScrollHeight(y);
        collectionView.revalidateScroll();

        Widget scrollbar = client.getWidget(COLLECTION_LOG_GROUP_ID, COLLECTION_VIEW_SCROLLBAR);
        client.runScript(
                ScriptID.UPDATE_SCROLLBAR,
                scrollbar.getId(),
                collectionView.getId(),
                newHeight);
    }

    private void addItemToCollectionLog(Widget collectionView, Integer itemId, int x, int y, int index) {
        String itemName = itemManager.getItemComposition(itemId).getName();
        Widget newItem = collectionView.createChild(index, 5);
        newItem.setContentType(0);
        newItem.setItemId(itemId);
        newItem.setItemQuantity(1);
        newItem.setItemQuantityMode(0);
        newItem.setModelId(-1);
        newItem.setModelType(1);
        newItem.setSpriteId(-1);
        newItem.setBorderType(1);
        newItem.setFilled(false);
        newItem.setOriginalX(x);
        newItem.setOriginalY(y);
        newItem.setOriginalWidth(36);
        newItem.setOriginalHeight(32);
        newItem.setHasListener(true);
        newItem.setAction(1, "Inspect");
        newItem.setAction(2, "Remove");
        newItem.setOnOpListener((JavaScriptCallback) e -> handleItemAction(itemId, itemName, e));
        newItem.setName(itemName);
        newItem.revalidate();
    }

    private void makeBronzemanWidget(Widget categories, Widget template, int position, int originalY) {
        Widget bronzemanUnlocks = categories.createChild(position, template.getType());
        bronzemanUnlocks.setText("Crabman Unlocks");
        bronzemanUnlocks.setName("<col=ff9040>Crabman Unlocks</col>");
        bronzemanUnlocks.setOpacity(UNSELECTED_OPACITY);
        if (template.hasListener()) {
            bronzemanUnlocks.setHasListener(true);
            bronzemanUnlocks.setAction(1, "View");
            bronzemanUnlocks.setOnOpListener((JavaScriptCallback) e -> openBronzemanCategory(bronzemanUnlocks));
        }
        bronzemanUnlocks.setBorderType(template.getBorderType());
        bronzemanUnlocks.setItemId(template.getItemId());
        bronzemanUnlocks.setSpriteId(template.getSpriteId());
        bronzemanUnlocks.setOriginalHeight(template.getOriginalHeight());
        bronzemanUnlocks.setOriginalWidth((template.getOriginalWidth()));
        bronzemanUnlocks.setOriginalX(template.getOriginalX());
        bronzemanUnlocks.setOriginalY(originalY);
        bronzemanUnlocks.setXPositionMode(template.getXPositionMode());
        bronzemanUnlocks.setYPositionMode(template.getYPositionMode());
        bronzemanUnlocks.setContentType(template.getContentType());
        bronzemanUnlocks.setItemQuantity(template.getItemQuantity());
        bronzemanUnlocks.setItemQuantityMode(template.getItemQuantityMode());
        bronzemanUnlocks.setModelId(template.getModelId());
        bronzemanUnlocks.setModelType(template.getModelType());
        bronzemanUnlocks.setBorderType(template.getBorderType());
        bronzemanUnlocks.setFilled(template.isFilled());
        bronzemanUnlocks.setTextColor(template.getTextColor());
        bronzemanUnlocks.setFontId(template.getFontId());
        bronzemanUnlocks.setTextShadowed(template.getTextShadowed());
        bronzemanUnlocks.setWidthMode(template.getWidthMode());
        bronzemanUnlocks.setYTextAlignment(template.getYTextAlignment());
        bronzemanUnlocks.revalidate();
    }

    private void handleItemAction(Integer itemId, String itemName, ScriptEvent event) {
        switch (event.getOp()) {
            case MENU_INSPECT:
                final ChatMessageBuilder examination = new ChatMessageBuilder()
                        .append(ChatColorType.NORMAL)
                        .append("This is an unlocked item called '" + itemName + "'.");

                chatMessageManager.queue(QueuedMessage.builder()
                        .type(ChatMessageType.ITEM_EXAMINE)
                        .runeLiteFormattedMessage(examination.build())
                        .build());
                break;
            case MENU_DELETE:
                clientThread.invokeLater(() -> confirmDeleteItem(itemId, itemName));
                break;
        }
    }

    private void confirmDeleteItem(Integer itemId, String itemName) {
        chatboxPanelManager.openTextMenuInput("Do you want to re-lock: " + itemName)
                .option("1. Confirm re-locking of item", () -> clientThread.invoke(() -> {
                    deleteConfirmed = true;
                    queueItemDelete(itemId);
                    sendChatMessage("Item '" + itemName + "' is no longer unlocked.");
                    deleteConfirmed = false;
                }))
                .option("2. Cancel", Runnables::doNothing)
                .build();
    }

    /** Unlocks all items in the given item container. **/
    public void unlockItemContainerItems(ItemContainer itemContainer) {
        for (Item i : itemContainer.getItems()) {
            int itemId = i.getId();
            int realItemId = itemManager.canonicalize(itemId);
            ItemComposition itemComposition = itemManager.getItemComposition(itemId);
            int noteId = itemComposition.getNote();
            if (itemId != realItemId && noteId != 799)
                continue; // The 799 signifies that it is a noted item
            if (i.getId() <= 1)
                continue;
            if (i.getQuantity() <= 0)
                continue;
            if (!db.getUnlockedItems().containsKey(realItemId)) {
                queueItemUnlock(realItemId, false);
            }
        }
    }

    public void onItemsUnlocked(List<UnlockedItemEntity> unlockedItems) {
        if (!isLoggedIntoCrabman()) {
            return;
        }
        unlockedItems.forEach((unlockedItem) -> {
            AnotherBronzemanModeOverlay.addItemUnlock(unlockedItem.getItemId());
            notifier.notify("You have unlocked a new item: " + unlockedItem.getItemName() + ".");
            // sendChatMessage("You have unlocked a new item: " + unlockedItem.getItemName()
            // + ".");
        });
    }

    /** Queues a new unlock to be properly displayed **/
    public void queueItemUnlock(int itemId, boolean skipChecks) {
        // Should only be used for Bonds and Gold
        if (!skipChecks) {
            if (!isLoggedIntoCrabman()) {
                return;
            }
            boolean tradeable = itemManager.getItemComposition(itemId).isTradeable();
            if (!tradeable) {
                log.info("Item is not tradeable: " + client.getItemDefinition(itemId).getName());
                return;
            }
        }
        UnlockedItemEntity unlockedItem = db.createNewUnlockedItem(itemId, client.getItemDefinition(itemId).getName());
        log.info("Unlocking item: " + unlockedItem.getItemName());
        db.insertUnlockedItem(unlockedItem);
    }

    /** Queues the removal of an unlocked item **/
    public void queueItemDelete(int itemId) {
        // TODO - Do we want this?
    }

    /** Unlocks default items like a bond to a newly made profile **/
    private void unlockDefaultItems() {
        queueItemUnlock(ItemID.COINS_995, true);
        queueItemUnlock(ItemID.OLD_SCHOOL_BOND, true);
    }

    private void sendChatMessage(String chatMessage) {
        final String message = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append(chatMessage)
                .build();

        chatMessageManager.queue(
                QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(message)
                        .build());
    }

    void addBronzemanWidget(int widgetId) {
        Widget logCategories = client.getWidget(COLLECTION_LOG_GROUP_ID, widgetId);
        Widget[] categoryElements = logCategories.getDynamicChildren();
        if (categoryElements.length == 0) {
            return; // The category elements have not been loaded yet.
        }

        Widget aerialFishing = categoryElements[0]; // The aerial fishing category is used as a template.

        if (!(aerialFishing.getText().contains("Aerial Fishing")
                || aerialFishing.getName().contains("Aerial Fishing"))) {
            return; // This is not the 'other' page, as the first element is not 'Aerial Fishing'.
        }

        if (categoryElements[categoryElements.length - 1].getText().contains("Bronzeman Unlocks")) {
            categoryElements[categoryElements.length - 1].setOpacity(UNSELECTED_OPACITY); // Makes sure the button is
                                                                                          // unselected by default.
            return; // The Bronzeman Unlocks category has already been added.
        }

        int originalY = categoryElements.length * 15;
        makeBronzemanWidget(logCategories, aerialFishing, categoryElements.length, originalY);

        logCategories.setHeightMode(0);
        logCategories.setOriginalHeight(originalY + 18);
        logCategories.revalidate();
    }

    void updateContainerScroll() {
        Widget categoryContainer = client.getWidget(COLLECTION_LOG_GROUP_ID, COLLECTION_VIEW_CATEGORIES_CONTAINER);
        Widget logCategories = client.getWidget(COLLECTION_LOG_GROUP_ID, COLLECTION_VIEW_CATEGORIES_RECTANGLE);
        Widget[] categoryElements = logCategories.getDynamicChildren();
        int originalHeight = 315; // 21 elements * 15 height
        int scrollHeight = categoryElements.length * 18;

        int newHeight = 0;
        if (categoryContainer.getScrollHeight() > 0 && categoryContainer.getScrollHeight() != scrollHeight) {
            newHeight = (categoryContainer.getScrollY() * scrollHeight) / categoryContainer.getScrollHeight();
        }

        categoryContainer.setHeightMode(0);
        categoryContainer.setOriginalHeight(originalHeight);
        categoryContainer.setScrollHeight(scrollHeight);
        categoryContainer.revalidate();
        categoryContainer.revalidateScroll();

        Widget scrollbar = client.getWidget(COLLECTION_LOG_GROUP_ID, COLLECTION_VIEW_CATEGORIES_SCROLLBAR);

        client.runScript(
                ScriptID.UPDATE_SCROLLBAR,
                scrollbar.getId(),
                categoryContainer.getId(),
                newHeight);
    }

    void addBronzemanCategory() {
        clientThread.invokeLater(() -> {
            addBronzemanWidget(COLLECTION_VIEW_CATEGORIES_TEXT); // Creates and adds a text widget for the bronzeman
                                                                 // category.
            addBronzemanWidget(COLLECTION_VIEW_CATEGORIES_RECTANGLE); // Creates and adds a rectangle widget for the
                                                                      // bronzeman category.
            updateContainerScroll();
        });
    }

    void killSearchResults() {
        Widget grandExchangeSearchResults = client.getWidget(162, GE_SEARCH_RESULTS);

        if (grandExchangeSearchResults == null) {
            return;
        }

        Widget[] children = grandExchangeSearchResults.getDynamicChildren();

        if (children == null || children.length < 2 || children.length % 3 != 0) {
            return;
        }

        for (int i = 0; i < children.length; i += 3) {
            if (!db.getUnlockedItems().containsKey(children[i + 2].getItemId())) {
                children[i].setHidden(true);
                children[i + 1].setOpacity(70);
                children[i + 2].setOpacity(70);
            }
        }
    }

    private void updateNamesBronzeman() {
        namesBronzeman = Text.fromCSV(config.namesBronzeman());
    }

    private void updateAllowedCrabman() {
        enabledCrabman = config.enableCrabman();
    }

    private void initializeDatabase() {
        if (config.databaseString().isEmpty()) {
            log.info("No database connection string provided.");
            db.close();
            return;
        }
        if (config.databaseTable().isEmpty()) {
            log.info("No database table name provided.");
            db.close();
            return;
        }
        log.info("Initializing database connection.");
        db.updateConnection(config.databaseString(), config.databaseTable());
    }

    /**
     * Adds the Bronzeman Icon in front of player names.
     *
     * @param chatMessage chat message to edit sender name on
     */
    private void addBronzemanIconToMessage(ChatMessage chatMessage) {
        String name = chatMessage.getName();
        if (!name.equals(Text.removeTags(name))) {
            // If the name has any tags, no bronzeman icon will be added.
            // This makes it so Iron men can't be flagged as bronzeman, but
            // currently also excludes mods.
            return;
        }

        final MessageNode messageNode = chatMessage.getMessageNode();
        messageNode.setName(getNameWithIcon(bronzemanIconOffset, name));

        client.refreshChat();
    }

    /**
     * Checks if the world is a Seasonal world (Like leagues and seasonal deadman).
     *
     * @param worldNumber number of the world to check.
     * @return boolean true/false if it is a seasonal world or not.
     */
    private boolean isSeasonalWorld(int worldNumber) {
        WorldResult worlds = worldService.getWorlds();
        if (worlds == null) {
            return false;
        }

        World world = worlds.findWorld(worldNumber);
        return world != null && world.getTypes().contains(WorldType.SEASONAL);
    }

    /**
     * Checks if the given message was sent by the player
     *
     * @param chatMessage number of the world to check.
     * @return boolean true/false if the message was sent by the player.
     */
    private boolean sentByPlayer(ChatMessage chatMessage) {
        MessageNode messageNode = chatMessage.getMessageNode();

        return Text.sanitize(messageNode.getName()).equals(Text.sanitize(client.getLocalPlayer().getName()));
    }

    /**
     * Update the player name in the chatbox input
     */
    private void setChatboxName(String name) {
        Widget chatboxInput = client.getWidget(ComponentID.CHATBOX_INPUT);
        if (chatboxInput != null) {
            String text = chatboxInput.getText();
            int idx = text.indexOf(':');
            if (idx != -1) {
                String newText = name + text.substring(idx);
                chatboxInput.setText(newText);
            }
        }
    }

    /**
     * Gets the bronzeman name, including possible icon, of the local player.
     *
     * @return String of icon + name
     */
    private String getNameChatbox() {
        Player player = client.getLocalPlayer();
        if (player != null) {
            Widget chatboxInput = client.getWidget(ComponentID.CHATBOX_INPUT);
            String namePlusChannel = player.getName();
            if (chatboxInput != null) {
                String text = chatboxInput.getText();
                int idx = text.indexOf(':');
                if (idx != -1) {
                    namePlusChannel = text.substring(0, idx);
                }
            }
            return getNameWithIcon(bronzemanIconOffset, namePlusChannel);
        }
        return null;
    }

    /**
     * Gets the default name, including possible icon, of the local player.
     *
     * @return String of icon + name
     */
    private String getNameDefault() {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return null;
        }

        int iconIndex;
        switch (client.getAccountType()) {
            case IRONMAN:
                iconIndex = IconID.IRONMAN.getIndex();
                break;
            case HARDCORE_IRONMAN:
                iconIndex = IconID.HARDCORE_IRONMAN.getIndex();
                break;
            case ULTIMATE_IRONMAN:
                iconIndex = IconID.ULTIMATE_IRONMAN.getIndex();
                break;
            default:
                return player.getName();
        }

        return getNameWithIcon(iconIndex, player.getName());
    }

    /**
     * Get a name formatted with icon
     *
     * @param iconIndex index of the icon
     * @param name      name of the player
     * @return String of icon + name
     */
    private static String getNameWithIcon(int iconIndex, String name) {
        String icon = "<img=" + iconIndex + ">";
        return icon + name;
    }

    /**
     * Gets a ChatPlayer object from a clean name by searching friends chat and
     * friends list.
     *
     * @param name name of player to find.
     * @return ChatPlayer if found, else null.
     */
    private ChatPlayer getChatPlayerFromName(String name) {
        // Search friends chat members first, because if a friend is in the friends chat
        // but their private
        // chat is 'off', then we won't know the world
        FriendsChatManager friendsChatManager = client.getFriendsChatManager();
        if (friendsChatManager != null) {
            FriendsChatMember friendsChatMember = friendsChatManager.findByName(name);
            if (friendsChatMember != null) {
                return friendsChatMember;
            }
        }

        NameableContainer<Friend> friendContainer = client.getFriendContainer();
        return friendContainer.findByName(name);
    }

    /**
     * Checks if a player name is a friend or friends chat member is a bronzeman.
     *
     * @param name name of player to check.
     * @return boolean true/false.
     */
    private boolean isChatPlayerBronzeman(String name) {
        return isChatPlayerOnNormalWorld(name)
                && (namesBronzeman.contains(name) || namesBronzeman.contains(name.replace('\u00A0', ' ')));
    }

    /**
     * Checks if a player name is a friend or friends chat member on a normal world.
     *
     * @param name name of player to check.
     * @return boolean true/false.
     */
    private boolean isChatPlayerOnNormalWorld(String name) {
        ChatPlayer player = getChatPlayerFromName(name);

        if (player == null) {
            return true;
        }

        int world = player.getWorld();
        return !isSeasonalWorld(world);
    }

    private void OnUnlocksCountCommand(ChatMessage chatMessage, String message) {
        if (!sentByPlayer(chatMessage)) {
            return;
        }

        final ChatMessageBuilder builder = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append("You have unlocked ")
                .append(ChatColorType.NORMAL)
                .append(Integer.toString(db.getUnlockedItems().size()))
                .append(ChatColorType.HIGHLIGHT)
                .append(" items.");

        String response = builder.build();

        MessageNode messageNode = chatMessage.getMessageNode();
        messageNode.setRuneLiteFormatMessage(response);
        client.refreshChat();
    }

    /**
     * Loads the bronzeman resources into the client.
     */
    private void loadResources() {
        final IndexedSprite[] modIcons = client.getModIcons();

        if (bronzemanIconOffset != -1 || modIcons == null) {
            return;
        }

        unlockImage = ImageUtil.getResourceStreamFromClass(getClass(), "/item-unlocked.png");
        BufferedImage image = ImageUtil.getResourceStreamFromClass(getClass(), "/bronzeman_icon.png");
        IndexedSprite indexedSprite = ImageUtil.getImageIndexedSprite(image, client);

        bronzemanIconOffset = modIcons.length;

        final IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + 1);
        newModIcons[newModIcons.length - 1] = indexedSprite;

        client.setModIcons(newModIcons);
    }
}
