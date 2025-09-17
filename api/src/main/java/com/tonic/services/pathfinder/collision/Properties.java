package com.tonic.services.pathfinder.collision;

import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;

public class Properties {
    @Getter
    private static final ArrayList<Integer> blacklist = new ArrayList<>();

    static
    {
        //lumby dining room
        //blacklist.add(52726919);
        //blacklist.add(52776076);
        //blacklist.add(52792460);
        //weird zeah bridge tile
        blacklist.add(60212953);

        //3295, 3430, 0
        blacklist.add(WorldPointUtil.compress(3295, 3430, 0));
        blacklist.add(WorldPointUtil.compress(3295, 3429, 0));
        blacklist.add(WorldPointUtil.compress(3295, 3428, 0));
        blacklist.add(WorldPointUtil.compress(3295, 3427, 0));

        //draynor
        blacklist.add(WorldPointUtil.compress(3070, 3260, 0));
        blacklist.add(WorldPointUtil.compress(3071, 3260, 0));
    }
}
