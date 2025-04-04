package mvdicarlo.crabmanmode;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CrabmanModePluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(CrabmanModePlugin.class);
        RuneLite.main(args);
    }
}
