package com.tonic.services.pathfinder.sailing;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.data.EquipmentSlot;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.model.WalkerPath;
import com.tonic.services.pathfinder.sailing.sea.BoatHull;
import com.tonic.services.pathfinder.sailing.sea.BoatPose;
import com.tonic.services.pathfinder.sailing.sea.Compass;
import com.tonic.util.ThreadPool;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

/**
 * The sailing runtime's view of the live client, read on the client thread
 */
final class ClientSeaWorld implements SeaWorld
{
    private static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_OBJECT | CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION | CollisionDataFlag.BLOCK_MOVEMENT_FLOOR;
    private static final String CRATE = "Crate of";
    private static final int MOST_TUPLES = 16;

    private final Map<Integer, int[]> pivots = new HashMap<>();
    private final Map<Integer, BoatPose> lastTargets = new HashMap<>();
    private final Map<Integer, BoatPose> previousTargets = new HashMap<>();
    private CompletableFuture<WalkerPath> walk;
    private int vesselsTick = -1;

    private static Client client()
    {
        return Static.getClient();
    }

    @Override
    public int tick()
    {
        return GameManager.getTickCount();
    }

    @Override
    public int revision()
    {
        return client().getRevision();
    }

    @Override
    public int varbit(int id)
    {
        return Static.invoke(() -> client().getVarbitValue(id));
    }

    @Override
    public Ports.DockTable dockTable()
    {
        return (row, column) -> Static.invoke(() ->
        {
            List<Object> values = new ArrayList<>();
            for (int tuple = 0; tuple < MOST_TUPLES; tuple++)
            {
                Object[] fields;
                try
                {
                    fields = client().getDBTableField(row, column, tuple);
                }
                catch (RuntimeException e)
                {
                    break;
                }
                if (fields == null || fields.length == 0)
                {
                    break;
                }
                Collections.addAll(values, fields);
            }
            return values.toArray();
        });
    }

    @Override
    public boolean holdingCargo()
    {
        ItemEx held = EquipmentAPI.fromSlot(EquipmentSlot.WEAPON);
        return held != null && held.getName() != null && held.getName().startsWith(CRATE);
    }

    @Override
    public Boat boat()
    {
        return Static.invoke(() ->
        {
            WorldEntity entity = ownEntity();
            if (entity == null)
            {
                return null;
            }
            return new Boat(entity.getWorldView().getId(), target(entity), hull(entity));
        });
    }

    @Override
    public List<Vessel> vessels()
    {
        return Static.invoke(() ->
        {
            WorldView top = client().getTopLevelWorldView();
            WorldEntity own = ownEntity();
            int tick = tick();
            if (tick != vesselsTick)
            {
                previousTargets.clear();
                previousTargets.putAll(lastTargets);
                lastTargets.clear();
                vesselsTick = tick;
            }
            List<Vessel> vessels = new ArrayList<>();
            for (WorldEntity entity : top.worldEntities())
            {
                if (entity == own || entity.getWorldView() == null)
                {
                    continue;
                }
                int id = entity.getWorldView().getId();
                BoatPose next = target(entity);
                lastTargets.put(id, next);
                BoatPose now = previousTargets.getOrDefault(id, next);
                vessels.add(new Vessel(id, entity.getOwnerType() == WorldEntity.OWNER_TYPE_NOT_PLAYER, now, next, hull(entity)));
            }
            return vessels;
        });
    }

    @Override
    public List<Creature> npcs()
    {
        return Static.invoke(() ->
        {
            List<Creature> creatures = new ArrayList<>();
            for (NPC npc : client().getTopLevelWorldView().npcs())
            {
                NPCComposition composition = npc.getTransformedComposition();
                WorldPoint tile = npc.getWorldLocation();
                if (composition == null || tile == null)
                {
                    continue;
                }
                creatures.add(new Creature(npc.getIndex(), composition.getId(), composition.getName(), tile, composition.getSize(), npc.getAnimation()));
            }
            return creatures;
        });
    }

    @Override
    public List<Loc> locs(IntPredicate ids)
    {
        return Static.invoke(() ->
        {
            List<Loc> locs = new ArrayList<>();
            WorldView top = client().getTopLevelWorldView();
            Scene scene = top.getScene();
            Tile[][] tiles = scene.getTiles()[0];
            for (Tile[] column : tiles)
            {
                for (Tile tile : column)
                {
                    if (tile == null)
                    {
                        continue;
                    }
                    for (GameObject object : tile.getGameObjects())
                    {
                        if (object == null || !object.getSceneMinLocation().equals(tile.getSceneLocation()))
                        {
                            continue;
                        }
                        int id = transformed(object.getId());
                        if (ids.test(id))
                        {
                            WorldPoint corner = WorldPoint.fromScene(top, object.getSceneMinLocation().getX(), object.getSceneMinLocation().getY(), 0);
                            locs.add(new Loc(id, corner, object.sizeX(), object.sizeY()));
                        }
                    }
                }
            }
            return locs;
        });
    }

