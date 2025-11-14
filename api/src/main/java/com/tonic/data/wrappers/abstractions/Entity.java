package com.tonic.data.wrappers.abstractions;

import com.tonic.queries.combined.EntityQuery;

public interface Entity extends Locatable, Interactable, Identifiable
{
    static EntityQuery search()
    {
        return new EntityQuery();
    }
}
