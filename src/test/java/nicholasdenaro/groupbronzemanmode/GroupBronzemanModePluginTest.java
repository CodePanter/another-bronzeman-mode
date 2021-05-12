package nicholasdenaro.groupbronzemanmode;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GroupBronzemanModePluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(GroupBronzemanModePlugin.class);
        RuneLite.main(args);
    }
}
