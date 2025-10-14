package com.tonic.plugins.bankvaluer;

import com.tonic.model.ui.components.FancyCard;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;

public class BankValuerPanel extends PluginPanel
{
	private final JPanel itemContainer;

	public BankValuerPanel()
	{
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.insets = new Insets(0, 0, 10, 0);

		FancyCard card = new FancyCard("Bank Valuer", "Top 10 most valuable items in your bank");
		add(card, c);
		c.gridy++;

		itemContainer = new JPanel();
		itemContainer.setLayout(new BoxLayout(itemContainer, BoxLayout.Y_AXIS));
		itemContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(itemContainer, c);
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

			Map<Integer, Long> topItems = BankValuerUtils.getTopTenItems();

			if (topItems.isEmpty())
			{
				JLabel emptyLabel = new JLabel("No bank data available");
				emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				emptyLabel.setFont(FontManager.getRunescapeSmallFont());
				emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
				emptyLabel.setBorder(new EmptyBorder(20, 0, 0, 0));
				itemContainer.add(emptyLabel);
			}
			else
			{
				topItems.entrySet().stream()
					.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
					.forEach(entry -> {
						JPanel itemRow = createItemRow(entry.getKey(), entry.getValue());
						itemContainer.add(itemRow);
						itemContainer.add(Box.createRigidArea(new Dimension(0, 5)));
					});
			}

			itemContainer.revalidate();
			itemContainer.repaint();
		});
	}

	private JPanel createItemRow(int itemId, long value)
	{
		JPanel row = new JPanel(new BorderLayout(10, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(8, 10, 8, 10));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

		JLabel iconLabel = new JLabel();
		iconLabel.setVerticalAlignment(SwingConstants.CENTER);
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
		BankValuerUtils.getItemImage(iconLabel, itemId, 1);
		row.add(iconLabel, BorderLayout.WEST);

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel nameLabel = new JLabel(BankValuerUtils.getName(itemId));
		nameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		nameLabel.setFont(FontManager.getRunescapeFont());
		nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel valueLabel = new JLabel(QuantityFormatter.quantityToStackSize(value));
		valueLabel.setForeground(getColor(value));
		valueLabel.setFont(FontManager.getRunescapeSmallFont());
		valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		centerPanel.add(nameLabel);
		centerPanel.add(Box.createVerticalStrut(2));
		centerPanel.add(valueLabel);

		row.add(centerPanel, BorderLayout.CENTER);

		return row;
	}

	public Color getColor(long value)
	{
		if(value < 100_000)
		{
			return Color.WHITE;
		}
		else if(value < 1_000_000)
		{
			return Color.YELLOW;
		}
		else if(value < 10_000_000)
		{
			return ColorScheme.GRAND_EXCHANGE_PRICE;
		}
		else
		{
			return Color.RED;
		}
	}
}
