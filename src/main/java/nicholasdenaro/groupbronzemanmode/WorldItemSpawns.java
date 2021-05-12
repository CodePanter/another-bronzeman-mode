/*
 * Copyright (c) 2021, Nicholas Denaro <ndenarodev@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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