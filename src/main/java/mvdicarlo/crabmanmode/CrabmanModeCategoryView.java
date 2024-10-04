package mvdicarlo.crabmanmode;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.Runnables;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ScriptEvent;
import net.runelite.api.ScriptID;
import net.runelite.api.SoundEffectID;
import net.runelite.api.SpriteID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.game.chatbox.ChatboxTextInput;

public class CrabmanModeCategoryView {
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

    private static final int X_INCREMENT = 42;
    private static final int Y_INCREMENT = 40;
    private static final int MAX_X = 210;
    private static final int PADDING_BOTTOM = 3;

    private final Client client;

    private final ClientThread clientThread;

    private final ItemManager itemManager;

    private final ChatboxPanelManager chatboxPanelManager;

    private final ChatMessageManager chatMessageManager;

    private Collection<Widget> itemEntries;

    private Widget searchButton;

    private ChatboxTextInput searchInput;

    private final CrabmanModeStorageTableRepo db;

    public CrabmanModeCategoryView(CrabmanModeStorageTableRepo database, Client client, ClientThread clientThread,
            ItemManager itemManager,
            ChatboxPanelManager chatboxPanelManager, ChatMessageManager chatMessageManager) {
        db = database;
        this.client = client;
        this.clientThread = clientThread;
        this.itemManager = itemManager;
        this.chatboxPanelManager = chatboxPanelManager;
        this.chatMessageManager = chatMessageManager;
    }

    public void openCollectionLog(Widget widget) {
        widget.setOpacity(SELECTED_OPACITY);

        clientThread.invokeLater(() -> {
            Widget collectionViewHeader = client.getWidget(COLLECTION_LOG_GROUP_ID, COLLECTION_VIEW_HEADER);
            Widget combatAchievementsButton = client.getWidget(COLLECTION_LOG_GROUP_ID, COMBAT_ACHIEVEMENT_BUTTON);
            combatAchievementsButton.setHidden(true);
            Widget[] headerComponents = collectionViewHeader.getDynamicChildren();
            if (headerComponents.length == 0) {
                return false;
            }

            setHeaderText(headerComponents);
            createSearchButton(collectionViewHeader);

            Widget collectionView = client.getWidget(COLLECTION_LOG_GROUP_ID, COLLECTION_VIEW);
            Widget scrollbar = client.getWidget(COLLECTION_LOG_GROUP_ID, COLLECTION_VIEW_SCROLLBAR);
            collectionView.deleteAllChildren();

            populateCollectionView(collectionView);

            int y = 0;
            collectionView.setScrollHeight(calculateScrollHeight(y));
            int scrollHeight = (collectionView.getScrollY() * collectionView.getScrollHeight())
                    / collectionView.getScrollHeight();
            collectionView.revalidateScroll();
            client.runScript(ScriptID.UPDATE_SCROLLBAR, scrollbar.getId(), collectionView.getId(), scrollHeight);
            collectionView.setScrollY(0);
            scrollbar.setScrollY(0);
            return true;
        });
    }

    private void setHeaderText(Widget[] headerComponents) {
        headerComponents[0].setText("Group Bronzeman Unlocks");
        headerComponents[1].setText("Unlocks: <col=ff0000>" + db.getUnlockedItems().size());
        if (headerComponents.length > 2) {
            headerComponents[2].setText("");
        }
    }

    private void populateCollectionView(Widget collectionView) {
        int index = 0;
        int x = 0;
        int y = 0;

        for (Integer itemId : db.getUnlockedItems().keySet()) {
            boolean tradeable = itemManager.getItemComposition(itemId).isTradeable();
            if (!tradeable) {
                continue;
            }

            addItemToCollectionLog(collectionView, itemId, x, y, index);
            x += X_INCREMENT;
            index++;
            if (x > MAX_X) {
                x = 0;
                y += Y_INCREMENT;
            }
        }
    }

    private int calculateScrollHeight(int y) {
        return y + Y_INCREMENT + PADDING_BOTTOM;
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

    private void closeSearch() {
        updateFilter("");
        chatboxPanelManager.close();
        client.playSoundEffect(SoundEffectID.UI_BOOP);
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
                    deleteItem(itemId);
                    sendChatMessage("Item '" + itemName + "' is no longer unlocked.");
                }))
                .option("2. Cancel", Runnables::doNothing)
                .build();
    }

    public void deleteItem(int itemId) {
        db.deleteUnlockedItem(itemId);
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

    public void addCollectionCategory() {
        clientThread.invokeLater(() -> {
            try {
                // Creates and adds a text widget for the crabman category
                addWidget(COLLECTION_VIEW_CATEGORIES_TEXT);
                // Creates and adds a rectangle widget for the crabman category
                addWidget(COLLECTION_VIEW_CATEGORIES_RECTANGLE);
                updateContainerScroll();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        });
    }

    private void addWidget(int widgetId) {
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

        if (categoryElements[categoryElements.length - 1].getText().contains("Group Bronzeman Unlocks")) {
            // categoryElements[categoryElements.length - 1].setOpacity(UNSELECTED_OPACITY);
            // // Makes sure the button is
            // unselected by default.
            return; // The Crabman Unlocks category has already been added.
        }

        int originalY = categoryElements.length * 15;
        makeCrabmanWidget(logCategories, aerialFishing, categoryElements.length, originalY);

        logCategories.setHeightMode(0);
        logCategories.setOriginalHeight(originalY + 18);
        logCategories.revalidate();
    }

    private void makeCrabmanWidget(Widget categories, Widget template, int position, int originalY) {
        Widget crabmanUnlocks = categories.createChild(position, template.getType());
        crabmanUnlocks.setText("Group Bronzeman Unlocks");
        crabmanUnlocks.setName("<col=ff9040>Group Bronzeman Unlocks</col>");
        // crabmanUnlocks.setOpacity(UNSELECTED_OPACITY);
        if (template.hasListener()) {
            crabmanUnlocks.setHasListener(true);
            crabmanUnlocks.setAction(1, "View");
            crabmanUnlocks.setOnOpListener((JavaScriptCallback) e -> openCollectionLog(crabmanUnlocks));
        }
        crabmanUnlocks.setBorderType(template.getBorderType());
        crabmanUnlocks.setItemId(template.getItemId());
        crabmanUnlocks.setSpriteId(template.getSpriteId());
        crabmanUnlocks.setOriginalHeight(template.getOriginalHeight());
        crabmanUnlocks.setOriginalWidth((template.getOriginalWidth()));
        crabmanUnlocks.setOriginalX(template.getOriginalX());
        crabmanUnlocks.setOriginalY(originalY);
        crabmanUnlocks.setXPositionMode(template.getXPositionMode());
        crabmanUnlocks.setYPositionMode(template.getYPositionMode());
        crabmanUnlocks.setContentType(template.getContentType());
        crabmanUnlocks.setItemQuantity(template.getItemQuantity());
        crabmanUnlocks.setItemQuantityMode(template.getItemQuantityMode());
        crabmanUnlocks.setModelId(template.getModelId());
        crabmanUnlocks.setModelType(template.getModelType());
        crabmanUnlocks.setBorderType(template.getBorderType());
        crabmanUnlocks.setFilled(template.isFilled());
        crabmanUnlocks.setTextColor(template.getTextColor());
        crabmanUnlocks.setFontId(template.getFontId());
        crabmanUnlocks.setTextShadowed(template.getTextShadowed());
        crabmanUnlocks.setWidthMode(template.getWidthMode());
        crabmanUnlocks.setYTextAlignment(template.getYTextAlignment());
        crabmanUnlocks.revalidate();
    }

    private void updateContainerScroll() {
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
}