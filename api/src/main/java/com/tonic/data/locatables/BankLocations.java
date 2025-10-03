package com.tonic.data.locatables;

import com.tonic.Static;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.QuestAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.BankAPI;
import com.tonic.data.TileObjectEx;
import com.tonic.services.pathfinder.Walker;
import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A list of known bank locations in the game.
 */
public enum BankLocations {
    LUMBRIDGE_BANK(new WorldArea(3207, 3215, 4, 8, 2)),
    VARROCK_WEST_BANK(new WorldArea(3180, 3433, 6, 15, 0)),
    VARROCK_EAST_BANK(new WorldArea(3250, 3419, 8, 6, 0)),
    GRAND_EXCHANGE_BANK(new WorldArea(3154, 3480, 22, 22, 0)),
    EDGEVILLE_BANK(new WorldArea(3091, 3488, 8, 12, 0)),
    FALADOR_EAST_BANK(new WorldArea(3009, 3355, 13, 4, 0)),
    FALADOR_WEST_BANK(new WorldArea(2943, 3368, 5, 6, 0)),
    DRAYNOR_BANK(new WorldArea(3092, 3240, 6, 7, 0)),
    DUEL_ARENA_BANK(new WorldArea(3380, 3267, 5, 7, 0)),
    SHANTAY_PASS_BANK(new WorldArea(3299, 3118, 11, 10, 0)),
    AL_KHARID_BANK(new WorldArea(3269, 3161, 4, 13, 0)),
    CATHERBY_BANK(new WorldArea(2806, 3438, 7, 4, 0)),
    SEERS_VILLAGE_BANK(new WorldArea(2721, 3487, 10, 7, 0)),
    ARDOUGNE_NORTH_BANK(new WorldArea(2612, 3330, 10, 6, 0)),
    ARDOUGNE_SOUTH_BANK(new WorldArea(2649, 3280, 7, 8, 0)),
    PORT_KHAZARD_BANK(new WorldArea(2658, 3156, 7, 9, 0)),
    YANILLE_BANK(new WorldArea(2609, 3088, 6, 10, 0)),
    CORSAIR_COVE_BANK(new WorldArea(2567, 2862, 7, 7, 0)),
    CASTLE_WARS_BANK(new WorldArea(2435, 3081, 12, 18, 0)),
    LLETYA_BANK(new WorldArea(2349, 3160, 8, 7, 0)),
    GRAND_TREE_WEST_BANK(new WorldArea(2436, 3484, 9, 8, 1)),
    GRAND_TREE_SOUTH_BANK(new WorldArea(2448, 3476, 8, 8, 1)),
    TREE_GNOME_STRONGHOLD_BANK(new WorldArea(2441, 3414, 11, 23, 1)),
    SHILO_VILLAGE_BANK(new WorldArea(2842, 2951, 20, 8, 0)),
    NEITIZNOT_BANK(new WorldArea(2334, 3805, 6, 2, 0)),
    JATIZSO_BANK(new WorldArea(2413, 3798, 7, 7, 0)),
    BARBARIAN_OUTPOST_BANK(new WorldArea(2532, 3570, 6, 10, 0)),
    //	ETCETERIA_BANK(new WorldArea(2618, 3893, 4, 4, 0)), has quest requirements
    DARKMEYER_BANK(new WorldArea(3601, 3365, 9, 3, 0)),
    CHARCOAL_BURNERS_BANK(new WorldArea(1711, 3460, 14, 10, 0)),
    HOSIDIUS_BANK(new WorldArea(1748, 3594, 5, 8, 0)),
    PORT_PISCARILIUS_BANK(new WorldArea(1794, 3784, 18, 7, 0)),
    //	HALLOWED_SEPULCHRE_BANK(new WorldArea(2393, 5975, 15, 15, 0)), has quest requirements
    CANIFIS_BANK(new WorldArea(3508, 3474, 6, 10, 0), () ->
    {
        QuestState state = QuestAPI.getState(Quest.PRIEST_IN_PERIL);
        return state != null && state.equals(QuestState.FINISHED);
    }),
    //	MOTHERLODE_MINE_BANK(new WorldArea(3754, 5664, 4, 3, 0)), has pickaxe requirement
    BURGH_DE_ROTT_BANK(new WorldArea(3492, 3208, 10, 6, 0), () ->
    {
        QuestState state = QuestAPI.getState(Quest.PRIEST_IN_PERIL);
        return state != null && state.equals(QuestState.FINISHED);
    }),
    VER_SINHAZA_BANK(new WorldArea(3646, 3204, 10, 13, 0)),
    FEROX_ENCLAVE_BANK(new WorldArea(3127, 3627, 10, 6, 0)),
    BURTHORPE_BANK(new WorldArea(3037, 4961, 13, 17, 1)),
    ZANERIS_BANK(new WorldArea(2379, 4453, 6, 7, 0), () -> {
        Client client = Static.getClient();
        QuestState state = QuestAPI.getState(Quest.CHILDREN_OF_THE_SUN);
        return state != null && state.equals(QuestState.FINISHED);
    }),

