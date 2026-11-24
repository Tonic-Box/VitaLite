package com.tonic.services.pathfinder.ui;

import com.tonic.Static;
import com.tonic.services.pathfinder.requirements.Comparison;
import com.tonic.services.pathfinder.requirements.ItemForm;
import com.tonic.services.pathfinder.requirements.ItemRequirement;
import com.tonic.services.pathfinder.requirements.OtherRequirement;
import com.tonic.services.pathfinder.requirements.QuestRequirement;
import com.tonic.services.pathfinder.requirements.Reduction;
import com.tonic.services.pathfinder.requirements.Requirement;
import com.tonic.services.pathfinder.requirements.SkillRequirement;
import com.tonic.services.pathfinder.requirements.VarRequirement;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import net.runelite.api.Client;
import net.runelite.api.QuestState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * One-line descriptions of requirements and transport targets for the editor, with item, object and NPC names read from the client's definitions on the client thread and ids alone when a name cannot be read.
 */
final class RequirementDescriptions
{
    private final Map<Integer, String> items = new ConcurrentHashMap<>();
    private final Map<Integer, String> objects = new ConcurrentHashMap<>();
    private final Map<Integer, String> npcs = new ConcurrentHashMap<>();

    String itemName(int id)
    {
        return cached(items, id, () ->
        {
            Client client = Static.getClient();
            return client.getItemDefinition(id).getName();
        });
    }

    String targetName(boolean object, int id)
    {
        if (object)
        {
            return cached(objects, id, () ->
            {
                Client client = Static.getClient();
                return client.getObjectDefinition(id).getName();
            });
        }
        return cached(npcs, id, () ->
        {
            Client client = Static.getClient();
            return client.getNpcDefinition(id).getName();
        });
    }

    String item(int id)
    {
        String name = itemName(id);
        return name.isEmpty() ? String.valueOf(id) : name + " (" + id + ")";
    }

    String describe(Requirement requirement)
    {
        if (requirement instanceof ItemRequirement)
        {
            return describe((ItemRequirement) requirement);
        }
        if (requirement instanceof SkillRequirement)
        {
            SkillRequirement skill = (SkillRequirement) requirement;
            return skill.getSkill().getName() + " " + skill.getLevel() + (skill.isBoostable() ? " (boostable)" : " (base)");
        }
        if (requirement instanceof VarRequirement)
        {
            VarRequirement var = (VarRequirement) requirement;
            return var.getType().name().toLowerCase(Locale.ROOT) + " " + var.getVar() + " " + symbol(var.getComparison()) + " " + var.getValue();
        }
        if (requirement instanceof QuestRequirement)
        {
            QuestRequirement quest = (QuestRequirement) requirement;
            List<String> states = new ArrayList<>();
            for (QuestState state : QuestState.values())
            {
                if (quest.getStates() != null && quest.getStates().contains(state))
                {
                    states.add(state.name());
                }
            }
            return (quest.getQuest() == null ? "?" : quest.getQuest().getName()) + ": " + String.join(" | ", states);
        }
        if (requirement instanceof WorldRequirement)
        {
            return ((WorldRequirement) requirement).isMemberWorld() ? "Members' world" : "Any world";
        }
        if (requirement instanceof OtherRequirement)
        {
            return "Code-only requirement";
        }
        return String.valueOf(requirement);
    }

    private String describe(ItemRequirement item)
    {
        List<String> names = new ArrayList<>();
        for (int id : item.getIds())
        {
            names.add(item(id));
        }

        String where = item.isEquipped() == null ? "carried or worn" : item.isEquipped() ? "worn" : "in inventory";
        ItemForm form = item.getForm() == null ? ItemForm.UNNOTED : item.getForm();
        String logic = item.getReduction() == Reduction.AND ? "all of" : item.getReduction() == Reduction.NOT ? "none of" : "any of";
        String forms = form == ItemForm.CANONICAL ? "either" : form.name().toLowerCase(Locale.ROOT);
        return logic + " " + String.join(", ", names) + " x" + item.getAmount() + ", " + where + ", " + forms;
    }

    private static String symbol(Comparison comparison)
    {
        switch (comparison)
        {
            case LESS_THAN:
                return "<";
            case LESS_THAN_EQUAL:
                return "<=";
            case GREATER_THAN:
                return ">";
            case GREATER_THAN_EQUAL:
                return ">=";
            case NOT_EQUAL:
                return "!=";
            default:
                return "=";
        }
    }

    private static String cached(Map<Integer, String> cache, int id, Supplier<String> lookup)
    {
        String known = cache.get(id);
        if (known != null)
        {
            return known;
        }

        String name;
        try
        {
            name = Static.invoke(lookup);
        }
        catch (RuntimeException e)
        {
            return "";
        }
        if (name == null || name.isEmpty() || "null".equals(name))
        {
            return "";
        }
        cache.put(id, name);
        return name;
    }
}
