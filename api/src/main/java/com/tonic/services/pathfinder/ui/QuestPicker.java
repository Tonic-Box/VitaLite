package com.tonic.services.pathfinder.ui;

import net.runelite.api.Quest;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Locale;

/**
 * A quest chooser: a filter field over the quest list, matching display names and constant names.
 */
final class QuestPicker extends JPanel
{
    private final JTextField filter = new JTextField();
    private final DefaultListModel<Quest> model = new DefaultListModel<>();
    private final JList<Quest> list = new JList<>(model);

    QuestPicker(Quest selected)
    {
        super(new BorderLayout(0, 4));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(10);
        list.setCellRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> owner, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(owner, value, index, isSelected, cellHasFocus);
                if (value instanceof Quest)
                {
                    Quest quest = (Quest) value;
                    label.setText(quest.getName() + "  (" + quest.name() + ")");
                }
                return label;
            }
        });
        filter.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                refill();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                refill();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                refill();
            }
        });
        refill();
        if (selected != null)
        {
            list.setSelectedValue(selected, true);
        }

        add(filter, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(420, 200));
        add(scroll, BorderLayout.CENTER);
    }

    Quest selected()
    {
        return list.getSelectedValue();
    }

    JComponent filterField()
    {
        return filter;
    }

    private void refill()
    {
        Quest keep = list.getSelectedValue();
        String needle = filter.getText().trim().toLowerCase(Locale.ROOT);
        model.clear();
        for (Quest quest : Quest.values())
        {
            if (needle.isEmpty() || quest.getName().toLowerCase(Locale.ROOT).contains(needle) || quest.name().toLowerCase(Locale.ROOT).contains(needle))
            {
                model.addElement(quest);
            }
        }
        if (keep != null && model.contains(keep))
        {
            list.setSelectedValue(keep, true);
        }
    }
}
