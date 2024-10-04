package mvdicarlo.crabmanmode;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(CrabmanModeModePlugin.CONFIG_GROUP)
public interface CrabmanModeModeConfig extends Config {
    @ConfigItem(keyName = "namesBronzeman", name = "Bronzeman Names", position = 1, description = "Configures names of bronzemen to highlight in chat. Format: (name), (name)")
    default String namesBronzeman() {
        return "";
    }

    @ConfigItem(keyName = "enableCrabman", name = "Enable Crabman for character", position = 2, description = "Enables Crabman mode for provided character name.")
    default String enableCrabman() {
        return "";
    }

    @ConfigItem(secret = true, keyName = "databaseString", name = "Azure Storage Account Connection String", position = 3, description = "The connection string for your group storage account.")
    default String databaseString() {
        return "";
    }

    @ConfigItem(keyName = "databaseTable", name = "Azure StorageAccount Data Table Name", position = 3, description = "The name of the data table to use.")
    default String databaseTable() {
        return "unlockeditems";
    }

    @ConfigItem(keyName = "sendNotification", name = "Notify on unlock", description = "Send a notification when a new item is unlocked.", position = 5)
    default boolean sendNotification() {
        return false;
    }

    @ConfigItem(keyName = "sendChatMessage", name = "Chat message on unlock", description = "Send a chat message when a new item is unlocked.", position = 6)
    default boolean sendChatMessage() {
        return false;
    }

    @ConfigItem(keyName = "moveCollectionLogUnlocks", name = "Move collection log unlocks", description = "Moves the bronzeman mode unlocks to the bottom of the 'Other' tab.", position = 7)
    default boolean moveCollectionLogUnlocks() {
        return false;
    }
}
