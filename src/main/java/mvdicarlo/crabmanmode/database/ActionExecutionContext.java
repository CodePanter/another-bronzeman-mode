package mvdicarlo.crabmanmode.database;

import java.util.Map;
import mvdicarlo.crabmanmode.AzureTableApi;
import mvdicarlo.crabmanmode.UnlockedItemEntity;

/**
 * Context provided to actions during execution.
 * Contains all the resources and state needed to execute database operations.
 */
public class ActionExecutionContext {

    private final AzureTableApi api;
    private final Map<Integer, UnlockedItemEntity> localCache;
    private final String currentUser;
    private final boolean ready;

    public ActionExecutionContext(AzureTableApi api,
            Map<Integer, UnlockedItemEntity> localCache,
            String currentUser,
            boolean ready) {
        this.api = api;
        this.localCache = localCache;
        this.currentUser = currentUser;
        this.ready = ready;
    }

    /**
     * Get the Azure Table API instance
     */
    public AzureTableApi getApi() {
        return api;
    }

    /**
     * Get the local cache of unlocked items (thread-safe access required)
     */
    public Map<Integer, UnlockedItemEntity> getLocalCache() {
        return localCache;
    }

    /**
     * Get the current user name
     */
    public String getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if the system is ready for database operations
     */
    public boolean isReady() {
        return ready && api != null && currentUser != null && !currentUser.isEmpty();
    }
}
