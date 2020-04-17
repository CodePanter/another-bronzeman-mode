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
            description = "Configures whether or not the client frame is included in screenshots",
            position = 3
    )
    default boolean includeFrame()
    {
        return true;
    }

}
