package codepanter.anotherbronzemanmode;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(AnotherBronzemanModePlugin.CONFIG_GROUP)
public interface AnotherBronzemanModeConfig extends Config
{
    @ConfigItem(
            keyName = "namesBronzeman",
            name = "Bronzeman Names",
            position = 1,
            description = "Configures names of bronzemen to highlight in chat. Format: (name), (name)"
    )
    default String namesBronzeman()
    {
        return "";
    }

    @ConfigItem(
            keyName = "screenshotUnlock",
            name = "Screenshot new Unlocks",
            position = 2,
            description = "Take a screenshot whenever a new item is unlocked."
    )
    default boolean screenshotUnlock()
    {
        return false;
    }

    @ConfigItem(
            keyName = "includeFrame",
            name = "Include Client Frame",
            description = "Configures whether or not the client frame is included in screenshots.",
            position = 3
    )
    default boolean includeFrame()
    {
        return false;
    }

    @ConfigItem(
        keyName = "sendNotification",
        name = "Notify on unlock",
        description = "Send a notification when a new item is unlocked.",
        position = 4
    )
    default boolean sendNotification()
    {
        return false;
    }

    @ConfigItem(
        keyName = "sendChatMessage",
        name = "Chat message on unlock",
        description = "Send a chat message when a new item is unlocked.",
        position = 5
    )
    default boolean sendChatMessage()
    {
        return false;
    }

    @ConfigItem(
        keyName = "moveCollectionLogUnlocks",
        name = "Move collection log unlocks",
        description = "Moves the bronzeman mode unlocks to the bottom of the 'Other' tab.",
        position = 6
    )
    default boolean moveCollectionLogUnlocks()
    {
        return false;
    }

    @ConfigItem(
        keyName = "allowTrading",
        name = "Allow trading as a bronzeman",
        description = "Allows the player to trade even though they are a bronzeman.",
        position = 7
    )
    default boolean allowTrading()
    {
        return false;
    }

    @ConfigItem(
            keyName = "resetCommand",
            name = "Enable reset command",
            description = "Enables the !bmreset command used for wiping your unlocked items.",
            position = 8
    )
    default boolean resetCommand()
    {
        return false;
    }

    @ConfigItem(
            keyName = "syncGroup",
            name = "Enable group syncing",
            description = "Enables syncing item unlocks with a group by using a shared Google Sheet.",
            position = 9
    )
    default boolean syncGroup(){return false;}

    @ConfigItem(
            keyName = "syncSheetId",
            name = "Google Sheet Id",
            description = "Google Sheet Id for syncing item unlocks",
            position = 10
    )
    default String syncSheetId(){return "";}

    @ConfigItem(
            keyName = "oAuth2ClientDetails",
            name = "Google Sheet OAuth2 client details",
            description = "This is the file contents of the client details from Google Cloud Platform.",
            position = 11
    )
    default String oAuth2ClientDetails(){return "";}

    @ConfigItem(
            keyName = "authorize",
            name = "Google Sheet Authorize",
            description = "Toggle this to do authorization.",
            position = 12
    )
    default boolean authorize(){return false;}
}
