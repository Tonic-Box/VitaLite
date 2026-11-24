package com.tonic.services.pathfinder.ui;

import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportJson;
import net.runelite.api.coords.WorldPoint;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * The editor's table of live and staged transports with the edits waiting to be saved; rows are addressed by object, never by index, so sorting and filtering cannot point an edit at the wrong transport. Event dispatch thread only.
 */
final class TransportTableModel extends AbstractTableModel
{
    static final int DIRTY_COLUMN = 0;
    static final int ORIGIN_COLUMN = 1;
    static final int NAME_COLUMN = 2;
    static final int TYPE_COLUMN = 3;
    static final int ID_COLUMN = 4;
    static final int OPTION_COLUMN = 5;
    static final int SOURCE_COLUMN = 6;
    static final int DESTINATION_COLUMN = 7;
    static final int DURATION_COLUMN = 8;
    static final int REQUIREMENTS_COLUMN = 9;
    static final int MET_COLUMN = 10;
    private static final String[] COLUMNS = {"*", "Origin", "Name", "Type", "Id", "Opt", "Source", "Destination", "Dur", "Reqs", "Met"};

    private final List<TransportRow> rows = new ArrayList<>();
    private transient Function<Transport, Optional<Boolean>> fulfilled = transport -> Optional.empty();

    void fulfilled(Function<Transport, Optional<Boolean>> fulfilled)
    {
        this.fulfilled = fulfilled;
    }

    void load(List<Transport> live, List<Transport> staged)
    {
        rows.clear();
        for (Transport transport : live)
        {
            rows.add(new TransportRow(TransportRow.Origin.LIVE, transport));
        }
        for (Transport transport : staged)
        {
            rows.add(new TransportRow(TransportRow.Origin.STAGED, transport));
        }
        fireTableDataChanged();
    }

    TransportRow row(int index)
    {
        return rows.get(index);
    }

    int indexOf(TransportRow row)
    {
        for (int i = 0; i < rows.size(); i++)
        {
            if (rows.get(i) == row)
            {
                return i;
            }
        }
        return -1;
    }

    TransportRow rowOf(Transport transport)
    {
        for (TransportRow row : rows)
        {
            if (row.saved() == transport || row.draft() == transport)
            {
                return row;
            }
        }

        String key = transport.identityKey();
        for (TransportRow row : rows)
        {
            Transport current = row.current();
            if (current != null && current.identityKey().equals(key))
            {
                return row;
            }
        }
        return null;
    }

    int syncStaged(List<Transport> staged)
    {
        int added = 0;
        for (Transport transport : staged)
        {
            if (rowOf(transport) == null)
            {
                rows.add(new TransportRow(TransportRow.Origin.STAGED, transport));
                added++;
            }
        }
        if (added > 0)
        {
            fireTableRowsInserted(rows.size() - added, rows.size() - 1);
        }
        return added;
    }

    TransportRow add(Transport draft)
    {
        TransportRow row = TransportRow.added(draft);
        rows.add(row);
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        return row;
    }

    void apply(TransportRow row, Transport draft)
    {
        Transport saved = row.saved();
        row.draft(saved != null && sameAs(saved, draft) ? null : draft);
        changed(row);
    }

    void revert(TransportRow row)
    {
        if (row.isNew())
        {
            removed(row);
            return;
        }

        row.revert();
        changed(row);
    }

    void delete(TransportRow row)
    {
        if (row.isNew())
        {
            removed(row);
            return;
        }

        row.delete();
        changed(row);
    }

    void saved(TransportRow row, TransportRow.Origin origin, Transport transport)
    {
        row.savedAs(origin, transport);
        changed(row);
    }

    void removed(TransportRow row)
    {
        int index = indexOf(row);
        if (index >= 0)
        {
            rows.remove(index);
            fireTableRowsDeleted(index, index);
        }
    }

    boolean isDirty()
    {
        for (TransportRow row : rows)
        {
            if (row.isDirty())
            {
                return true;
            }
        }
        return false;
    }

    List<TransportRow> dirtyRows()
    {
        List<TransportRow> dirty = new ArrayList<>();
        for (TransportRow row : rows)
        {
            if (row.isDirty())
            {
                dirty.add(row);
            }
        }
        return dirty;
    }

    static boolean matches(TransportRow row, String search)
    {
        if (search == null || search.trim().isEmpty())
        {
            return true;
        }

        Transport transport = row.current();
        if (transport == null)
        {
            return false;
        }

        String haystack = (transport.getName() + " " + (transport.isObject() ? "object" : "npc") + " id" + transport.getId() + " opt" + transport.getOption() + " " + tile(transport.getLocation().toWorldPoint()) + " " + tile(transport.getDestination().toWorldPoint())).toLowerCase(Locale.ROOT);
        for (String token : search.trim().toLowerCase(Locale.ROOT).split("\\s+"))
        {
            if (!haystack.contains(token))
            {
                return false;
            }
        }
        return true;
    }

    static String tile(WorldPoint point)
    {
        return point.getX() + "," + point.getY() + "," + point.getPlane();
    }

    static boolean sameAs(Transport a, Transport b)
    {
        String nameA = a.getName() == null ? "" : a.getName();
        String nameB = b.getName() == null ? "" : b.getName();
        return a.identityKey().equals(b.identityKey()) && a.getDuration() == b.getDuration() && nameA.equals(nameB) && TransportJson.GSON.toJson(a.getRequirements()).equals(TransportJson.GSON.toJson(b.getRequirements()));
    }

    private void changed(TransportRow row)
    {
        int index = indexOf(row);
        if (index >= 0)
        {
            fireTableRowsUpdated(index, index);
        }
    }

    @Override
    public int getRowCount()
    {
        return rows.size();
    }

    @Override
    public int getColumnCount()
    {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column)
    {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column)
    {
        switch (column)
        {
            case ID_COLUMN:
            case OPTION_COLUMN:
            case DURATION_COLUMN:
            case REQUIREMENTS_COLUMN:
                return Integer.class;
            default:
                return String.class;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int column)
    {
        TransportRow row = rows.get(rowIndex);
        Transport transport = row.current();
        switch (column)
        {
            case DIRTY_COLUMN:
                return row.isDeleted() ? "x" : row.isDirty() ? "*" : "";
            case ORIGIN_COLUMN:
                return row.isNew() ? "new" : row.origin() == TransportRow.Origin.LIVE ? "live" : "staged";
            case NAME_COLUMN:
                return transport.getName();
            case TYPE_COLUMN:
                return transport.isObject() ? "obj" : "npc";
            case ID_COLUMN:
                return transport.getId();
            case OPTION_COLUMN:
                return transport.getOption();
            case SOURCE_COLUMN:
                return tile(transport.getLocation().toWorldPoint());
            case DESTINATION_COLUMN:
                return tile(transport.getDestination().toWorldPoint());
            case DURATION_COLUMN:
                return transport.getDuration();
            case REQUIREMENTS_COLUMN:
                return transport.getRequirements().getAll().size();
            case MET_COLUMN:
                return transport.hasRequirements() ? fulfilled.apply(transport).map(met -> met ? "yes" : "no").orElse("") : "";
            default:
                return null;
        }
    }
}
