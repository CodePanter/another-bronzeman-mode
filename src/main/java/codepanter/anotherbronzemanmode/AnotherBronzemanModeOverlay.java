/*
 * Copyright (c) 2019, CodePanter <https://github.com/codepanter>
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
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.grounditems.GroundItemsConfig;
import net.runelite.client.plugins.grounditems.config.PriceDisplayMode;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageUploadStyle;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.QuantityFormatter;

import static codepanter.anotherbronzemanmode.AnotherBronzemanModePlugin.MAX_QUANTITY;

@Slf4j
public class AnotherBronzemanModeOverlay extends Overlay
{
    private ConfigManager configManager;
    private final Client client;
    private final AnotherBronzemanModePlugin plugin;
    private final AnotherBronzemanModeConfig config;
    private GroundItemsConfig gconfig;

    private Integer currentUnlock;
    private long displayTime;
    private int displayY;

    private final List<Integer> itemUnlockList;
    private boolean screenshotUnlock;
    private boolean includeFrame;

    private final StringBuilder itemStringBuilder = new StringBuilder();
    private static final int MAX_DISTANCE = 2500;
    private final Map<WorldPoint, Integer> offsetMap = new HashMap<>();
    // We must offset the text on the z-axis such that
    // it doesn't obscure the ground items below it.
    private static final int OFFSET_Z = 20;
    // The 15 pixel gap between each drawn ground item.
    private static final int STRING_GAP = 15;
    private final TextComponent textComponent = new TextComponent();

    @Inject
    private ItemManager itemManager;

    @Inject
    private ImageCapture imageCapture;

    @Inject
    private ClientUI clientUi;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private DrawManager drawManager;

    @Inject
    public AnotherBronzemanModeOverlay(ConfigManager configManager, Client client, AnotherBronzemanModePlugin plugin, AnotherBronzemanModeConfig config)
    {
        super(plugin);
        this.client = client;
        this.config = config;
        this.gconfig = configManager.getConfig(GroundItemsConfig.class);
        this.plugin = plugin;
        this.itemUnlockList = new ArrayList<>();
        this.screenshotUnlock = false;
        this.includeFrame = false;
    }

    public void addItemUnlock(int itemId)
    {
        itemUnlockList.add(itemId);
    }

    public void updateScreenshotUnlock(boolean doScreenshotUnlock, boolean doIncludeFrame)
    {
        screenshotUnlock = doScreenshotUnlock;
        includeFrame = doIncludeFrame;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        if (config.showBronzemanLoot())
        {
            renderGroundItems(graphics);
        }
        //setPosition(OverlayPosition.TOP_CENTER);

        if (itemUnlockList.isEmpty())
        {
            return null;
        }

        if (itemManager == null)
        {
            System.out.println("Item-manager is null");
            return null;
        }
        if (currentUnlock == null)
        {
            currentUnlock = itemUnlockList.get(0);
            displayTime = System.currentTimeMillis();
            displayY = -20;
            return null;
        }

        // Drawing unlock pop-up at the top of the screen.
        graphics.drawImage(plugin.getUnlockImage(),-62, displayY, null);
        graphics.drawImage(itemManager.getImage(currentUnlock, 1, false),-50, displayY + 7, null);
        if (displayY < 10)
        {
            displayY = displayY + 1;
        }

        if (System.currentTimeMillis() > displayTime + (5000))
        {
            if (screenshotUnlock)
            {
                int itemID = currentUnlock;
                ItemComposition itemComposition = itemManager.getItemComposition(itemID);
                String itemName = itemComposition.getName();
                String fileName = "ItemUnlocked " + itemName + " ";
                takeScreenshot(fileName);
            }
            itemUnlockList.remove(currentUnlock);
            currentUnlock = null;
        }
        return null;
    }

    private boolean isDefaultGroundItemTracker(GroundItem item)
    {
        return item.getLootType() != LootType.PROJECTILES && item.getLootType() != LootType.WORLD;
    }

    private void renderGroundItems(Graphics2D graphics)
    {
        // Start Item render

        offsetMap.clear();

//        plugin.setTextBoxBounds(null);
//        plugin.setHiddenBoxBounds(null);
//        plugin.setHighlightBoxBounds(null);

        Collection<GroundItem> groundItemList = plugin.getCollectedGroundItems().values();
//        final boolean outline = config.textOutline();
        final boolean outline = false;
        //final DespawnTimerMode groundItemTimers = config.groundItemTimers();
        //final boolean outline = config.textOutline();


        if (plugin.isHotKeyPressed())
        {
            // Make copy of ground items because we are going to modify them here, and the array list supports our
            // desired behaviour here
            groundItemList = new ArrayList<>(groundItemList);
//            final java.awt.Point awtMousePos = new java.awt.Point(mousePos.getX(), mousePos.getY());
            GroundItem groundItem = null;

//            if (gconfig.onlyShowLoot())
//            {
//                groundItemList.stream().filter(item -> item.getLootType() != LootType.PROJECTILES).forEach(item ->
//                {
//                    item.setOffset(offsetMap.compute(item.getLocation(), (k, v) -> v != null ? v + 1 : 0));
//                });
//
//                groundItemList.stream().filter(item -> item.getLootType() == LootType.PROJECTILES).forEach(item ->
//                {
//                    item.setOffset(offsetMap.compute(item.getLocation(), (k, v) -> v != null ? v + 1 : 0));
//                });
//            }
//            else
            {
                groundItemList.stream().forEach(item ->
                {
                    item.setOffset(offsetMap.compute(item.getLocation(), (k, v) -> v != null ? v + 1 : 0));
                });
            }

//            if (groundItem != null)
//            {
//                groundItemList.remove(groundItem);
//                groundItemList.add(groundItem);
////                topGroundItem = groundItem;
//            }
        }

        if (gconfig.onlyShowLoot() && !plugin.isHotKeyPressed())
        {
            groundItemList.stream().filter(item -> isDefaultGroundItemTracker(item)).forEach(item ->
            {
                markBronzemanItem(graphics, item);
            });

            groundItemList.stream().filter(item -> !isDefaultGroundItemTracker(item)).forEach(item ->
            {
                markBronzemanItem(graphics, item);
            });
        }
        else {
            groundItemList.stream().forEach(item ->
            {
                markBronzemanItem(graphics, item);
            });
        }
        // End Item render
    }

    private void markBronzemanItem(Graphics2D graphics, GroundItem item)
    {
        final boolean onlyShowLoot = config.showBronzemanLoot();
        final Player player = client.getLocalPlayer();
        final LocalPoint localLocation = player.getLocalLocation();
        final LocalPoint groundPoint = LocalPoint.fromWorld(client, item.getLocation());

        if (groundPoint == null || localLocation.distanceTo(groundPoint) > MAX_DISTANCE
                || (onlyShowLoot && !item.isMine()))
        {
            return;
            //continue;
        }

        final Color highlighted = plugin.getHighlighted(new NamedQuantity(item), item.getGePrice(), item.getHaPrice());
        final Color hidden = plugin.getHidden(new NamedQuantity(item), item.getGePrice(), item.getHaPrice(), item.isTradeable());

//            if (highlighted == null && !plugin.isHotKeyPressed())
//            {
//                // Do not display hidden items
//                if (hidden != null)
//                {
//                    continue;
//                }
//
//                // Do not display non-highlighted items
//                if (config.showHighlightedOnly())
//                {
//                    continue;
//                }
//            }

        final Color color = plugin.getItemColor(highlighted, hidden);

////            if (config.highlightTiles())
//            {
//                final Polygon poly = Perspective.getCanvasTilePoly(client, groundPoint, item.getHeight());
//
//                if (poly != null)
//                {
//                    OverlayUtil.renderPolygon(graphics, poly, color);
//                }
//            }

//            if (dontShowOverlay)
//            {
//                continue;
//            }

        itemStringBuilder.append(item.getName());

        if (item.getQuantity() > 1)
        {
            if (item.getQuantity() >= MAX_QUANTITY)
            {
                itemStringBuilder.append(" (Lots!)");
            }
            else
            {
                itemStringBuilder.append(" (")
                        .append(QuantityFormatter.quantityToStackSize(item.getQuantity()))
                        .append(")");
            }
        }

        if (gconfig.priceDisplayMode() == PriceDisplayMode.BOTH)
        {
            if (item.getGePrice() > 0)
            {
                itemStringBuilder.append(" (GE: ")
                        .append(QuantityFormatter.quantityToStackSize(item.getGePrice()))
                        .append(" gp)");
            }

            if (item.getHaPrice() > 0)
            {
                itemStringBuilder.append(" (HA: ")
                        .append(QuantityFormatter.quantityToStackSize(item.getHaPrice()))
                        .append(" gp)");
            }
        }
        else if (gconfig.priceDisplayMode() != PriceDisplayMode.OFF)
        {
            final int price = gconfig.priceDisplayMode() == PriceDisplayMode.GE
                    ? item.getGePrice()
                    : item.getHaPrice();

            if (price > 0)
            {
                itemStringBuilder
                        .append(" (")
                        .append(QuantityFormatter.quantityToStackSize(price))
                        .append(" gp)");
            }
        }

        final String itemString = itemStringBuilder.toString();
        itemStringBuilder.setLength(0);

        final Point textPoint = Perspective.getCanvasTextLocation(client,
                graphics,
                groundPoint,
                itemString,
                item.getHeight() + OFFSET_Z);

        if (textPoint == null)
        {
            return;
            //continue;
        }

        final int offset = plugin.isHotKeyPressed()
                ? item.getOffset()
                : offsetMap.compute(item.getLocation(), (k, v) -> v != null ? v + 1 : 0);

//        final int offset = offsetMap.compute(item.getLocation(), (k, v) -> v != null ? v + 1 : 0);

        final int textX = textPoint.getX();
        final int textY = textPoint.getY() - (STRING_GAP * offset);

//            if (plugin.isHotKeyPressed())
//            {
//                final int stringWidth = fm.stringWidth(itemString);
//                final int stringHeight = fm.getHeight();
//
//                // Item bounds
//                int x = textX - 2;
//                int y = textY - stringHeight - 2;
//                int width = stringWidth + 4;
//                int height = stringHeight + 4;
//                final Rectangle itemBounds = new Rectangle(x, y, width, height);
//
//                // Hidden box
//                x += width + 2;
//                y = textY - (RECTANGLE_SIZE + stringHeight) / 2;
//                width = height = RECTANGLE_SIZE;
//                final Rectangle itemHiddenBox = new Rectangle(x, y, width, height);
//
//                // Highlight box
//                x += width + 2;
//                final Rectangle itemHighlightBox = new Rectangle(x, y, width, height);
//
//                boolean mouseInBox = itemBounds.contains(mousePos.getX(), mousePos.getY());
//                boolean mouseInHiddenBox = itemHiddenBox.contains(mousePos.getX(), mousePos.getY());
//                boolean mouseInHighlightBox = itemHighlightBox.contains(mousePos.getX(), mousePos.getY());
//
//                if (mouseInBox)
//                {
//                    plugin.setTextBoxBounds(new SimpleEntry<>(itemBounds, item));
//                }
//                else if (mouseInHiddenBox)
//                {
//                    plugin.setHiddenBoxBounds(new SimpleEntry<>(itemHiddenBox, item));
//
//                }
//                else if (mouseInHighlightBox)
//                {
//                    plugin.setHighlightBoxBounds(new SimpleEntry<>(itemHighlightBox, item));
//                }
//
//                boolean topItem = topGroundItem == item;
//
//                // Draw background if hovering
//                if (topItem && (mouseInBox || mouseInHiddenBox || mouseInHighlightBox))
//                {
//                    backgroundComponent.setRectangle(itemBounds);
//                    backgroundComponent.render(graphics);
//                }
//
//                // Draw hidden box
//                drawRectangle(graphics, itemHiddenBox, topItem && mouseInHiddenBox ? Color.RED : color, hidden != null, true);
//
//                // Draw highlight box
//                drawRectangle(graphics, itemHighlightBox, topItem && mouseInHighlightBox ? Color.GREEN : color, highlighted != null, false);
//            }

        // When the hotkey is pressed the hidden/highlight boxes are drawn to the right of the text,
        // so always draw the pie since it is on the left hand side.
//            if (groundItemTimers == DespawnTimerMode.PIE || plugin.isHotKeyPressed())
//            {
//                drawTimerPieOverlay(graphics, textX, textY, item);
//            }
//            else if (groundItemTimers == DespawnTimerMode.SECONDS || groundItemTimers == DespawnTimerMode.TICKS)
//            {
//                Instant despawnTime = calculateDespawnTime(item);
//                Color timerColor = getItemTimerColor(item);
//                if (despawnTime != null && timerColor != null)
//                {
//                    long despawnTimeMillis = despawnTime.toEpochMilli() - Instant.now().toEpochMilli();
//                    final String timerText;
//                    if (groundItemTimers == DespawnTimerMode.SECONDS)
//                    {
//                        timerText = String.format(" - %.1f", despawnTimeMillis / 1000f);
//                    }
//                    else // TICKS
//                    {
//                        timerText = String.format(" - %d", despawnTimeMillis / 600);
//                    }
//
//                    // The timer text is drawn separately to have its own color, and is intentionally not included
//                    // in the getCanvasTextLocation() call because the timer text can change per frame and we do not
//                    // use a monospaced font, which causes the text location on screen to jump around slightly each frame.
//                    textComponent.setText(timerText);
//                    textComponent.setColor(timerColor);
//                    textComponent.setOutline(outline);
//                    textComponent.setPosition(new java.awt.Point(textX + fm.stringWidth(itemString), textY));
//                    textComponent.render(graphics);
//                }
//            }

        textComponent.setText(itemString);
        textComponent.setColor(Color.orange);
        //textComponent.setOutline(outline);
        textComponent.setPosition(new java.awt.Point(textX, textY));
        textComponent.render(graphics);
    }

    /**
     * Saves a screenshot of the client window to the screenshot folder as a PNG,
     * and optionally uploads it to an image-hosting service.
     *
     * @param fileName    Filename to use, without file extension.
     */
    private void takeScreenshot(String fileName)
    {
        Consumer<Image> imageCallback = (img) ->
        {
            // This callback is on the game thread, move to executor thread
            executor.submit(() -> takeScreenshot(fileName, img));
        };

        drawManager.requestNextFrameListener(imageCallback);
    }

    private void takeScreenshot(String fileName, Image image)
    {
        BufferedImage screenshot = includeFrame
                ? new BufferedImage(clientUi.getWidth(), clientUi.getHeight(), BufferedImage.TYPE_INT_ARGB)
                : new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        Graphics graphics = screenshot.getGraphics();

        int gameOffsetX = 0;
        int gameOffsetY = 0;

        if (includeFrame)
        {
            // Draw the client frame onto the screenshot
            try
            {
                SwingUtilities.invokeAndWait(() -> clientUi.paint(graphics));
            }
            catch (InterruptedException | InvocationTargetException e)
            {
                log.warn("unable to paint client UI on screenshot", e);
            }

            // Evaluate the position of the game inside the frame
            final Point canvasOffset = clientUi.getCanvasOffset();
            gameOffsetX = canvasOffset.getX();
            gameOffsetY = canvasOffset.getY();
        }

        // Draw the game onto the screenshot
        graphics.drawImage(image, gameOffsetX, gameOffsetY, null);
        imageCapture.takeScreenshot(screenshot, fileName, "Item Unlocks", false, ImageUploadStyle.NEITHER);
    }
}
