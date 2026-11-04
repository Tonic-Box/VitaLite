package com.tonic.mixins;

import com.tonic.api.TAngle;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;

@Mixin("Angle")
public abstract class TAngleMixin implements TAngle
{
    @Shadow("units")
    public int units;

    @Inject
    @Override
    public void setUnits(int units)
    {
        this.units = units & 16383;
    }
}
