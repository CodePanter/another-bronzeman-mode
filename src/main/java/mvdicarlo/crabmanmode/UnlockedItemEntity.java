package mvdicarlo.crabmanmode;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public class UnlockedItemEntity {
    public final static String PartitionKey = "UnlockedItem";
    private String itemName;
    private Integer itemId;
    private String acquiredBy;
    private OffsetDateTime acquiredOn;

    public UnlockedItemEntity(String itemName, Integer itemId, String acquiredBy, OffsetDateTime acquiredOn) {
        this.itemName = itemName;
        this.itemId = itemId;
        this.acquiredBy = acquiredBy;
        this.acquiredOn = acquiredOn;
    }

    public UnlockedItemEntity(String itemName, Integer itemId, String acquiredBy) {
        this(itemName, itemId, acquiredBy, OffsetDateTime.now());
    }

    public Integer getItemId() {
        return itemId;
    }

    public String getAcquiredBy() {
        return acquiredBy;
    }

    public String setAcquiredBy(String acquiredBy) {
        return this.acquiredBy = acquiredBy;
    }

    public String getItemName() {
        return itemName;
    }

    public OffsetDateTime getAcquiredOn() {
        return acquiredOn;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("PartitionKey", PartitionKey);
        map.put("RowKey", itemId.toString());
        map.put("ItemName", itemName);
        map.put("AcquiredBy", acquiredBy);
        // map.put("Timestamp", acquiredOn.toString()); // Fine to allow null
        return map;
    }

    public static UnlockedItemEntity fromMap(Map<String, Object> map) {
        String itemName = (String) map.get("ItemName");
        Integer itemId = Integer.parseInt((String) map.get("RowKey"));
        String acquiredBy = (String) map.get("AcquiredBy");
        OffsetDateTime acquiredOn = OffsetDateTime.parse((String) map.get("Timestamp"));
        return new UnlockedItemEntity(itemName, itemId, acquiredBy, acquiredOn);
    }
}