    @Override
    public WorldPoint playerTile()
    {
        return Static.invoke(() -> WorldPointUtil.getTopWorldViewLocation());
    }

    @Override
    public WorldPoint sceneBase()
    {
        return Static.invoke(() ->
        {
            WorldView top = client().getTopLevelWorldView();
            return new WorldPoint(top.getBaseX(), top.getBaseY(), 0);
        });
    }

    @Override
    public boolean moving()
    {
        return walk != null || MovementAPI.isMoving();
    }

    @Override
    public boolean optionPresent(String option)
    {
        for (String present : DialogueAPI.getOptions())
        {
            if (present != null && present.toLowerCase().contains(option.toLowerCase()))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void selectOption(String option)
    {
        DialogueAPI.selectOption(option);
    }

    @Override
    public void note(String message)
    {
        Logger.info(message);
    }

    @Override
    public <T> T invoke(Supplier<T> supplier)
    {
        return Static.invoke(supplier);
    }

    @Override
    public void invoke(Runnable runnable)
    {
        Static.invoke(() ->
        {
            runnable.run();
            return null;
        });
    }

    /**
     * Starts walking to a tile on land, the path planned off the client thread
     * @param tile the tile to walk to
     */
    void walk(WorldPoint tile)
    {
        cancelWalk();
        walk = CompletableFuture.supplyAsync(() -> WalkerPath.get(tile), ThreadPool::submit);
    }

    /**
     * Advances a walk under way by a step, once a game tick
     */
    void stepWalk()
    {
        if (walk == null || !walk.isDone())
        {
            return;
        }
        WalkerPath path = walk.getNow(null);
        if (path == null || walk.isCompletedExceptionally() || !path.step())
        {
            walk = null;
        }
    }

    void cancelWalk()
    {
        if (walk != null)
        {
            WalkerPath path = walk.getNow(null);
            if (path != null)
            {
                path.shutdown();
            }
            walk.cancel(false);
            walk = null;
        }
    }

    private WorldEntity ownEntity()
    {
        WorldView view = client().getLocalPlayer() == null ? null : client().getLocalPlayer().getWorldView();
        if (view == null || view.isTopLevel())
        {
            return null;
        }
        return client().getTopLevelWorldView().worldEntities().byIndex(view.getId());
    }

    private BoatPose target(WorldEntity entity)
    {
        WorldView top = client().getTopLevelWorldView();
        LocalPoint target = entity.getTargetLocation();
        return new BoatPose((top.getBaseX() << Compass.FINE_SHIFT) + target.getX(), (top.getBaseY() << Compass.FINE_SHIFT) + target.getY(), entity.getTargetOrientation());
    }

    private BoatHull hull(WorldEntity entity)
    {
        WorldView deck = entity.getWorldView();
        CollisionData[] maps = deck.getCollisionMaps();
        int sizeX = deck.getSizeX();
        int sizeY = deck.getSizeY();
        int[] pivot = pivots.computeIfAbsent(entity.getConfig().getId(), type -> pivot(entity));
        if (maps == null || maps[deck.getPlane()] == null)
        {
            return BoatHull.of(sizeX, sizeY, pivot[0], pivot[1], (x, y) -> false);
        }
        int[][] flags = maps[deck.getPlane()].getFlags();
        return BoatHull.of(sizeX, sizeY, pivot[0], pivot[1], (x, y) -> (flags[x + 1][y + 1] & BLOCKED) != 0 && flags[x + 1][y + 1] != 0xFFFFFF);
    }

    /**
     * Finds the deck point the boat turns about, worked back from where the deck's centre lands on the main world
     * @param entity the boat's world entity
     * @return the pivot as deck x and y
     */
    private static int[] pivot(WorldEntity entity)
    {
        WorldView deck = entity.getWorldView();
        int centreX = deck.getSizeX() * 64;
        int centreY = deck.getSizeY() * 64;
        LocalPoint main = entity.transformToMainWorld(new LocalPoint(centreX, centreY, deck));
        LocalPoint at = entity.getLocalLocation();
        double angle = entity.getOrientation() * (2 * Math.PI / Compass.TURN);
        double dx = main.getX() - at.getX();
        double dy = main.getY() - at.getY();
        double offsetX = dx * Math.cos(angle) - dy * Math.sin(angle);
        double offsetY = dx * Math.sin(angle) + dy * Math.cos(angle);
        return new int[]{(int) Math.round(centreX - offsetX), (int) Math.round(centreY - offsetY)};
    }

    private static int transformed(int id)
    {
        ObjectComposition composition = client().getObjectDefinition(id);
        if (composition != null && composition.getImpostorIds() != null)
        {
            ObjectComposition impostor = composition.getImpostor();
            if (impostor != null)
            {
                return impostor.getId();
            }
        }
        return id;
    }
}
