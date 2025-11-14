package com.tonic.data.wrappers;

import com.tonic.Static;
import com.tonic.api.game.CombatAPI;
import com.tonic.data.wrappers.abstractions.Entity;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.PlayerQuery;
import com.tonic.util.TextUtil;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.ArrayUtils;
import java.awt.*;
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
        Actor interacting = actor.getInteracting();
        if(interacting == null)
            return null;

        if(interacting instanceof Player)
            return new PlayerEx((Player) interacting);

        return new NpcEx((NPC) interacting);
    }

    @Override
    public String getName()
    {
        return TextUtil.sanitize(Static.invoke(() -> {
            if(this instanceof PlayerEx)
                return actor.getName();

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
        return actor.isDead();
    }

    public int getCombatLevel()
    {
        return actor.getCombatLevel();
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
                    .keepIf(n -> n.getInteracting() != null && n.equals(this))
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

            if(healthBarVisible())
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

    @Override
    public boolean equals(Object obj)
    {
        if(!(obj instanceof ActorEx))
            return false;

        ActorEx<?> actorEx = (ActorEx<?>) obj;
        return actor == actorEx.getActor();
    }
}
