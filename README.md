# Group Bronzeman Mode Mode RuneLite Plugin

This plugin implements the custom game mode called 'Group Bronzeman Mode'. (aka Crabman... by me)
The idea of this game mode lies somewhere between a 'normal' account and an 'Ironman' account; as you can't buy an item on the Grand Exchange until you have obtained that item through other means, such as getting it as a drop or buying it in a shop.

The plugin enforces this rule by keeping track of all items you acquire, and only allowing you to buy this item.
When the plugin is enabled for the first time it will unlock all items in your inventory, as well as unlock all items in your bank the next time you open it.

This plugin requires an Azure Storage Data Table to be setup to allow for multiple users in your group to share an unlock list.

Original implementation based off of [Another Bronzeman Mode](https://github.com/CodePanter/another-bronzeman-mode)

## Why Azure Storage Account Data Tables?

Azure Storage Account Data Tables were chosen for this plugin because they are easy to set up and cost-effective. Setting up an Azure Storage Account is straightforward. It is also relatively cheap at around a dollar or less a month. This ensures that you and your group can use the plugin without significant expenses or personal database management.

You can check current pricing [here](https://azure.microsoft.com/en-us/pricing/details/storage/tables/).

## ⚠️ Important: Setting Up Azure Storage Account ⚠️

To use the Group Bronzeman Mode RuneLite Plugin, you need to set up an Azure Storage Account. This is essential for enabling multiple users in your group to share an unlock list. Follow the steps below to create and configure your Azure Storage Account:

1. **Create an Azure Storage Account**:
   - Go to the [Azure Portal](https://portal.azure.com/).
   - Click on "Create a resource" and select "Storage account".
   - Fill in the required details such as Subscription, Resource group, Storage account name, Region, and Performance.
   - Click "Review + create" and then "Create" to finalize the creation of your storage account.

2. **Configure the Storage Account**:
- Once the storage account is created, navigate to it in the Azure Portal.
   1. Go to the **Data Storage** section and select **Tables**.
   2. Create a new table if you haven't already.
   3. Navigate to the **Storage Browser** section.
   4. Locate your table, click on the **'...'** icon next to it, and select **Generate SAS**.
   5. In the **Generate SAS** dialog:
      - Ensure all permissions (Read, Write, Delete, etc.) are selected.
      - Set an expiration time far in the future to avoid frequent renewals.
   6. Click **Generate** and copy the generated SAS URL. You will need this to configure the plugin.

3. **Configure the Plugin**:
   - Open the RuneLite client and go to the plugin configuration for Group Bronzeman Mode.
   - Enter the SAS Url you copied earlier into the appropriate field.

By following these steps, you will have a fully functional Azure Storage Account set up to use with the Group Bronzeman Mode RuneLite Plugin. This will allow you and your group members to share and synchronize item unlocks seamlessly.



## Features

- Restricts buying items from the Grand Exchange until that item is obtained through self-sufficient methods.
- Will disallow trading players depending on your settings.
- Shows an item unlock graphic every time you obtain an item for the first time.
- Unlocks are handled per account so you can have multiple bronze-men or not effect the status of your bronze-man accounts.
- Can optionally take screenshots of all new item unlocks.
- Supports adding a list of names of other Bronzeman accounts, and will provide them with (client side) Bronzeman chat icons.
- Has settings for sending chat messages and notifications for every item unlock.
- Allows the command `!gbmcount` and `!bgmunlocks` to get a total number for all unlocked items.
- Allows the command `!gbmrecent` to list most recent unlocked items.

## Screenshots

![Unlocking an item](https://i.imgur.com/odE4nVo.png)
Unlocking an item right after getting off tutorial island.

![Chat icons](https://i.imgur.com/D8Zl6Ss.png)
Talking to fellow Bronzemen with chat icons and everything.

![Grand exchange](https://i.imgur.com/lTd0I6P.png)
This player has only unlocked bronze arrows, so the other items are greyed out and not clickable.

![Collection log](https://i.imgur.com/6ae3Qml.png)
You can see all your unlocks in the collection log as a neatly ordered list of items.
Depending on your settings, you will either see this when you open the log, or under the 'Other' tab, scrolling all the way down and clicking "Bronzeman Unlocks".
This interface comes with search functionality, as well as the ability to re-lock an item by right clicking it and selecting "Remove".

## Credits

- First envisioned by [GUDI (Mod Ronan)](https://www.youtube.com/watch?v=GFNfa2saOJg)
- [Initial](https://github.com/sethrem/bronzeman) code written by [Sethrem](https://github.com/sethrem)
- Original implementation based off of [Another Bronzeman Mode](https://github.com/CodePanter/another-bronzeman-mode)
