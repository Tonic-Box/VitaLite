package com.tonic.services.pathfinder.requirements;

import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Items held in some amount: each id is counted in the inventory, the worn equipment or both (equipped null), in the unnoted, noted or either form, and the reduction says whether every id, any id or no id must reach the amount. A missing form reads as unnoted, a missing reduction as any.
 */
@Value
public class ItemRequirement implements Requirement
{
    Reduction reduction;
    Boolean equipped;
    List<Integer> ids;
    int amount;
    ItemForm form;

    /**
     * Any of the ids, unnoted.
     *
     * @param equipped null to count inventory and equipment, true for equipment only, false for inventory only
     * @param amount how many must be held
     * @param ids the item ids
     */
    public ItemRequirement(Boolean equipped, int amount, int... ids)
    {
        this(Reduction.OR, equipped, ItemForm.UNNOTED, amount, ids);
    }

    /**
     * @param reduction whether every id, any id or no id must reach the amount
     * @param equipped null to count inventory and equipment, true for equipment only, false for inventory only
     * @param form which forms of each id count
     * @param amount how many must be held
     * @param ids the item ids
     */
    public ItemRequirement(Reduction reduction, Boolean equipped, ItemForm form, int amount, int... ids)
    {
        this.reduction = reduction;
        this.equipped = equipped;
        this.form = form;
        this.amount = amount;
        this.ids = new ArrayList<>();
        for (int id : ids)
        {
            this.ids.add(id);
        }
    }

    /**
     * @return null to count inventory and equipment, true for equipment only, false for inventory only
     */
    public Boolean isEquipped()
    {
        return equipped;
    }

    @Override
    public Boolean get()
    {
        Reduction how = reduction == null ? Reduction.OR : reduction;
        switch (how)
        {
            case AND:
                return ids.stream().allMatch(this::held);
            case OR:
                return ids.stream().anyMatch(this::held);
            case NOT:
                return ids.stream().noneMatch(this::held);
            default:
                return false;
        }
    }

    private boolean held(int id)
    {
        ItemForm counted = form == null ? ItemForm.UNNOTED : form;
        int total = 0;
        for (int counts : counted.idsOf(id))
        {
            if (equipped == null || equipped)
            {
                total += EquipmentAPI.getCount(counts);
            }
            if (equipped == null || !equipped)
            {
                total += InventoryAPI.getCount(counts);
            }
        }
        return total >= amount;
    }
}
