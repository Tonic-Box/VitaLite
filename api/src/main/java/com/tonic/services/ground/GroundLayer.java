package com.tonic.services.ground;

import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One source's ground triangles, held per world view, kept until the source paints again.
 */
final class GroundLayer
{
    private final Map<Integer, GroundTriangles> views = new LinkedHashMap<>();

    GroundTriangles view(int worldViewId)
    {
        return views.computeIfAbsent(worldViewId, GroundTriangles::new);
    }

    Collection<GroundTriangles> views()
    {
        return views.values();
    }

    void clear()
    {
        for (Iterator<GroundTriangles> it = views.values().iterator(); it.hasNext(); )
        {
            GroundTriangles triangles = it.next();
            if (triangles.count() == 0)
            {
                it.remove();
            }
            else
            {
                triangles.clear();
            }
        }
    }

    boolean isEmpty()
    {
        for (GroundTriangles triangles : views.values())
        {
            if (triangles.count() > 0)
            {
                return false;
            }
        }
        return true;
    }
}
