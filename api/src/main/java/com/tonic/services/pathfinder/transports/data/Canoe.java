package com.tonic.services.pathfinder.transports.data;

import com.tonic.data.ItemConstants;
import com.tonic.services.pathfinder.requirements.ItemRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.RequirementsBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.InterfaceID;

@RequiredArgsConstructor
@Getter
public enum Canoe
{
    LOG(1, 12, InterfaceID.Canoeing.LOG),
    DUGOUT(2, 27, InterfaceID.Canoeing.DUGOUT),
    STABLE_DUGOUT(3, 42, InterfaceID.Canoeing.STABLE_DUGOUT),
    ;

    private final int distance;
    private final int level;
    private final int widgetId;
    private final Requirements requirements = RequirementsBuilder.get()
            .addRequirement(new ItemRequirement(null, 1, ItemConstants.AXES))
            .build();
}