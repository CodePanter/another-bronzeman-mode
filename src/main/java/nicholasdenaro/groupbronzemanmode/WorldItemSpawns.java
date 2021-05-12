package nicholasdenaro.groupbronzemanmode;

import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.runelite.http.api.RuneLiteAPI.GSON;

public class WorldItemSpawns {

    static Map<String, WorldSpawnItem> items;

    public static void populateWorldSpawns(ItemManager itemManager)
    {
        items = new HashMap<>();

        InputStream in = WorldItemSpawns.class.getResourceAsStream("/osrs-items.json");
        ItemsWrapper wrapper = GSON.fromJson(new InputStreamReader(in), ItemsWrapper.class);
        for (Integer item : wrapper.Items.keySet())
        {
            List<ItemLoader> loaders = wrapper.Items.get(item);
            for (ItemLoader load : loaders)
            {
                items.putIfAbsent(load.lat + "|" +load.lon, new WorldSpawnItem(item, load.qty));
            }
        }

    }

    public static boolean isNaturalSpawn(WorldPoint point, TileItem item)
    {
        String loc = point.getX() + "|" + point.getY();
        return items.containsKey(loc) && items.get(loc).id == item.getId() && items.get(loc).quantity == item.getQuantity();
    }
}

class ItemsWrapper
{
    public Map<Integer, List<ItemLoader>> Items;
}

class WorldSpawnItem
{
    public int id;
    public int quantity;

    public WorldSpawnItem(int id, int quantity)
    {
        this.id = id;
        this.quantity = quantity;
    }
}

class ItemLocation
{
    public int lat;
    public int lon;

    public ItemLocation(int lat, int lon)
    {
        this.lat = lat;
        this.lon = lon;
    }
}

class ItemLoader
{
    public int lat;
    public int lon;
    public int qty;
}