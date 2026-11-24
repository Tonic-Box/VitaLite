package com.tonic.services.pathfinder.recorder;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.events.PacketSent;
import com.tonic.packets.PacketMapReader;
import com.tonic.packets.types.PacketDefinition;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportAvailability;
import com.tonic.services.pathfinder.transports.TransportLoader;
import com.tonic.util.WorldPointUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Records transports from the player's own clicks: arms a capture on an object or NPC option, advances it each tick and stages what it finishes, dropping edges already live or staged; also serves the transport tiles and their right-click editor entries.
 */
public final class TransportRecorder
{
    private static final Color MENU_MET = new Color(0x50C878);
    private static final Color MENU_UNMET = new Color(0xE05050);
    private static final Color MENU_MUTED = new Color(0x9AA0A6);
    private static final Color MENU_TARGET = new Color(0x00FFFF);
    private static final String CONTINUE_HINT = "continue";
    private static final String RESUME_PACKET = "OP_RESUME_PAUSEBUTTON";
    private static final String RESUME_WIDGET = "widgetID";
    private static final String RESUME_CHILD = "optionIndex";

    private final TransportRecordStore store;
    private final RecordingSession session = new RecordingSession();
    private volatile boolean recording;
    private volatile boolean showTiles;
    private volatile boolean membersOnly;
    private volatile String status = "idle";
    private volatile boolean active;
    private volatile Map<Transport, Boolean> fulfilled = Collections.emptyMap();
    private volatile Map<WorldPoint, Integer> tiles = Collections.emptyMap();
    private volatile Consumer<Transport> editor;
    private volatile BooleanSupplier fulfilmentWanted = () -> false;
    private int resumePacketId = -1;

    /**
     * @param store where finished recordings are staged
     */
    public TransportRecorder(TransportRecordStore store)
    {
        this.store = store;
    }

