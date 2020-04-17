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
package codepanter.anotherbronzemanmode;

import com.google.common.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.events.PluginChanged;
import net.runelite.api.ChatMessageType;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.client.Notifier;
import net.runelite.client.game.ItemManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.Text;
import net.runelite.client.game.WorldService;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.Set;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
@PluginDescriptor(
        name = "Another Bronzeman Mode",
        description = "Limits access to buying an item on the Grand Exchange until it is obtained otherwise.",
        tags = {"overlay", "bronzeman"},
        enabledByDefault = false
)
public class AnotherBronzemanModePlugin extends Plugin
{
    static final String CONFIG_GROUP = "anotherbronzemanmode";
    private static final String BM_UNLOCKS_STRING = "!bmunlocks";
    private static final String BM_COUNT_STRING = "!bmcount";
    private static final String BM_RESET_STRING = "!bmreset";
    private static final String BM_BACKUP_STRING = "!bmbackup";

    private static final int GE_SEARCH_BUILD_SCRIPT = 751;

    static final Set<Integer> OWNED_INVENTORY_IDS = ImmutableSet.of(
            0,    // Reward from fishing trawler.
            93,   // Standard player inventory.
            94,   // Equipment inventory.
            95,   // Bank inventory.
            141,  // Barrows reward chest inventory.
            390,  // Kingdom Of Miscellania reward inventory.
            581,  // Chambers of Xeric chest inventory.
            612,  // Theater of Blood reward chest inventory (Raids 2).
            626); // Seed vault located inside the Farming Guild.

    @Inject
    private Client client;

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
    private ChatMessageManager chatMessageManager;

    @Inject
    private ChatCommandManager chatCommandManager;

    @Inject
    private AnotherBronzemanModeConfig config;

    @Inject
    private AnotherBronzemanModeOverlay AnotherBronzemanModeOverlay;

    private List<Integer> unlockedItems;

    @Getter
    private BufferedImage unlockImage = null;

    private static final String SCRIPT_EVENT_SET_CHATBOX_INPUT = "setChatboxInput";
    private static final String IRONMAN_PREFIX = "<img=" + IconID.IRONMAN.getIndex() + ">";

    private List<String> namesBronzeman = new ArrayList<>();
    private int bronzemanIconOffset = -1; // offset for bronzeman icon
    private boolean onLeagueWorld;
    private File playerFile;
    private File playerFolder;

    @Provides
    AnotherBronzemanModeConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AnotherBronzemanModeConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        super.startUp();
        onLeagueWorld = false;
        updateNamesBronzeman();
        updateScreenshotUnlock();
        loadResources();
        unlockedItems = new ArrayList<>();
        overlayManager.add(AnotherBronzemanModeOverlay);
        chatCommandManager.registerCommand(BM_UNLOCKS_STRING, this::unlockedItemsLookup);
        chatCommandManager.registerCommand(BM_COUNT_STRING, this::unlockedItemsLookup);
        chatCommandManager.registerCommand(BM_BACKUP_STRING, this::backupUnlocks);

        if (config.resetCommand())
        {
            chatCommandManager.registerCommand(BM_RESET_STRING, this::resetUnlocks);
        }

