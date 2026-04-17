package com.tonic.mixins;

import com.tonic.api.TItemComposition;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import com.tonic.util.TextUtil;
import net.runelite.api.EntityOps;

@Mixin("ItemComposition")
public abstract class TItemCompositionMixin implements TItemComposition
{
    @Shadow("groundActions")
    public Object groundActions;

    public String[] getGroundActions()
    {
        if (groundActions == null)
            return new String[0];

        EntityOps ops = (EntityOps) groundActions;
        String[] result = new String[EntityOps.MAX_OPS];
        for (int i = 0; i < EntityOps.MAX_OPS; i++)
        {
            String op = ops.getOp(i);
            if (op != null)
            {
                result[i] = TextUtil.sanitize(op);
            }
        }
        return result;
    }
}
