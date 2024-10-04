package mvdicarlo.crabmanmode;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.ListEntitiesOptions;

public class CrabmanModeStorageTableRepo {
    private TableClient tableClient;
    private final Map<Integer, UnlockedItemEntity> unlockedItems = new HashMap<>();
    private ScheduledExecutorService scheduler;
    private final List<Consumer<List<UnlockedItemEntity>>> listeners = new ArrayList<>();
    private String user;

    // Used to flush items added to the list while the username is still missing for
    // whatever reason
    private final List<UnlockedItemEntity> unlockedItemQueue = new ArrayList<>();

    public Map<Integer, UnlockedItemEntity> getUnlockedItems() {
        return unlockedItems;
    }

    public void updateConnection(String connectionString, String tableName) {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduleUnlocksUpdate();
            scheduleFlushQueue();
        }
        tableClient = new TableClientBuilder()
                .connectionString(connectionString)
                .tableName(tableName)
                .buildClient();
        try {
            tableClient.createTable();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        unlockedItems.clear();
        unlockedItems.putAll(getAllUnlockedItems());
    }

    public void close() {
        user = null;
        tableClient = null;
        unlockedItems.clear();
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        if (user == null) {
            return "";
        }
        return user;
    }

    public boolean isInitialized() {
        return tableClient != null && user != null && !user.isEmpty();
    }

    private void flushQueue() {
        if (isInitialized()) {
            unlockedItemQueue.forEach(this::insertUnlockedItem);
            unlockedItemQueue.clear();
        }
    }

    private boolean itemExists(String itemId) {
        try {
            if (unlockedItems.containsKey(Integer.parseInt(itemId))) {
                return true;
            }
            return tableClient.getEntity(UnlockedItemEntity.PartitionKey, itemId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public UnlockedItemEntity createNewUnlockedItem(Integer itemId, String itemName) {
        return new UnlockedItemEntity(itemName, itemId, user);
    }

    public void insertUnlockedItem(UnlockedItemEntity unlockedItem) {
        System.out.println("Attempting to insert unlocked item: " + unlockedItem.getItemName());
        if (!isInitialized()) {
            unlockedItemQueue.add(unlockedItem);
            return;
        }
        if (itemExists(unlockedItem.getEntity().getRowKey())) {
            return;
        }
        unlockedItem.setAcquiredBy(user);
        tableClient.upsertEntity(unlockedItem.getEntity());
        List<UnlockedItemEntity> entities = new ArrayList<>();
        entities.add(unlockedItem);
        notifyListeners(entities);
    }

    public Map<Integer, UnlockedItemEntity> getAllUnlockedItems() {
        Map<Integer, UnlockedItemEntity> unlockedItemsMap = new HashMap<>();
        this.tableClient.listEntities().forEach(entity -> {
            UnlockedItemEntity unlockedItem = new UnlockedItemEntity(entity);
            unlockedItemsMap.put(unlockedItem.getItemId(), unlockedItem);
        });
        return unlockedItemsMap;
    }

    public Map<Integer, UnlockedItemEntity> getAllUnlockedAfter(OffsetDateTime acquiredOn) {
        Map<Integer, UnlockedItemEntity> unlockedItemsMap = new HashMap<>();

        // Create OData filter
        String filter = "Timestamp gt datetime'" + acquiredOn.toString() + "'";
        ListEntitiesOptions listEntitiesOptions = new ListEntitiesOptions()
                .setFilter(filter);
        this.tableClient.listEntities(listEntitiesOptions, Duration.ofSeconds(5), null).forEach(entity -> {
            UnlockedItemEntity unlockedItem = new UnlockedItemEntity(entity);
            unlockedItemsMap.put(unlockedItem.getItemId(), unlockedItem);
        });
        return unlockedItemsMap;
    }

    public void updateUnlockedItems() {
        OffsetDateTime acquiredOn = unlockedItems.values().stream()
                .map(UnlockedItemEntity::getAcquiredOn)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now().minusMinutes(1));
        Map<Integer, UnlockedItemEntity> newUnlockedItems = getAllUnlockedAfter(acquiredOn);
        List<UnlockedItemEntity> newItemsUnlockedByOthers = newUnlockedItems.values().stream()
                .filter(item -> !item.getAcquiredBy().equals(this.user))
                .collect(Collectors.toList());
        synchronized (unlockedItems) {
            unlockedItems.putAll(newUnlockedItems);
        }
        notifyListeners(newItemsUnlockedByOthers);
    }

    public void deleteUnlockedItem(int itemId) {
        if (isInitialized()) {
            tableClient.deleteEntity(UnlockedItemEntity.PartitionKey, Integer.toString(itemId));
            unlockedItems.remove(itemId);
        }
    }

    private void scheduleUnlocksUpdate() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isInitialized()) {
                    updateUnlockedItems();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    public void scheduleFlushQueue() {
        scheduler.scheduleAtFixedRate(() -> {
            if (isInitialized() && !unlockedItemQueue.isEmpty()) {
                flushQueue();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void addUnlockedItemsListener(Consumer<List<UnlockedItemEntity>> listener) {
        listeners.add(listener);
    }

    private void notifyListeners(List<UnlockedItemEntity> newUnlockedItems) {
        for (Consumer<List<UnlockedItemEntity>> listener : listeners) {
            listener.accept(newUnlockedItems);
        }
    }
}