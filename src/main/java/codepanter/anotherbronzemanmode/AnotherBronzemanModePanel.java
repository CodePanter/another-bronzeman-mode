package codepanter.anotherbronzemanmode;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.SwingUtil;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.game.chatbox.ChatboxPanelManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
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

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private ChatboxPanelManager chatboxPanelManager;

    JPanel itemsPanel;

    AnotherBronzemanModePanel()
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel selectionPanel = new JPanel();
        selectionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        selectionPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        selectionPanel.setLayout(new GridLayout(0, 1));

        // Sort label
        JLabel sortLabel = new JLabel();
        sortLabel.setText("Sort order: ");
        selectionPanel.add(sortLabel);

        // Sort dropdown field
        JComboBox sortDropDown = new JComboBox(SortOption.values());
        sortDropDown.setFocusable(false);
        sortDropDown.setRenderer(new SortOptionDropdownRenderer());
        selectionPanel.add(sortDropDown);

        // Search label
        JLabel searchLabel = new JLabel();
        searchLabel.setText("Search filter: ");
        selectionPanel.add(searchLabel);

        // Search text field
        IconTextField searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        selectionPanel.add(searchBar);

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
            clientThread.invokeLater(() -> plugin.unlockFilter(showUntradeableItems.isSelected(), (SortOption) sortDropDown.getSelectedItem(), searchBar.getText()));
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
        SwingUtilities.invokeLater((() -> {
            SwingUtil.fastRemoveAll(itemsPanel);

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
                EmptyBorder itemBorder = new EmptyBorder(10, 10, 10, 10);

                itemContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
                itemContainer.setBorder(itemBorder);
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

                    // Create popup menu
                    final JPopupMenu popupMenu = new JPopupMenu();
                    popupMenu.setBorder(itemBorder);
                    itemPanel.setComponentPopupMenu(popupMenu);

                    final JMenuItem inspectButton = new JMenuItem("Inspect " + item.getName());
                    inspectButton.addActionListener(e ->
                    {
                        final ChatMessageBuilder examination = new ChatMessageBuilder()
                        .append(ChatColorType.NORMAL)
                        .append("This is an unlocked item called '" + item.getName() + "'.");

                        chatMessageManager.queue(QueuedMessage.builder()
                                .type(ChatMessageType.ITEM_EXAMINE)
                                .runeLiteFormattedMessage(examination.build())
                                .build());
                    });
                    popupMenu.add(inspectButton);

                    final JMenuItem deleteButton = new JMenuItem("Remove " + item.getName());
                    deleteButton.addActionListener(e ->
                    {
                        if (plugin.isDeletionConfirmed("Do you want to re-lock: " + item.getName(), "Warning"))
                        {
                            plugin.queueItemDelete(item.getId());
                            plugin.sendChatMessage("Item '" + item.getName() + "' is no longer unlocked.");
                            displayItems(new ArrayList<ItemObject>()); // Redraw the panel
                        }
                    });
                    popupMenu.add(deleteButton);

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
        }));
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

    private static class SortOptionDropdownRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            value = ((SortOption) value).getDisplayName();
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }
}
