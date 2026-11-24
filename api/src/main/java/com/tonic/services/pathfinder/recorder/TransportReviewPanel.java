package com.tonic.services.pathfinder.recorder;

import com.tonic.Logger;
import com.tonic.services.pathfinder.transports.Transport;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The sidebar panel of the transport recorder: its switches, the staged captures, and promoting, deleting, timing, copying and editing them; every action works on the selected captures themselves, never on list positions.
 */
public final class TransportReviewPanel extends PluginPanel
{
    private final transient TransportRecorder recorder;
    private final transient TransportRecordStore store;
    private final transient Consumer<Transport> editTransport;
    private final transient Runnable openEditor;
    private final transient BooleanSupplier overlayShown;
    private final transient Consumer<Boolean> showOverlay;
    private final DefaultListModel<Transport> model = new DefaultListModel<>();
    private final JList<Transport> list = new JList<>(model);

    /**
     * @param recorder the recorder
     * @param editTransport opens the editor on one staged transport
     * @param openEditor opens the editor as it is
     * @param overlayShown whether the recorder overlay is switched on
     * @param showOverlay switches the recorder overlay on or off
     */
    public TransportReviewPanel(TransportRecorder recorder, Consumer<Transport> editTransport, Runnable openEditor, BooleanSupplier overlayShown, Consumer<Boolean> showOverlay)
    {
        super(false);
        this.recorder = recorder;
        this.store = recorder.store();
        this.editTransport = editTransport;
        this.openEditor = openEditor;
        this.overlayShown = overlayShown;
        this.showOverlay = showOverlay;
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildToggles(), BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        list.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        list.setCellRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> owner, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(owner, value, index, isSelected, cellHasFocus);
                if (value instanceof Transport)
                {
                    label.setText(describe((Transport) value));
                }
                return label;
            }
        });

        JPopupMenu popup = new JPopupMenu();
        JMenuItem setDuration = new JMenuItem("Set duration...");
        setDuration.addActionListener(e -> setDurationForSelected());
        popup.add(setDuration);
        popup.addSeparator();
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> deleteSelected());
        popup.add(deleteItem);

        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteTransport");
        list.getActionMap().put("deleteTransport", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                deleteSelected();
            }
        });
        list.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                {
                    editRow(list.locationToIndex(e.getPoint()));
                }
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e)
            {
                if (!e.isPopupTrigger())
                {
                    return;
                }

                int index = list.locationToIndex(e.getPoint());
                if (index >= 0 && !list.isSelectedIndex(index))
                {
                    list.setSelectedIndex(index);
                }
                if (!list.isSelectionEmpty())
                {
                    popup.show(list, e.getX(), e.getY());
                }
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        add(scroll, BorderLayout.CENTER);
        add(buildToolbar(), BorderLayout.SOUTH);

        refresh();
    }

    /**
     * Rebuilds the list from the staging store; call on the event dispatch thread.
     */
    public void refresh()
    {
        Set<Transport> kept = identitySet(list.getSelectedValuesList());
        model.clear();
        List<Integer> reselect = new ArrayList<>();
        for (Transport transport : store.getRecords())
        {
            if (kept.contains(transport))
            {
                reselect.add(model.size());
            }
            model.addElement(transport);
        }
        list.setSelectedIndices(reselect.stream().mapToInt(Integer::intValue).toArray());
    }

    @Override
    public void onActivate()
    {
        refresh();
    }

    private JPanel buildToggles()
    {
        JPanel bar = new JPanel(new GridLayout(0, 2, 4, 4));
        bar.setOpaque(false);
        JToggleButton record = button(new JToggleButton("Record", recorder.recording()));
        record.addActionListener(e -> recorder.recording(record.isSelected()));
        bar.add(record);
        JToggleButton tiles = button(new JToggleButton("Show tiles", recorder.showTiles()));
        tiles.addActionListener(e -> recorder.showTiles(tiles.isSelected()));
        bar.add(tiles);
        JToggleButton members = button(new JToggleButton("Members only", recorder.membersOnly()));
        members.addActionListener(e -> recorder.membersOnly(members.isSelected()));
        bar.add(members);
        JToggleButton overlay = button(new JToggleButton("Overlay", overlayShown.getAsBoolean()));
        overlay.setToolTipText("Draw what the recorder is doing over the game");
        overlay.addActionListener(e -> showOverlay.accept(overlay.isSelected()));
        bar.add(overlay);
        return bar;
    }

    private JPanel buildToolbar()
    {
        JPanel bar = new JPanel(new GridLayout(0, 2, 4, 4));
        bar.setOpaque(false);
        JButton refresh = button(new JButton("Refresh"));
        refresh.addActionListener(e -> refresh());
        bar.add(refresh);
        JButton promote = button(new JButton("Promote"));
        promote.addActionListener(e -> promoteSelected());
        bar.add(promote);
        JButton delete = button(new JButton("Delete"));
        delete.addActionListener(e -> deleteSelected());
        bar.add(delete);
        JButton copy = button(new JButton("Copy JSON"));
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(store.toJson()), null));
        bar.add(copy);
        JButton editor = button(new JButton("Editor"));
        editor.addActionListener(e -> openEditor.run());
        bar.add(editor);
        return bar;
    }

    private static <T extends AbstractButton> T button(T button)
    {
        button.setFocusable(false);
        return button;
    }

    private void promoteSelected()
    {
        List<Transport> selected = stillStaged(list.getSelectedValuesList());
        if (selected.isEmpty())
        {
            refresh();
            return;
        }

        int promoted = recorder.promote(selected);
        store.removeExact(selected);
        refresh();
        Logger.info("[TransportRecorder] Promoted " + promoted + " of " + selected.size() + " to the live transports");
    }

    private void deleteSelected()
    {
        List<Transport> selected = list.getSelectedValuesList();
        if (selected.isEmpty())
        {
            return;
        }

        store.removeExact(selected);
        refresh();
    }

    private void setDurationForSelected()
    {
        List<Transport> selected = list.getSelectedValuesList();
        if (selected.isEmpty())
        {
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Duration (ticks) for " + selected.size() + " selected transport(s):", "Set Duration", JOptionPane.PLAIN_MESSAGE);
        if (input == null)
        {
            return;
        }

        int duration;
        try
        {
            duration = Math.max(0, Integer.parseInt(input.trim()));
        }
        catch (NumberFormatException ex)
        {
            return;
        }

        for (Transport transport : selected)
        {
            store.replaceExact(transport, transport.withDuration(duration));
        }
        refresh();
    }

    private void editRow(int index)
    {
        if (index < 0 || index >= model.size())
        {
            return;
        }

        editTransport.accept(model.get(index));
    }

    private List<Transport> stillStaged(List<Transport> selected)
    {
        Set<Transport> staged = identitySet(store.getRecords());
        List<Transport> present = new ArrayList<>();
        for (Transport transport : selected)
        {
            if (staged.contains(transport))
            {
                present.add(transport);
            }
        }
        return present;
    }

    private static Set<Transport> identitySet(List<Transport> transports)
    {
        Set<Transport> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(transports);
        return set;
    }

    private static String describe(Transport transport)
    {
        String dialogue = transport.getDialogueOptions() != null && transport.getDialogueOptions().length > 0 ? "  diag=" + Arrays.toString(transport.getDialogueOptions()) : "";
        return transport.getName() + "  [" + (transport.isObject() ? "obj" : "npc") + " id" + transport.getId() + " opt" + transport.getOption() + "]  " + transport.getLocation().toWorldPoint() + " -> " + transport.getDestination().toWorldPoint() + "  dur" + transport.getDuration() + dialogue;
    }
}
