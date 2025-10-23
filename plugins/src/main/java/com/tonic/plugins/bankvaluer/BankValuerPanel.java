package com.tonic.plugins.bankvaluer;

import com.tonic.model.ui.components.FancyCard;
import com.tonic.services.BankCache;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;

public class BankValuerPanel extends PluginPanel
{
        private static final int DEFAULT_ITEM_LIMIT = 10;
        private static final int MIN_ITEM_LIMIT = 1;
        private static final int MAX_ITEM_LIMIT = 50;
        private final FancyCard card;
        private final JPanel itemContainer;
        private final JSpinner itemLimitSpinner;
        private final JCheckBox hideUntradeablesCheckBox;
        private final JButton refreshButton;

        @Inject
        public BankValuerPanel()
        {
                setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                setBackground(ColorScheme.DARK_GRAY_COLOR);
                setLayout(new BorderLayout());

                card = new FancyCard("Bank Valuer", buildSubtitleText(DEFAULT_ITEM_LIMIT));
                add(card, BorderLayout.NORTH);

                SpinnerNumberModel spinnerModel = new SpinnerNumberModel(DEFAULT_ITEM_LIMIT, MIN_ITEM_LIMIT, MAX_ITEM_LIMIT, 1);
                itemLimitSpinner = new JSpinner(spinnerModel);
                itemLimitSpinner.setPreferredSize(new Dimension(60, 26));
                itemLimitSpinner.setMaximumSize(new Dimension(60, 26));
                itemLimitSpinner.setFont(FontManager.getRunescapeFont());
                itemLimitSpinner.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR));

                JComponent editor = itemLimitSpinner.getEditor();
                if (editor instanceof JSpinner.DefaultEditor)
                {
                        JSpinner.DefaultEditor defaultEditor = (JSpinner.DefaultEditor) editor;
                        defaultEditor.getTextField().setColumns(2);
                        defaultEditor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
                        defaultEditor.getTextField().setBackground(ColorScheme.DARKER_GRAY_COLOR);
                        defaultEditor.getTextField().setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                        defaultEditor.getTextField().setFont(FontManager.getRunescapeFont());
                }

                JPanel controlsPanel = new JPanel();
                controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
                controlsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                controlsPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

                JLabel limitLabel = new JLabel("Items to show:");
                limitLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                limitLabel.setFont(FontManager.getRunescapeFont());
                controlsPanel.add(limitLabel);
                controlsPanel.add(Box.createHorizontalStrut(8));
                controlsPanel.add(itemLimitSpinner);
                refreshButton = new JButton("Refresh");
                refreshButton.setFont(FontManager.getRunescapeFont());
                refreshButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                refreshButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                refreshButton.setFocusPainted(false);
                refreshButton.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_COLOR));

                controlsPanel.add(Box.createHorizontalStrut(8));
                controlsPanel.add(refreshButton);
                controlsPanel.add(Box.createHorizontalGlue());

                JPanel contentPanel = new JPanel();
                contentPanel.setLayout(new BorderLayout());
                contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                contentPanel.add(controlsPanel, BorderLayout.NORTH);

                hideUntradeablesCheckBox = new JCheckBox("Hide untradeables");
                hideUntradeablesCheckBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
                hideUntradeablesCheckBox.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                hideUntradeablesCheckBox.setFont(FontManager.getRunescapeFont());
                hideUntradeablesCheckBox.setFocusPainted(false);

                JPanel filtersPanel = new JPanel();
                filtersPanel.setLayout(new BorderLayout());
                filtersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                filtersPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
                filtersPanel.add(hideUntradeablesCheckBox, BorderLayout.WEST);

                itemContainer = new JPanel();
                itemContainer.setLayout(new BoxLayout(itemContainer, BoxLayout.Y_AXIS));
                itemContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
                itemContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
                JPanel itemsWrapper = new JPanel(new BorderLayout());
                itemsWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
                itemsWrapper.add(itemContainer, BorderLayout.NORTH);
                JPanel centerContent = new JPanel(new BorderLayout());
                centerContent.setBackground(ColorScheme.DARK_GRAY_COLOR);
                centerContent.add(filtersPanel, BorderLayout.NORTH);
                centerContent.add(itemsWrapper, BorderLayout.CENTER);

                contentPanel.add(centerContent, BorderLayout.CENTER);

                add(contentPanel, BorderLayout.CENTER);

                refreshButton.addActionListener(e -> refresh());

                card.setTaglineText(buildSubtitle());
        }

        public void reset()
        {
                SwingUtilities.invokeLater(() -> {
			itemContainer.removeAll();
			itemContainer.revalidate();
			itemContainer.repaint();
		});
	}

        public void refresh()
        {
                SwingUtilities.invokeLater(() -> {
                        itemContainer.removeAll();

                        card.setTaglineText(buildSubtitle());

                        int limit = getSelectedLimit();
                        boolean hideUntradeables = hideUntradeablesCheckBox.isSelected();
                        Map<Integer, Long> topItems = BankValuerUtils.getTopItems(limit, hideUntradeables);

                        if (topItems.isEmpty())
                        {
                                JLabel emptyLabel = new JLabel("No bank items to display");
                                emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                                emptyLabel.setFont(FontManager.getRunescapeSmallFont());
                                emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
                                emptyLabel.setBorder(new EmptyBorder(20, 0, 0, 0));
                                itemContainer.add(emptyLabel);
                        }
                        else
                        {
                                final int[] rank = {1};
                                topItems.entrySet().stream()
                                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                                        .forEach(entry -> {
                                                int quantity = BankCache.cachedBankCount(entry.getKey());
                                                JPanel itemRow = createItemRow(rank[0], entry.getKey(), quantity, entry.getValue());
                                                itemContainer.add(itemRow);
                                                itemContainer.add(Box.createRigidArea(new Dimension(0, 5)));
                                                rank[0]++;
                                        });
                        }

                        itemContainer.revalidate();
                        itemContainer.repaint();
                });
        }

        private String buildSubtitle()
        {
                return buildSubtitleText(getSelectedLimit());
        }

        private static String buildSubtitleText(int limit)
        {
                return String.format("Top %d most valuable items in your bank", limit);
        }

        private int getSelectedLimit()
        {
                return ((Number) itemLimitSpinner.getValue()).intValue();
        }

	private JPanel createItemRow(int rank, int itemId, int quantity, long value)
	{
		JPanel wrapper = new JPanel(new BorderLayout(5, 0));
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

		JPanel row = new JPanel(new BorderLayout(10, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(8, 10, 8, 10));

		JLabel iconLabel = new JLabel();
		iconLabel.setVerticalAlignment(SwingConstants.CENTER);
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
		BankValuerUtils.getItemImage(iconLabel, itemId, quantity);
		row.add(iconLabel, BorderLayout.WEST);

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel nameLabel = new JLabel(BankValuerUtils.getName(itemId));
		nameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		nameLabel.setFont(FontManager.getRunescapeBoldFont());
		nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel valueLabel = new JLabel(QuantityFormatter.quantityToStackSize(value));
		valueLabel.setForeground(getColor(value));
		valueLabel.setFont(FontManager.getRunescapeFont());
		valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		centerPanel.add(nameLabel);
		centerPanel.add(Box.createVerticalStrut(2));
		centerPanel.add(valueLabel);

		row.add(centerPanel, BorderLayout.CENTER);

		wrapper.add(row, BorderLayout.CENTER);

		return wrapper;
	}

	public Color getColor(long value)
	{
		String valueStr = QuantityFormatter.quantityToStackSize(value);
		if(valueStr.endsWith("K"))
		{
			return Color.WHITE;
		}
		else if(valueStr.endsWith("M"))
		{
			return ColorScheme.GRAND_EXCHANGE_PRICE;
		}
		else
		{
			return Color.YELLOW;
		}
	}
}
