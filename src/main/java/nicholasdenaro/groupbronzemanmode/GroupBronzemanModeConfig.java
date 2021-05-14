/*
 * Copyright (c) 2019, CodePanter <https://github.com/codepanter>
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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(GroupBronzemanModePlugin.CONFIG_GROUP)
public interface GroupBronzemanModeConfig extends Config
{
    @ConfigItem(
            keyName = "namesBronzeman",
            name = "Bronzeman Names",
            position = 1,
            description = "Configures names of bronzemen to highlight in chat. Format: (name), (name)"
    )
    default String namesBronzeman()
    {
        return "";
    }

    @ConfigItem(
            keyName = "screenshotUnlock",
            name = "Screenshot new Unlocks",
            position = 2,
            description = "Take a screenshot whenever a new item is unlocked."
    )
    default boolean screenshotUnlock()
    {
        return false;
    }

    @ConfigItem(
            keyName = "includeFrame",
            name = "Include Client Frame",
            description = "Configures whether or not the client frame is included in screenshots.",
            position = 3
    )
    default boolean includeFrame()
    {
        return false;
    }

    @ConfigItem(
        keyName = "sendNotification",
        name = "Notify on unlock",
        description = "Send a notification when a new item is unlocked.",
        position = 4
    )
    default boolean sendNotification()
    {
        return false;
    }

    @ConfigItem(
        keyName = "sendChatMessage",
        name = "Chat message on unlock",
        description = "Send a chat message when a new item is unlocked.",
        position = 5
    )
    default boolean sendChatMessage()
    {
        return false;
    }

    @ConfigItem(
        keyName = "moveCollectionLogUnlocks",
        name = "Move collection log unlocks",
        description = "Moves the bronzeman mode unlocks to the bottom of the 'Other' tab.",
        position = 6
    )
    default boolean moveCollectionLogUnlocks()
    {
        return false;
    }

    @ConfigItem(
        keyName = "allowTrading",
        name = "Allow trading as a bronzeman",
        description = "Allows the player to trade even though they are a bronzeman.",
        position = 7
    )
    default boolean allowTrading()
    {
        return false;
    }

    @ConfigItem(
            keyName = "resetCommand",
            name = "Enable reset command",
            description = "Enables the !bmreset command used for wiping your unlocked items.",
            position = 8
    )
    default boolean resetCommand()
    {
        return false;
    }

    @ConfigItem(
            keyName = "syncGroup",
            name = "Enable group syncing",
            description = "Enables syncing item unlocks with a group by using a shared Google Sheet.",
            position = 9
    )
    default boolean syncGroup(){ return false; }

    @ConfigItem(
            keyName = "syncSheetId",
            name = "Google Sheet Id",
            description = "Google Sheet Id for syncing item unlocks",
            position = 10
    )
    default String syncSheetId(){ return ""; }

    @ConfigItem(
            keyName = "oAuth2ClientDetails",
            name = "Client Credentials",
            description = "This is the file contents of the client details from Google Cloud Platform.",
            position = 11
    )
    default String oAuth2ClientDetails(){ return ""; }

    @ConfigItem(
            keyName = "authorize",
            name = "Google Sheet Authorize",
            description = "Toggle this to do authorization.",
            position = 12
    )
    default boolean authorize(){return false;}

    @ConfigItem(
            keyName = "markBronzemanLoot",
            name = "Mark Bronzeman Loot",
            description = "This will also track other items of yours, like arrows, darts, and bolts. This also shows naturally spawning items.",
            position = 13
    )
    default boolean markBronzemanLoot(){return false;}

    @ConfigItem(
            keyName = "markUnlockableLoot",
            name = "Mark Unlockable Loot",
            description = "This will indicate which loot is an unlock.",
            position = 14
    )
    default boolean markUnlockableLoot(){return false;}

    @ConfigItem(
            keyName = "restrictLootLeftClick",
            name = "Restrict Left Click",
            description = "This will prevent you from picking up loot that is not yours.",
            position = 15
    )
    default boolean restrictLootLeftClick(){return false;}

    @ConfigItem(
            keyName = "restrictLootMenu",
            name = "Restrict Menu Take",
            description = "This will prevent you from selecting the take menu option for loot that is not yours.",
            position = 16
    )
    default boolean restrictLootMenu(){return false;}
}
