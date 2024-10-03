package mvdicarlo.crabmanmode;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.ListEntitiesOptions;

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

public class CrabmanModeStorageTableRepo {
    private TableClient tableClient;
    private final Map<Integer, UnlockedItemEntity> unlockedItems = new HashMap<>();
    private ScheduledExecutorService scheduler;
    private final List<Consumer<List<UnlockedItemEntity>>> listeners = new ArrayList<>();
    private String user;

    public Map<Integer, UnlockedItemEntity> getUnlockedItems()
    {
        return unlockedItems;
    }

    public void initialize()
    {
        if (tableClient != null) {
            return;
        }
        this.tableClient = new TableClientBuilder()
                .connectionString(
                        "stub")
                .tableName("stub")
                .buildClient();
        unlockedItems.putAll(getAllUnlockedItems());

        // Initialize the scheduler
        scheduler = Executors.newScheduledThreadPool(1);
        scheduleUnlocksUpdate();
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    private boolean itemExists(String itemId) {
        try {
            return tableClient.getEntity(UnlockedItemEntity.PartitionKey, itemId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public UnlockedItemEntity createNewUnlockedItem(Integer itemId, String itemName) {
        return new UnlockedItemEntity(itemName, itemId, user);
    }

    public void insertUnlockedItem(UnlockedItemEntity unlockedItem) {
        if (itemExists(unlockedItem.getEntity().getRowKey())) {
            return;
        }
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
            .toList();
        synchronized (unlockedItems) {
            unlockedItems.putAll(newUnlockedItems);
        }
        notifyListeners(newItemsUnlockedByOthers);
    }

    private void scheduleUnlocksUpdate() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateUnlockedItems();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void addUnlockedItemsListener(Consumer<List<UnlockedItemEntity>> listener) {
        listeners.add(listener);
    }

    public void clear()
    {
        unlockedItems.clear();
    }

    private void notifyListeners(List<UnlockedItemEntity> newUnlockedItems) {
        for (Consumer<List<UnlockedItemEntity>> listener : listeners) {
            listener.accept(newUnlockedItems);
        }
    }
}