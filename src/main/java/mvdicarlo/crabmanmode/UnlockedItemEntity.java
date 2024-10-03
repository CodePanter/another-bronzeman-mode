package mvdicarlo.crabmanmode;

import java.time.OffsetDateTime;

import com.azure.data.tables.models.TableEntity;

public class UnlockedItemEntity {
    public final static String PartitionKey = "UnlockedItem";

    private final TableEntity entity;

    public UnlockedItemEntity(TableEntity entity) {
        this.entity = entity;
    }

    public UnlockedItemEntity(String itemName, Integer itemId, String acquiredBy) {
        this.entity = new TableEntity(PartitionKey, itemId.toString());
        this.entity.addProperty("AcquiredBy", acquiredBy);
        this.entity.addProperty("ItemName", itemName);
    }

    public Integer getItemId() {
        return Integer.parseInt(this.entity.getProperty("RowKey").toString());
    }

    public String getAcquiredBy() {
        return this.entity.getProperty("AcquiredBy").toString();
    }

    public String getItemName() {
        return this.entity.getProperty("ItemName").toString();
    }

    public OffsetDateTime getAcquiredOn() {
        return this.entity.getTimestamp();
    }

    public TableEntity getEntity() {
        return this.entity;
    }
}