package com.tonic.services.pathfinder.sailing;

import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.data.EquipmentSlot;
import com.tonic.services.pathfinder.requirements.OtherRequirement;
import com.tonic.services.pathfinder.requirements.QuestRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.SkillRequirement;
import com.tonic.services.pathfinder.sailing.sea.SeaMap;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.ObjectID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The game's docks, read from its dock table and placed by the sea map
 */
public final class Ports
{
    /**
     * The first and last rows of the dock table, then the two rows added after them
     */
    private static final int FIRST_ROW = 8587;
    private static final int LAST_ROW = 8645;
    private static final int[] LATER_ROWS = {7169, 7170};
    static final int SARIM_ROW = 8587;
    static final int FIRST_GANGPLANK = ObjectID.SAILING_GANGPLANK_PORT_SARIM;
    static final int FIRST_BUOY = ObjectID.SAILING_DOCKING_BUOY_PORT_SARIM;
    static final int LAST_NUMBERED_DOCK = ObjectID.SAILING_MOORING_GRIMSTONE - FIRST_GANGPLANK;
    private static final String ENTRANA = "Entrana";
    private static final EquipmentSlot[] ENTRANA_BARE_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.WEAPON, EquipmentSlot.BODY, EquipmentSlot.SHIELD, EquipmentSlot.LEGS};

    /**
     * Reads a column of a row of the game's database: every value of every tuple, in order
     */
    @FunctionalInterface
    public interface DockTable
    {
        Object[] values(int row, int column);
    }

    private final List<Port> ports;
    private final Map<Integer, Port> byDock = new HashMap<>();
    private final Map<Integer, Port> byRow = new HashMap<>();
    private final Map<String, Port> byName = new HashMap<>();

    private Ports(List<Port> ports)
    {
        this.ports = Collections.unmodifiableList(ports);
        for (Port port : ports)
        {
            byDock.put(port.dockId(), port);
            byRow.put(port.row(), port);
            byName.put(port.name().toLowerCase(Locale.ROOT), port);
        }
    }

    /**
     * Loads the docks of the table that the sea map knows
     * @param table the game's dock table
     * @param sea the sea map that places the docks
     * @return the loaded ports
     */
    public static Ports load(DockTable table, SeaMap sea)
    {
        if (table == null || sea == null)
        {
            throw new IllegalArgumentException("table and sea cannot be null");
        }
        Map<Integer, SeaMap.Dock> placed = new HashMap<>();
        for (SeaMap.Dock dock : sea.docks())
        {
            placed.put(dock.dockId(), dock);
        }
        List<Port> ports = new ArrayList<>();
        for (int row : rows())
        {
            int dockId = number(table.values(row, DBTableID.SailingDock.COL_DOCK_ID), -1);
            String name = text(table.values(row, DBTableID.SailingDock.COL_NICE_NAME));
            if (dockId < 0 || name == null)
            {
                continue;
            }
            int level = number(table.values(row, DBTableID.SailingDock.COL_LEVEL_REQUIRED), 1);
            List<Quest> quests = quests(table.values(row, DBTableID.SailingDock.COL_QUEST_REQUIRED));
            ports.add(port(placed.get(dockId), dockId, row, name, level, quests, requirements(name, level, quests)));
        }
        return new Ports(ports);
    }

    /**
     * Builds ports from the sea map's docks alone, named by dock id and needing nothing
     * @param sea the sea map
     * @return the ports of the sea map's docks
     */
    public static Ports placedOnly(SeaMap sea)
    {
        List<Port> ports = new ArrayList<>();
        for (SeaMap.Dock dock : sea.docks())
        {
            ports.add(port(dock, dock.dockId(), -1, "Dock " + dock.dockId(), 1, List.of(), null));
        }
        return new Ports(ports);
    }

    public List<Port> all()
    {
        return ports;
    }

    public Port byRow(int row)
    {
        return byRow.get(row);
    }

    public Port byDock(int dockId)
    {
        return byDock.get(dockId);
    }

    public Port byName(String name)
    {
        return name == null ? null : byName.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Finds the placed port whose gangplank is nearest a tile
     * @param from the tile to measure from
     * @return the nearest placed port, or null when none is placed
     */
    public Port nearest(WorldPoint from)
    {
        if (from == null)
        {
            throw new IllegalArgumentException("from cannot be null");
        }
        Port best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Port port : ports)
        {
            if (!port.placed())
            {
                continue;
            }
            int distance = Math.max(Math.abs(port.land().getX() - from.getX()), Math.abs(port.land().getY() - from.getY()));
            if (distance < bestDistance)
            {
                bestDistance = distance;
                best = port;
            }
        }
        return best;
    }

    /**
     * Gets the placed ports the player may use
     * @return the allowed ports
     */
    public List<Port> allowed()
    {
        List<Port> allowed = new ArrayList<>();
        for (Port port : ports)
        {
            if (port.placed() && port.allowed())
            {
                allowed.add(port);
            }
        }
        return allowed;
    }

    private static Port port(SeaMap.Dock dock, int dockId, int row, String name, int level, List<Quest> quests, Requirements requirements)
    {
        int gangplank = dockId <= LAST_NUMBERED_DOCK ? FIRST_GANGPLANK + dockId : -1;
        return new Port(dockId, row, name, level, quests, dock != null ? dock.locId() : gangplank, FIRST_BUOY + dockId, dock == null ? null : dock.land(), dock == null ? null : dock.approach(), requirements);
    }

    private static int[] rows()
    {
        int[] rows = new int[LAST_ROW - FIRST_ROW + 1 + LATER_ROWS.length];
        for (int row = FIRST_ROW; row <= LAST_ROW; row++)
        {
            rows[row - FIRST_ROW] = row;
        }
        System.arraycopy(LATER_ROWS, 0, rows, LAST_ROW - FIRST_ROW + 1, LATER_ROWS.length);
        return rows;
    }

    private static Requirements requirements(String name, int level, List<Quest> quests)
    {
        Requirements requirements = new Requirements();
        requirements.addRequirement(new SkillRequirement(Skill.SAILING, level));
        for (Quest quest : quests)
        {
            requirements.addRequirement(new QuestRequirement(quest, QuestState.FINISHED));
        }
        if (ENTRANA.equals(name))
        {
            requirements.addRequirement(new OtherRequirement(Ports::bareOfArms));
        }
        return requirements;
    }

    private static boolean bareOfArms()
    {
        for (EquipmentSlot slot : ENTRANA_BARE_SLOTS)
        {
            if (EquipmentAPI.fromSlot(slot) != null)
            {
                return false;
            }
        }
        return true;
    }

    private static List<Quest> quests(Object[] values)
    {
        List<Quest> quests = new ArrayList<>();
        for (Object value : values == null ? new Object[0] : values)
        {
            if (!(value instanceof Integer))
            {
                continue;
            }
            Quest quest = questById((Integer) value);
            if (quest != null && !quests.contains(quest))
            {
                quests.add(quest);
            }
        }
        return quests;
    }

    private static Quest questById(int id)
    {
        for (Quest quest : Quest.values())
        {
            if (quest.getId() == id)
            {
                return quest;
            }
        }
        return null;
    }

    private static int number(Object[] values, int fallback)
    {
        return values != null && values.length > 0 && values[0] instanceof Integer ? (Integer) values[0] : fallback;
    }

    private static String text(Object[] values)
    {
        return values != null && values.length > 0 && values[0] instanceof String ? (String) values[0] : null;
    }
}
