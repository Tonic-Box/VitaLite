package com.tonic.services.pathfinder.transports;

import com.tonic.Static;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Which transports a search may take, worked out once per search: an edge is blocked when its requirements do not hold, or when it is a teleport-like network and teleports are not to be used.
 */
public final class TransportAvailability
{
    /**
     * Every transport allowed.
     */
    public static final TransportAvailability ALL = new TransportAvailability(Collections.emptySet());

    private final Set<Transport> blocked;

    private TransportAvailability(Set<Transport> blocked)
    {
        this.blocked = blocked;
    }

    /**
     * Evaluates every transport with requirements in one pass on the client thread.
     *
     * @param transports the transports
     * @param useTeleports whether teleport-like networks may be taken
     * @return the availability
     */
    public static TransportAvailability of(Collection<? extends Collection<Transport>> transports, boolean useTeleports)
    {
        Set<Transport> blocked = Static.invoke(() -> evaluate(transports, useTeleports));
        return blocked.isEmpty() ? ALL : new TransportAvailability(blocked);
    }

    /**
     * Works out which transports are blocked. Call it on the client thread.
     *
     * @param transports the transports
     * @param useTeleports whether teleport-like networks may be taken
     * @return the blocked ones, by identity
     */
    public static Set<Transport> evaluate(Collection<? extends Collection<Transport>> transports, boolean useTeleports)
    {
        Set<Transport> blocked = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Collection<Transport> list : new ArrayList<>(transports))
        {
            for (Transport transport : list)
            {
                if (!useTeleports && !transport.allowedWithoutTeleports())
                {
                    blocked.add(transport);
                }
                else if (transport.hasRequirements() && !transport.getRequirements().fulfilled())
                {
                    blocked.add(transport);
                }
            }
        }
        return blocked;
    }

    /**
     * @param transports transports to block as well
     * @return this availability with them blocked too
     */
    public TransportAvailability without(Collection<Transport> transports)
    {
        if (transports.isEmpty())
        {
            return this;
        }
        Set<Transport> more = Collections.newSetFromMap(new IdentityHashMap<>());
        more.addAll(blocked);
        more.addAll(transports);
        return new TransportAvailability(more);
    }

    /**
     * @param transport a transport
     * @return whether the search may take it
     */
    public boolean allows(Transport transport)
    {
        return !blocked.contains(transport);
    }
}
