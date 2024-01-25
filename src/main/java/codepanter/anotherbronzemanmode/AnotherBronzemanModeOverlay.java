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

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import java.awt.Point;
import net.runelite.client.game.ItemManager;
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
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnotherBronzemanModeOverlay extends Overlay
{

    private final Client client;
    private final AnotherBronzemanModePlugin plugin;

    private Integer currentUnlock;
    private long displayTime;
    private int displayY;

    private final List<Integer> itemUnlockList;
    private boolean screenshotUnlock;
    private boolean includeFrame;

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
    public AnotherBronzemanModeOverlay(Client client, AnotherBronzemanModePlugin plugin)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.itemUnlockList = new ArrayList<>();
        this.screenshotUnlock = false;
        this.includeFrame = false;
        setPosition(OverlayPosition.TOP_CENTER);
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
        if (client.getGameState() != GameState.LOGGED_IN || itemUnlockList.isEmpty())
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
            gameOffsetX = (int)canvasOffset.getX();
            gameOffsetY = (int)canvasOffset.getY();
        }

        // Draw the game onto the screenshot
        graphics.drawImage(image, gameOffsetX, gameOffsetY, null);
        imageCapture.takeScreenshot(screenshot, fileName, "Item Unlocks", false, ImageUploadStyle.NEITHER);
    }
}
