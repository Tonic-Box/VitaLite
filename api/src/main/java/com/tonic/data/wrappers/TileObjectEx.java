package com.tonic.data.wrappers;

import com.tonic.Static;
import com.tonic.api.TObjectComposition;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.data.ObjectBlockAccessFlags;
import com.tonic.data.Walls;
import com.tonic.data.wrappers.abstractions.Entity;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.local.LocalCollisionMap;
import com.tonic.util.Distance;
import com.tonic.util.TextUtil;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class TileObjectEx implements Entity
{
    public static TileObjectEx of(TileObject object)
    {
        if(object == null)
            return null;
        return new TileObjectEx(object);
    }

    @Getter
    private final TileObject tileObject;
    private String[] actions;

    public TileObjectEx(TileObject tileObject)
    {
        this.tileObject = tileObject;
    }

    @Override
    public int getId() {
        return tileObject.getId();
    }

    @Override
    public String getName() {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            ObjectComposition composition = client.getObjectDefinition(tileObject.getId());
            if(composition.getImpostorIds() != null)
            {
                composition = composition.getImpostor();
            }
            if(composition == null)
                return null;
            return TextUtil.sanitize(composition.getName());
        });
    }

    public boolean hasAction(String action) {
        return getActionIndex(action) != -1;
    }

    @Override
    public void interact(String... actions) {
        TileObjectAPI.interact(this, actions);
    }

    @Override
    public void interact(int action) {
        TileObjectAPI.interact(this, action);
    }

    @Override
    public String[] getActions() {
        if(actions == null)
        {
            Client client = Static.getClient();
            actions = Static.invoke(() -> {
                ObjectComposition composition = client.getObjectDefinition(tileObject.getId());
                if(composition.getImpostorIds() != null)
                {
                    composition = composition.getImpostor();
                }
                if(composition == null)
                    return new String[]{};
                return composition.getActions();
            });
        }
        return actions;
    }

    public int getActionIndex(String action) {
        String[] actions = getActions();
        for(int i = 0; i < actions.length; i++)
        {
            if(actions[i] == null)
                continue;
            if(!actions[i].toLowerCase().contains(action.toLowerCase()))
                continue;
            return i;
        }
        return -1;
    }

    @Override
    public WorldPoint getWorldPoint() {
        WorldPoint wp = tileObject.getWorldLocation();
        if(tileObject instanceof GameObject)
        {
            WorldView wv = getWorldView();
            GameObject go = (GameObject) tileObject;
            Point p = go.getSceneMinLocation();
            wp = WorldPoint.fromScene(wv, p.getX(), p.getY(), wv.getPlane());
        }
        return wp;
    }

    @Override
    public WorldArea getWorldArea()
    {
        int width = 1;
        int height = 1;
        if(tileObject instanceof GameObject) {
            GameObject go = (GameObject) tileObject;
            Point min = go.getSceneMinLocation();
            Point max = go.getSceneMaxLocation();
            width = max.getX() - min.getX() + 1;
            height = max.getY() - min.getY() + 1;
        }
        return new WorldArea(getWorldPoint(), width, height);
    }

    @Override
    public LocalPoint getLocalPoint() {
        return tileObject.getLocalLocation();
    }

    @Override
    public Tile getTile()
    {
        return SceneAPI.getTile(getWorldPoint());
    }

    public Shape getShape()
    {
        if(tileObject instanceof GameObject) {
            GameObject go = (GameObject) tileObject;
            return go.getConvexHull();
        }
        else if(tileObject instanceof WallObject) {
            WallObject wo = (WallObject) tileObject;
            return wo.getConvexHull();
        }
        else if(tileObject instanceof DecorativeObject) {
            DecorativeObject deco = (DecorativeObject) tileObject;
            return deco.getConvexHull();
        }
        else if(tileObject instanceof GroundObject) {
            GroundObject ground = (GroundObject) tileObject;
            return ground.getConvexHull();
        }
        return tileObject.getClickbox();
    }

    public ObjectComposition getObjectComposition()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            ObjectComposition composition = client.getObjectDefinition(getId());
            if(composition.getImpostorIds() != null)
            {
                composition = composition.getImpostor();
            }
            return composition;
        });
    }

    public boolean isReachable()
    {
        for(WorldPoint wp : interactableFrom())
        {
            if(GameManager.isReachable(wp))
                return true;
        }
        return false;
    }

    /**
     * Gets the closest WorldPoint from which this object can be interacted with.
     * @return The closest WorldPoint from which the object can be interacted with.
     */
    public WorldPoint getInteractionPoint()
    {
        WorldPoint player = PlayerEx.getLocal().getWorldPoint();
        return getInteractionPoint(player);
    }

    @Override
    public WorldView getWorldView() {
        return Static.invoke(tileObject::getWorldView);
    }

    @Override
    public int getWorldViewId()
    {
        WorldView worldView = getWorldView();
        if(worldView == null)
            return -1;
        return worldView.getId();
    }

    public WorldPoint getInteractionPoint(WorldPoint to)
    {
        WorldPoint closest = null;
        int closestDist = Integer.MAX_VALUE;
        for(WorldPoint wp : interactableFrom())
        {
            int dist = Distance.chebyshev(to, wp);
            if(dist < closestDist)
            {
                closestDist = dist;
                closest = wp;
            }
        }
        return closest;
    }

    /**
     * Gets all WorldPoints from which this object can be interacted with.
     * Takes into account walls that may block access. Does not account for reachability
     * Only works for GameObjects (type 2).
     *
     * @return A set of WorldPoints from which the object can be interacted with.
     */
    public Set<WorldPoint> interactableFrom() {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            ObjectComposition composition = client.getObjectDefinition(getId());
            TObjectComposition tComp = (TObjectComposition) composition;

            int modelRotation = getOrientation();
            int type = getConfig() & 0x1F;

            int rotation = modelRotation;
            if (type == 2 || type == 6 || type == 8) {
                rotation -= 4;
            } else if (type == 7) {
                rotation = (rotation - 2) & 0x3;
            }
            rotation = rotation & 0x3;

            // Get object dimensions
            WorldArea area = getWorldArea();
            int width = area.getWidth();
            int height = area.getHeight();

            WorldPoint objPos = getWorldPoint();
            int rotatedFlags = tComp.rotateBlockAccessFlags(rotation);
            Set<WorldPoint> accessibleFrom = new HashSet<>();

            if(tileObject instanceof DecorativeObject)
            {
                accessibleFrom.add(objPos);
                return accessibleFrom;
            }

            if ((rotatedFlags & ObjectBlockAccessFlags.BLOCK_NORTH) == 0) {
                int y = objPos.getY() + height;
                int plane = objPos.getPlane();
                for (int x = objPos.getX(); x < objPos.getX() + width; x++) {
                    if(Walls.of(x, y, objPos.getPlane()).hasSouthWall())
                        continue;
                    if(!LocalCollisionMap.canStep(x, y, plane))
                        continue;
                    accessibleFrom.add(new WorldPoint(x, y, plane));
                }
            }

            if ((rotatedFlags & ObjectBlockAccessFlags.BLOCK_EAST) == 0) {
                int x = objPos.getX() + width;
                int plane = objPos.getPlane();
                for (int y = objPos.getY(); y < objPos.getY() + height; y++) {
                    if(Walls.of(x, y, objPos.getPlane()).hasWestWall())
                        continue;
                    if(!LocalCollisionMap.canStep(x, y, plane))
                        continue;
                    accessibleFrom.add(new WorldPoint(x, y, objPos.getPlane()));
                }
            }

            if ((rotatedFlags & ObjectBlockAccessFlags.BLOCK_SOUTH) == 0) {
                int y = objPos.getY() - 1;
                int plane = objPos.getPlane();
                for (int x = objPos.getX(); x < objPos.getX() + width; x++) {
                    if(Walls.of(x, y, objPos.getPlane()).hasNorthWall())
                        continue;
                    if(!LocalCollisionMap.canStep(x, y, plane))
                        continue;
                    accessibleFrom.add(new WorldPoint(x, y, objPos.getPlane()));
                }
            }

            if ((rotatedFlags & ObjectBlockAccessFlags.BLOCK_WEST) == 0) {
                int x = objPos.getX() - 1;
                int plane = objPos.getPlane();
                for (int y = objPos.getY(); y < objPos.getY() + height; y++) {
                    if(Walls.of(x, y, objPos.getPlane()).hasEastWall())
                        continue;
                    if(!LocalCollisionMap.canStep(x, y, plane))
                        continue;
                    accessibleFrom.add(new WorldPoint(x, y, objPos.getPlane()));
                }
            }

            return accessibleFrom;
        });
    }

    public int getConfig()
    {
        if (tileObject instanceof GameObject) {
            GameObject gameObject = (GameObject) tileObject;
            return gameObject.getConfig();
        } else if (tileObject instanceof WallObject) {
            WallObject wallObject = (WallObject) tileObject;
            return wallObject.getConfig();
        } else if (tileObject instanceof DecorativeObject) {
            DecorativeObject decorativeObject = (DecorativeObject) tileObject;
            return decorativeObject.getConfig();
        } else if (tileObject instanceof GroundObject) {
            GroundObject groundObject = (GroundObject) tileObject;
            return groundObject.getConfig();
        }
        return -1;
    }

    public int getOrientation()
    {
        return  (getConfig() >>> 6) & 3;
    }

    public int getShapeFlag()
    {
        return  getConfig() & 31;
    }
    public int getType() {
        return (int) (tileObject.getHash() >>> 16 & 0x7L);
    }
}
