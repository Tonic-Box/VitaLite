package com.tonic.mixins;

import com.tonic.api.TRasterState;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;

@Mixin("RasterState")
public abstract class TRasterStateMixin implements TRasterState
{
    @Shadow("zoom")
    public int zoom;

    @Inject
    @Override
    public int getZoom()
    {
        return zoom;
    }

    @Inject
    @Override
    public void setZoom(int zoom)
    {
        this.zoom = zoom;
    }
}
