package mvdicarlo.crabmanmode.database;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;
import mvdicarlo.crabmanmode.AzureTableApi;
import mvdicarlo.crabmanmode.UnlockedItemEntity;
import okhttp3.OkHttpClient;

/**
 * New database repository implementation using the action queue pattern.
 * All database operations are queued as actions and executed asynchronously.
 * This eliminates blocking operations and race conditions.
 */
@Slf4j
public class DatabaseRepository {
    private final DatabaseActionQueue actionQueue;
    private final DatabaseState databaseState;
    private final Map<Integer, UnlockedItemEntity> unlockedItems = new HashMap<>();
    private final ScheduledExecutorService scheduler;

    // Pre-initialization queue for items inserted before database is ready
    private final List<PendingInsert> preInitInsertQueue = new java.util.concurrent.CopyOnWriteArrayList<>();

    // Track the periodic sync task so we can cancel it
    private java.util.concurrent.ScheduledFuture<?> syncTask;

    // Dependencies
    private AzureTableApi api;
    private String currentUser;
    private Gson gson;
    private OkHttpClient httpClient;

    // Listeners
    private final List<Consumer<List<UnlockedItemEntity>>> itemListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    // Helper class for pre-initialization inserts
    private static class PendingInsert {
        final UnlockedItemEntity item;
        final CompletableFuture<UnlockedItemEntity> future;

        PendingInsert(UnlockedItemEntity item, CompletableFuture<UnlockedItemEntity> future) {
            this.item = item;
            this.future = future;
        }
    }

