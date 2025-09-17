package com.tonic.services.pathfinder.requirements;

import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import lombok.Value;
import java.util.List;

@Value
public class ItemRequirement implements Requirement
{
    Reduction reduction;
    boolean equipped;
    List<Integer> ids;
    int amount;

    @Override
    public Boolean get()
    {
        switch (reduction)
        {
            case AND:
                if (equipped)
                {
                    return ids.stream().allMatch(it -> EquipmentAPI.getCount(it) >= amount);
                }
                else
                {
                    return ids.stream().allMatch(it -> InventoryAPI.getCount(it) >= amount);
                }
            case OR:
                if (equipped)
                {
                    return ids.stream().anyMatch(it -> EquipmentAPI.getCount(it) >= amount);
                }
                else
                {
                    return ids.stream().anyMatch(it -> InventoryAPI.getCount(it) >= amount);
                }
            case NOT:
                if (equipped)
                {
                    return ids.stream().noneMatch(it -> EquipmentAPI.getCount(it) >= amount);
                }
                else
                {
                    return ids.stream().noneMatch(it -> InventoryAPI.getCount(it) >= amount);
                }
        }
        return false;
    }
}