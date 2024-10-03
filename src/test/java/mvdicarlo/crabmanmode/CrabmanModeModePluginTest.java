package mvdicarlo.crabmanmode;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CrabmanModeModePluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(CrabmanModeModePlugin.class);
        RuneLite.main(args);
    }
}
