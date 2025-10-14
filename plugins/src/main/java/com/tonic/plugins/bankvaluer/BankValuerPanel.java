package com.tonic.plugins.bankvaluer;

import com.tonic.model.ui.components.FancyCard;
import com.tonic.services.BankCache;
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
		setLayout(new BorderLayout());

		FancyCard card = new FancyCard("Bank Valuer", "Top 10 most valuable items in your bank");
		add(card, BorderLayout.NORTH);

		itemContainer = new JPanel();
		itemContainer.setLayout(new BoxLayout(itemContainer, BoxLayout.Y_AXIS));
		itemContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		itemContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
		add(itemContainer, BorderLayout.CENTER);
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
			return Color.YELLOW;
		}
		else if(valueStr.endsWith("M"))
		{
			return ColorScheme.GRAND_EXCHANGE_PRICE;
		}
		else
		{
			return Color.WHITE;
		}
	}
}
