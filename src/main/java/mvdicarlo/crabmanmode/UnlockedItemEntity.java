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
        entity = new TableEntity(PartitionKey, itemId.toString());
        entity.addProperty("AcquiredBy", acquiredBy);
        entity.addProperty("ItemName", itemName);
    }

    public Integer getItemId() {
        return Integer.parseInt(entity.getProperty("RowKey").toString());
    }

    public String getAcquiredBy() {
        Object acBy = entity.getProperty("AcquiredBy");
        if (acBy == null) {
            return "";
        }
        return acBy.toString();
    }

    public void setAcquiredBy(String acquiredBy) {
        entity.addProperty("AcquiredBy", acquiredBy);
    }

    public String getItemName() {
        return entity.getProperty("ItemName").toString();
    }

    public OffsetDateTime getAcquiredOn() {
        return entity.getTimestamp();
    }

    public TableEntity getEntity() {
        return entity;
    }
}