        clientThread.invoke(() ->
        {
            if (client.getGameState() == GameState.LOGGED_IN)
            {
                onLeagueWorld = isLeagueWorld(client.getWorld());
                // A player can not be a bronze man on a league world.
                if (!onLeagueWorld)
                {
                    setChatboxName(getNameChatbox());
                }
            }
        });
    }

    @Override
    protected void shutDown() throws Exception
    {
        super.shutDown();
        unlockedItems = null;
        overlayManager.remove(AnotherBronzemanModeOverlay);
        chatCommandManager.unregisterCommand(BM_UNLOCKS_STRING);
        chatCommandManager.unregisterCommand(BM_COUNT_STRING);
        chatCommandManager.unregisterCommand(BM_BACKUP_STRING);
        if (config.resetCommand())
        {
            chatCommandManager.unregisterCommand(BM_RESET_STRING);
        }

        clientThread.invoke(() ->
        {
            // Cleanup is not required after having played on a league world.
            if (client.getGameState() == GameState.LOGGED_IN && !onLeagueWorld)
            {
                setChatboxName(getNameDefault());
            }
        });
    }

    /** Loads players unlocks on login **/
    @Subscribe
    public void onGameStateChanged(GameStateChanged e)
    {
        if (e.getGameState() == GameState.LOGGED_IN)
        {
            setupPlayerFile();
            loadPlayerUnlocks();
            loadResources();
            onLeagueWorld = isLeagueWorld(client.getWorld());
        }
    }

    @Subscribe
    public void onPluginChanged(PluginChanged e)
    {
        if (e.getPlugin() == this && client.getGameState() == GameState.LOGGED_IN)
        {
            setupPlayerFile();
            loadPlayerUnlocks();
        }
    }

    /** Unlocks all new items that are currently not unlocked **/
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged e)
    {
        if (OWNED_INVENTORY_IDS.contains(e.getContainerId()))
        {
            unlockItemContainerItems(e.getContainerId());
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        if (event.getScriptId() == GE_SEARCH_BUILD_SCRIPT)
        {
            killSearchResults();
        }
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent scriptCallbackEvent)
    {
        if (scriptCallbackEvent.getEventName().equals(SCRIPT_EVENT_SET_CHATBOX_INPUT) && !onLeagueWorld)
        {
            setChatboxName(getNameChatbox());
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage)
    {
        if (client.getGameState() != GameState.LOADING && client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        String name = Text.removeTags(chatMessage.getName());
        switch (chatMessage.getType())
        {
            case PRIVATECHAT:
            case MODPRIVATECHAT:
                // Note this is unable to change icon on PMs if they are not a friend or in clan chat
            case FRIENDSCHAT:
                if (isChatPlayerOnNormalWorld(name) && isChatPlayerBronzeman(name))
                {
                    addBronzemanIconToMessage(chatMessage);
                }
                break;
            case PUBLICCHAT:
            case MODCHAT:
                if (!onLeagueWorld && isChatPlayerBronzeman(name))
                {
                    addBronzemanIconToMessage(chatMessage);
                }
                break;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals(CONFIG_GROUP))
        {
            if (event.getKey() == "namesBronzeman")
            {
                updateNamesBronzeman();
            }
            elif (event.getKey() == "screenshotUnlock" || event.getKey() == "includeFrame")
            {
                updateScreenshotUnlock();
            }
            elif (event.getKey() == "resetCommand")
            {
                if (config.resetCommand())
                {
                    chatCommandManager.registerCommand(BM_RESET_STRING, this::resetUnlocks);
                }
                else
                {
                    chatCommandManager.unregisterCommand(BM_RESET_STRING);
                }
            }

        }
    }

    /** Unlocks all items in the given item container. **/
    public void unlockItemContainerItems(InventoryID containerId)
    {
        ItemContainer itemContainer = client.getItemContainer(containerId);
        for (Item i : itemContainer.getItems())
        {
            int itemId = i.getId();
            int realItemId = itemManager.canonicalize(itemId);
            ItemComposition itemComposition = itemManager.getItemComposition(itemId);
            int noteId = itemComposition.getNote();
            if (itemId != realItemId && noteId != 799) continue;  // The 799 signifies that it is a noted item
            if (i.getId() <= 1) continue;
            if (i.getQuantity() <= 0) continue;
            if (!unlockedItems.contains(realItemId))
            {
                queueItemUnlock(realItemId);
                if (config.sendNotification())
                {
                    notifier.notify("New bronzeman unlock!");
                }
                if (config.sendChatMessage())
                {
                    sendChatMessage("You have unlocked a new item: " + client.getItemDefinition(realItemId).getName() + ".");
                }
            }
        }
    }

    /** Queues a new unlock to be properly displayed **/
    public void queueItemUnlock(int itemId)
    {
        unlockedItems.add(itemId);
        AnotherBronzemanModeOverlay.addItemUnlock(itemId);
        savePlayerUnlocks();// Save after every item to fail-safe logging out
    }

    /** Unlocks default items like a bond to a newly made profile **/
    private void unlockDefaultItems()
    {
        queueItemUnlock(ItemID.COINS_995);
        queueItemUnlock(ItemID.OLD_SCHOOL_BOND);
    }

    private void sendChatMessage(String chatMessage)
    {
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

    void killSearchResults()
    {
        Widget grandExchangeSearchResults = client.getWidget(162, 53);

        if (grandExchangeSearchResults == null)
        {
            return;
        }

        Widget[] children = grandExchangeSearchResults.getDynamicChildren();

        if (children == null || children.length < 2)
        {
            return;
        }

        for (int i = 0; i < children.length; i+= 3) {
            if (!unlockedItems.contains(children[i + 2].getItemId()))
            {
                children[i].setHidden(true);
                children[i + 1].setOpacity(70);
                children[i + 2].setOpacity(70);
            }
        }
    }

    /* Saves players unlock JSON to a .txt file every time they unlock a new item */
    private void savePlayerUnlocks()
    {
        try
        {
            PrintWriter w = new PrintWriter(playerFile);
            String json = GSON.toJson(unlockedItems);
            w.println(json);
            w.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /* Loads a players unlock JSON everytime they login */
    private void loadPlayerUnlocks()
    {
        unlockedItems.clear();
        try
        {
            String json = Files.readString(Paths.get(String.valueOf(playerFile)));
            unlockedItems.addAll(GSON.fromJson(json, new TypeToken<List<Integer>>(){}.getType()));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void updateNamesBronzeman()
    {
        namesBronzeman = Text.fromCSV(config.namesBronzeman());
    }

    private void updateScreenshotUnlock()
    {
        boolean screenshotUnlock = config.screenshotUnlock();
        boolean includeFrame = config.includeFrame();
        AnotherBronzemanModeOverlay.updateScreenshotUnlock(screenshotUnlock, includeFrame);
    }

    /**
     * Adds the Bronze man Icon in front of player names.
     *
     * @param chatMessage chat message to edit sender name on
     */
    private void addBronzemanIconToMessage(ChatMessage chatMessage)
    {
        String name = chatMessage.getName();
        if (!name.equals(Text.removeTags(name)))
        {
            // If the name has any tags, no bronze man icon will be added.
            // This makes it so Iron men can't be flagged as bronze man, but
            // currently also excludes mods.
            return;
        }

        final MessageNode messageNode = chatMessage.getMessageNode();
        messageNode.setName(getNameWithIcon(bronzemanIconOffset, name));

        chatMessageManager.update(messageNode);
        client.refreshChat();
    }

    /**
     * Checks if the world is a League world.
     *
     * @param worldNumber number of the world to check.
     * @return boolean true/false if it is a league world or not.
     */
    private boolean isLeagueWorld(int worldNumber)
    {
        WorldResult worlds = worldService.getWorlds();
        if (worlds == null)
        {
            return false;
        }

        World world = worlds.findWorld(worldNumber);
        return world != null && world.getTypes().contains(WorldType.LEAGUE);
    }

    /**
     * Update the player name in the chatbox input
     */
    private void setChatboxName(String name)
    {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        if (chatboxInput != null)
        {
            String text = chatboxInput.getText();
            int idx = text.indexOf(':');
            if (idx != -1)
            {
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
    private String getNameChatbox()
    {
        Player player = client.getLocalPlayer();
        if (player != null)
        {
            return getNameWithIcon(bronzemanIconOffset, player.getName());
        }
        return null;
    }

    /**
     * Gets the default name, including possible icon, of the local player.
     *
     * @return String of icon + name
     */
    private String getNameDefault()
    {
        Player player = client.getLocalPlayer();
        if (player == null)
        {
            return null;
        }

        int iconIndex;
        switch (client.getAccountType())
        {
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
    private static String getNameWithIcon(int iconIndex, String name)
    {
        String icon = "<img=" + iconIndex + ">";
        return icon + name;
    }

    /**
     * Gets a ChatPlayer object from a clean name by searching clan and friends list.
     *
     * @param name name of player to find.
     * @return ChatPlayer if found, else null.
     */
    private ChatPlayer getChatPlayerFromName(String name)
    {
        // Search clan members first, because if a friend is in the clan chat but their private
        // chat is 'off', then we won't know the world
        ClanMemberManager clanMemberManager = client.getClanMemberManager();
        if (clanMemberManager != null)
        {
            ClanMember clanMember = clanMemberManager.findByName(name);
            if (clanMember != null)
            {
                return clanMember;
            }
        }

        NameableContainer<Friend> friendContainer = client.getFriendContainer();
        return friendContainer.findByName(name);
    }

    /**
     * Checks if a player name is a friend or clan member is a bronzeman.
     *
     * @param name name of player to check.
     * @return boolean true/false.
     */
    private boolean isChatPlayerBronzeman(String name)
    {
        return isChatPlayerOnNormalWorld(name) && (namesBronzeman.contains(name) || namesBronzeman.contains(name.replace('\u00A0', ' ')));
    }

    /**
     * Checks if a player name is a friend or clan member on a normal world.
     *
     * @param name name of player to check.
     * @return boolean true/false.
     */
    private boolean isChatPlayerOnNormalWorld(String name)
    {
        ChatPlayer player = getChatPlayerFromName(name);

        if (player == null)
        {
            return true;
        }

        int world = player.getWorld();
        return !isLeagueWorld(world);
    }

    /**
     * Sets up the playerFile variable, and makes the player file if needed.
     */
    private void setupPlayerFile()
    {
        playerFolder = new File(RuneLite.PROFILES_DIR, client.getUsername());
        if (!playerFolder.exists())
        {
            playerFolder.mkdirs();
        }
        playerFile = new File(playerFolder, "bronzeman-unlocks.txt");
        if (!playerFile.exists())
        {
            try {
                playerFile.createNewFile();
                unlockDefaultItems();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void unlockedItemsLookup(ChatMessage chatMessage, String message)
    {
        MessageNode messageNode = chatMessage.getMessageNode();

        if (!Text.sanitize(messageNode.getName()).equals(Text.sanitize(client.getLocalPlayer().getName())))
        {
            return;
        }

        final ChatMessageBuilder builder = new ChatMessageBuilder()
            .append(ChatColorType.HIGHLIGHT)
            .append("You have unlocked ")
            .append(ChatColorType.NORMAL)
            .append(Integer.toString(unlockedItems.size()))
            .append(ChatColorType.HIGHLIGHT)
            .append(" items.");

        String response = builder.build();

        messageNode.setRuneLiteFormatMessage(response);
        chatMessageManager.update(messageNode);
        client.refreshChat();
    }

    private void resetUnlocks(ChatMessage chatMessage, String message)
    {
        try {
            playerFile.delete();
            unlockedItems.clear();
            savePlayerUnlocks();
            unlockDefaultItems();
            unlockItemContainerItems(InventoryID.INVENTORY);
            unlockItemContainerItems(InventoryID.EQUIPMENT);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        sendChatMessage("Unlocks succesfully reset!");
    }


    private void backupUnlocks(ChatMessage chatMessage, String message)
    {
        Path originalPath = playerFile.toPath();
        try {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("MM_WW_HH_mm_ss");
            Files.copy(originalPath, Paths.get(playerFolder.getPath() + "_" + sdf.format(cal.getTime()) + ".backup"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        sendChatMessage("Successfully backed up file!");
    }

    /**
     * Loads the bronzeman resources into the client.
     */
    private void loadResources()
    {
        final IndexedSprite[] modIcons = client.getModIcons();

        if (bronzemanIconOffset != -1 || modIcons == null)
        {
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
