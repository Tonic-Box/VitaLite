package com.tonic.data.wrappers;

import com.tonic.Static;
import com.tonic.api.game.CombatAPI;
import com.tonic.api.game.reach.Reachability;
import com.tonic.api.game.reach.ReachRule;
import com.tonic.api.game.reach.SceneCollision;
import com.tonic.data.Orientation;
import com.tonic.data.wrappers.abstractions.Entity;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.PlayerQuery;
import com.tonic.services.GameManager;
import com.tonic.util.Distance;
import com.tonic.util.TextUtil;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.ArrayUtils;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Getter
public abstract class ActorEx<T extends Actor> implements Entity
{
    public static ActorEx<?> fromActor(Actor actor)
    {
        if(actor instanceof NPC)
            return new NpcEx((NPC) actor);

        if(actor instanceof Player)
            return new PlayerEx((Player) actor);

        return null;
    }

    protected final T actor;

    public ActorEx(T actor)
    {
        this.actor = actor;
    }

    public ActorEx<?> getInteracting()
    {
        return Static.invoke(() -> {
            Actor interacting = actor.getInteracting();
            if(interacting == null)
                return null;

            if(interacting instanceof Player)
                return new PlayerEx((Player) interacting);

            return new NpcEx((NPC) interacting);
        });
    }

    @Override
    public String getName()
    {
        return TextUtil.sanitize(Static.invoke(() -> {
            if(this instanceof PlayerEx)
                return actor.getName();

            NpcEx npcEx = (NpcEx) this;
            NPCComposition composition = npcEx.getComposition();
            if(composition == null)
                return null;
            return ((NpcEx) this).getComposition().getName();
        }));
    }

    /**
     * check if the local player is idle
     * @return true if idle
     */
    public boolean isIdle()
    {
        return (actor.getIdlePoseAnimation() == actor.getPoseAnimation() && actor.getAnimation() == -1);
    }

    public boolean isDead()
    {
        return Static.invoke(actor::isDead);
    }

    public int getCombatLevel()
    {
        return Static.invoke(actor::getCombatLevel);
    }

    public int getIndex()
    {
        if(actor instanceof Player)
            return ((Player) actor).getId();
        return ((NPC) actor).getIndex();
    }

    public HeadIcon getHeadIcon()
    {
        if(actor instanceof NPC)
        {
            NPC npc = (NPC) actor;
            short[] spriteIds = npc.getOverheadSpriteIds();
            if (spriteIds == null)
            {
                return null;
            }

            return HeadIcon.values()[spriteIds[0]];
        }

        return Static.invoke(() -> ((PlayerEx) this).getActor().getOverheadIcon());
    }

    /**
     * find the actor currently in combat with the target actor
     * @return the actor, or null if none found
     */
    public ActorEx<?> getInCombatWith()
    {
        ActorEx<?> actor = new NpcQuery()
                .keepIf(n -> n.getInteracting() != null && n.getInteracting().equals(this))
                .keepIf(n -> !n.isIdle() || healthBarVisible())
                .nearest();

        if(actor == null)
        {
            actor = new PlayerQuery()
                    .keepIf(n -> n.getInteracting() != null && n.getInteracting().equals(this))
                    .keepIf(n -> !n.isIdle() || healthBarVisible())
                    .nearest();
        }
        return actor;
    }

    public boolean healthBarVisible()
    {
        return actor.getHealthRatio() != -1 && actor.getHealthScale() != -1;
    }

    /**
     * check if an actor can be attacked
     * @return true if can be attacked
     */
    public boolean canAttack()
    {
        if(isDead())
            return false;

        return Static.invoke(() ->
        {
            if(getActions() == null || !ArrayUtils.contains(getActions(), "Attack"))
                return false;

            if(getName() == null)
                return false;

            if(CombatAPI.inMultiWay())
            {
                return true;
            }

            if(!healthBarVisible())
                return true;

            Client client = Static.getClient();
            return actor.getInteracting() == null || actor.getInteracting().equals(client.getLocalPlayer());
        });
    }

