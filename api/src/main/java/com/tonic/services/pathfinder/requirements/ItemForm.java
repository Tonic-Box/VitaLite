package com.tonic.services.pathfinder.requirements;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;

/**
 * Which forms of an item an ItemRequirement counts: as given, its noted form, or both.
 */
public enum ItemForm
{
    UNNOTED,
    NOTED,
    CANONICAL;

    private static final int TEMPLATE_FOR_CERT = 799;

    /**
     * @param id an item id
     * @return the ids counted for it in this form
     */
    public int[] idsOf(int id)
    {
        if (this == UNNOTED)
        {
            return new int[] {id};
        }
        return Static.invoke(() ->
        {
            Client client = Static.getClient();
            ItemComposition item = client.getItemDefinition(id);
            boolean noted = item.getNote() == TEMPLATE_FOR_CERT;
            int unnoted = noted ? item.getLinkedNoteId() : id;
            int note = noted ? id : item.getLinkedNoteId();
            if (this == NOTED)
            {
                return note == -1 ? new int[0] : new int[] {note};
            }
            return note == -1 || note == unnoted ? new int[] {unnoted} : new int[] {unnoted, note};
        });
    }
}
