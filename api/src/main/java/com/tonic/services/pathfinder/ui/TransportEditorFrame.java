package com.tonic.services.pathfinder.ui;

import com.tonic.Static;
import com.tonic.model.ui.components.VitaFrame;
import com.tonic.services.pathfinder.recorder.TransportRecordStore;
import com.tonic.services.pathfinder.recorder.TransportTools;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportJson;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The transport editor window: every live and staged transport in a searchable table beside a detail form, edits kept as drafts until Apply and written on Save, live rows through the loader and staged or new rows into the staging store.
 */
public final class TransportEditorFrame extends VitaFrame
{
    private static final String[] ORIGINS = {"All", "Live", "Staged"};
    private static final String TITLE = "Transport Editor";
    private static final String ICON = "/com/tonic/vitalite/icon.png";

    private static volatile TransportEditorFrame instance;

    private final transient TransportRecordStore store;
    private final transient TransportEditorSettings settings;
    private final TransportTableModel model = new TransportTableModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<TransportTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField search = new JTextField(18);
    private final JComboBox<String> origin = new JComboBox<>(ORIGINS);
    private final JLabel status = new JLabel(" ");
    private final TransportDetailPanel detail;

    /**
     * @param store the staging store staged and new rows are saved to
     * @param fulfilled whether a transport's requirements held on the last tick they were worked out
     * @param settings the editor's run-to-run settings
     */
    public TransportEditorFrame(TransportRecordStore store, Function<Transport, Optional<Boolean>> fulfilled, TransportEditorSettings settings)
    {
        super(TITLE);
        this.store = store;
        this.settings = settings;
        this.detail = new TransportDetailPanel(new RequirementDescriptions(), this::applyToSelected, this::revertSelected);
        setIconImage(ImageUtil.loadImageResource(TransportEditorFrame.class, ICON));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                close();
            }
        });
        model.fulfilled(fulfilled);

        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(TransportTableModel.DIRTY_COLUMN).setMaxWidth(24);
        table.getColumnModel().getColumn(TransportTableModel.ORIGIN_COLUMN).setMaxWidth(60);
        table.getColumnModel().getColumn(TransportTableModel.TYPE_COLUMN).setMaxWidth(40);
        table.getColumnModel().getColumn(TransportTableModel.OPTION_COLUMN).setMaxWidth(40);
        table.getColumnModel().getColumn(TransportTableModel.DURATION_COLUMN).setMaxWidth(40);
        table.getColumnModel().getColumn(TransportTableModel.REQUIREMENTS_COLUMN).setMaxWidth(44);
        table.getColumnModel().getColumn(TransportTableModel.MET_COLUMN).setMaxWidth(40);
        table.getSelectionModel().addListSelectionListener(e ->
        {
            if (!e.getValueIsAdjusting())
            {
                detail.show(selectedRow());
            }
        });
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                filter();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                filter();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                filter();
            }
        });
        origin.setSelectedItem(settings.originFilter());
        origin.addActionListener(e ->
        {
            settings.originFilter((String) origin.getSelectedItem());
            filter();
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        filters.add(new JLabel("Search"));
        filters.add(search);
        filters.add(new JLabel("Show"));
        filters.add(origin);
        JPanel left = new JPanel(new BorderLayout());
        left.add(filters, BorderLayout.NORTH);
        left.add(new JScrollPane(table), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, detail);
        split.setDividerLocation(settings.divider());
        split.setResizeWeight(0.5);
        split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> settings.divider(split.getDividerLocation()));
        getContentPanel().add(split, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        getContentPanel().add(status, BorderLayout.SOUTH);
        setJMenuBar(menuBar());
        setAlwaysOnTop(settings.alwaysOnTop());
        setPreferredSize(new Dimension(1180, 640));
        pack();
        reload();
        filter();
    }

    /**
     * Shows the editor, or hides it after asking about unsaved rows; any thread.
     */
    public static void toggle()
    {
        SwingUtilities.invokeLater(() ->
        {
            TransportEditorFrame frame = instance();
            if (frame.isVisible())
            {
                frame.close();
            }
            else
            {
                frame.reveal();
            }
        });
    }

    /**
     * Shows the editor, listing anything staged since it was loaded; any thread.
     */
    public static void open()
    {
        SwingUtilities.invokeLater(() -> instance().reveal());
    }

    /**
     * Shows the editor on the row of one transport; any thread.
     *
     * @param transport the transport
     */
    public static void open(Transport transport)
    {
        SwingUtilities.invokeLater(() ->
        {
            TransportEditorFrame frame = instance();
            frame.reveal();
            frame.select(transport);
        });
    }

    /**
     * @return whether the editor window is showing
     */
    public static boolean showing()
    {
        TransportEditorFrame frame = instance;
        return frame != null && frame.isVisible();
    }

    private static TransportEditorFrame instance()
    {
        if (instance == null)
        {
            TransportEditorFrame frame = new TransportEditorFrame(TransportTools.store(), TransportTools::fulfilled, TransportTools.settings());
            frame.setLocationByPlatform(true);
            instance = frame;
        }
        return instance;
    }

    private void reveal()
    {
        setVisible(true);
        toFront();
        syncStaged();
    }

    @Override
    protected void closeRequested()
    {
        close();
    }

    private void close()
    {
        if (confirmDiscard("closing"))
        {
            setVisible(false);
        }
    }

    private void reload()
    {
        if (model.isDirty() && !confirmDiscard("reloading"))
        {
            return;
        }

        model.load(TransportLoader.live(), store.getRecords());
        detail.show(null);
        updateTitle();
        status.setText(model.getRowCount() + " transports" + (TransportLoader.sourceEditable() ? "" : "; source file not on disk, edits reach the running walker only"));
    }

    private void syncStaged()
    {
        int added = model.syncStaged(store.getRecords());
        if (added > 0)
        {
            status.setText(model.getRowCount() + " transports, " + added + " newly staged");
        }
    }

    private void select(Transport transport)
    {
        syncStaged();
        TransportRow row = model.rowOf(transport);
        if (row == null)
        {
            return;
        }

        int viewIndex = table.convertRowIndexToView(model.indexOf(row));
        if (viewIndex < 0)
        {
            search.setText("");
            origin.setSelectedItem("All");
            viewIndex = table.convertRowIndexToView(model.indexOf(row));
        }
        if (viewIndex >= 0)
        {
            table.setRowSelectionInterval(viewIndex, viewIndex);
            table.scrollRectToVisible(table.getCellRect(viewIndex, 0, true));
        }
    }

    private void addAt(WorldPoint tile)
    {
        WorldPoint at = tile != null ? tile : playerTile();
        TransportRow row = model.add(TransportDraft.at(at).build());
        selectRow(row);
        updateTitle();
    }

    private static WorldPoint playerTile()
    {
        Client client = Static.getClient();
        if (client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }

        return Static.invoke(() ->
        {
            Player local = client.getLocalPlayer();
            return local == null ? null : WorldPointUtil.fromInstance(local.getWorldLocation());
        });
    }

    private JMenuBar menuBar()
    {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add(item("Save", KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), this::save));
        file.add(item("Reload", null, this::reload));
        file.add(item("Revert all", null, this::revertAll));
        bar.add(file);
        JMenu transport = new JMenu("Transport");
        transport.add(item("Add", KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), () -> addAt(null)));
        transport.add(item("Duplicate", KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK), this::duplicateSelected));
        transport.add(item("Delete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), this::deleteSelected));
        transport.add(item("Promote staged", null, this::promoteSelected));
        bar.add(transport);
        bar.add(Box.createHorizontalGlue());
        JCheckBoxMenuItem onTop = new JCheckBoxMenuItem("Always on top", settings.alwaysOnTop());
        onTop.addActionListener(e ->
        {
            settings.alwaysOnTop(onTop.isSelected());
            setAlwaysOnTop(onTop.isSelected());
        });
        bar.add(onTop);
        return bar;
    }

    private static JMenuItem item(String text, KeyStroke key, Runnable action)
    {
        JMenuItem item = new JMenuItem(text);
        if (key != null)
        {
            item.setAccelerator(key);
        }
        item.addActionListener(e -> action.run());
        return item;
    }

    private void filter()
    {
        String text = search.getText();
        String which = (String) origin.getSelectedItem();
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TransportTableModel, ? extends Integer> entry) {
                TransportRow row = entry.getModel().row(entry.getIdentifier());
                if ("Live".equals(which) && (row.isNew() || row.origin() != TransportRow.Origin.LIVE)) {
                    return false;
                }
                if ("Staged".equals(which) && !row.isNew() && row.origin() != TransportRow.Origin.STAGED) {
                    return false;
                }
                return TransportTableModel.matches(row, text);
            }
        });
    }

    private TransportRow selectedRow()
    {
        int viewIndex = table.getSelectedRow();
        return viewIndex < 0 ? null : model.row(table.convertRowIndexToModel(viewIndex));
    }

    private void selectRow(TransportRow row)
    {
        int viewIndex = table.convertRowIndexToView(model.indexOf(row));
        if (viewIndex >= 0)
        {
            table.setRowSelectionInterval(viewIndex, viewIndex);
        }
    }

    private void applyToSelected(Transport transport)
    {
        TransportRow row = detail.row();
        if (row == null)
        {
            return;
        }

        model.apply(row, transport);
        detail.show(row);
        updateTitle();
        status.setText("Applied; press Ctrl+S to save");
    }

    private void revertSelected()
    {
        TransportRow row = detail.row();
        if (row == null)
        {
            return;
        }

        model.revert(row);
        detail.show(model.indexOf(row) >= 0 ? row : null);
        updateTitle();
    }

    private void revertAll()
    {
        for (TransportRow row : model.dirtyRows())
        {
            model.revert(row);
        }
        detail.show(selectedRow());
        updateTitle();
    }

    private void duplicateSelected()
    {
        TransportRow row = selectedRow();
        if (row == null || row.current() == null)
        {
            return;
        }

        Transport copy = TransportJson.read(TransportJson.write(Collections.singletonList(row.current()))).get(0);
        selectRow(model.add(copy));
        updateTitle();
    }

    private void deleteSelected()
    {
        TransportRow row = selectedRow();
        if (row == null)
        {
            return;
        }

        model.delete(row);
        detail.show(model.indexOf(row) >= 0 ? row : null);
        updateTitle();
    }

    private void promoteSelected()
    {
        TransportRow row = selectedRow();
        if (row == null || row.isDirty() || row.origin() != TransportRow.Origin.STAGED || row.isNew())
        {
            status.setText("Promote applies to a saved staged row; save first.");
            return;
        }

        Transport transport = row.saved();
        TransportLoader.promote(Collections.singletonList(transport));
        store.remove(transport);
        model.saved(row, TransportRow.Origin.LIVE, transport);
        status.setText("Promoted " + transport.getName() + "; the walker uses it now");
    }

    private void save()
    {
        List<TransportRow> dirty = model.dirtyRows();
        if (dirty.isEmpty())
        {
            status.setText("Nothing to save");
            return;
        }

        int stagedWrites = 0;
        int liveWrites = 0;
        for (TransportRow row : dirty)
        {
            if (row.isNew())
            {
                Transport draft = row.draft();
                store.add(draft);
                model.saved(row, TransportRow.Origin.STAGED, draft);
                stagedWrites++;
            }
            else if (row.origin() == TransportRow.Origin.STAGED)
            {
                if (row.isDeleted())
                {
                    store.remove(row.saved());
                    model.removed(row);
                }
                else
                {
                    if (!store.replace(row.saved(), row.draft()))
                    {
                        store.add(row.draft());
                    }
                    model.saved(row, TransportRow.Origin.STAGED, row.draft());
                }
                stagedWrites++;
            }
            else
            {
                if (row.isDeleted())
                {
                    TransportLoader.remove(row.saved());
                    model.removed(row);
                }
                else
                {
                    Transport updated = row.draft();
                    TransportLoader.replace(row.saved(), updated);
                    model.saved(row, TransportRow.Origin.LIVE, updated);
                }
                liveWrites++;
            }
        }
        updateTitle();
        status.setText("Saved " + stagedWrites + " staged and " + liveWrites + " live transport(s)" + (TransportLoader.sourceEditable() ? " to " + TransportLoader.sourcePath() : "; no source file on disk"));
    }

    private boolean confirmDiscard(String action)
    {
        if (!model.isDirty())
        {
            return true;
        }

        int choice = JOptionPane.showConfirmDialog(this, "There are unsaved transports. Save before " + action + "?", TITLE, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION)
        {
            save();
            return true;
        }
        return choice == JOptionPane.NO_OPTION;
    }

    private void updateTitle()
    {
        setTitle(TITLE + (model.isDirty() ? " *" : ""));
    }
}