    public boolean hasGraphic(int... graphicIds)
    {
        for(int graphicId : graphicIds)
        {
            if(actor.hasSpotAnim(graphicId))
                return true;
        }
        return false;
    }

    public int[] getGraphicIds()
    {
        return Static.invoke(() -> StreamSupport.stream(actor.getSpotAnims().spliterator(), false)
                .mapToInt(ActorSpotAnim::getId)
                .toArray());
    }

    public abstract WorldPoint getWorldPoint();

    public abstract WorldArea getWorldArea();

    public abstract LocalPoint getLocalPoint();

    public abstract Tile getTile();

    @Override
    public Shape getShape()
    {
        return Static.invoke(actor::getConvexHull);
    }

    public abstract void interact(String... actions);

    public abstract void interact(int action);

    public abstract String[] getActions();

    public WorldView getWorldView()
    {
        return Static.invoke(actor::getWorldView);
    }

    public int getWorldViewId()
    {
        WorldView worldView = getWorldView();
        if(worldView == null)
            return -1;
        return worldView.getId();
    }

    public Orientation getOrientation()
    {
        return Orientation.of(this);
    }

    /**
     * Gets the tiles this actor can be interacted with from: straight beside its footprint, or across a counter
     * @return the tiles in the actor's world view, reachable or not, empty when its scene is unloaded
     */
    public List<WorldPoint> interactableFrom()
    {
        return Static.invoke(() -> {
            int[] footprint = footprint();
            SceneCollision map = footprint == null ? null : SceneCollision.of(actor.getWorldView(), footprint[3]);
            if(map == null)
                return new ArrayList<>();
            return ReachRule.actorTiles(map, footprint[0], footprint[1], footprint[2]);
        });
    }

    /**
     * Whether this actor can be interacted with from a tile
     * @param point the tile stood on, in the actor's world view
     * @return whether the tile is one of its interaction tiles
     */
    public boolean reachedFrom(WorldPoint point)
    {
        return point != null && interactableFrom().contains(point);
    }

    /**
     * Gets the nearest tile to the local player this actor can be interacted with from, reachable or not
     * @return the tile, the actor's own tile for the local player, or null when there is none
     */
    @Override
    public WorldPoint getInteractionPoint()
    {
        Client client = Static.getClient();
        if(actor == client.getLocalPlayer())
            return getWorldPoint();
        return getInteractionPoint(PlayerEx.getLocal().getWorldPoint());
    }

    /**
     * Gets the nearest tile to a point this actor can be interacted with from, reachable or not
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
     * Whether the local player can walk now to a tile this actor can be interacted with from
     * @return false when the actor stands in another world view than the player
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

    private int[] footprint()
    {
        WorldView view = actor.getWorldView();
        LocalPoint local = actor.getLocalLocation();
        if(view == null || local == null)
            return null;

        int size = 1;
        if(actor instanceof NPC)
        {
            NPCComposition composition = ((NPC) actor).getTransformedComposition();
            if(composition == null)
                composition = ((NPC) actor).getComposition();
            if(composition != null)
                size = Math.max(1, composition.getSize());
        }

        int offset = (size - 1) * Perspective.LOCAL_HALF_TILE_SIZE;
        int x = (local.getX() - offset) >> Perspective.LOCAL_COORD_BITS;
        int y = (local.getY() - offset) >> Perspective.LOCAL_COORD_BITS;
        return new int[]{x, y, size, view.getPlane()};
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof Actor)
        {
            Actor other = (Actor) obj;
            return actor == other;
        }

        if(obj instanceof ActorEx)
        {
            ActorEx<?> actorEx = (ActorEx<?>) obj;
            return actor == actorEx.getActor();
        }

        return false;
    }
}
