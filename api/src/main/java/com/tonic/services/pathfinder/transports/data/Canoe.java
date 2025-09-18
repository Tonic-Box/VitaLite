package com.tonic.services.pathfinder.transports.data;

import com.tonic.data.ItemConstants;
import com.tonic.services.pathfinder.requirements.ItemRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.RequirementsBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Canoe
{
    LOG(1, 12, 27262996),
    DUGOUT(2, 27, 27262994),
    STABLE_DUGOUT(3, 42, 27262988),
    //WAKA(Integer.MAX_VALUE, 57, 27262990),
    ;

    private final int distance;
    private final int level;
    private final int widgetId;
    private final Requirements requirements = RequirementsBuilder.get()
            .addRequirement(new ItemRequirement(null, 1, ItemConstants.AXES))
            .build();
}