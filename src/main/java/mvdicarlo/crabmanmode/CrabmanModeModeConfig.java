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

    @ConfigItem(keyName = "enableCrabman", name = "Enable group bronzeman for character name", position = 2, description = "Enables group bronzeman mode for provided character name.")
    default String enableCrabman() {
        return "";
    }

    @ConfigItem(secret = true, keyName = "databaseString", name = "Azure Storage Account Connection String", position = 3, description = "The connection string for your group storage account.")
    default String databaseString() {
        return "";
    }

    @ConfigItem(keyName = "databaseTable", name = "Table Name", position = 3, description = "The name of the data table to use.")
    default String databaseTable() {
        return "unlockeditems";
    }
}
