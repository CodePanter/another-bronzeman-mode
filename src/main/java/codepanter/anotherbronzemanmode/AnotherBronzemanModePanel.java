package codepanter.anotherbronzemanmode;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

@Singleton
@Slf4j
public class AnotherBronzemanModePanel extends PluginPanel
{
    private static final int COLUMN_SIZE = 5;
    private static final int ICON_WIDTH = 36;
    private static final int ICON_HEIGHT = 32;

    @Inject
    AnotherBronzemanModePlugin plugin;

    @Inject
    private ClientThread clientThread;

    JPanel itemsPanel;

    AnotherBronzemanModePanel()
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel selectionPanel = new JPanel();
        selectionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        selectionPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        selectionPanel.setLayout(new GridLayout(0, 2));

        // Show untradeable items
        JCheckBox showUntradeableItems = new JCheckBox();
        showUntradeableItems.setFocusable(false);
        showUntradeableItems.setText("Show untradeable items");
        showUntradeableItems.setSelected(true);
        selectionPanel.add(showUntradeableItems);

        // TO-DO: Add a search function

        // Button to search bank
        JButton filterButton = new JButton();
        filterButton.addActionListener((actionEvent) ->
        {
            clientThread.invokeLater(() -> plugin.unlockFilter(showUntradeableItems.isSelected()));
        });
        filterButton.setText("View Items");
        filterButton.setFocusable(false);
        selectionPanel.add(filterButton);

        add(selectionPanel, BorderLayout.NORTH);

        itemsPanel = new JPanel();
        itemsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        itemsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));

        add(itemsPanel);
    }

    public void displayItems(List<ItemObject> filteredItems)
    {
        itemsPanel.removeAll();

        if (!filteredItems.isEmpty())
        {
            //print item names
            JPanel titlePanel = new JPanel();
            titlePanel.setLayout(new BorderLayout());

            //Get title and unlock count
            JLabel titleLabel = new JLabel();
            titleLabel.setText("Bronzeman Unlocks: " + Integer.toString(filteredItems.size()));
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

            // Add to panel
            titlePanel.add(titleLabel, BorderLayout.CENTER);
            itemsPanel.add(titlePanel);

            JPanel itemContainer = new JPanel();
            itemContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
            itemContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
            itemContainer.setLayout(new GridLayout(0, COLUMN_SIZE, 1, 1));

            for (ItemObject item : filteredItems) {
                JPanel itemPanel = new JPanel();
                JLabel itemLabel = new JLabel();

                itemLabel.setHorizontalAlignment(SwingConstants.CENTER);
                itemLabel.setVerticalAlignment(SwingConstants.CENTER);
                item.getIcon().addTo(itemLabel);
                itemLabel.setSize(item.getIcon().getWidth(), item.getIcon().getHeight());
                itemLabel.setMaximumSize(new Dimension(ICON_WIDTH, ICON_HEIGHT));
                itemLabel.setToolTipText(item.getName());

                itemPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

                itemPanel.add(itemLabel);
                itemContainer.add(itemPanel);
            }
            if (filteredItems.size() % COLUMN_SIZE != 0)
            {
                for (int i = 0; i < COLUMN_SIZE - (filteredItems.size() % COLUMN_SIZE); i++)
                {
                    JPanel panel = new JPanel();
                    panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                    itemContainer.add(panel);
                }
            }

            itemsPanel.add(itemContainer);
        }
        else {
            displayMessage("No items found.");
        }

        repaint();
        revalidate();
    }

    public void displayMessage(final String message)
    {
        itemsPanel.removeAll();

        final JTextArea textArea = new JTextArea();
        textArea.setText(message);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusable(false);
        textArea.setEditable(false);
        textArea.setBackground(ColorScheme.DARK_GRAY_COLOR);
        itemsPanel.add(textArea);

        repaint();
        revalidate();
    }
}