    /**
     * Arms a capture on an object or NPC option, or notes a dialogue click made while one is under way.
     *
     * @param event the click
     */
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!recording)
        {
            return;
        }

        try
        {
            MenuAction action = event.getMenuAction();
            if (action == MenuAction.WIDGET_CONTINUE || (action == MenuAction.CC_OP && event.getParam1() >>> 16 == InterfaceID.CHATMENU))
            {
                if (resumePacketId() < 0)
                {
                    dialogueChosen(widgetText(event.getParam1(), event.getParam0()));
                }
                return;
            }

            int objectOption = objectOption(action);
            if (objectOption > 0)
            {
                armObject(event, objectOption);
                return;
            }

            int npcOption = npcOption(action);
            if (npcOption > 0)
            {
                armNpc(event, npcOption);
            }
        }
        catch (Throwable t)
        {
            Logger.error(t, "[TransportRecorder] Click not recorded: %e");
        }
    }

    /**
     * Notes the dialogue line a resume packet answers while a capture is under way: an option is recorded as part of the transport, a continue prompt only keeps the capture alive. Mouse and keyboard answers both send it.
     *
     * @param event the packet sent
     */
    @Subscribe
    public void onPacketSent(PacketSent event)
    {
        if (!recording || !session.isActive() || resumePacketId() < 0 || event.getId() != resumePacketId())
        {
            return;
        }

        try
        {
            PacketDefinition packet = PacketMapReader.analyze(event.getFreshBuffer());
            if (packet == null || !RESUME_PACKET.equals(packet.getName()))
            {
                return;
            }

            Long widgetId = packet.getMap().get(RESUME_WIDGET);
            Long childIndex = packet.getMap().get(RESUME_CHILD);
            if (widgetId != null)
            {
                dialogueChosen(widgetText(widgetId.intValue(), childIndex == null ? -1 : childIndex.intValue()));
            }
        }
        catch (Throwable t)
        {
            Logger.error(t, "[TransportRecorder] Dialogue not recorded: %e");
        }
    }

    /**
     * Advances the capture, stages a finished one, and works out the tiles and fulfilled requirements shown.
     *
     * @param tick the tick
     */
    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (!recording && session.isActive())
        {
            session.reset();
        }

        Transport record = session.onTick();
        if (record != null)
        {
            stage(record);
        }
        active = session.isActive();
        status = session.status();

        Client client = Static.getClient();
        boolean inGame = client.getGameState() == GameState.LOGGED_IN;
        if (inGame && (showTiles || fulfilmentWanted.getAsBoolean()))
        {
            evaluateFulfilled();
        }
        tiles = inGame && showTiles ? countTiles(client) : Collections.emptyMap();
    }

    /**
     * Adds an "Edit transport" entry to a walk-here entry for every live or staged transport leaving the tile, while the tiles are shown.
     *
     * @param event the entry added
     */
    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        Consumer<Transport> open = editor;
        if (!showTiles || open == null || event.getType() != MenuAction.WALK.getId())
        {
            return;
        }

        Client client = Static.getClient();
        Tile tile = client.getTopLevelWorldView().getSelectedSceneTile();
        if (tile == null)
        {
            return;
        }

        int packed = WorldPointUtil.compress(WorldPointUtil.fromInstance(tile.getWorldLocation()));
        ObjectArrayList<Transport> live = TransportLoader.getTransports().get(packed);
        if (live != null)
        {
            for (Transport transport : live)
            {
                if (transport.getClass() == Transport.class)
                {
                    addEntry(client, transport, false, open);
                }
            }
        }
        for (Transport transport : store.getRecords())
        {
            if (transport.getLocation().getPacked() == packed)
            {
                addEntry(client, transport, true, open);
            }
        }
    }

    /**
     * Adds transports to the live file and the running walker.
     *
     * @param transports the transports
     * @return how many were not live before
     */
    public int promote(List<Transport> transports)
    {
        int added = 0;
        for (Transport transport : transports)
        {
            if (TransportLoader.live(transport) == null)
            {
                added++;
            }
        }
        TransportLoader.promote(transports);
        return added;
    }

    /**
     * @param transport a transport
     * @return whether its requirements held on the last tick they were worked out, empty when it has none or they were not worked out
     */
    public Optional<Boolean> fulfilled(Transport transport)
    {
        if (!transport.hasRequirements())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(fulfilled.get(transport));
    }

    /**
     * @param editor opens the editor on a transport, or null for none
     */
    public void editor(Consumer<Transport> editor)
    {
        this.editor = editor;
    }

    /**
     * @param wanted whether fulfilled requirements must be worked out each tick even while the tiles are hidden
     */
    public void fulfilmentWanted(BooleanSupplier wanted)
    {
        this.fulfilmentWanted = wanted;
    }

    /**
     * @return the staging store
     */
    public TransportRecordStore store()
    {
        return store;
    }

    /**
     * @return whether clicks are being recorded
     */
    public boolean recording()
    {
        return recording;
    }

    /**
     * @param value whether clicks are recorded; turning it off drops the capture under way
     */
    public void recording(boolean value)
    {
        recording = value;
    }

    /**
     * @return whether the transport tiles are shown
     */
    public boolean showTiles()
    {
        return showTiles;
    }

    /**
     * @param value whether the transport tiles are shown
     */
    public void showTiles(boolean value)
    {
        showTiles = value;
    }

    /**
     * @return whether finished recordings get a members-world requirement
     */
    public boolean membersOnly()
    {
        return membersOnly;
    }

    /**
     * @param value whether finished recordings get a members-world requirement
     */
    public void membersOnly(boolean value)
    {
        membersOnly = value;
        session.membersOnly(value);
    }

    /**
     * @return what the capture is doing
     */
    public String status()
    {
        return status;
    }

    /**
     * @return whether a capture is under way
     */
    public boolean active()
    {
        return active;
    }

    Map<WorldPoint, Integer> tiles()
    {
        return tiles;
    }

    private void stage(Transport record)
    {
        Transport staged = store.find(record);
        if (staged != null)
        {
            if (requiresMembers(staged) != requiresMembers(record))
            {
                store.replace(staged, record);
                Logger.info("[TransportRecorder] Updated staged (members-only=" + requiresMembers(record) + "): " + describe(record));
            }
            else
            {
                Logger.info("[TransportRecorder] Discarded duplicate (already staged): " + describe(record));
            }
            return;
        }

        Transport live = TransportLoader.live(record);
        if (live == null)
        {
            store.add(record);
            Logger.info("[TransportRecorder] Recorded " + describe(record));
        }
        else if (requiresMembers(live) != requiresMembers(record))
        {
            store.add(record);
            Logger.info("[TransportRecorder] Staged members-only change to a live transport: " + describe(record));
        }
        else
        {
            Logger.info("[TransportRecorder] Discarded duplicate (already live): " + describe(record));
        }
    }

    private void dialogueChosen(String text)
    {
        if (!session.isActive())
        {
            return;
        }

        if (text == null || text.trim().isEmpty() || text.toLowerCase().contains(CONTINUE_HINT))
        {
            session.onDialogueContinue();
            return;
        }

        session.onDialogueOption(text.trim());
        Logger.info("[TransportRecorder] Dialogue option '" + text.trim() + "'");
    }

    private static String widgetText(int widgetId, int childIndex)
    {
        Client client = Static.getClient();
        Widget widget = client.getWidget(widgetId);
        if (widget != null && childIndex >= 0)
        {
            Widget child = widget.getChild(childIndex);
            widget = child != null ? child : widget;
        }
        return widget == null || widget.getText() == null ? null : Text.removeTags(widget.getText());
    }

    private int resumePacketId()
    {
        if (resumePacketId < 0)
        {
            resumePacketId = PacketMapReader.getId(RESUME_PACKET);
        }
        return resumePacketId;
    }

    private void armObject(MenuOptionClicked event, int option)
    {
        Client client = Static.getClient();
        int id = event.getId();
        TileObject object = findObject(viewOf(client, event.getMenuEntry()), event.getParam0(), event.getParam1(), id);
        WorldPoint wall = null;
        int shape = 0;
        int rotation = 0;
        if (object instanceof WallObject)
        {
            int config = ((WallObject) object).getConfig();
            shape = config & 0x1F;
            rotation = (config >> 6) & 3;
            wall = object.getWorldLocation();
        }
        session.membersOnly(membersOnly);
        session.arm(true, option, id, objectName(client, id), wall, shape, rotation);
    }

    private void armNpc(MenuOptionClicked event, int option)
    {
        NPC npc = event.getMenuEntry().getNpc();
        if (npc == null)
        {
            return;
        }

        NPCComposition transformed = npc.getTransformedComposition();
        String name = transformed != null ? transformed.getName() : npc.getName();
        session.membersOnly(membersOnly);
        session.arm(false, option, npc.getId(), name, null, 0, 0);
    }

    private static WorldView viewOf(Client client, MenuEntry entry)
    {
        WorldView view = client.getWorldView(entry.getWorldViewId());
        return view != null ? view : client.getTopLevelWorldView();
    }

    private static TileObject findObject(WorldView view, int sceneX, int sceneY, int id)
    {
        Scene scene = view.getScene();
        if (scene == null || sceneX < 0 || sceneY < 0 || sceneX >= view.getSizeX() || sceneY >= view.getSizeY())
        {
            return null;
        }

        Tile[][][] tiles = scene.getTiles();
        TileObject found = objectOn(tiles[view.getPlane()][sceneX][sceneY], id);
        for (int plane = 0; found == null && plane < tiles.length; plane++)
        {
            if (plane != view.getPlane())
            {
                found = objectOn(tiles[plane][sceneX][sceneY], id);
            }
        }
        return found;
    }

    private static TileObject objectOn(Tile tile, int id)
    {
        if (tile == null)
        {
            return null;
        }

        List<TileObject> candidates = new ArrayList<>();
        candidates.add(tile.getWallObject());
        candidates.add(tile.getDecorativeObject());
        candidates.add(tile.getGroundObject());
        if (tile.getGameObjects() != null)
        {
            candidates.addAll(Arrays.asList(tile.getGameObjects()));
        }
        for (TileObject candidate : candidates)
        {
            if (candidate != null && candidate.getId() == id)
            {
                return candidate;
            }
        }
        return null;
    }

    private static String objectName(Client client, int id)
    {
        ObjectComposition definition = client.getObjectDefinition(id);
        if (definition == null)
        {
            return null;
        }
        if (definition.getImpostorIds() != null)
        {
            ObjectComposition impostor = definition.getImpostor();
            if (impostor != null)
            {
                return impostor.getName();
            }
        }
        return definition.getName();
    }

    private static int objectOption(MenuAction action)
    {
        switch (action)
        {
            case GAME_OBJECT_FIRST_OPTION:
                return 1;
            case GAME_OBJECT_SECOND_OPTION:
                return 2;
            case GAME_OBJECT_THIRD_OPTION:
                return 3;
            case GAME_OBJECT_FOURTH_OPTION:
                return 4;
            case GAME_OBJECT_FIFTH_OPTION:
                return 5;
            default:
                return 0;
        }
    }

    private static int npcOption(MenuAction action)
    {
        switch (action)
        {
            case NPC_FIRST_OPTION:
                return 1;
            case NPC_SECOND_OPTION:
                return 2;
            case NPC_THIRD_OPTION:
                return 3;
            case NPC_FOURTH_OPTION:
                return 4;
            case NPC_FIFTH_OPTION:
                return 5;
            default:
                return 0;
        }
    }

    private void addEntry(Client client, Transport transport, boolean staged, Consumer<Transport> open)
    {
        WorldPoint to = transport.getDestination().toWorldPoint();
        Color colour = fulfilled(transport).map(met -> met ? MENU_MET : MENU_UNMET).orElse(MENU_TARGET);
        String target = ColorUtil.wrapWithColorTag(transport.getName() + " -> " + to.getX() + "," + to.getY() + "," + to.getPlane(), colour) + (staged ? ColorUtil.wrapWithColorTag(" (staged)", MENU_MUTED) : "");
        client.getMenu().createMenuEntry(1).setOption("Edit transport").setTarget(target).setType(MenuAction.RUNELITE).onClick(entry -> SwingUtilities.invokeLater(() -> open.accept(transport)));
    }

    private void evaluateFulfilled()
    {
        List<Collection<Transport>> lists = new ArrayList<>(TransportLoader.all());
        lists.add(store.getRecords());
        try
        {
            Set<Transport> blocked = TransportAvailability.evaluate(lists, true);
            Map<Transport, Boolean> answers = new IdentityHashMap<>();
            for (Collection<Transport> list : lists)
            {
                for (Transport transport : list)
                {
                    if (transport.hasRequirements())
                    {
                        answers.put(transport, !blocked.contains(transport));
                    }
                }
            }
            fulfilled = answers;
        }
        catch (RuntimeException e)
        {
            Logger.error(e, "[TransportRecorder] Requirements not worked out: %e");
        }
    }

    private Map<WorldPoint, Integer> countTiles(Client client)
    {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Int2ObjectMap.Entry<ObjectArrayList<Transport>> entry : TransportLoader.getTransports().int2ObjectEntrySet())
        {
            counts.merge(entry.getIntKey(), entry.getValue().size(), Integer::sum);
        }
        for (Transport staged : store.getRecords())
        {
            counts.merge(staged.getLocation().getPacked(), 1, Integer::sum);
        }

        WorldView view = client.getTopLevelWorldView();
        int plane = view.getPlane();
        int baseX = view.getBaseX();
        int baseY = view.getBaseY();
        Map<WorldPoint, Integer> shown = new HashMap<>();
        if (!view.isInstance())
        {
            for (Map.Entry<Integer, Integer> count : counts.entrySet())
            {
                int packed = count.getKey();
                int x = WorldPointUtil.getCompressedX(packed);
                int y = WorldPointUtil.getCompressedY(packed);
                if (WorldPointUtil.getCompressedPlane(packed) == plane && x >= baseX && y >= baseY && x < baseX + view.getSizeX() && y < baseY + view.getSizeY())
                {
                    shown.put(new WorldPoint(x, y, plane), count.getValue());
                }
            }
            return shown;
        }

        for (int x = 0; x < view.getSizeX(); x++)
        {
            for (int y = 0; y < view.getSizeY(); y++)
            {
                WorldPoint live = new WorldPoint(baseX + x, baseY + y, plane);
                Integer count = counts.get(WorldPointUtil.compress(WorldPointUtil.fromInstance(live)));
                if (count != null)
                {
                    shown.put(live, count);
                }
            }
        }
        return shown;
    }

    private static boolean requiresMembers(Transport transport)
    {
        for (WorldRequirement world : transport.getRequirements().getWorldRequirements())
        {
            if (world.isMemberWorld())
            {
                return true;
            }
        }
        return false;
    }

    private static String describe(Transport transport)
    {
        String dialogue = transport.getDialogueOptions() != null && transport.getDialogueOptions().length > 0 ? " diag=" + Arrays.toString(transport.getDialogueOptions()) : "";
        return transport.getName() + " opt" + transport.getOption() + " " + transport.getLocation().toWorldPoint() + " -> " + transport.getDestination().toWorldPoint() + " dur=" + transport.getDuration() + dialogue;
    }
}
