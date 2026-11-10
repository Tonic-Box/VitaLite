package com.tonic.services.pathfinder.sailing;

import com.tonic.services.pathfinder.sailing.sea.Rapids;
import com.tonic.services.pathfinder.sailing.sea.SeaMap;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The rapids in view on the open sea, split by what the boat's helm takes
 */
final class SeaRapids
{
    private static final int GENTLE = 1;
    private static final int STRONG = 2;
    private static final int POWERFUL = 3;
    private static final int DEADLY = 4;
    private static final Map<Integer, Integer> TIERS = Map.of(ObjectID.SAILING_RAPIDS, GENTLE, ObjectID.SAILING_RAPIDS_STRONG, STRONG, ObjectID.SAILING_RAPIDS_POWERFUL, POWERFUL, ObjectID.SAILING_RAPIDS_DEADLY, DEADLY);

    private static final String FIELD_SEPARATOR = "\t";
    private static final String COMMENT = "#";
    private static volatile List<Placed> mapped;

    private final Rapids rapids;
    private final Set<WorldPoint> harmful;

    /**
     * A patch of rapids where it lies
     */
    public static final class Placed
    {
        private final int x;
        private final int y;
        private final int sizeX;
        private final int sizeY;
        private final int tier;

        public Placed(int x, int y, int sizeX, int sizeY, int tier)
        {
            this.x = x;
            this.y = y;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.tier = tier;
        }

        public int x()
        {
            return x;
        }

        public int y()
        {
            return y;
        }

        public int sizeX()
        {
            return sizeX;
        }

        public int sizeY()
        {
            return sizeY;
        }

        public int tier()
        {
            return tier;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Placed))
            {
                return false;
            }
            Placed other = (Placed) o;
            return x == other.x && y == other.y && sizeX == other.sizeX && sizeY == other.sizeY && tier == other.tier;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(x, y, sizeX, sizeY, tier);
        }

        @Override
        public String toString()
        {
            return "Placed[" + "x=" + x + ", " + "y=" + y + ", " + "sizeX=" + sizeX + ", " + "sizeY=" + sizeY + ", " + "tier=" + tier + "]";
        }
    }

    private SeaRapids(Rapids rapids, Set<WorldPoint> harmful)
    {
        this.rapids = rapids;
        this.harmful = harmful;
    }

    /**
     * Gets the rapids in view now
     * @param world the game state to read
     * @param rapidResistance the highest rapids tier the boat's helm takes
     * @return the rapids in view, split into boosting and harmful
     */
    public static SeaRapids inView(SeaWorld world, int rapidResistance)
    {
        Set<Placed> placed = new LinkedHashSet<>(mapped(world.revision()));
        for (SeaWorld.Loc loc : world.locs(id -> tierOf(id) != 0))
        {
            placed.add(new Placed(loc.corner().getX(), loc.corner().getY(), loc.sizeX(), loc.sizeY(), tierOf(loc.id())));
        }
        List<Rapids.Patch> boosting = new ArrayList<>();
        Set<WorldPoint> harmful = new HashSet<>();
        for (Placed patch : placed)
        {
            if (patch.tier() < DEADLY && patch.tier() <= rapidResistance)
            {
                boosting.add(new Rapids.Patch(patch.x(), patch.y(), patch.sizeX(), patch.sizeY(), patch.tier()));
                continue;
            }
            for (int dx = 0; dx < patch.sizeX(); dx++)
            {
                for (int dy = 0; dy < patch.sizeY(); dy++)
                {
                    harmful.add(new WorldPoint(patch.x() + dx, patch.y() + dy, 0));
                }
            }
        }
        return new SeaRapids(boosting.isEmpty() ? Rapids.NONE : new Rapids(boosting), harmful);
    }

    /**
     * Gets a loc's rapids tier
     * @param locId the loc id
     * @return 1 gentle, 2 strong, 3 powerful, 4 deadly, or 0 when the loc is not rapids
     */
    public static int tierOf(int locId)
    {
        return TIERS.getOrDefault(locId, 0);
    }

    /**
     * Gets the rapids bundled for a revision or the newest one before it
     * @param revision the game revision
     * @return the bundled rapids, empty when there are none
     */
    public static List<Placed> mapped(int revision)
    {
        List<Placed> known = mapped;
        if (known == null)
        {
            known = read(SeaMap.available(revision));
            mapped = known;
        }
        return known;
    }

    private static List<Placed> read(int revision)
    {
        String resource = "/com/tonic/services/pathfinder/rapids-" + revision + ".tsv";
        InputStream stream = revision < 0 ? null : SeaRapids.class.getResourceAsStream(resource);
        if (stream == null)
        {
            return List.of();
        }
        List<Placed> placed = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.isBlank() || line.startsWith(COMMENT))
                {
                    continue;
                }
                String[] fields = line.split(FIELD_SEPARATOR);
                placed.add(new Placed(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), Integer.parseInt(fields[3]), Integer.parseInt(fields[4])));
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(resource + " could not be read", e);
        }
        return List.copyOf(placed);
    }

    /**
     * Gets the rapids the helm takes, which boost the boat
     * @return the boosting rapids
     */
    public Rapids rapids()
    {
        return rapids;
    }

    /**
     * Gets the tiles of the rapids the helm does not take, which harm the boat
     * @return the harmful tiles
     */
    public Set<WorldPoint> harmful()
    {
        return harmful;
    }
}