    public DatabaseRepository() {
        this.actionQueue = new DatabaseActionQueue();
        this.databaseState = new DatabaseState();
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "DatabaseRepository-Scheduler");
            t.setDaemon(true);
            return t;
        });

        // Update action queue context when state changes
        databaseState.addStateListener(this::onStateChanged);
    }

    // === Public API ===

    /**
     * Initialize the database connection
     */
    public CompletableFuture<Void> initialize(String sasUrl, String user) {
        log.info("Initializing database connection for user: {}", user);
        databaseState.setState(DatabaseState.State.INITIALIZING);

        this.currentUser = user;
        this.api = new AzureTableApi(sasUrl, gson, httpClient);

        // Update action queue context
        updateActionQueueContext();

        // Load all data
        databaseState.setState(DatabaseState.State.LOADING_DATA);

        return actionQueue.submit(new DatabaseActions.LoadAllItemsAction())
                .thenAccept(items -> {
                    synchronized (unlockedItems) {
                        unlockedItems.clear();
                        unlockedItems.putAll(items);
                    }
                    databaseState.setState(DatabaseState.State.READY);
                    log.info("Database initialized successfully with {} items", items.size());

                    // Process any pre-initialization inserts
                    processPendingInserts();

                    // Start periodic sync
                    startPeriodicSync();
                })
                .exceptionally(throwable -> {
                    log.error("Failed to initialize database", throwable);
                    databaseState.setState(DatabaseState.State.ERROR, throwable);
                    return null;
                });
    }

    /**
     * Insert a new unlocked item
     */
    public CompletableFuture<UnlockedItemEntity> insertItem(UnlockedItemEntity item) {
        log.debug("Queuing insert for item: {}", item.getItemName());

        // If database is not ready, queue the insert for later processing
        if (!databaseState.isReady()) {
            log.debug("Database not ready, queuing insert for later: {}", item.getItemName());
            CompletableFuture<UnlockedItemEntity> future = new CompletableFuture<>();
            preInitInsertQueue.add(new PendingInsert(item, future));
            return future;
        }

        // Check if the item already exists in the cache
        synchronized (unlockedItems) {
            if (unlockedItems.containsKey(item.getItemId())) {
                log.debug("Item {} already exists, returning cached instance", item.getItemName());
                return CompletableFuture.completedFuture(unlockedItems.get(item.getItemId()));
            }
        }

        // Database is ready, process normally
        return actionQueue.submit(new DatabaseActions.InsertItemAction(item))
                .thenApply(insertedItem -> {
                    // Notify listeners of new item
                    notifyItemListeners(List.of(insertedItem));
                    return insertedItem;
                });
    }

    /**
     * Delete an unlocked item
     */
    public CompletableFuture<Void> deleteItem(int itemId) {
        log.debug("Queuing delete for item ID: {}", itemId);

        return actionQueue.submit(new DatabaseActions.DeleteItemAction(itemId));
    }

    /**
     * Get current unlocked items (thread-safe snapshot)
     */
    public Map<Integer, UnlockedItemEntity> getUnlockedItems() {
        synchronized (unlockedItems) {
            return new HashMap<>(unlockedItems);
        }
    }

    /**
     * Get current database state
     */
    public DatabaseState.State getState() {
        return databaseState.getCurrentState();
    }

    /**
     * Check if database is ready for operations
     */
    public boolean isReady() {
        return databaseState.isReady();
    }

    public boolean hasItem(int itemId) {
        synchronized (unlockedItems) {
            return unlockedItems.containsKey(itemId);
        }
    }

    /**
     * Add listener for new items
     */
    public void addItemListener(Consumer<List<UnlockedItemEntity>> listener) {
        itemListeners.add(listener);
    }

    /**
     * Add listener for state changes
     */
    public void addStateListener(Consumer<DatabaseState.State> listener) {
        databaseState.addStateListener(listener);
    }

    /**
     * Create a new unlocked item entity
     */
    public UnlockedItemEntity createNewItem(Integer itemId, String itemName) {
        return new UnlockedItemEntity(itemName, itemId, currentUser);
    }

    /**
     * Close the repository and clean up resources.
     * This clears the queue and cached data but keeps the repository reusable.
     */
    public void close() {
        log.info("Closing database repository");

        databaseState.reset();
        actionQueue.clear();

        // Cancel the periodic sync task if it exists
        if (syncTask != null && !syncTask.isCancelled()) {
            log.debug("Cancelling periodic sync task");
            syncTask.cancel(false); // Don't interrupt if currently running
            syncTask = null;
        }

        synchronized (unlockedItems) {
            unlockedItems.clear();
        }

        // Cancel any pending inserts
        for (PendingInsert pending : preInitInsertQueue) {
            pending.future.completeExceptionally(new IllegalStateException("Repository closed"));
        }
        preInitInsertQueue.clear();

        api = null;
        currentUser = null;
    }

    // === Configuration ===

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public void setHttpClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getCurrentUser() {
        return currentUser != null ? currentUser : "";
    }

    // === Private Methods ===

    private void onStateChanged(DatabaseState.State newState) {
        updateActionQueueContext();
    }

    private void updateActionQueueContext() {
        ActionExecutionContext context = new ActionExecutionContext(
                api,
                unlockedItems,
                currentUser,
                databaseState.isReady());

        actionQueue.updateExecutionContext(context);
    }

    /**
     * Process any inserts that were queued before database initialization
     */
    private void processPendingInserts() {
        if (preInitInsertQueue.isEmpty()) {
            return;
        }

        log.info("Processing {} pending inserts from before initialization", preInitInsertQueue.size());

        // Process all pending inserts
        for (PendingInsert pending : preInitInsertQueue) {
            try {
                // Check if the item is already in the cache
                synchronized (unlockedItems) {
                    if (unlockedItems.containsKey(pending.item.getItemId())) {
                        log.debug("Item {} already exists, skipping insert", pending.item.getItemName());
                        pending.future.complete(unlockedItems.get(pending.item.getItemId()));
                        continue;
                    }
                }

                // Now submit to the action queue with the properly initialized cache
                actionQueue.submit(new DatabaseActions.InsertItemAction(pending.item))
                        .thenApply(insertedItem -> {
                            // Notify listeners of new item
                            notifyItemListeners(List.of(insertedItem));
                            return insertedItem;
                        })
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                pending.future.completeExceptionally(throwable);
                            } else {
                                pending.future.complete(result);
                            }
                        });
            } catch (Exception e) {
                log.error("Failed to process pending insert for item: {}", pending.item.getItemName(), e);
                pending.future.completeExceptionally(e);
            }
        }

        // Clear the queue
        preInitInsertQueue.clear();
    }

    private void startPeriodicSync() {
        syncTask = scheduler.scheduleAtFixedRate(() -> {
            if (databaseState.isReady()) {
                performSync();
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    private void performSync() {
        OffsetDateTime lastSync = getLastSyncTime();

        actionQueue.submit(new DatabaseActions.SyncAction(lastSync, currentUser))
                .thenAccept(newItemsFromOthers -> {
                    if (!newItemsFromOthers.isEmpty()) {
                        log.debug("Synced {} new items from other users", newItemsFromOthers.size());
                        notifyItemListeners(newItemsFromOthers);
                    }
                })
                .exceptionally(throwable -> {
                    log.warn("Sync failed: {}", throwable.getMessage());
                    return null;
                });
    }

    private OffsetDateTime getLastSyncTime() {
        synchronized (unlockedItems) {
            return unlockedItems.values().stream()
                    .map(UnlockedItemEntity::getAcquiredOn)
                    .max(OffsetDateTime::compareTo)
                    .orElse(OffsetDateTime.now().minusMinutes(10));
        }
    }

    private void notifyItemListeners(List<UnlockedItemEntity> newItems) {
        for (Consumer<List<UnlockedItemEntity>> listener : itemListeners) {
            try {
                listener.accept(newItems);
            } catch (Exception e) {
                log.error("Error notifying item listener", e);
            }
        }
    }
}
