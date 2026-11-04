package com.tonic.mixins;

import com.tonic.api.TModel;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;

@Mixin("Model")
public abstract class TModelMixin implements TModel
{
    @Shadow("vertexLabels")
    public int[][] vertexLabels;

    @Shadow("faceTransparencies")
    public byte[] faceTransparencies;

    @Inject
    @Override
    public int[][] getVertexGroups()
    {
        return vertexLabels;
    }

    @Inject
    @Override
    public void setFaceTransparencies(byte[] transparencies)
    {
        faceTransparencies = transparencies;
    }
}