    CIVITAS_ILLA_FORTIS(new WorldArea(1777, 3093, 9, 11, 0), () -> {
        Client client = Static.getClient();
        QuestState state = QuestAPI.getState(Quest.LOST_CITY);
        return state != null && state.equals(QuestState.FINISHED);
    }),

    ;

    @Getter
    private final WorldArea area;
    private final Supplier<Boolean> condition;

    BankLocations(WorldArea area)
    {
        this.area = area;
        this.condition = null;
    }

    BankLocations(WorldArea area, Supplier<Boolean> condition)
    {
        this.area = area;
        this.condition = condition;
    }

    public boolean test()
    {
        return condition == null || condition.get();
    }

    public boolean containsTile(int tileIndex)
    {
        int[] tiles = WorldPointUtil.toCompressedPoints(getArea());
        return ArrayUtils.contains(tiles, tileIndex);
    }

    public static boolean isBankTile(int tileIndex)
    {
        for(BankLocations location : values())
        {
            if(location.containsTile(tileIndex))
            {
                return true;
            }
        }
        return false;
    }

    public static BankLocations getNearest(WorldPoint source)
    {
        return Arrays.stream(values())
                .filter(BankLocations::test)
                .min(Comparator.comparingInt(x -> x.getArea().distanceTo2D(source)))
                .orElse(null);
    }

    public static void walkToNearest()
    {
        List<WorldArea> areas = Arrays.stream(BankLocations.values()).filter(BankLocations::test).map(BankLocations::getArea).collect(Collectors.toList());
        Walker.walkTo(areas, true);
    }

    public static void walkToAndUseNearestBank()
    {
        if(BankAPI.isOpen())
        {
            return;
        }

        Client client = Static.getClient();
        WorldPoint start = Static.invoke(() ->
        {
            Player localPlayer = client.getLocalPlayer();
            return localPlayer != null ? localPlayer.getWorldLocation() : null;
        });

        if(start == null)
        {
            return;
        }

        BankLocations nearest = getNearest(start);
        if(nearest == null)
        {
            return;
        }

        WorldPoint target = getNearestPointInArea(nearest.getArea(), start);

        if(start.distanceTo2D(target) > 5)
        {
            Walker.walkTo(Collections.singletonList(nearest.getArea()), true);

            boolean reached = Delays.waitUntil(() ->
            {
                if(BankAPI.isOpen())
                {
                    return true;
                }

                return Static.invoke(() ->
                {
                    Player localPlayer = client.getLocalPlayer();
                    if(localPlayer == null)
                    {
                        return false;
                    }
                    return localPlayer.getWorldLocation().distanceTo2D(target) <= 5;
                });
            }, 80);

            if(BankAPI.isOpen())
            {
                return;
            }

            if(!reached)
            {
                return;
            }
        }

        if(MovementAPI.isMoving())
        {
            if(!Delays.waitUntil(() -> !MovementAPI.isMoving(), 20))
            {
                return;
            }
        }

        TileObjectEx bankObject = findNearestBankObject();
        if(bankObject == null)
        {
            return;
        }

        String interaction = determineBankAction(bankObject);
        if(interaction == null)
        {
            return;
        }

        TileObjectAPI.interact(bankObject, interaction);

        Delays.waitUntil(BankAPI::isOpen, 40);
    }

    private static TileObjectEx findNearestBankObject()
    {
        TileObjectEx bankObject = null;
        for(int attempt = 0; attempt < 5 && bankObject == null; attempt++)
        {
            bankObject = TileObjectAPI.get(BankLocations::isSupportedBankObject);
            if(bankObject == null)
            {
                Delays.wait(200);
            }
        }
        return bankObject;
    }

    private static boolean isSupportedBankObject(TileObjectEx object)
    {
        if(object == null || object.getName() == null)
        {
            return false;
        }

        String name = object.getName();
        if("Bank chest".equalsIgnoreCase(name))
        {
            return object.hasAction("Use");
        }

        if("Bank booth".equalsIgnoreCase(name) || "Grand Exchange booth".equalsIgnoreCase(name))
        {
            return object.hasAction("Bank");
        }

        return false;
    }

    private static String determineBankAction(TileObjectEx object)
    {
        if(object == null || object.getName() == null)
        {
            return null;
        }

        String name = object.getName();
        if("Bank chest".equalsIgnoreCase(name))
        {
            return "Use";
        }

        if("Bank booth".equalsIgnoreCase(name) || "Grand Exchange booth".equalsIgnoreCase(name))
        {
            return "Bank";
        }

        return null;
    }

    private static WorldPoint getNearestPointInArea(WorldArea area, WorldPoint reference)
    {
        if(area == null || reference == null)
        {
            return reference;
        }

        int x = Math.max(area.getX(), Math.min(reference.getX(), area.getX() + area.getWidth() - 1));
        int y = Math.max(area.getY(), Math.min(reference.getY(), area.getY() + area.getHeight() - 1));
        return new WorldPoint(x, y, area.getPlane());
    }
}
