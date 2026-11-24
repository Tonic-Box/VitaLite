package com.tonic.services.pathfinder.ui;

import com.tonic.services.pathfinder.transports.Transport;

/**
 * One transport in the editor: where it lives, the transport as its source holds it, and the edited copy waiting to be saved; a row added in the editor has no saved copy until its first save.
 */
final class TransportRow
{
    enum Origin
    {
        LIVE,
        STAGED
    }

    private Origin origin;
    private Transport saved;
    private Transport draft;
    private boolean deleted;

    TransportRow(Origin origin, Transport saved)
    {
        this.origin = origin;
        this.saved = saved;
    }

    static TransportRow added(Transport draft)
    {
        TransportRow row = new TransportRow(Origin.STAGED, null);
        row.draft = draft;
        return row;
    }

    Origin origin()
    {
        return origin;
    }

    Transport saved()
    {
        return saved;
    }

    Transport draft()
    {
        return draft;
    }

    Transport current()
    {
        return draft != null ? draft : saved;
    }

    boolean isDirty()
    {
        return draft != null || deleted;
    }

    boolean isDeleted()
    {
        return deleted;
    }

    boolean isNew()
    {
        return saved == null;
    }

    void draft(Transport draft)
    {
        this.draft = draft;
    }

    void revert()
    {
        draft = null;
        deleted = false;
    }

    void delete()
    {
        deleted = true;
    }

    void savedAs(Origin origin, Transport transport)
    {
        this.origin = origin;
        this.saved = transport;
        this.draft = null;
        this.deleted = false;
    }
}
