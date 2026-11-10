package com.tonic.data.wrappers;

import com.tonic.Static;
import com.tonic.api.TObjectComposition;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.api.game.reach.Reachability;
import com.tonic.api.game.reach.ReachRule;
import com.tonic.api.game.reach.SceneCollision;
import com.tonic.data.wrappers.abstractions.Entity;
import com.tonic.services.GameManager;
import com.tonic.util.Distance;
import com.tonic.util.TextUtil;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import java.awt.*;
import java.util.LinkedHashSet;
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

    public int getAnimation()
    {
        return Static.invoke(() -> {
            if(tileObject instanceof GameObject) {
                GameObject gameObject = (GameObject) tileObject;
                Animation animation = getAnimationFromRenderable(gameObject.getRenderable());
                if(animation != null) {
                    return animation.getId();
                }
            } else if (tileObject instanceof WallObject) {
                WallObject wallObject = (WallObject) tileObject;
                Animation animation1 = getAnimationFromRenderable(wallObject.getRenderable1());
                if(animation1 != null) {
                    return animation1.getId();
                }
                Animation animation2 = getAnimationFromRenderable(wallObject.getRenderable2());
                if(animation2 != null) {
                    return animation2.getId();
                }
            } else if (tileObject instanceof DecorativeObject) {
                DecorativeObject decorativeObject = (DecorativeObject) tileObject;
                Animation animation = getAnimationFromRenderable(decorativeObject.getRenderable());
                if(animation != null) {
                    return animation.getId();
                }
            } else if (tileObject instanceof GroundObject) {
                GroundObject groundObject = (GroundObject) tileObject;
                Animation animation = getAnimationFromRenderable(groundObject.getRenderable());
                if(animation != null) {
                    return animation.getId();
                }
            }
            return -1;
        });
    }

    private static Animation getAnimationFromRenderable(Renderable renderable)
    {
        if(renderable instanceof DynamicObject)
        {
            return ((DynamicObject) renderable).getAnimation();
        }
        return null;
    }

    /**
     * Checks if the specified action index is visible for this object.
     *
     * @param op The action index to check (0-based).
     * @return True if the action is visible, false otherwise.
     */
    public boolean isOpVisible(int op)
    {
        return Static.invoke(() -> {
            try
            {
                return tileObject.isOpShown(op);
            }
            catch (Exception e)
            {
                return false;
            }
        });
    }

    public boolean isActionVisible(String action) {
        int index = getActionIndex(action);
        if(index == -1)
            return false;
        return isOpVisible(index);
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
            if (wv == null || p == null) {
                return wp;
            }
            wp = WorldPoint.fromScene(wv, p.getX(), p.getY(), tileObject.getPlane());
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
            if(min != null && max != null) {
                width = max.getX() - min.getX() + 1;
                height = max.getY() - min.getY() + 1;
            }
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

    /**
     * Whether the local player can walk now to a tile this object can be interacted with from
     * @return false when the object lies in another world view than the player
     */
    public boolean isReachable()
    {
        if(getWorldViewId() != Reachability.viewId())
            return false;

        for(WorldPoint wp : interactableFrom())
        {
            if(GameManager.isReachable(wp))
                return true;
        }
        return false;
    }

    /**
     * Gets the nearest tile to the local player this object can be interacted with from, reachable or not
     * @return the tile, or null when there is none
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

    /**
     * Gets the nearest tile to a point this object can be interacted with from, reachable or not
     * @param to the point measured from
     * @return the tile, or null when there is none
     */
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
     * Gets the tiles this object can be interacted with from that can be stood on, reachable or not
     * @return the tiles in the object's world view, west to east then south to north, empty when its scene is unloaded
     */
    public Set<WorldPoint> interactableFrom()
    {
        return Static.invoke(() -> {
            WorldView view = tileObject.getWorldView();
            SceneCollision map = SceneCollision.of(view, tileObject.getPlane());
            ReachRule.Target target = target();
            if(map == null || target == null)
                return new LinkedHashSet<WorldPoint>();
            return new LinkedHashSet<>(ReachRule.tiles(map, target));
        });
    }

    /**
     * Whether this object can be interacted with from a tile
     * @param point the tile stood on, in the object's world view
     * @return whether the server accepts the interaction from there
     */
    public boolean reachedFrom(WorldPoint point)
    {
        if(point == null)
            return false;

        return Static.invoke(() -> {
            WorldView view = tileObject.getWorldView();
            SceneCollision map = SceneCollision.of(view, tileObject.getPlane());
            ReachRule.Target target = target();
            if(map == null || target == null || point.getPlane() != tileObject.getPlane())
                return false;
            return ReachRule.reached(map, map.sceneX(point), map.sceneY(point), target);
        });
    }

    private ReachRule.Target target()
    {
        int x;
        int y;
        int sizeX = 1;
        int sizeY = 1;
        if(tileObject instanceof GameObject)
        {
            GameObject go = (GameObject) tileObject;
            Point min = go.getSceneMinLocation();
            Point max = go.getSceneMaxLocation();
            if(min == null || max == null)
                return null;
            x = min.getX();
            y = min.getY();
            sizeX = max.getX() - min.getX() + 1;
            sizeY = max.getY() - min.getY() + 1;
        }
        else
        {
            LocalPoint local = tileObject.getLocalLocation();
            x = local.getSceneX();
            y = local.getSceneY();
        }

        Client client = Static.getClient();
        ObjectComposition composition = client.getObjectDefinition(getId());
        int access = composition == null ? 0 : ((TObjectComposition) composition).getBlockAccessFlags();
        return new ReachRule.Target(x, y, getShapeFlag(), getOrientation(), sizeX, sizeY, access);
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
