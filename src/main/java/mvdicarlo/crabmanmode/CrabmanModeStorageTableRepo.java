package mvdicarlo.crabmanmode;

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

import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

@Slf4j
public class CrabmanModeStorageTableRepo {
    private AzureTableApi api;
    private final Map<Integer, UnlockedItemEntity> unlockedItems = new HashMap<>();
    private ScheduledExecutorService scheduler;
    private final List<Consumer<List<UnlockedItemEntity>>> listeners = new ArrayList<>();
    private String user;

    private Gson gson;
    private OkHttpClient httpClient;

    // Used to flush items added to the list while the username is still missing for
    // whatever reason
    private final List<UnlockedItemEntity> unlockedItemQueue = new ArrayList<>();

    public Map<Integer, UnlockedItemEntity> getUnlockedItems() {
        return unlockedItems;
    }

    public void updateConnection(String sasUrl) {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduleUnlocksUpdate();
            scheduleFlushQueue();
        }
        api = new AzureTableApi(sasUrl, gson, httpClient);
        unlockedItems.clear();
        unlockedItems.putAll(getAllUnlockedItems());
    }

    public void close() {
        user = null;
        api = null;
        unlockedItems.clear();
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public void setHttpClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
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
        return api != null && user != null && !user.isEmpty();
    }

    private void flushQueue() {
        if (isInitialized()) {
            unlockedItemQueue.forEach(item -> {
                try {
                    this.insertUnlockedItem(item);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            unlockedItemQueue.clear();
        }
    }

    private boolean itemExists(Integer itemId) {
        try {
            if (unlockedItems.containsKey(itemId)) {
                return true;
            }
            return api.getEntity(UnlockedItemEntity.PartitionKey, itemId.toString()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public UnlockedItemEntity createNewUnlockedItem(Integer itemId, String itemName) {
        return new UnlockedItemEntity(itemName, itemId, user);
    }

    public void insertUnlockedItem(UnlockedItemEntity unlockedItem) throws Exception {
        log.debug("Attempting to insert unlocked item: " + unlockedItem.getItemName());
        if (!isInitialized()) {
            log.debug("Not initialized, adding to queue");
            unlockedItemQueue.add(unlockedItem);
            return;
        }
        if (itemExists(unlockedItem.getItemId())) {
            log.debug("Item already exists, skipping");
            return;
        }
        unlockedItem.setAcquiredBy(user);
        api.insertEntity(unlockedItem);
        List<UnlockedItemEntity> entities = new ArrayList<>();
        entities.add(unlockedItem);
        notifyListeners(entities);
    }

    public Map<Integer, UnlockedItemEntity> getAllUnlockedItems() {
        Map<Integer, UnlockedItemEntity> unlockedItemsMap = new HashMap<>();
        try {
            api.listEntities().forEach(unlockedItem -> {
                unlockedItemsMap.put(unlockedItem.getItemId(), unlockedItem);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return unlockedItemsMap;
    }

    public Map<Integer, UnlockedItemEntity> getAllUnlockedAfter(OffsetDateTime acquiredOn) {
        Map<Integer, UnlockedItemEntity> unlockedItemsMap = new HashMap<>();

        // Create OData filter
        String filter = "Timestamp%20gt%20datetime'" + acquiredOn.toString() + "'";
        try {
            api.listEntities(filter).forEach(unlockedItem -> {
                unlockedItemsMap.put(unlockedItem.getItemId(), unlockedItem);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            try {
                api.deleteEntity(UnlockedItemEntity.PartitionKey, Integer.toString(itemId));
            } catch (Exception e) {
                e.printStackTrace();
            }
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