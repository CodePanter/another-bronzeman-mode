# Crabman Mode Mode RuneLite Plugin
This plugin implements the custom game mode called 'Group Bronzeman Mode'. (aka Crabman... by me)
The idea of this game mode lies somewhere between a 'normal' account and an 'Ironman' account; as you can't buy an item on the Grand Exchange until you have obtained that item through other means, such as getting it as a drop or buying it in a shop.

The plugin enforces this rule by keeping track of all items you acquire, and only allowing you to buy this item.
When the plugin is enabled for the first time it will unlock all items in your inventory, as well as unlock all items in your bank the next time you open it.

This plugin requires an Azure Storage Data Table to be setup to allow for multiple users in your group to share an unlock list.

Original implementation based off of [Another Bronzeman Mode](https://github.com/CodePanter/another-bronzeman-mode)

## Features

- Restricts buying items from the Grand Exchange until that item is obtained through self-sufficient methods.
- Will disallow trading players depending on your settings.
- Shows an item unlock graphic every time you obtain an item for the first time.
- Unlocks are handled per account so you can have multiple bronze-men or not effect the status of your bronze-man accounts.
- Can optionally take screenshots of all new item unlocks.
- Supports adding a list of names of other Bronzeman accounts, and will provide them with (client side) Bronzeman chat icons.
- Has settings for sending chat messages and notifications for every item unlock.
- Allows the command '!gbmcount' and '!bgmunlocks' to get a total number for all unlocked items.

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
