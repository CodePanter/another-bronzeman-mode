package mvdicarlo.crabmanmode.database;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import mvdicarlo.crabmanmode.UnlockedItemEntity;

/**
 * Concrete implementations of database actions.
 */
@Slf4j
public class DatabaseActions {

    /**
     * Action to insert a new unlocked item
     */
    public static class InsertItemAction extends BaseDatabaseAction<UnlockedItemEntity> {
        private final UnlockedItemEntity item;

        public InsertItemAction(UnlockedItemEntity item) {
            this.item = item;
        }

        @Override
        public String getActionType() {
            return "INSERT_ITEM";
        }

        @Override
        public String getDescription() {
            return String.format("Insert item '%s' (ID: %d)", item.getItemName(), item.getItemId());
        }

        @Override
        protected UnlockedItemEntity executeInternal(ActionExecutionContext context) throws Exception {
            // Check if item already exists in local cache
            synchronized (context.getLocalCache()) {
                if (context.getLocalCache().containsKey(item.getItemId())) {
                    log.debug("Item {} already exists, skipping insert", item.getItemName());
                    return context.getLocalCache().get(item.getItemId());
                }

                // Set user and add to local cache optimistically
                item.setAcquiredBy(context.getCurrentUser());
                context.getLocalCache().put(item.getItemId(), item);
            }

            try {
                // Send to remote database
                context.getApi().insertEntity(item);
                log.debug("Successfully inserted item '{}' to remote database", item.getItemName());
                return item;

            } catch (Exception e) {
                // Remove from local cache if remote insert failed
                synchronized (context.getLocalCache()) {
                    context.getLocalCache().remove(item.getItemId());
                }
                throw new RuntimeException("Failed to insert item to remote database", e);
            }
        }

        public UnlockedItemEntity getItem() {
            return item;
        }
    }

    /**
     * Action to delete an unlocked item
     */
    public static class DeleteItemAction extends BaseDatabaseAction<Void> {
        private final int itemId;

        public DeleteItemAction(int itemId) {
            this.itemId = itemId;
        }

        @Override
        public String getActionType() {
            return "DELETE_ITEM";
        }

        @Override
        public String getDescription() {
            return String.format("Delete item ID: %d", itemId);
        }

        @Override
        protected Void executeInternal(ActionExecutionContext context) throws Exception {
            UnlockedItemEntity removed = null;

            // Remove from local cache optimistically
            synchronized (context.getLocalCache()) {
                removed = context.getLocalCache().remove(itemId);
            }

            try {
                // Send delete to remote
                context.getApi().deleteEntity(UnlockedItemEntity.PartitionKey, Integer.toString(itemId));
                log.debug("Successfully deleted item ID {} from remote database", itemId);
                return null;

            } catch (Exception e) {
                // Restore to local cache if remote delete failed
                if (removed != null) {
                    synchronized (context.getLocalCache()) {
                        context.getLocalCache().put(itemId, removed);
                    }
                }
                throw new RuntimeException("Failed to delete item from remote database", e);
            }
        }

        public int getItemId() {
            return itemId;
        }
    }

    /**
     * Action to load all items from the database (used during initialization)
     */
    public static class LoadAllItemsAction extends BaseDatabaseAction<Map<Integer, UnlockedItemEntity>> {

        @Override
        public String getActionType() {
            return "LOAD_ALL_ITEMS";
        }

        @Override
        public String getDescription() {
            return "Load all unlocked items from database";
        }

        @Override
        public int getPriority() {
            return 100; // High priority for initialization
        }

        @Override
        protected Map<Integer, UnlockedItemEntity> executeInternal(ActionExecutionContext context) throws Exception {
            Map<Integer, UnlockedItemEntity> result = new HashMap<>();

            try {
                List<UnlockedItemEntity> items = context.getApi().listEntities();
                for (UnlockedItemEntity item : items) {
                    result.put(item.getItemId(), item);
                }
                log.info("Successfully loaded {} items from database", result.size());
                return result;

            } catch (Exception e) {
                throw new RuntimeException("Failed to load items from database", e);
            }
        }

        @Override
        public boolean canExecute(ActionExecutionContext context) {
            // Can execute even if not fully ready (used during initialization)
            return context.getApi() != null;
        }
    }

    /**
     * Action to sync with remote database (periodic background sync)
     */
    public static class SyncAction extends BaseDatabaseAction<List<UnlockedItemEntity>> {
        private final OffsetDateTime since;
        private final String currentUser;

        public SyncAction(OffsetDateTime since, String currentUser) {
            this.since = since;
            this.currentUser = currentUser;
        }

        @Override
        public String getActionType() {
            return "SYNC";
        }

        @Override
        public String getDescription() {
            return String.format("Sync items since %s", since);
        }

        @Override
        public int getPriority() {
            return -10; // Low priority for background sync
        }

        @Override
        protected List<UnlockedItemEntity> executeInternal(ActionExecutionContext context) throws Exception {
            try {
                String filter = "Timestamp%20gt%20datetime'" + since.toString() + "'";
                List<UnlockedItemEntity> newItems = context.getApi().listEntities(filter);

                // Filter out items from current user and update local cache
                List<UnlockedItemEntity> newItemsFromOthers = new ArrayList<>();

                synchronized (context.getLocalCache()) {
                    for (UnlockedItemEntity item : newItems) {
                        context.getLocalCache().put(item.getItemId(), item);

                        if (!item.getAcquiredBy().equals(currentUser)) {
                            newItemsFromOthers.add(item);
                        }
                    }
                }

                log.debug("Synced {} new items ({} from others)", newItems.size(), newItemsFromOthers.size());
                return newItemsFromOthers;

            } catch (Exception e) {
                log.warn("Failed to sync with remote database: {}", e.getMessage());
                // Don't throw for sync failures - just log and continue
                return new ArrayList<>();
            }
        }
    }
}
