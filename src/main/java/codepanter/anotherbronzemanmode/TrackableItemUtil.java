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
package codepanter.anotherbronzemanmode;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.kit.KitType;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TrackableItemUtil
{
    @Inject
    private Client client;

    public boolean isProjectile(TileItem item)
    {
        PlayerComposition equips = client.getLocalPlayer().getPlayerComposition();

        return
                (isArrow(item)
                        && isBow(equips.getEquipmentId(KitType.WEAPON))
                        && item.getId() == client.getItemContainer(InventoryID.EQUIPMENT).getItem(EquipmentInventorySlot.AMMO.getSlotIdx()).getId()
                )
                        || (isBolt(item)
                        && isCrossbow(equips.getEquipmentId(KitType.WEAPON))
                        && item.getId() == client.getItemContainer(InventoryID.EQUIPMENT).getItem(EquipmentInventorySlot.AMMO.getSlotIdx()).getId()
                )
                        || (isDart(item.getId())
                        && isDart(equips.getEquipmentId(KitType.WEAPON))
                );
    }

    public boolean isDart(int itemId)
    {
        final Set<Integer> darts = new HashSet<Integer>(Arrays.asList(new Integer[]{
                ItemID.DART,
                ItemID.BRONZE_DART,
                ItemID.BRONZE_DARTP,
                ItemID.BRONZE_DARTP_5628,
                ItemID.BRONZE_DARTP_5635,
                ItemID.IRON_DART,
                ItemID.IRON_DARTP,
                ItemID.IRON_DARTP_5629,
                ItemID.IRON_DARTP_5636,
                ItemID.STEEL_DART,
                ItemID.STEEL_DARTP,
                ItemID.STEEL_DARTP_5630,
                ItemID.STEEL_DARTP_5637,
                ItemID.BLACK_DART,
                ItemID.BLACK_DARTP,
                ItemID.BLACK_DARTP_5631,
                ItemID.BLACK_DARTP_5638,
                ItemID.MITHRIL_DART,
                ItemID.MITHRIL_DARTP,
                ItemID.MITHRIL_DARTP_5632,
                ItemID.MITHRIL_DARTP_5639,
                ItemID.ADAMANT_DART,
                ItemID.ADAMANT_DARTP,
                ItemID.ADAMANT_DARTP_5633,
                ItemID.ADAMANT_DARTP_5640,
                ItemID.RUNE_DART,
                ItemID.RUNE_DARTP,
                ItemID.RUNE_DARTP_5634,
                ItemID.RUNE_DARTP_5641,
                ItemID.DRAGON_DART,
                ItemID.DRAGON_DARTP,
                ItemID.DRAGON_DARTP_11233,
                ItemID.DRAGON_DARTP_11234,
        }));

        return darts.contains(itemId);
    }

    public boolean isBow(int itemId)
    {
        final Set<Integer> bows = new HashSet<Integer>(Arrays.asList(new Integer[]{
                ItemID.SHORTBOW,
                ItemID.LONGBOW,
                ItemID.OAK_SHORTBOW,
                ItemID.OAK_LONGBOW,
                ItemID.WILLOW_SHORTBOW,
                ItemID.WILLOW_LONGBOW,
                ItemID.MAPLE_SHORTBOW,
                ItemID.MAPLE_LONGBOW,
                ItemID.YEW_SHORTBOW,
                ItemID.YEW_LONGBOW,
                ItemID.MAGIC_SHORTBOW,
                ItemID.MAGIC_LONGBOW,
                ItemID.CRYSTAL_BOW,
                ItemID.CURSED_GOBLIN_BOW,
                ItemID.COMP_OGRE_BOW,
                ItemID.CRAWS_BOW,
                ItemID.DARK_BOW,
                ItemID.MAGIC_COMP_BOW,
                ItemID.NEW_CRYSTAL_BOW,
                ItemID.OGRE_BOW,
                ItemID.RAIN_BOW,
                ItemID.SIGNED_OAK_BOW,
                ItemID.STARTER_BOW,
                ItemID.TRAINING_BOW,
                ItemID.TWISTED_BOW,
                ItemID.WILLOW_COMP_BOW,
                ItemID.YEW_COMP_BOW,
                ItemID.CORRUPTED_BOW_ATTUNED,
                ItemID.CORRUPTED_BOW_BASIC,
                ItemID.CORRUPTED_BOW_PERFECTED,
        }));

        return bows.contains(itemId);
    }

    public boolean isCrossbow(int itemId)
    {
        final Set<Integer> crossbows = new HashSet<Integer>(Arrays.asList(new Integer[]{
                ItemID.CROSSBOW,
                ItemID.BRONZE_CROSSBOW,
                ItemID.IRON_CROSSBOW,
                ItemID.STEEL_CROSSBOW,
                ItemID.MITHRIL_CROSSBOW,
                ItemID.ADAMANT_CROSSBOW,
                ItemID.RUNE_CROSSBOW,
                ItemID.ARMADYL_CROSSBOW,
                ItemID.BLURITE_CROSSBOW,
                ItemID.DORGESHUUN_CROSSBOW,
                ItemID.DRAGON_CROSSBOW,
                ItemID.HUNTERS_CROSSBOW,
                ItemID.KARILS_CROSSBOW,
                ItemID.PHOENIX_CROSSBOW,
        }));

        return crossbows.contains(itemId);
    }

    public boolean isArrow(TileItem item)
    {
        final Set<Integer> arrows = new HashSet<Integer>(Arrays.asList(new Integer[]{
                ItemID.BRONZE_ARROW,
                ItemID.BRONZE_ARROWP,
                ItemID.BRONZE_ARROWP_5616,
                ItemID.BRONZE_ARROWP_5622,
                ItemID.IRON_ARROW,
                ItemID.IRON_ARROWP,
                ItemID.IRON_ARROWP_5617,
                ItemID.IRON_ARROWP_5623,
                ItemID.STEEL_ARROW,
                ItemID.STEEL_ARROWP,
                ItemID.STEEL_ARROWP_5618,
                ItemID.STEEL_ARROWP_5624,
                ItemID.MITHRIL_ARROW,
                ItemID.MITHRIL_ARROWP,
                ItemID.MITHRIL_ARROWP_5619,
                ItemID.MITHRIL_ARROWP_5625,
                ItemID.ADAMANT_ARROW,
                ItemID.ADAMANT_ARROWP,
                ItemID.ADAMANT_ARROWP_5620,
                ItemID.ADAMANT_ARROWP_5626,
                ItemID.RUNE_ARROW,
                ItemID.RUNE_ARROWP,
                ItemID.RUNE_ARROWP_5621,
                ItemID.RUNE_ARROWP_5627,
        }));

        return arrows.contains(item.getId());
    }

    public boolean isBolt(TileItem item)
    {
        final Set<Integer> bolts = new HashSet<Integer>(Arrays.asList(new Integer[]{
                ItemID.BRONZE_BOLTS,
                ItemID.BRONZE_BOLTS_P,
                ItemID.BRONZE_BOLTS_P_6061,
                ItemID.BRONZE_BOLTS_P_6062,
                ItemID.IRON_BOLTS,
                ItemID.IRON_BOLTS_P,
                ItemID.IRON_BOLTS_P_9294,
                ItemID.IRON_BOLTS_P_9301,
                ItemID.STEEL_BOLTS,
                ItemID.STEEL_BOLTS_P,
                ItemID.STEEL_BOLTS_P_9295,
                ItemID.STEEL_BOLTS_P_9302,
                ItemID.MITHRIL_BOLTS,
                ItemID.MITHRIL_BOLTS_P,
                ItemID.MITHRIL_BOLTS_P_9296,
                ItemID.MITHRIL_BOLTS_P_9303,
                ItemID.ADAMANT_BOLTS,
                ItemID.ADAMANT_BOLTS_P,
                ItemID.ADAMANT_BOLTS_P_9297,
                ItemID.ADAMANT_BOLTS_P_9304,
                ItemID.RUNITE_BOLTS,
                ItemID.RUNITE_BOLTS_P,
                ItemID.RUNITE_BOLTS_P_9298,
                ItemID.RUNITE_BOLTS_P_9305,
        }));

        return bolts.contains(item.getId());
    }

    public boolean isNaturalSpawn(WorldPoint point, TileItem item)
    {
        return WorldItemSpawns.isNaturalSpawn(point, item);
    }
}
